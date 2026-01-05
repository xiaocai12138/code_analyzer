package com.code.java.analyzer.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IssueCollector {
    private final Path projectRoot;
    private final List<Issue> issues = new ArrayList<>();

    public IssueCollector(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public Path projectRoot() { return projectRoot; }

    public void report(Issue issue) { issues.add(issue); }

    public List<Issue> issues() { return List.copyOf(issues); }
}
