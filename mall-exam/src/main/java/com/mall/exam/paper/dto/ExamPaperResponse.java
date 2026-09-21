package com.mall.exam.paper.dto;
import com.mall.exam.attempt.dto.ExamResultResponse;
import com.mall.exam.attempt.dto.SavedAnswerResponse;
import com.mall.exam.paper.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
/** Student projection: includes saved student answers but no correct answers, explanations or marking references. */
public final class ExamPaperResponse {
    public final Long id;
    public final String paperNo,status,blueprintVersion;
    public final String fixedPaperCode;
    public final Integer fixedPaperVersion;
    public final LocalDateTime startedAt,dueAt,serverTime;
    public final List<Group> groups;
    public final List<Item> items;
    public final List<SavedAnswerResponse> answers;
    public final ExamResultResponse result;
    public ExamPaperResponse(ExamPaperEntity paper,List<ExamPaperGroupSnapshotEntity> groups,List<ExamPaperItemSnapshotEntity> items,LocalDateTime serverTime) {
        this(paper,groups,items,serverTime,List.of(),null);
    }
    public ExamPaperResponse(ExamPaperEntity paper,List<ExamPaperGroupSnapshotEntity> groups,List<ExamPaperItemSnapshotEntity> items,LocalDateTime serverTime,
                             List<SavedAnswerResponse> answers,ExamResultResponse result) {
        id=paper.getId();paperNo=paper.getPaperNo();status=paper.getStatus();blueprintVersion=paper.getBlueprintVersionSnapshot();
        fixedPaperCode=paper.getFixedPaperCode();fixedPaperVersion=paper.getFixedPaperVersion();
        startedAt=paper.getStartedAt();dueAt=paper.getDueAt();this.serverTime=serverTime;
        this.groups=Collections.unmodifiableList(groups.stream().map(Group::new).collect(Collectors.toList()));
        this.items=Collections.unmodifiableList(items.stream().map(Item::new).collect(Collectors.toList()));
        this.answers=List.copyOf(answers);this.result=result;
    }
    public static final class Group {
        public final Long id;
        public final Integer sectionOrder,groupOrder;
        public final String title,instruction,content,sharedOptions;
        public Group(ExamPaperGroupSnapshotEntity g) {id=g.getId();sectionOrder=g.getSectionOrder();groupOrder=g.getGroupOrder();title=g.getTitle();instruction=g.getInstruction();content=g.getContent();sharedOptions=g.getSharedOptions();}
    }
    public static final class Item {
        public final Long id,paperGroupSnapshotId;
        public final Integer sectionOrder,itemOrder;
        public final String questionType,stem,options;
        public final BigDecimal score;
        Item(ExamPaperItemSnapshotEntity q) {id=q.getId();paperGroupSnapshotId=q.getPaperGroupSnapshotId();sectionOrder=q.getSectionOrder();itemOrder=q.getItemOrder();questionType=q.getQuestionType();stem=q.getStem();options=q.getOptions();score=q.getScore();}
    }
}
