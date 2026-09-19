package com.mall.admin.config;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamMyBatisWiringTest {

    private static final List<String> EXAM_MAPPER_PACKAGES = Arrays.asList(
            "com.mall.exam.source.mapper",
            "com.mall.exam.question.mapper",
            "com.mall.exam.importer.mapper",
            "com.mall.exam.paper.mapper",
            "com.mall.exam.attempt.mapper"
    );

    private static final List<String> EXAM_MODEL_PACKAGES = Arrays.asList(
            "com.mall.exam.source.model",
            "com.mall.exam.question.model",
            "com.mall.exam.importer.model",
            "com.mall.exam.paper.model",
            "com.mall.exam.attempt.model"
    );

    @Test
    void registersOnlyNestedExamMapperAndModelPackages() {
        MapperScan mapperScan = AdminMyBatisConfig.class.getAnnotation(MapperScan.class);

        assertEquals(EXAM_MAPPER_PACKAGES, examPackages(Arrays.asList(mapperScan.basePackages())));
        assertEquals(EXAM_MODEL_PACKAGES, examPackages(aliasPackages()));
    }

    @SuppressWarnings("unchecked")
    private static List<String> aliasPackages() {
        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Map<String, Object> mybatis = (Map<String, Object>) yaml.getObject().get("mybatis");
        return Arrays.asList(((String) mybatis.get("type-aliases-package")).split(","));
    }

    private static List<String> examPackages(List<String> packages) {
        return packages.stream()
                .filter(packageName -> packageName.startsWith("com.mall.exam."))
                .collect(Collectors.toList());
    }
}
