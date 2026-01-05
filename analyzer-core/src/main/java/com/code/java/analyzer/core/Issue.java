package com.code.java.analyzer.core;

import java.util.Arrays;

public final class Issue {
    private final String engineId;
    private final String ruleId;
    private final String severity;
    private final String type;

    // 现在存 messageKey，例如 "MYJAVA0001.message"
    private final String message;

    private final Object[] messageArgs;

    private final String file;
    private final int startLine;
    private final int startCol;
    private final int endLine;
    private final int endCol;

    // 兼容旧构造：不带 args
    public Issue(String engineId, String ruleId, String severity, String type,
                 String message, String file,
                 int startLine, int startCol, int endLine, int endCol) {
        this(engineId, ruleId, severity, type, message, new Object[0], file, startLine, startCol, endLine, endCol);
    }

    // 新构造：带 args
    public Issue(String engineId, String ruleId, String severity, String type,
                 String message, Object[] messageArgs, String file,
                 int startLine, int startCol, int endLine, int endCol) {
        this.engineId = engineId;
        this.ruleId = ruleId;
        this.severity = severity;
        this.type = type;
        this.message = message;
        this.messageArgs = messageArgs == null ? new Object[0] : messageArgs;
        this.file = file;
        this.startLine = startLine;
        this.startCol = startCol;
        this.endLine = endLine;
        this.endCol = endCol;
    }

    public String engineId() { return engineId; }
    public String ruleId() { return ruleId; }
    public String severity() { return severity; }
    public String type() { return type; }

    // messageKey
    public String message() { return message; }
    public Object[] messageArgs() { return Arrays.copyOf(messageArgs, messageArgs.length); }

    public String file() { return file; }
    public int startLine() { return startLine; }
    public int startCol() { return startCol; }
    public int endLine() { return endLine; }
    public int endCol() { return endCol; }
}
