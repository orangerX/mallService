package com.mall.exam.source.mapper;

import com.mall.exam.source.model.QuestionSourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface QuestionSourceMapper {

    QuestionSourceEntity findById(@Param("id") Long id);

    QuestionSourceEntity findByIdForUpdate(@Param("id") Long id);

    int insert(QuestionSourceEntity source);

    int update(QuestionSourceEntity source);

    int approveSourceWithReusableCopyright(@Param("id") Long id,
                                           @Param("copyrightStatus") String copyrightStatus,
                                           @Param("reviewedBy") Long reviewedBy,
                                           @Param("reviewedAt") LocalDateTime reviewedAt,
                                           @Param("reviewComment") String reviewComment);

    int rejectSource(@Param("id") Long id,
                     @Param("copyrightStatus") String copyrightStatus,
                     @Param("reviewedBy") Long reviewedBy,
                     @Param("reviewedAt") LocalDateTime reviewedAt,
                     @Param("reviewComment") String reviewComment);
}
