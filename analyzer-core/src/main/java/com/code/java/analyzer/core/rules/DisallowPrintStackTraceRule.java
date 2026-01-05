package com.code.java.analyzer.core.rules;

import com.code.java.analyzer.core.Issue;
import com.code.java.analyzer.core.IssueCollector;
import com.code.java.analyzer.core.Rule;
import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.nio.file.Path;

public final class DisallowPrintStackTraceRule implements Rule {

    @Override public String id() { return "MYJAVA0003"; }
    @Override public String engineId() { return "my-java-analyzer"; }
    @Override public String defaultSeverity() { return "MAJOR"; }
    @Override public String type() { return "CODE_SMELL"; }

    @Override
    public void apply(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector) {

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                ExpressionTree select = node.getMethodSelect();
                if (select instanceof MemberSelectTree ms) {
                    if ("printStackTrace".equals(ms.getIdentifier().toString())
                            && node.getArguments().isEmpty()) {
                        report(node, id() + ".message");
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }

            private void report(Tree where, String msg) {
                CompilationUnitTree cu = getCurrentPath().getCompilationUnit();
                long start = trees.getSourcePositions().getStartPosition(cu, where);
                long end = trees.getSourcePositions().getEndPosition(cu, where);
                if (end < 0) end = start;

                var lineMap = cu.getLineMap();
                int startLine = (int) lineMap.getLineNumber(start);
                int startCol  = (int) lineMap.getColumnNumber(start);
                int endLine   = (int) lineMap.getLineNumber(end);
                int endCol    = (int) lineMap.getColumnNumber(end);

                Path abs = Path.of(cu.getSourceFile().toUri()).normalize();
                String rel = collector.projectRoot().toAbsolutePath().normalize()
                        .relativize(abs).toString().replace('\\', '/');

                collector.report(new Issue(
                        engineId(), id(), defaultSeverity(), type(),
                        msg, rel,
                        startLine, startCol, endLine, endCol
                ));
            }
        }.scan(asts, null);
    }
}
