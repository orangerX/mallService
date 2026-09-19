package com.mall.exam.paper.dto;
import com.mall.exam.paper.model.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
/** Student projection: deliberately has no fields for answers, explanations or marking references. */
public final class ExamPaperResponse {
    public final Long id;
    public final String paperNo,status,blueprintVersion;
    public final LocalDateTime startedAt,dueAt,serverTime;
    public final List<Group> groups;
    public final List<Item> items;
    public ExamPaperResponse(ExamPaperEntity paper,List<ExamPaperGroupSnapshotEntity> groups,List<ExamPaperItemSnapshotEntity> items,LocalDateTime serverTime) {
        id=paper.getId();paperNo=paper.getPaperNo();status=paper.getStatus();blueprintVersion=paper.getBlueprintVersionSnapshot();
        startedAt=paper.getStartedAt();dueAt=paper.getDueAt();this.serverTime=serverTime;
        this.groups=Collections.unmodifiableList(groups.stream().map(Group::new).collect(Collectors.toList()));
        this.items=Collections.unmodifiableList(items.stream().map(Item::new).collect(Collectors.toList()));
    }
    public static final class Group {
        public final Long id;
        public final Integer sectionOrder,groupOrder;
        public final String title,instruction,content,sharedOptions;
        Group(ExamPaperGroupSnapshotEntity g) {id=g.getId();sectionOrder=g.getSectionOrder();groupOrder=g.getGroupOrder();title=g.getTitle();instruction=g.getInstruction();content=g.getContent();sharedOptions=g.getSharedOptions();}
    }
    public static final class Item {
        public final Long id,paperGroupSnapshotId;
        public final Integer sectionOrder,itemOrder;
        public final String questionType,stem,options;
        public final BigDecimal score;
        Item(ExamPaperItemSnapshotEntity q) {id=q.getId();paperGroupSnapshotId=q.getPaperGroupSnapshotId();sectionOrder=q.getSectionOrder();itemOrder=q.getItemOrder();questionType=q.getQuestionType();stem=q.getStem();options=q.getOptions();score=q.getScore();}
    }
}
