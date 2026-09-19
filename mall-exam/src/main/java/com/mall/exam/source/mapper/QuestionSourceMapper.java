package com.mall.exam.source.mapper;

import com.mall.exam.source.model.QuestionSourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface QuestionSourceMapper {

    QuestionSourceEntity findById(@Param("id") Long id);

    int insert(QuestionSourceEntity source);

    int update(QuestionSourceEntity source);

    int updateReview(@Param("id") Long id,
                     @Param("copyrightStatus") String copyrightStatus,
                     @Param("reviewStatus") String reviewStatus,
                     @Param("reviewedBy") Long reviewedBy,
                     @Param("reviewedAt") LocalDateTime reviewedAt,
                     @Param("reviewComment") String reviewComment);
}
