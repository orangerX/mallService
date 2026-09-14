package com.mall.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControllerRoutePolicyTest {

    @Test
    void controllersOnlyDeclareGetAndPostWithoutPathVariables() throws ClassNotFoundException {
        Set<Class<?>> controllers = findControllers();
        assertFalse(controllers.isEmpty(), "至少应存在一个 Controller");

        for (Class<?> controller : controllers) {
            RequestMapping classMapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            assertNoPathVariables(controller.getName(), paths(classMapping));

            for (Method method : ReflectionUtils.getUniqueDeclaredMethods(controller)) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                assertTrue(mapping.method().length > 0,
                        () -> controller.getSimpleName() + "." + method.getName() + " 必须显式声明 HTTP 方法");
                Arrays.stream(mapping.method()).forEach(requestMethod -> assertTrue(
                        requestMethod == RequestMethod.GET || requestMethod == RequestMethod.POST,
                        () -> controller.getSimpleName() + "." + method.getName() + " 使用了不允许的方法 " + requestMethod
                ));
                assertNoPathVariables(controller.getSimpleName() + "." + method.getName(), paths(mapping));
            }
        }
    }

    private static Set<Class<?>> findControllers() throws ClassNotFoundException {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<Class<?>> controllers = new HashSet<>();
        for (org.springframework.beans.factory.config.BeanDefinition candidate
                : scanner.findCandidateComponents("com.mall")) {
            controllers.add(Class.forName(candidate.getBeanClassName()));
        }
        return controllers;
    }

    private static String[] paths(RequestMapping mapping) {
        if (mapping == null) {
            return new String[0];
        }
        Set<String> paths = new HashSet<>();
        paths.addAll(Arrays.asList(mapping.value()));
        paths.addAll(Arrays.asList(mapping.path()));
        return paths.toArray(new String[0]);
    }

    private static void assertNoPathVariables(String source, String[] paths) {
        Arrays.stream(paths).forEach(path -> {
            assertFalse(path.contains("{"), () -> source + " 禁止使用路径变量: " + path);
            assertFalse(path.contains("}"), () -> source + " 禁止使用路径变量: " + path);
        });
    }
}
