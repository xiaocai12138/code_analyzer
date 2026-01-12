package com.code.java.analyzer.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;

public class CodeAnalyzerPlugin implements Plugin<Project> {

    public static final String EXT_NAME = "codeAnalyzer";
    public static final String TASK_NAME = "codeAnalyze";

    @Override
    public void apply(Project project) {

        // 1) 必须有 java 插件（否则拿不到 SourceSets / classpath）
        project.getPlugins().withType(JavaPlugin.class, jp -> {

            // 2) 注册 extension
            CodeAnalyzerExtension ext = project.getExtensions()
                    .create(EXT_NAME, CodeAnalyzerExtension.class, project.getObjects());

            // 3) extension 默认值（convention）
            ext.getIncludeTests().convention(false);
            ext.getFailOnIssues().convention(false);
            ext.getEngineId().convention("company-java");
            ext.getLogPrefix().convention("myanalyzer");
            ext.getOpenReport().convention(false);

            ext.getReports().getOutputDir()
                    .convention(project.getLayout().getBuildDirectory().dir("myanalyzer"));
            ext.getReports().getSarifFileName()
                    .convention("report.sarif");


            // 4) 注册 task
            project.getTasks().register(TASK_NAME, CodeAnalyzeTask.class, task -> {

                // task 归类到 verification（IDEA 右侧 Gradle 常见分组）
                task.setGroup("verification");
                task.setDescription("Run Java code analyzer and generate SARIF report.");

                // ===== 把 extension 配置“连线”到 task =====
                task.getIncludeTests().set(ext.getIncludeTests());
                task.getFailOnIssues().set(ext.getFailOnIssues());
                task.getEngineId().set(ext.getEngineId());

                task.getLogPrefix().set(ext.getLogPrefix());
                task.getOpenReport().set(ext.getOpenReport());

                task.getOutputDir().set(ext.getReports().getOutputDir());
                task.getSarifFileName().set(ext.getReports().getSarifFileName());

                // ===== 绑定 SourceSets / classpath =====
                SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

                SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                task.getMainSourceDirs().from(main.getAllJava().getSrcDirs());
                task.getMainCompileClasspath().from(main.getCompileClasspath(), main.getOutput());

                // test SourceSet 可能不存在（比如纯 java library 通常都有，但保险处理）
                SourceSet test = sourceSets.findByName(SourceSet.TEST_SOURCE_SET_NAME);
                if (test != null) {
                    task.getTestSourceDirs().from(test.getAllJava().getSrcDirs());
                    task.getTestCompileClasspath().from(test.getCompileClasspath(), test.getOutput());
                }
            });
        });

        // 5) 如果用户没应用 java 插件，给出明确错误（afterEvaluate 更友好）
        project.afterEvaluate(p -> {
            if (!p.getPlugins().hasPlugin(JavaPlugin.class)) {
                throw new IllegalStateException(
                        "com.code.java.analyzer plugin requires the 'java' plugin. " +
                                "Please add: plugins { id 'java' }"
                );
            }
        });
    }
}
