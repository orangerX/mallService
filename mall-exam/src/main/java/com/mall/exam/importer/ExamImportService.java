package com.mall.exam.importer;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ExamImportService {

    private static final Set<String> REUSABLE_COPYRIGHT = new HashSet<>(
            Arrays.asList("ORIGINAL", "AUTHORIZED", "PUBLIC_OFFICIAL"));
    private final QuestionSourceMapper sourceMapper;
    private final QuestionMapper questionMapper;
    private final ExamImportParser parser;
    private final AtomicLong previewIds = new AtomicLong(1);
    private final Map<Long, Batch> previews = new ConcurrentHashMap<>();

    @Autowired
    public ExamImportService(QuestionSourceMapper sourceMapper, QuestionMapper questionMapper) {
        this(sourceMapper, questionMapper, new ExamImportParser());
    }

    ExamImportService(QuestionSourceMapper sourceMapper, QuestionMapper questionMapper, ExamImportParser parser) {
        this.sourceMapper = sourceMapper;
        this.questionMapper = questionMapper;
        this.parser = parser;
    }

    public ImportPreviewResponse preview(long sourceId, String filename, byte[] bytes) {
        ExamImportParser.ParsedPreview parsed = parser.parse(filename, bytes);
        List<String> errors = new ArrayList<>(parsed.errors);
        List<ImportPreviewResponse.Item> validItems = new ArrayList<>();
        Set<String> batchFingerprints = new HashSet<>();
        int duplicates = 0;
        for (ImportPreviewResponse.Item item : parsed.items) {
            boolean duplicate = !batchFingerprints.add(item.getFingerprint())
                    || questionMapper.countQuestionsByContentFingerprint(item.getFingerprint()) > 0;
            ImportPreviewResponse.Group group = item.getGroup();
            if (group != null && questionMapper.countGroupsByContentFingerprint(group.getFingerprint()) > 0) duplicate = true;
            if (duplicate) {
                duplicates++;
                errors.add("row " + item.getRowNumber() + ": duplicate content fingerprint");
            } else {
                validItems.add(item);
            }
        }
        long batchId = previewIds.getAndIncrement();
        ImportPreviewResponse response = new ImportPreviewResponse(batchId, parsed.totalRows, validItems.size(), duplicates,
                errors, validItems);
        previews.put(batchId, new Batch(sourceId, response));
        return response;
    }

    @Transactional(rollbackFor = Exception.class)
    public void commit(long batchId, long adminId) {
        Batch batch = previews.get(batchId);
        if (batch == null || batch.response.getErrors().size() > 0 || batch.response.getTotalRows() == 0) {
            throw invalidBatch();
        }
        synchronized (batch) {
            if (batch.committed) throw invalidBatch();
            QuestionSourceEntity source = sourceMapper.findByIdForUpdate(batch.sourceId);
            if (!reusable(source)) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
            }
            persist(batch.sourceId, batch.response.getItems());
            batch.committed = true;
        }
    }

    private void persist(long sourceId, List<ImportPreviewResponse.Item> items) {
        Map<String, Long> groups = new HashMap<>();
        Map<String, Integer> nextGroupOrder = new HashMap<>();
        for (ImportPreviewResponse.Item item : items) {
            Long groupId = null;
            ImportPreviewResponse.Group importedGroup = item.getGroup();
            if (importedGroup != null) {
                groupId = groups.get(importedGroup.getFingerprint());
                if (groupId == null) {
                    QuestionGroupEntity group = new QuestionGroupEntity();
                    group.setGroupType(importedGroup.getGroupType());
                    group.setTitle(importedGroup.getTitle());
                    group.setInstruction(importedGroup.getInstruction());
                    group.setContent(importedGroup.getContent());
                    group.setSharedOptions(importedGroup.getSharedOptions());
                    group.setSourceId(sourceId);
                    group.setDifficulty(item.getDifficulty());
                    group.setKnowledgePoints(item.getKnowledgePoints());
                    group.setContentFingerprint(importedGroup.getFingerprint());
                    group.setReviewStatus("DRAFT");
                    group.setEnabled(1);
                    requireInsert(questionMapper.insertGroup(group));
                    groupId = group.getId();
                    groups.put(importedGroup.getFingerprint(), groupId);
                }
            }
            QuestionEntity question = new QuestionEntity();
            question.setGroupId(groupId);
            question.setQuestionType(item.getQuestionType());
            question.setStem(item.getStem());
            question.setOptions(item.getOptions());
            question.setCorrectAnswer(item.getCorrectAnswer());
            question.setExplanation(item.getExplanation());
            question.setReferenceAnswer(item.getReferenceAnswer());
            question.setSampleAnswer(item.getSampleAnswer());
            question.setScoringRubric(item.getScoringRubric());
            question.setSourceId(sourceId);
            question.setGroupSortOrder(groupId == null ? null : item.getGroupSortOrder() == null
                    ? nextGroupOrder.merge(importedGroup.getFingerprint(), 1, Integer::sum) : item.getGroupSortOrder());
            question.setDifficulty(item.getDifficulty());
            question.setKnowledgePoints(item.getKnowledgePoints());
            question.setContentFingerprint(item.getFingerprint());
            question.setReviewStatus("DRAFT");
            question.setEnabled(1);
            requireInsert(questionMapper.insert(question));
        }
    }

    private static void requireInsert(int rows) {
        if (rows != 1) throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
    }

    private static boolean reusable(QuestionSourceEntity source) {
        return source != null && "APPROVED".equals(source.getReviewStatus())
                && REUSABLE_COPYRIGHT.contains(source.getCopyrightStatus());
    }

    private static BusinessException invalidBatch() {
        return new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.EXAM_IMPORT_INVALID);
    }

    private static final class Batch {
        final long sourceId;
        final ImportPreviewResponse response;
        boolean committed;
        Batch(long sourceId, ImportPreviewResponse response) {
            this.sourceId = sourceId;
            this.response = response;
        }
    }
}
