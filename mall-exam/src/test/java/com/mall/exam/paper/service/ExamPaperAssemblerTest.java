package com.mall.exam.paper.service;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.blueprint.ExamBlueprintDefinition;
import com.mall.exam.question.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

class ExamPaperAssemblerTest {
    @Test void exactAtomicBlueprintNoDuplicatesForOneHundredSeeds() {
        for(int seed=0;seed<100;seed++) {
            QuestionPool pool=pool();AssembledPaper p=assemble(pool,Set.of(),seed);
            assertEquals(7,p.getGroups().size());assertEquals(52,p.getItems().size());
            assertEquals(3,p.getGroups().stream().filter(g->g.getGroupType().equals("DIALOGUE")).count());
            assertEquals(10,p.getItems().stream().filter(q->q.getQuestionType().equals("DIALOGUE_BLANK")).count());
            assertEquals(20,p.getItems().stream().filter(q->q.getQuestionType().equals("READING")).count());
            assertEquals(new BigDecimal("100"),p.getTotalScore());
            assertEquals(52,p.getItems().stream().map(QuestionEntity::getId).distinct().count());
            assertEquals(7,p.getGroups().stream().map(QuestionGroupEntity::getId).distinct().count());
            for(QuestionGroupEntity g:p.getGroups())
                assertEquals(pool.getQuestions().stream().filter(q->g.getId().equals(q.getGroupId())).count(),
                    p.getItems().stream().filter(q->g.getId().equals(q.getGroupId())).count());
        }
    }
    @Test void unusedPreferenceThenFallback() {
        QuestionPool pool=pool();Set<Long> used=new HashSet<>();
        pool.getQuestions().stream().filter(q->q.getId()<1000).forEach(q->used.add(q.getId()));
        assertTrue(assemble(pool,used,2).getItems().stream().noneMatch(q->used.contains(q.getId())));
        pool.getQuestions().forEach(q->used.add(q.getId()));
        assertEquals(52,assemble(pool,used,2).getItems().size());
    }
    @Test void supportsExactOneOneEightDialoguePattern() {
        QuestionPool p=pool();p.getGroups().removeIf(g->g.getGroupType().equals("DIALOGUE"));
        p.getQuestions().removeIf(q->q.getQuestionType().equals("DIALOGUE_BLANK"));
        p.getMemberCounts().keySet().retainAll(p.getGroups().stream().map(QuestionGroupEntity::getId).collect(Collectors.toSet()));
        addGroup(p,80,8000,"DIALOGUE",1);addGroup(p,81,8100,"DIALOGUE",1);addGroup(p,82,8200,"DIALOGUE",8);
        assertEquals(52,assemble(p,Set.of(),1).getItems().size());
    }
    @Test void rejectsIncompleteUnavailableOrInconsistentPools() {
        for(int fault=0;fault<8;fault++) {
            QuestionPool p=pool();QuestionEntity q=p.getQuestions().get(0);
            switch(fault) {
                case 0:p.getQuestions().removeIf(i->i.getQuestionType().equals("WRITING"));break;
                case 1:p.getQuestions().remove(q);break;
                case 2:q.setEnabled(0);break;
                case 3:q.setSourceId(99L);break;
                case 4:q.setQuestionType("READING");break;
                case 5:p.getQuestions().add(q);break;
                case 6:p.getGroups().add(p.getGroups().get(0));break;
                case 7:p.getQuestions().get(1).setGroupSortOrder(q.getGroupSortOrder());break;
            }
            assertEquals(ErrorCode.EXAM_BANK_INSUFFICIENT,assertThrows(BusinessException.class,()->assemble(p,Set.of(),1)).getErrorCode());
        }
    }
    @Test void seededChoicesAreDeterministicAndDoNotMutatePool() {
        QuestionPool p=pool();List<Long> before=ids(p.getQuestions());
        assertEquals(ids(assemble(p,Set.of(),5).getItems()),ids(assemble(p,Set.of(),5).getItems()));
        assertEquals(before,ids(p.getQuestions()));
    }
    @Test void rejectsOversizedReadingGroupsAndMissingContent() {
        QuestionPool oversized=pool();
        oversized.getQuestions().add(question(9999,"READING",4L,6));oversized.getMemberCounts().put(4L,6);
        assertEquals(ErrorCode.EXAM_BANK_INSUFFICIENT,assertThrows(BusinessException.class,()->assemble(oversized,Set.of(),1)).getErrorCode());
        QuestionPool missing=pool();missing.getGroups().get(0).setContent(" ");
        assertEquals(ErrorCode.EXAM_BANK_INSUFFICIENT,assertThrows(BusinessException.class,()->assemble(missing,Set.of(),1)).getErrorCode());
    }
    @Test void noExactDialogueTripleFailsDespiteLargeBlankCount() {
        QuestionPool p=pool();p.getGroups().removeIf(g->g.getGroupType().equals("DIALOGUE"));
        p.getQuestions().removeIf(q->q.getQuestionType().equals("DIALOGUE_BLANK"));
        addGroup(p,80,8000,"DIALOGUE",4);addGroup(p,81,8100,"DIALOGUE",4);addGroup(p,82,8200,"DIALOGUE",4);
        assertEquals(ErrorCode.EXAM_BANK_INSUFFICIENT,assertThrows(BusinessException.class,()->assemble(p,Set.of(),1)).getErrorCode());
    }
    @Test void groupHistoryUsesSeparateIdNamespaceAndAvoidsAlreadySeenGroups() {
        QuestionPool base=pool();QuestionPool p=new QuestionPool(base.getGroups(),base.getQuestions(),base.getMemberCounts(),Set.of(1L,2L,3L,4L,5L,6L,7L));
        assertTrue(assemble(p,Set.of(),1).getGroups().stream().allMatch(g->g.getId()>1000));
    }
    static List<Long> ids(List<QuestionEntity> questions) {return questions.stream().map(QuestionEntity::getId).collect(Collectors.toList());}
    static AssembledPaper assemble(QuestionPool p,Set<Long> used,int seed) {return new ExamPaperAssembler(new Random(seed)).assemble(ExamBlueprintDefinition.degreeEnglish2016V2(),p,used);}
    static QuestionPool pool() {
        QuestionPool p=new QuestionPool(new ArrayList<>(),new ArrayList<>(),new HashMap<>(),Set.of());
        for(int copy=0;copy<2;copy++) {
            long base=copy*1000L;
            addGroup(p,base+1,base+10,"DIALOGUE",3);addGroup(p,base+2,base+20,"DIALOGUE",3);addGroup(p,base+3,base+30,"DIALOGUE",4);
            for(int g=0;g<4;g++)addGroup(p,base+4+g,base+40+10*g,"READING",5);
            List<String> types=List.of("VOCABULARY","GRAMMAR","TRANSLATION","WRITING");
            for(int type=0;type<types.size();type++)
                for(int i=0;i<(type<2?10:1);i++)p.getQuestions().add(question(base+100+type*20+i,types.get(type),null,null));
        }
        return p;
    }
    static void addGroup(QuestionPool p,long id,long first,String type,int count) {
        QuestionGroupEntity g=new QuestionGroupEntity();g.setId(id);g.setGroupType(type);g.setTitle("Title "+id);g.setContent("Passage "+id);
        g.setInstruction("Choose");g.setSharedOptions("[\"A\",\"B\"]");g.setSourceId(1L);g.setEnabled(1);g.setReviewStatus("APPROVED");
        p.getGroups().add(g);p.getMemberCounts().put(id,count);
        for(int i=0;i<count;i++)p.getQuestions().add(question(first+i,type.equals("DIALOGUE")?"DIALOGUE_BLANK":type,id,i+1));
    }
    static QuestionEntity question(long id,String type,Long group,Integer order) {
        QuestionEntity q=new QuestionEntity();q.setId(id);q.setQuestionType(type);q.setGroupId(group);q.setGroupSortOrder(order);
        q.setSourceId(1L);q.setEnabled(1);q.setReviewStatus("APPROVED");q.setStem("Question "+id);q.setOptions("[\"A\",\"B\"]");
        q.setCorrectAnswer("\"A\"");q.setExplanation("secret explanation");q.setReferenceAnswer("secret reference");q.setSampleAnswer("secret sample");
        q.setScoringRubric("{\"points\":15}");q.setKnowledgePoints("[\"tense\"]");return q;
    }
}
