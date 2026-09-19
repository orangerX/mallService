package com.mall.exam.paper.service;
import com.mall.exam.question.model.*;
import java.math.BigDecimal;
import java.util.*;
public final class AssembledPaper {
    private final List<QuestionGroupEntity> groups;
    private final List<QuestionEntity> items;
    private final BigDecimal totalScore;
    AssembledPaper(List<QuestionGroupEntity> groups,List<QuestionEntity> items,BigDecimal totalScore) {
        this.groups=List.copyOf(groups);this.items=List.copyOf(items);this.totalScore=totalScore;
    }
    public List<QuestionGroupEntity> getGroups(){return groups;}
    public List<QuestionEntity> getItems(){return items;}
    public BigDecimal getTotalScore(){return totalScore;}
}
