package com.code.java.acme.analyzer.maven;

import com.code.java.analyzer.core.JavacAnalyzeFacade;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Mojo(
        name = "analyze",
        defaultPhase = LifecyclePhase.VERIFY,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE
)
public class AnalyzeMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(property = "myanalyzer.includeTests", defaultValue = "false")
    private boolean includeTests;

    @Parameter(property = "myanalyzer.failOnIssues", defaultValue = "false")
    private boolean failOnIssues;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            List<String> sourceRoots = new ArrayList<>(project.getCompileSourceRoots());
            List<String> classpath = new ArrayList<>(project.getCompileClasspathElements());

            if (includeTests) {
                sourceRoots.addAll(project.getTestCompileSourceRoots());
                classpath.addAll(project.getTestClasspathElements());
            }

            File outDir = new File(project.getBuild().getDirectory(), "myanalyzer");
            File sarif = new File(outDir, "report.sarif");
            outDir.mkdirs();

            var result = JavacAnalyzeFacade.analyzeForMaven(
                    project.getBasedir().toPath(),
                    sourceRoots,
                    classpath
            );

            getLog().info("myanalyzer issues: " + result.issueCount());
            getLog().info("myanalyzer SARIF: " + sarif.getAbsolutePath());

            if (failOnIssues && result.issueCount() > 0) {
                throw new MojoExecutionException("myanalyzer found issues: " + result.issueCount());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("myanalyzer failed", e);
        }
    }
}

