package com.code.java.analyzer.core;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class RuleLoader {
    private RuleLoader() {}

    /**
     * 自动扫描并实例化指定包下所有 Rule 实现类（支持 IDE 运行、java -jar、fat-jar）
     */
    public static List<Rule> load(String basePackage) {
        List<Rule> rules = new ArrayList<>();
        System.out.println("RuleLoader CL=" + RuleLoader.class.getClassLoader());
        System.out.println("Has ClassGraph=" + (io.github.classgraph.ClassGraph.class != null));

        try (ScanResult scan = new ClassGraph()
                .enableClassInfo()
                .acceptPackages(basePackage)
                .scan()) {

            for (var ci : scan.getClassesImplementing(Rule.class.getName())) {
                Class<?> cls = ci.loadClass();
                if (cls.isInterface() || java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
                    continue;
                }

                // 必须可赋值给 Rule
                if (!Rule.class.isAssignableFrom(cls)) continue;

                @SuppressWarnings("unchecked")
                Class<? extends Rule> rc = (Class<? extends Rule>) cls;

                // 要求无参构造
                try {
                    Constructor<? extends Rule> ctor = rc.getDeclaredConstructor();
                    ctor.setAccessible(true);
                    Rule r = ctor.newInstance();
                    rules.add(r);
                } catch (NoSuchMethodException e) {
                    // 你可以选择：直接跳过 / 或抛错
                    // 为了“新增规则即生效”，这里建议抛错让你及时修正
                    throw new IllegalStateException("Rule must have a no-arg constructor: " + rc.getName(), e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // 为了稳定性：按 ruleId 排序（输出稳定）
        rules.sort(Comparator.comparing(Rule::id));
        return rules;
    }
}
