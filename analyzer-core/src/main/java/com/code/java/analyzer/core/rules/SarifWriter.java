package com.code.java.analyzer.core.rules;

import com.code.java.analyzer.core.Issue;
import com.code.java.analyzer.core.i18n.Localizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SarifWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SarifWriter() {}

    public static void write(Path out, List<Issue> issues, String toolName) throws Exception {
        Files.createDirectories(out.getParent());

        Locale locale = resolveLocale();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("version", "2.1.0");
        root.put("$schema", "https://json.schemastore.org/sarif-2.1.0.json");

        var runs = root.putArray("runs");
        var run = runs.addObject();

        /* ===================== tool ===================== */
        var tool = run.putObject("tool");
        var driver = tool.putObject("driver");
        driver.put("name", toolName);

        /* ===================== rules（去重） ===================== */
        Map<String, ObjectNode> ruleMap = new LinkedHashMap<>();
        for (Issue i : issues) {
            ruleMap.computeIfAbsent(i.ruleId(), rid -> {
                ObjectNode r = MAPPER.createObjectNode();
                r.put("id", rid);

                // 使用 <ruleId>.message 作为规则描述 key
                String descKey = rid + ".message";
                String desc = Localizer.format(descKey, locale);

                // 兜底：如果没有找到中文描述
                if (desc == null || desc.isBlank() || desc.equals(descKey)) {
                    desc = rid;
                }

                r.putObject("shortDescription").put("text", desc);
                return r;
            });
        }

        var rules = driver.putArray("rules");
        for (ObjectNode r : ruleMap.values()) {
            rules.add(r);
        }

        /* ===================== results ===================== */
        var results = run.putArray("results");
        for (Issue i : issues) {
            var res = results.addObject();
            res.put("ruleId", i.ruleId());

            // message：使用 messageKey + args 本地化
            String msg = Localizer.format(i.message(), locale, i.messageArgs());
            res.putObject("message").put("text", msg);

            var locations = res.putArray("locations");
            var location = locations.addObject();
            var physical = location.putObject("physicalLocation");

            physical.putObject("artifactLocation")
                    .put("uri", i.file());

            var region = physical.putObject("region");
            region.put("startLine", i.startLine());
            region.put("startColumn", i.startCol());
            region.put("endLine", i.endLine());
            region.put("endColumn", i.endCol());
        }

        MAPPER.writerWithDefaultPrettyPrinter()
                .writeValue(out.toFile(), root);
    }

    /**
     * 语言解析：
     *   -Dmyanalyzer.locale=zh_CN
     *   -Dmyanalyzer.locale=en_US
     *
     * 默认：zh_CN
     */
    private static Locale resolveLocale() {
        String raw = System.getProperty("myanalyzer.locale", "zh_CN").trim();
        if (raw.isEmpty()) {
            return Locale.SIMPLIFIED_CHINESE;
        }

        String[] parts = raw.split("[-_]");
        if (parts.length == 1) {
            return new Locale(parts[0]);
        }
        return new Locale(parts[0], parts[1]);
    }
}
