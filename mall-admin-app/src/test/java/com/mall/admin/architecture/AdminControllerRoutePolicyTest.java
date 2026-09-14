package com.mall.admin.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminControllerRoutePolicyTest {
    @Test
    void adminControllersOnlyUseGetPostAndNoPathVariables() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<Class<?>> controllers = new HashSet<>();
        scanner.findCandidateComponents("com.mall.admin").forEach(candidate -> {
            try { controllers.add(Class.forName(candidate.getBeanClassName())); }
            catch (ClassNotFoundException exception) { throw new IllegalStateException(exception); }
        });
        assertFalse(controllers.isEmpty());
        for (Class<?> controller : controllers) {
            assertPaths(controller.getName(), AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
            for (Method method : ReflectionUtils.getUniqueDeclaredMethods(controller)) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) continue;
                assertTrue(mapping.method().length > 0);
                Arrays.stream(mapping.method()).forEach(value -> assertTrue(
                        value == RequestMethod.GET || value == RequestMethod.POST));
                assertPaths(controller.getSimpleName() + "." + method.getName(), mapping);
            }
        }
    }
    private static void assertPaths(String source, RequestMapping mapping) {
        if (mapping == null) return;
        Arrays.stream(mapping.value()).forEach(path -> assertFalse(path.contains("{") || path.contains("}"), source));
        Arrays.stream(mapping.path()).forEach(path -> assertFalse(path.contains("{") || path.contains("}"), source));
    }
}
