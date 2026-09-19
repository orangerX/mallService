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
        assertTrue(controllers.stream().filter(c -> c.getPackageName().equals("com.mall.admin.exam")).count() == 5,
                "All five exam controllers must be registered");
        Set<String> routes = new HashSet<>();
        for (Class<?> controller : controllers) {
            RequestMapping root = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            assertPaths(controller.getName(), AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
            for (Method method : ReflectionUtils.getUniqueDeclaredMethods(controller)) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) continue;
                assertTrue(mapping.method().length > 0);
                Arrays.stream(mapping.method()).forEach(value -> assertTrue(
                        value == RequestMethod.GET || value == RequestMethod.POST));
                assertPaths(controller.getSimpleName() + "." + method.getName(), mapping);
                String[] prefixes = root == null || root.value().length == 0 ? new String[]{""} : root.value();
                String[] paths = mapping.value().length == 0 ? new String[]{""} : mapping.value();
                for (String prefix : prefixes) for (String path : paths) for (RequestMethod verb : mapping.method()) {
                    assertTrue(routes.add(verb + " " + prefix + path), "Duplicate route: " + verb + " " + prefix + path);
                }
            }
        }
        Set<String> examRoutes = new HashSet<>();
        for (String route : routes) if (route.contains(" /admin/api/exam/")) examRoutes.add(route);
        Set<String> expected = new HashSet<>(Arrays.asList(
                "GET /admin/api/exam/sources", "POST /admin/api/exam/sources/create",
                "POST /admin/api/exam/sources/update", "POST /admin/api/exam/sources/review",
                "POST /admin/api/exam/sources/status", "GET /admin/api/exam/questions",
                "GET /admin/api/exam/questions/detail", "POST /admin/api/exam/questions/create",
                "POST /admin/api/exam/questions/update", "POST /admin/api/exam/questions/review",
                "POST /admin/api/exam/questions/status", "POST /admin/api/exam/imports/preview",
                "POST /admin/api/exam/imports/commit", "GET /admin/api/exam/blueprints",
                "POST /admin/api/exam/blueprints/status", "GET /admin/api/exam/records"));
        org.junit.jupiter.api.Assertions.assertEquals(expected, examRoutes);
    }
    private static void assertPaths(String source, RequestMapping mapping) {
        if (mapping == null) return;
        Arrays.stream(mapping.value()).forEach(path -> assertFalse(path.contains("{") || path.contains("}"), source));
        Arrays.stream(mapping.path()).forEach(path -> assertFalse(path.contains("{") || path.contains("}"), source));
    }
}
