package com.mall.exam.paper.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.attempt.service.ExamAttemptService;
import com.mall.exam.blueprint.*;
import com.mall.exam.paper.dto.ExamPaperResponse;
import com.mall.exam.paper.lock.ExamGenerationLock;
import com.mall.exam.paper.mapper.ExamPaperMapper;
import com.mall.exam.paper.model.*;
import com.mall.exam.question.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamPaperService {
    private final ExamPaperMapper mapper;
    private final ExamPaperAssembler assembler;
    private final ExamGenerationLock lock;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private ExamAttemptService attempts;
    public ExamPaperService(ExamPaperMapper mapper,ExamPaperAssembler assembler,ExamGenerationLock lock,PlatformTransactionManager transactions,Clock clock) {
        this.mapper=mapper;this.assembler=assembler;this.lock=lock;this.clock=clock;
        transaction=new TransactionTemplate(transactions);
        // Must finish commit before releasing Redis, even if called by a transactional caller.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    /** Required Spring collaborator; setter preserves the existing assembly constructor contract. */
    @Autowired
    public void setAttemptService(ExamAttemptService attempts) { this.attempts=Objects.requireNonNull(attempts); }
    public ExamPaperResponse current(long userId) {
        if(attempts!=null)return attempts.current(userId);
        ExamPaperEntity existing=mapper.findInProgress(userId);
        return existing==null?null:resume(existing,userId);
    }
    public ExamPaperResponse generateOrResume(long userId) {
        ExamPaperEntity existing=mapper.findInProgress(userId);
        if(existing!=null)return resume(existing,userId);
        String key="mall:exam:generate:user:"+userId;
        String token=lock.acquire(key,Duration.ofSeconds(10));
        if(token==null)throw new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT);
        Throwable failure=null;
        try {
            ExamPaperEntity paper=transaction.execute(status-> {
                // Serializes generation after lease expiry too; this lock is held through commit.
                if(mapper.lockUser(userId)==null)throw new BusinessException(HttpStatus.NOT_FOUND,ErrorCode.USER_NOT_FOUND);
                ExamPaperEntity resumed=mapper.findInProgress(userId);
                if(resumed!=null)return resumed;
                return generate(userId);
            });
            // Release the user row lock before the attempt transaction inserts wrong records
            // (their user FK also locks that row). The Redis generation lease still belongs to us.
            return resume(paper,userId);
        } catch(RuntimeException|Error e) {
            failure=e;throw e;
        } finally {
            try {lock.release(key,token);}
            catch(RuntimeException releaseFailure) {
                if(failure!=null)failure.addSuppressed(releaseFailure);
                else throw releaseFailure;
            }
        }
    }
    private ExamPaperEntity generate(long userId) {
        ExamBlueprintDefinition blueprint=ExamBlueprintDefinition.degreeEnglish2016V2();
        ExamPaperMapper.BlueprintRow row=mapper.findEnabledBlueprint(blueprint.getCode());
        if(row==null)throw ExamPaperAssembler.insufficient();
        List<ExamPaperMapper.AvailableGroup> available=mapper.findAvailableGroups();
        Map<Long,Integer> counts=available.stream().collect(Collectors.toMap(QuestionGroupEntity::getId,ExamPaperMapper.AvailableGroup::getMemberCount));
        QuestionPool pool=new QuestionPool(new ArrayList<>(available),mapper.findAvailableQuestions(),counts,new HashSet<>(mapper.findPreviouslyUsedGroupIds(userId)));
        AssembledPaper assembled=assembler.assemble(blueprint,pool,new HashSet<>(mapper.findPreviouslyUsedQuestionIds(userId)));
        ExamPaperEntity paper=new ExamPaperEntity();paper.setUserId(userId);paper.setBlueprintId(row.id);paper.setBlueprintVersionSnapshot(row.version);
        paper.setPaperNo("DE-"+UUID.randomUUID().toString().replace("-","").toUpperCase(Locale.ROOT));
        paper.setStatus("IN_PROGRESS");paper.setStartedAt(LocalDateTime.now(clock));paper.setDueAt(paper.getStartedAt().plusMinutes(blueprint.getDurationMinutes()));paper.setVersion(0);
        requireInsert(mapper.insertPaper(paper),paper.getId());
        Map<Long,Long> groupIds=new HashMap<>();
        for(ExamSectionSpec section:blueprint.getSections()) {
            int groupOrder=0;
            for(QuestionGroupEntity g:assembled.getGroups())if(section.getQuestionType().equals(g.getGroupType())) {
                ExamPaperGroupSnapshotEntity snap=new ExamPaperGroupSnapshotEntity();
                snap.setPaperId(paper.getId());snap.setOriginalGroupId(g.getId());snap.setSectionOrder(section.getSectionOrder());snap.setGroupOrder(++groupOrder);
                snap.setTitle(g.getTitle());snap.setInstruction(g.getInstruction());snap.setContent(g.getContent());snap.setSharedOptions(g.getSharedOptions());
                requireInsert(mapper.insertGroupSnapshot(snap),snap.getId());groupIds.put(g.getId(),snap.getId());
            }
            int itemOrder=0;
            String type="DIALOGUE".equals(section.getQuestionType())?"DIALOGUE_BLANK":section.getQuestionType();
            for(QuestionEntity q:assembled.getItems())if(type.equals(q.getQuestionType())) {
                ExamPaperItemSnapshotEntity snap=new ExamPaperItemSnapshotEntity();
                snap.setPaperId(paper.getId());snap.setOriginalQuestionId(q.getId());snap.setPaperGroupSnapshotId(groupIds.get(q.getGroupId()));
                snap.setSectionOrder(section.getSectionOrder());snap.setItemOrder(++itemOrder);snap.setQuestionType(q.getQuestionType());
                snap.setStem(q.getStem());snap.setOptions(q.getOptions());snap.setCorrectAnswer(q.getCorrectAnswer());snap.setExplanation(q.getExplanation());
                snap.setReferenceAnswer(q.getReferenceAnswer());snap.setSampleAnswer(q.getSampleAnswer());snap.setScoringRubric(q.getScoringRubric());snap.setKnowledgePoints(q.getKnowledgePoints());
                snap.setScore(BigDecimal.valueOf(section.getSectionScore()).divide(BigDecimal.valueOf(section.getAnswerItemCount())));
                requireInsert(mapper.insertItemSnapshot(snap),snap.getId());
            }
        }
        return paper;
    }
    private static void requireInsert(int count,Long id) {
        if(count!=1||id==null)throw new IllegalStateException("Snapshot insert did not return a generated ID");
    }
    private ExamPaperResponse response(ExamPaperEntity paper,long userId) {
        if(!Long.valueOf(userId).equals(paper.getUserId())||!"IN_PROGRESS".equals(paper.getStatus()))
            throw new BusinessException(HttpStatus.NOT_FOUND,ErrorCode.EXAM_NOT_FOUND);
        return new ExamPaperResponse(paper,mapper.findGroupSnapshots(paper.getId()),mapper.findItemSnapshots(paper.getId()),LocalDateTime.now(clock));
    }
    private ExamPaperResponse resume(ExamPaperEntity paper,long userId) {
        if(attempts!=null)return attempts.current(userId,paper.getId());
        // A manually constructed assembly-only service must never resume an expired paper.
        if(!LocalDateTime.now(clock).isBefore(paper.getDueAt()))
            throw new IllegalStateException("ExamAttemptService is required to process expired papers");
        return response(paper,userId);
    }
}
