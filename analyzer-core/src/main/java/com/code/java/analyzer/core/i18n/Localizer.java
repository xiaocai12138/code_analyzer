package com.code.java.analyzer.core.i18n;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;

public final class Localizer {

    private static final String BUNDLE_BASE =
            "com.code.java.analyzer.core.i18n.messages";

    private Localizer() {}

    public static String format(String key, Locale locale, Object... args) {
        if (key == null || key.isBlank()) return "";
        if (locale == null) locale = Locale.SIMPLIFIED_CHINESE;

        String pattern = key;
        try {
            ResourceBundle bundle =
                    ResourceBundle.getBundle(BUNDLE_BASE, locale, new UTF8Control());

            if (bundle.containsKey(key)) {
                pattern = bundle.getString(key);
            }
        } catch (MissingResourceException e) {
            // 找不到就直接回退 key，方便你排查
            pattern = key;
        }

        if (args == null || args.length == 0) {
            return pattern;
        }
        return MessageFormat.format(pattern, args);
    }

    /**
     * 强制 UTF-8 读取 properties，彻底解决 ???? 问题
     */
    private static final class UTF8Control extends ResourceBundle.Control {
        @Override
        public ResourceBundle newBundle(
                String baseName,
                Locale locale,
                String format,
                ClassLoader loader,
                boolean reload
        ) throws java.io.IOException {

            String bundleName = toBundleName(baseName, locale);
            String resourceName = toResourceName(bundleName, "properties");

            try (InputStream is = loader.getResourceAsStream(resourceName)) {
                if (is == null) return null;
                return new PropertyResourceBundle(
                        new InputStreamReader(is, StandardCharsets.UTF_8)
                );
            }
        }
    }
}
