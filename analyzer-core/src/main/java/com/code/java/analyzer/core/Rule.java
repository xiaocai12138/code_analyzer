package com.code.java.analyzer.core;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;

public interface Rule {
    String id();
    String engineId();
    String defaultSeverity(); // "MAJOR"/"CRITICAL"...
    String type();            // "BUG"/"VULNERABILITY"/"CODE_SMELL"
    void apply(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector);
}

