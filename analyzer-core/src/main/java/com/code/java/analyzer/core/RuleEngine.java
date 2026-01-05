package com.code.java.analyzer.core;

import com.code.java.analyzer.core.rules.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.Trees;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RuleEngine {
    private final List<Rule> rules = new ArrayList<>();
    private final String toolName;

    public RuleEngine(String toolName) {
        this.toolName = toolName;
    }

    public String toolName() {
        return toolName;
    }

    public RuleEngine register(Rule rule) {
        rules.add(rule);
        return this;
    }

    public List<Rule> rules() {
        return List.copyOf(rules);
    }

    public void execute(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector) {
        for (Rule r : rules) {
            r.apply(asts, trees, collector);
        }
    }

//    public static RuleEngine defaultRules() {
//        return new RuleEngine("my-java-analyzer")
//                .register(new DisallowSystemOutPrintlnRule())
//                .register(new DisallowEmptyCatchRule())
//                .register(new DisallowPrintStackTraceRule())
//                .register(new DisallowSystemExitRule())
//                .register(new DisallowThreadSleepRule());
//        // 如果你还有第 6 条规则，在这里再 register 一条
//    }

    public static RuleEngine defaultRules() {
        RuleEngine engine = new RuleEngine("my-java-analyzer");

        // 自动扫描 rules 包
        for (Rule r : RuleLoader.load("com.code.java.analyzer.core.rules")) {
            engine.register(r);
        }

        // 防止你再遇到 rules=0 却不自知
        if (engine.rules().isEmpty()) {
            throw new IllegalStateException("No rules loaded. Check package name or classpath scanning.");
        }

        return engine;
    }

}

