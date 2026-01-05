# Java Code Analyzer

一个基于 Java Compiler API 的静态代码分析工具，用于检测 Java 代码中的常见问题。

## 项目结构

```
code_analyzer/
├── analyzer-core/          # 核心分析引擎
│   └── src/main/java/com/code/java/analyzer/core/
│       ├── rules/          # 内置规则
│       ├── i18n/           # 国际化支持
│       └── ...
├── analyzer-cli/           # 命令行工具
│   └── src/main/java/com/code/java/analyzer/cli/
└── analyzer-maven-plugin/  # Maven 插件
```

## 功能特性

- 基于 Java Compiler API 的 AST 分析
- 支持自定义规则扩展
- 多种输出格式支持（SARIF、Sonar 外部问题 JSON）
- 提供 CLI 和 Maven Plugin 两种使用方式
- 自动扫描和加载规则

## 内置规则

- **DisallowSystemOutPrintlnRule** - 禁止使用 `System.out.println`
- **DisallowEmptyCatchRule** - 禁止空的 catch 块
- **DisallowPrintStackTraceRule** - 禁止使用 `printStackTrace()`
- **DisallowSystemExitRule** - 禁止使用 `System.exit()`
- **DisallowThreadSleepRule** - 禁止使用 `Thread.sleep()`

## 环境要求

- JDK 17 或更高版本
- Maven 3.6+

## 构建

```bash
mvn clean install
```

## 使用方式

### 命令行工具

```bash
java -jar analyzer-cli/target/analyzer-cli-0.0.1.jar \
  --projectRoot /path/to/project \
  --src /path/to/project/src/main/java \
  --outSarif /path/to/output.sarif \
  [--classpath "lib1.jar;lib2.jar"] \
  [--outSonar /path/to/sonar-issues.json]
```

#### 参数说明

- `--projectRoot`: 项目根目录（必需）
- `--src`: 源代码目录（必需）
- `--outSarif`: SARIF 输出文件路径（必需）
- `--classpath`: 类路径（可选，Windows 用 `;` 分隔，Linux 用 `:` 分隔）
- `--outSonar`: Sonar 外部问题 JSON 输出文件路径（可选）

### Maven 插件

在项目的 `pom.xml` 中配置：

```xml
<build>
  <plugins>
    <plugin>
      <groupId>com.code.java</groupId>
      <artifactId>analyzer-maven-plugin</artifactId>
      <version>0.0.1</version>
      <executions>
        <execution>
          <goals>
            <goal>analyze</goal>
          </goals>
          <configuration>
            <outputSarif>${project.build.directory}/analysis.sarif</outputSarif>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

运行分析：

```bash
mvn analyzer:analyze
```

## 输出格式

### SARIF 格式

SARIF（Static Analysis Results Interchange Format）是静态分析结果的标准交换格式，可以被多种工具（如 GitHub Advanced Security、VS Code 等）识别和使用。

### Sonar 外部问题格式

输出 SonarQube/SonarCloud 可识别的外部问题 JSON 格式，便于集成到 CI/CD 流程中。

## 扩展开发

### 添加自定义规则

1. 在 `analyzer-core/src/main/java/com/code/java/analyzer/core/rules/` 包下创建新规则类
2. 实现 `Rule` 接口
3. 规则会自动被 `RuleLoader` 扫描并加载

示例：

```java
package com.code.java.analyzer.core.rules;

import com.code.java.analyzer.core.*;
import com.sun.source.tree.*;

public class MyCustomRule implements Rule {
    @Override
    public void apply(Iterable<? extends CompilationUnitTree> asts, Trees trees, IssueCollector collector) {
        // 实现规则逻辑
    }
}
```

## 技术栈

- Java 17
- Maven
- Java Compiler API (javac)
- Jackson (JSON 处理)
- Commons CLI (命令行参数解析)

## 许可证

本项目仅供学习和参考使用。
