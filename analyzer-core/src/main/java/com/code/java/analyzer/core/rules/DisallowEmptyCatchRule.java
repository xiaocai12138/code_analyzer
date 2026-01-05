package com.code.java.analyzer.core.rules;

import com.code.java.analyzer.core.Issue;
import com.code.java.analyzer.core.IssueCollector;
import com.code.java.analyzer.core.Rule;
import com.sun.source.tree.*;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

import java.nio.file.Path;

public final class DisallowEmptyCatchRule implements Rule {

    @Override public String id() { return "MYJAVA0002"; }
    @Override public String engineId() { return "my-java-analyzer"; }
    @Override public String defaultSeverity() { return "CRITICAL"; }
    @Override public String type() { return "BUG"; }

    @Override
    public void apply(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector) {
        new TreePathScanner<Void, Void>() {

            @Override
            public Void visitTry(TryTree node, Void unused) {
                for (CatchTree c : node.getCatches()) {
                    BlockTree body = c.getBlock();
                    if (isEmptyCatchBody(body)) {
                        report(c, id() + ".message");
                    }
                }
                return super.visitTry(node, unused);
            }

            private boolean isEmptyCatchBody(BlockTree body) {
                if (body == null) return true;
                // 仅注释也算空：这里用 statements 数量判断
                return body.getStatements() == null || body.getStatements().isEmpty();
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
