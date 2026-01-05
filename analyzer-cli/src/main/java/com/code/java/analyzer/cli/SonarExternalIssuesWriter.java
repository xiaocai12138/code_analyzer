package com.code.java.analyzer.cli;

import com.code.java.analyzer.core.Issue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SonarExternalIssuesWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SonarExternalIssuesWriter() {}

    /**
     * SonarQube External Issues (Generic Issue Data) JSON:
     * {
     *   "issues": [
     *     {
     *       "engineId": "...",
     *       "ruleId": "...",
     *       "severity": "MAJOR|MINOR|CRITICAL|BLOCKER|INFO",
     *       "type": "BUG|CODE_SMELL|VULNERABILITY",
     *       "primaryLocation": {
     *          "message": "...",
     *          "filePath": "src/main/java/xxx.java",
     *          "textRange": { "startLine":1, "endLine":1, "startColumn":1, "endColumn":1 }
     *       }
     *     }
     *   ]
     * }
     */
    public static void write(Path out, List<Issue> issues) throws Exception {
        Files.createDirectories(out.toAbsolutePath().getParent());

        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode arr = root.putArray("issues");

        for (Issue i : issues) {
            ObjectNode issue = arr.addObject();

            issue.put("engineId", i.engineId());
            issue.put("ruleId", i.ruleId());
            issue.put("severity", mapSeverity(i.severity()));
            issue.put("type", mapType(i.type()));

            ObjectNode primary = issue.putObject("primaryLocation");
            primary.put("message", i.message());
            primary.put("filePath", normalizePath(i.file()));

            ObjectNode range = primary.putObject("textRange");
            range.put("startLine", i.startLine());
            range.put("endLine", Math.max(i.endLine(), i.startLine()));

            // 列号可选，保守处理：>= 1
            int sc = Math.max(i.startCol(), 1);
            int ec = Math.max(i.endCol(), sc);
            range.put("startColumn", sc);
            range.put("endColumn", ec);
        }

        MAPPER.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), root);
    }

    private static String normalizePath(String p) {
        return p == null ? "" : p.replace('\\', '/');
    }

    private static String mapSeverity(String s) {
        if (s == null) return "MAJOR";
        String v = s.trim().toUpperCase();
        return switch (v) {
            case "INFO" -> "INFO";
            case "MINOR", "LOW" -> "MINOR";
            case "MAJOR", "MEDIUM" -> "MAJOR";
            case "CRITICAL", "HIGH" -> "CRITICAL";
            case "BLOCKER" -> "BLOCKER";
            default -> "MAJOR";
        };
    }

    private static String mapType(String t) {
        if (t == null) return "CODE_SMELL";
        String v = t.trim().toUpperCase();
        return switch (v) {
            case "BUG" -> "BUG";
            case "VULNERABILITY" -> "VULNERABILITY";
            default -> "CODE_SMELL";
        };
    }
}
