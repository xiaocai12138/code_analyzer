package com.code.java.analyzer.core;

import java.util.List;

public final class AnalyzeResult {
    private final List<Issue> issues;

    public AnalyzeResult(List<Issue> issues) {
        this.issues = List.copyOf(issues);
    }

    public List<Issue> issues() {
        return issues;
    }

    public int issueCount() {
        return issues.size();
    }
}

