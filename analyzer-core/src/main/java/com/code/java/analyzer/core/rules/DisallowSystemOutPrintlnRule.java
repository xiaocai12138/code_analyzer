package com.code.java.analyzer.core.rules;

import com.code.java.analyzer.core.*;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.nio.file.Path;

public final class DisallowSystemOutPrintlnRule implements Rule {

    @Override public String id() { return "MYJAVA0001"; }
    @Override public String engineId() { return "my-java-analyzer"; }
    @Override public String defaultSeverity() { return "MAJOR"; }
    @Override public String type() { return "CODE_SMELL"; }

    @Override
    public void apply(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector) {

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
                // 识别类似：System.out.println(...)
                ExpressionTree select = node.getMethodSelect();
                if (select instanceof MemberSelectTree ms) {
                    String methodName = ms.getIdentifier().toString();
                    if ("println".equals(methodName)) {
                        ExpressionTree expr = ms.getExpression(); // 可能是 System.out
                        if (expr instanceof MemberSelectTree sysOut) {
                            boolean isOut = "out".equals(sysOut.getIdentifier().toString());
                            boolean isSystem = sysOut.getExpression().toString().equals("System");
                            if (isOut && isSystem) {
                                report(node, id() + ".message");
                            }
                        }
                    }
                }
                return super.visitMethodInvocation(node, unused);
            }

            private void report(MethodInvocationTree node, String msg) {
                CompilationUnitTree cu = getCurrentPath().getCompilationUnit();

                long start = trees.getSourcePositions().getStartPosition(cu, node);
                long end = trees.getSourcePositions().getEndPosition(cu, node);

                // 容错：有时 end = -1
                if (end < 0) end = start;

                var lineMap = cu.getLineMap();
                int startLine = (int) lineMap.getLineNumber(start);
                int startCol  = (int) lineMap.getColumnNumber(start);
                int endLine   = (int) lineMap.getLineNumber(end);
                int endCol    = (int) lineMap.getColumnNumber(end);

                Path abs = Path.of(cu.getSourceFile().toUri()).normalize();
                String rel = collector.projectRoot().toAbsolutePath().normalize().relativize(abs).toString()
                        .replace('\\', '/');

                collector.report(new Issue(
                        engineId(),
                        id(),
                        defaultSeverity(),
                        type(),
                        msg,
                        rel,
                        startLine, startCol,
                        endLine, endCol
                ));
            }
        }.scan(asts, null);
    }
}

