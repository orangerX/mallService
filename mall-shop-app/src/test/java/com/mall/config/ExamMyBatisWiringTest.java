package com.mall.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamMyBatisWiringTest {

    private static final List<String> EXAM_MODEL_PACKAGES = Arrays.asList(
            "com.mall.exam.source.model",
            "com.mall.exam.question.model",
            "com.mall.exam.paper.model",
            "com.mall.exam.attempt.model"
    );

    @Test
    void registersOnlyNestedExamModelAliases() {
        assertEquals(EXAM_MODEL_PACKAGES, examModelPackages());
    }

    @SuppressWarnings("unchecked")
    private static List<String> examModelPackages() {
        YamlMapFactoryBean yaml = new YamlMapFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));
        Map<String, Object> mybatis = (Map<String, Object>) yaml.getObject().get("mybatis");
        return Arrays.stream(((String) mybatis.get("type-aliases-package")).split(","))
                .filter(packageName -> packageName.startsWith("com.mall.exam."))
                .collect(Collectors.toList());
    }
}
