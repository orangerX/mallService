package com.mall.exam.paper.service;
import com.mall.exam.question.model.*;
import java.util.*;
/** Full eligible groups plus ALL their members. Counts come from the independent database group projection. */
public final class QuestionPool {
    private final List<QuestionGroupEntity> groups;
    private final List<QuestionEntity> questions;
    private final Map<Long,Integer> memberCounts;
    private final Set<Long> previouslyUsedGroupIds;
    public QuestionPool(List<QuestionGroupEntity> groups,List<QuestionEntity> questions,Map<Long,Integer> memberCounts,Set<Long> previouslyUsedGroupIds) {
        this.groups=groups;this.questions=questions;this.memberCounts=memberCounts;this.previouslyUsedGroupIds=previouslyUsedGroupIds;
    }
    public List<QuestionGroupEntity> getGroups(){return groups;}
    public List<QuestionEntity> getQuestions(){return questions;}
    public Map<Long,Integer> getMemberCounts(){return memberCounts;}
    public Set<Long> getPreviouslyUsedGroupIds(){return previouslyUsedGroupIds;}
}
