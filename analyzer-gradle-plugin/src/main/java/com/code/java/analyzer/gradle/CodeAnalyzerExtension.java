package com.code.java.analyzer.gradle;

import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class CodeAnalyzerExtension {

    private final Property<Boolean> includeTests;
    private final Property<Boolean> failOnIssues;
    private final Property<String> engineId;
    private final Property<String> logPrefix;
    private final Property<Boolean> openReport;

    private final CodeAnalyzerReportsExtension reports;

    @Inject
    public CodeAnalyzerExtension(ObjectFactory objects) {
        this.includeTests = objects.property(Boolean.class);
        this.failOnIssues = objects.property(Boolean.class);
        this.engineId = objects.property(String.class);

        this.logPrefix = objects.property(String.class);
        this.openReport = objects.property(Boolean.class);

        this.reports = objects.newInstance(CodeAnalyzerReportsExtension.class);
    }

    public Property<Boolean> getIncludeTests() {
        return includeTests;
    }

    public Property<Boolean> getFailOnIssues() {
        return failOnIssues;
    }

    public Property<String> getEngineId() {
        return engineId;
    }

    public Property<String> getLogPrefix() {
        return logPrefix;
    }

    public Property<Boolean> getOpenReport() {
        return openReport;
    }

    public CodeAnalyzerReportsExtension getReports() {
        return reports;
    }

    /**
     * 关键：让 Groovy DSL 支持 reports { ... }
     */
    public void reports(Action<? super CodeAnalyzerReportsExtension> action) {
        action.execute(reports);
    }
}
