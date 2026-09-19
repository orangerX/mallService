package com.mall.exam.admin;

import com.mall.exam.admin.dto.AdminExamResponses.*;
import com.mall.exam.admin.service.AdminExamQueryService;
import com.mall.exam.question.mapper.*;
import com.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminExamQueryServiceTest {
    AdminExamMapper mapper=mock(AdminExamMapper.class);
    AdminExamQueryService service=new AdminExamQueryService(mapper,mock(QuestionMapper.class));
    @Test void paginationUsesLongOffsetAndReturnsTotalInsteadOfPageLength() {
        when(mapper.records(anyMap())).thenReturn(Collections.emptyList());
        when(mapper.countRecords(anyMap())).thenReturn(321L);
        assertEquals(321,service.records(7L,"EX","SUBMITTED",Integer.MAX_VALUE,100).getTotal());
        ArgumentCaptor<Map> parameters=ArgumentCaptor.forClass(Map.class); verify(mapper).records(parameters.capture());
        assertEquals(214748364600L,parameters.getValue().get("offset"));
        assertEquals(7L,parameters.getValue().get("userId"));
    }
    @Test void serviceAlsoEnforcesPaginationBoundaries() {
        for(int[] pair:new int[][]{{0,20},{1,0},{1,101}})
            assertThrows(BusinessException.class,()->service.records(null,null,null,pair[0],pair[1]));
        verifyNoInteractions(mapper);
    }
    @Test void capacityUsesTheLimitingSectionAndReportsZeroForIncompleteBank() {
        Blueprint blueprint=new Blueprint(); blueprint.id=1L;
        Capacity vocab=new Capacity(); vocab.questionType="VOCABULARY";vocab.answerItemCount=10;vocab.availableItems=59;
        Capacity writing=new Capacity();writing.questionType="WRITING";writing.answerItemCount=1;writing.availableItems=2;
        when(mapper.blueprints()).thenReturn(Collections.singletonList(blueprint));
        when(mapper.capacities(1L)).thenReturn(Arrays.asList(vocab,writing));
        assertEquals(2,service.blueprints().get(0).completePaperCapacity);
        writing.availableItems=0;
        assertEquals(0,service.blueprints().get(0).completePaperCapacity);
    }
}
