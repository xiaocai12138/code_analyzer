package com.code.java.analyzer.cli;

import com.code.java.analyzer.core.AnalyzeResult;
import com.code.java.analyzer.core.JavacAnalyzeFacade;
import com.code.java.analyzer.core.rules.SarifWriter;
import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AnalyzerCliMain {

    public static void main(String[] args) throws Exception {

        System.out.println("ClassGraph loaded from: " +
                io.github.classgraph.ClassGraph.class.getProtectionDomain().getCodeSource().getLocation());

        Options options = new Options();

        options.addOption(Option.builder().longOpt("projectRoot").hasArg().required()
                .desc("Target project root directory").build());
        options.addOption(Option.builder().longOpt("src").hasArg().required()
                .desc("Source directory to analyze (e.g., src/main/java)").build());
        options.addOption(Option.builder().longOpt("outSarif").hasArg().required()
                .desc("Output SARIF file path").build());

        options.addOption(Option.builder().longOpt("classpath").hasArg()
                .desc("Classpath (optional, separated by ';' on Windows, ':' on Linux)").build());

        options.addOption(Option.builder().longOpt("outSonar").hasArg()
                .desc("Output Sonar external issues json (optional)").build());

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            new HelpFormatter().printHelp("my-java-analyzer-cli", options, true);
            System.exit(2);
            return;
        }

        Path projectRoot = Path.of(cmd.getOptionValue("projectRoot")).toAbsolutePath().normalize();
        Path srcDir = Path.of(cmd.getOptionValue("src")).toAbsolutePath().normalize();
        Path outSarif = Path.of(cmd.getOptionValue("outSarif")).toAbsolutePath().normalize();
        String classpath = cmd.getOptionValue("classpath");

        if (!Files.isDirectory(projectRoot)) {
            throw new IllegalArgumentException("projectRoot not a directory: " + projectRoot);
        }
        if (!Files.isDirectory(srcDir)) {
            throw new IllegalArgumentException("src not a directory: " + srcDir);
        }

        // 1) 执行分析（门面返回 AnalyzeResult）
        AnalyzeResult result = JavacAnalyzeFacade.analyzeForCli(projectRoot, srcDir, classpath);

        // 2) 输出 SARIF
        SarifWriter.write(outSarif, result.issues(), "my-java-analyzer");
        System.out.println("[my-java-analyzer] sarif=" + outSarif);

        // 3) 可选：输出 Sonar external issues JSON
        if (cmd.hasOption("outSonar")) {
            Path outSonar = Path.of(cmd.getOptionValue("outSonar")).toAbsolutePath().normalize();
            SonarExternalIssuesWriter.write(outSonar, result.issues());
            System.out.println("[my-java-analyzer] sonar-json=" + outSonar);
        }

        System.out.println("[my-java-analyzer] issues=" + result.issueCount());
    }
}
