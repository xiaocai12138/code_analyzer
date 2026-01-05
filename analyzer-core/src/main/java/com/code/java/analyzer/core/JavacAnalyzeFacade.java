package com.code.java.analyzer.core;

import com.code.java.analyzer.core.rules.SarifWriter;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 统一门面：CLI / Maven Plugin 共用
 */
public final class JavacAnalyzeFacade {

    private JavacAnalyzeFacade() {}

    /* =========================
     * 1) CLI：零侵入
     * ========================= */
    public static AnalyzeResult analyzeForCli(Path projectRoot, Path srcDir, String classpath) throws Exception {
        List<Path> srcDirs = List.of(srcDir);
        List<String> cp = splitClasspath(classpath);

        RuleEngine engine = RuleEngine.defaultRules();
        IssueCollector collector = new IssueCollector(projectRoot);

        analyzeInternal(projectRoot, srcDirs, cp, engine, collector);

        return new AnalyzeResult(collector.issues());
    }

    /* =========================
     * 2) Maven Plugin：项目内
     * ========================= */
    public static AnalyzeResult analyzeForMaven(Path projectRoot, List<String> sourceRoots, List<String> classpath) throws Exception {
        List<Path> srcDirs = sourceRoots.stream().map(Path::of).collect(Collectors.toList());

        RuleEngine engine = RuleEngine.defaultRules();
        IssueCollector collector = new IssueCollector(projectRoot);

        analyzeInternal(projectRoot, srcDirs, classpath, engine, collector);

        return new AnalyzeResult(collector.issues());
    }

    /* =========================
     * 3) Maven Plugin 兼容旧接口：run(...)（你现有 Mojo 在调用）
     *    建议后续 Mojo 改用 analyzeForMaven，但现在先保留不破坏你已跑通的路径。
     * ========================= */
    public static AnalyzeResult run(Path projectRoot, List<String> sourceRoots, List<String> classpath, Path outSarif) throws Exception {
        AnalyzeResult result = analyzeForMaven(projectRoot, sourceRoots, classpath);
        SarifWriter.write(outSarif, result.issues(), "my-java-analyzer");
        return result;
    }

    /* =========================
     * 核心实现：唯一
     * ========================= */
    private static void analyzeInternal(
            Path projectRoot,
            List<Path> srcDirs,
            List<String> classpath,
            RuleEngine engine,
            IssueCollector collector
    ) throws Exception {

        projectRoot = projectRoot.toAbsolutePath().normalize();

        // 1) 收集 .java
        List<Path> javaFiles = collectJavaFiles(srcDirs);
        System.out.println("[my-java-analyzer] javaFiles=" + javaFiles.size());
        if (javaFiles.isEmpty()) return;

        System.out.println("[my-java-analyzer] rules=" + engine.rules().size());

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler found. Please run with a JDK (not JRE).");
        }

        // ✅ 关键：把诊断收集起来（你现在传 null，javac 的 stop 行为不可控）
        var diagnostics = new javax.tools.DiagnosticCollector<JavaFileObject>();

        StandardJavaFileManager fileManager =
                compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8);

        try {
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjectsFromPaths(javaFiles);

            List<String> options = new ArrayList<>();
            options.add("-proc:none");
            options.add("--release");
            options.add("17");
            options.add("-Xlint:none");

            // ✅ 关键：尽量不要因为错误“提前停止”导致 parse 为空（JDK 内部开关，工程实践常用）
            options.add("-XDshouldStopPolicy=GENERATE");

            if (classpath != null && !classpath.isEmpty()) {
                options.add("-classpath");
                options.add(String.join(File.pathSeparator, classpath));
            }

            options.add("-sourcepath");
            options.add(srcDirs.stream()
                    .map(p -> p.toAbsolutePath().normalize().toString())
                    .collect(Collectors.joining(File.pathSeparator)));

            JavacTask task = (JavacTask) compiler.getTask(
                    null,
                    fileManager,
                    diagnostics,   // ✅ 不要再传 null
                    options,
                    null,
                    units
            );

            Iterable<? extends CompilationUnitTree> asts;
            try {
                asts = task.parse();
            } catch (Throwable t) {
                // parse 都失败的话，至少把诊断打出来，便于定位
                diagnostics.getDiagnostics().forEach(d -> System.err.println(d));
                throw t;
            }

            // ✅ 打印 asts 是否为空（可先保留，稳定后再删）
            int astCount = 0;
            for (CompilationUnitTree ignored : asts) astCount++;
            System.out.println("[my-java-analyzer] asts=" + astCount);

            try {
                task.analyze();
            } catch (Throwable ignore) {
                // 语义失败也继续
            }

            Trees trees = Trees.instance(task);

            // 3) 执行规则
            engine.execute(asts, trees, collector);

            // ✅ 可选：把编译诊断也打出来（不影响规则输出）
            // diagnostics.getDiagnostics().forEach(d -> System.err.println(d));

        } finally {
            try { fileManager.close(); } catch (IOException ignore) {}
        }
    }


    private static List<Path> collectJavaFiles(List<Path> roots) throws IOException {
        List<Path> result = new ArrayList<>();
        for (Path root : roots) {
            if (root == null) continue;
            Path dir = root.toAbsolutePath().normalize();
            if (!Files.isDirectory(dir)) continue;

            try (Stream<Path> s = Files.walk(dir)) {
                s.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(result::add);
            }
        }
        return result;
    }

    private static List<String> splitClasspath(String cp) {
        if (cp == null || cp.isBlank()) return List.of();
        String[] parts = cp.split(java.util.regex.Pattern.quote(File.pathSeparator));
        List<String> list = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) list.add(p.trim());
        }
        return list;
    }
}
