package com.mall.exam.paper.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.blueprint.*;
import com.mall.exam.question.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** No persistence, clock or shared mutable selection state. Random is injectable for reproducible selection. */
@Component
public class ExamPaperAssembler {
    private final Random random;
    public ExamPaperAssembler(){this(new Random());}
    public ExamPaperAssembler(Random random){this.random=Objects.requireNonNull(random);}

    public AssembledPaper assemble(ExamBlueprintDefinition blueprint,QuestionPool pool,Set<Long> previouslyUsedIds) {
        Map<Long,List<QuestionEntity>> members=validate(pool);
        List<QuestionGroupEntity> candidates=new ArrayList<>(pool.getGroups());
        Collections.shuffle(candidates,random);
        Set<Long> usedGroups=new HashSet<>(pool.getPreviouslyUsedGroupIds());
        for(QuestionGroupEntity g:candidates)
            if(members.get(g.getId()).stream().anyMatch(q->previouslyUsedIds.contains(q.getId())))usedGroups.add(g.getId());
        candidates.sort(Comparator.comparing(g->usedGroups.contains(g.getId())));
        Map<Integer,List<QuestionGroupEntity>> bySize=new HashMap<>();
        for(QuestionGroupEntity g:candidates)if("DIALOGUE".equals(g.getGroupType()))
            bySize.computeIfAbsent(members.get(g.getId()).size(),k->new ArrayList<>()).add(g);
        List<QuestionGroupEntity> dialogue=null;int bestUsed=Integer.MAX_VALUE;
        // Every positive triple summing to ten, including 1+1+8 and 2+4+4.
        for(int a=1;a<=8;a++)for(int b=a;b<=8;b++) {
            int c=10-a-b;if(c<b||c>8)continue;
            List<QuestionGroupEntity> choice=new ArrayList<>();Map<Integer,Integer> offsets=new HashMap<>();
            for(int size:new int[]{a,b,c}) {
                int offset=offsets.getOrDefault(size,0);List<QuestionGroupEntity> bucket=bySize.getOrDefault(size,List.of());
                if(offset>=bucket.size()){choice.clear();break;}
                choice.add(bucket.get(offset));offsets.put(size,offset+1);
            }
            if(choice.size()!=3)continue;
            int used=(int)choice.stream().filter(g->usedGroups.contains(g.getId())).count();
            if(used<bestUsed){dialogue=choice;bestUsed=used;}
        }
        if(dialogue==null)throw insufficient();
        Collections.shuffle(dialogue,random);
        List<QuestionGroupEntity> groups=new ArrayList<>();List<QuestionEntity> items=new ArrayList<>();
        for(ExamSectionSpec section:blueprint.getSections()) {
            List<QuestionEntity> selected=new ArrayList<>();
            if(section.getGroupCount()>0) {
                List<QuestionGroupEntity> selectedGroups;
                if("DIALOGUE".equals(section.getQuestionType()))selectedGroups=dialogue;
                else selectedGroups=candidates.stream().filter(g->"READING".equals(g.getGroupType())&&members.get(g.getId()).size()==5)
                    .limit(section.getGroupCount()).collect(Collectors.toList());
                if(selectedGroups.size()!=section.getGroupCount())throw insufficient();
                groups.addAll(selectedGroups);
                for(QuestionGroupEntity g:selectedGroups)selected.addAll(members.get(g.getId()));
            } else {
                List<QuestionEntity> singles=pool.getQuestions().stream().filter(q->q.getGroupId()==null&&section.getQuestionType().equals(q.getQuestionType())).collect(Collectors.toList());
                Collections.shuffle(singles,random);singles.sort(Comparator.comparing(q->previouslyUsedIds.contains(q.getId())));
                selected.addAll(singles.subList(0,Math.min(section.getAnswerItemCount(),singles.size())));
            }
            if(selected.size()!=section.getAnswerItemCount())throw insufficient();
            items.addAll(selected);
        }
        return new AssembledPaper(groups,items,BigDecimal.valueOf(blueprint.getTotalScore()));
    }

    private Map<Long,List<QuestionEntity>> validate(QuestionPool pool) {
        Map<Long,QuestionGroupEntity> groups=new HashMap<>();Map<Long,List<QuestionEntity>> members=new HashMap<>();
        for(QuestionGroupEntity g:pool.getGroups()) {
            if(g==null||g.getId()==null||groups.put(g.getId(),g)!=null||g.getSourceId()==null
                ||g.getContent()==null||g.getContent().isBlank()
                ||!Integer.valueOf(1).equals(g.getEnabled())||!"APPROVED".equals(g.getReviewStatus())
                ||!Set.of("DIALOGUE","READING").contains(g.getGroupType()))throw insufficient();
            members.put(g.getId(),new ArrayList<>());
        }
        Set<Long> ids=new HashSet<>();
        for(QuestionEntity q:pool.getQuestions()) {
            if(q==null||q.getId()==null||!ids.add(q.getId())||q.getSourceId()==null
                ||!Integer.valueOf(1).equals(q.getEnabled())||!"APPROVED".equals(q.getReviewStatus()))throw insufficient();
            if(q.getGroupId()==null) {
                if(!Set.of("VOCABULARY","GRAMMAR","TRANSLATION","WRITING").contains(q.getQuestionType()))throw insufficient();
            } else {
                QuestionGroupEntity g=groups.get(q.getGroupId());
                if(g==null||!g.getSourceId().equals(q.getSourceId())||q.getGroupSortOrder()==null||q.getGroupSortOrder()<1
                    ||!("DIALOGUE".equals(g.getGroupType())?"DIALOGUE_BLANK":"READING").equals(q.getQuestionType()))throw insufficient();
                members.get(g.getId()).add(q);
            }
        }
        for(QuestionGroupEntity g:groups.values()) {
            List<QuestionEntity> questions=members.get(g.getId());
            if(questions.isEmpty()||!Integer.valueOf(questions.size()).equals(pool.getMemberCounts().get(g.getId()))
                ||("READING".equals(g.getGroupType())&&questions.size()!=5)
                ||("DIALOGUE".equals(g.getGroupType())&&questions.size()>8)
                ||questions.stream().map(QuestionEntity::getGroupSortOrder).distinct().count()!=questions.size())throw insufficient();
            questions.sort(Comparator.comparing(QuestionEntity::getGroupSortOrder).thenComparing(QuestionEntity::getId));
        }
        return members;
    }
    static BusinessException insufficient(){return new BusinessException(HttpStatus.CONFLICT,ErrorCode.EXAM_BANK_INSUFFICIENT);}
}
