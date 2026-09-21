package com.mall.exam.paper.template.mapper;

import com.mall.exam.paper.template.dto.FixedPaperSummary;
import com.mall.exam.paper.template.model.FixedPaperTemplate;
import com.mall.exam.paper.template.model.FixedPaperTemplateGroup;
import com.mall.exam.paper.template.model.FixedPaperTemplateItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Read-only access: publishing migrations insert new versions, never edit snapshots. */
@Mapper
public interface FixedPaperTemplateMapper {
    FixedPaperTemplate findPublished(@Param("paperCode") String paperCode);

    List<FixedPaperSummary> listPublished();

    List<FixedPaperTemplateGroup> findGroups(@Param("fixedPaperId") long fixedPaperId);

    List<FixedPaperTemplateItem> findItems(@Param("fixedPaperId") long fixedPaperId);
}
