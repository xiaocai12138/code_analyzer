package com.code.java.analyzer.gradle;

import com.code.java.analyzer.core.JavacAnalyzeFacade;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class CodeAnalyzeTask extends DefaultTask {

    /** 是否包含测试源码 */
    @Input
    public abstract Property<Boolean> getIncludeTests();

    /** 发现问题是否失败构建 */
    @Input
    public abstract Property<Boolean> getFailOnIssues();

    /** 仅日志用途/未来扩展 */
    @Input
    @Optional
    public abstract Property<String> getEngineId();

    /** 日志前缀（不要写死 myanalyzer） */
    @Input
    @Optional
    public abstract Property<String> getLogPrefix();

    /** 是否在任务结束后自动打开 SARIF 报告 */
    @Input
    @Optional
    public abstract Property<Boolean> getOpenReport();

    /** 主代码源码目录 */
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getMainSourceDirs();

    /** 主代码编译 classpath */
    @Classpath
    public abstract ConfigurableFileCollection getMainCompileClasspath();

    /** 测试源码目录 */
    @InputFiles
    @Optional
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getTestSourceDirs();

    /** 测试编译 classpath */
    @Classpath
    @Optional
    public abstract ConfigurableFileCollection getTestCompileClasspath();

    /** 输出目录（可配置） */
    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    /** SARIF 文件名（可配置） */
    @Input
    @Optional
    public abstract Property<String> getSarifFileName();

    @TaskAction
    public void run() {
        final boolean includeTests = Boolean.TRUE.equals(getIncludeTests().getOrElse(false));
        final boolean failOnIssues = Boolean.TRUE.equals(getFailOnIssues().getOrElse(false));
        final String engineId = getEngineId().getOrElse("company-java");
        final String logPrefix = getLogPrefix().getOrElse("myanalyzer");
        final boolean openReport = Boolean.TRUE.equals(getOpenReport().getOrElse(false));
        final String sarifFileName = getSarifFileName().getOrElse("report.sarif");

        final Path baseDir = getProject().getProjectDir().toPath();

        // 1) sourceRoots
        List<String> sourceRoots = new ArrayList<>();
        getMainSourceDirs().getFiles().forEach(f -> {
            if (f.exists()) sourceRoots.add(f.getAbsolutePath());
        });

        // 2) classpath
        List<String> classpath = new ArrayList<>();
        getMainCompileClasspath().getFiles().forEach(f -> {
            if (f.exists()) classpath.add(f.getAbsolutePath());
        });

        if (includeTests) {
            getTestSourceDirs().getFiles().forEach(f -> {
                if (f.exists()) sourceRoots.add(f.getAbsolutePath());
            });
            getTestCompileClasspath().getFiles().forEach(f -> {
                if (f.exists()) classpath.add(f.getAbsolutePath());
            });
        }

        // 3) output
        File outDir = getOutputDir().get().getAsFile();
        File sarifFile = new File(outDir, sarifFileName);
        // 确保父目录存在
        if (!outDir.exists() && !outDir.mkdirs()) {
            throw new GradleException("Failed to create outputDir: " + outDir.getAbsolutePath());
        }

        // 4) logs
        getLogger().lifecycle("[{}] engineId={}", logPrefix, engineId);
        getLogger().lifecycle("[{}] includeTests={}", logPrefix, includeTests);
        getLogger().lifecycle("[{}] failOnIssues={}", logPrefix, failOnIssues);
        getLogger().lifecycle("[{}] openReport={}", logPrefix, openReport);
        getLogger().lifecycle("[{}] sourceRoots={}", logPrefix, sourceRoots);
        getLogger().lifecycle("[{}] classpath size={}", logPrefix, classpath.size());
        getLogger().lifecycle("[{}] SARIF={}", logPrefix, sarifFile.getAbsolutePath());

        try {
            var result = JavacAnalyzeFacade.run(
                    baseDir,
                    sourceRoots,
                    classpath,
                    sarifFile.toPath()
            );

            getLogger().lifecycle("[{}] issues: {}", logPrefix, result.issueCount());

            if (failOnIssues && result.issueCount() > 0) {
                throw new GradleException(logPrefix + " found issues: " + result.issueCount());
            }

            // 5) auto open report (optional)
            if (openReport) {
                tryOpenFile(sarifFile, logPrefix);
            }

        } catch (Exception e) {
            throw new GradleException(logPrefix + " failed", e);
        }
    }

    private void tryOpenFile(File file, String logPrefix) {
        try {
            if (file == null || !file.exists()) {
                getLogger().warn("[{}] SARIF file not found, skip open: {}", logPrefix,
                        file == null ? "null" : file.getAbsolutePath());
                return;
            }

            String os = System.getProperty("os.name", "").toLowerCase();
            ProcessBuilder pb;

            // Windows: cmd /c start "" "<file>"
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", file.getAbsolutePath());
            }
            // macOS: open "<file>"
            else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", file.getAbsolutePath());
            }
            // Linux: xdg-open "<file>"
            else {
                pb = new ProcessBuilder("xdg-open", file.getAbsolutePath());
            }

            pb.start();
            getLogger().lifecycle("[{}] opened SARIF report", logPrefix);
        } catch (Exception e) {
            getLogger().warn("[{}] failed to open SARIF report: {}", logPrefix, e.toString());
        }
    }
}
