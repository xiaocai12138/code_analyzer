package com.code.java.acme.analyzer.maven;

import com.code.java.analyzer.core.JavacAnalyzeFacade;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.awt.Desktop;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mojo(
        name = "analyze",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.TEST
)
public class AnalyzeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /** 是否包含测试源码 */
    @Parameter(property = "analyzer.includeTests", defaultValue = "false")
    private boolean includeTests;

    /** 发现问题是否让构建失败 */
    @Parameter(property = "analyzer.failOnIssues", defaultValue = "false")
    private boolean failOnIssues;

    /** 引擎标识（日志 / SARIF run.tool.driver.name 等用途） */
    @Parameter(property = "analyzer.engineId", defaultValue = "company-java")
    private String engineId;

    /**
     * 输出目录（相对 target 目录的子目录，或绝对路径都可）
     * 你现在写 analyzer -> target/analyzer
     */
    @Parameter(property = "analyzer.outputDir", defaultValue = "myanalyzer")
    private String outputDir;

    /** SARIF 文件名 */
    @Parameter(property = "analyzer.sarifFileName", defaultValue = "report.sarif")
    private String sarifFileName;

    /** 执行完成后自动打开 SARIF */
    @Parameter(property = "analyzer.openReport", defaultValue = "false")
    private boolean openReport;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            Path baseDir = project.getBasedir().toPath();

            // 1) source roots
            List<String> sourceRoots = new ArrayList<>();
            sourceRoots.addAll(project.getCompileSourceRoots());
            if (includeTests) {
                sourceRoots.addAll(project.getTestCompileSourceRoots());
            }

            // 2) classpath
            List<String> classpath = new ArrayList<>();
            // compile classpath
            classpath.addAll(project.getCompileClasspathElements());
            // test classpath（可选）
            if (includeTests) {
                classpath.addAll(project.getTestClasspathElements());
            }

            // 3) output file
            Path outDirPath;
            File outDirAsFile = new File(outputDir);
            if (outDirAsFile.isAbsolute()) {
                outDirPath = outDirAsFile.toPath();
            } else {
                outDirPath = new File(project.getBuild().getDirectory(), outputDir).toPath();
            }
            File outDirFile = outDirPath.toFile();
            if (!outDirFile.exists() && !outDirFile.mkdirs()) {
                throw new MojoExecutionException("Failed to create outputDir: " + outDirFile.getAbsolutePath());
            }

            Path sarifPath = outDirPath.resolve(sarifFileName).toAbsolutePath().normalize();

            getLog().info("analyzer engineId: " + engineId);
            getLog().info("analyzer includeTests: " + includeTests);
            getLog().info("analyzer failOnIssues: " + failOnIssues);
            getLog().info("analyzer sourceRoots: " + sourceRoots);
            getLog().info("analyzer classpath size: " + classpath.size());
            getLog().info("analyzer SARIF: " + sarifPath);

            var result = JavacAnalyzeFacade.run(baseDir, sourceRoots, classpath, sarifPath);

            getLog().info("analyzer issues: " + result.issueCount());

            if (openReport) {
                tryOpenFile(sarifPath.toFile());
            }

            if (failOnIssues && result.issueCount() > 0) {
                throw new MojoExecutionException("analyzer found issues: " + result.issueCount());
            }
        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("analyzer failed", e);
        }
    }

    private void tryOpenFile(File file) {
        try {
            if (!file.exists()) {
                getLog().warn("SARIF not found, skip open: " + file.getAbsolutePath());
                return;
            }
            if (!Desktop.isDesktopSupported()) {
                getLog().info("Desktop not supported, skip openReport");
                return;
            }
            Desktop desktop = Desktop.getDesktop();
            if (!desktop.isSupported(Desktop.Action.OPEN)) {
                getLog().info("Desktop OPEN not supported, skip openReport");
                return;
            }
            desktop.open(file);
            getLog().info("Opened SARIF: " + file.getAbsolutePath());
        } catch (Exception ex) {
            getLog().warn("openReport failed: " + ex.getMessage());
        }
    }
}
