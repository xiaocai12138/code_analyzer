package com.code.java.analyzer.gradle;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;

import javax.inject.Inject;

public class CodeAnalyzerReportsExtension {

    private final DirectoryProperty outputDir;
    private final Property<String> sarifFileName;

    @Inject
    public CodeAnalyzerReportsExtension(ObjectFactory objects) {
        this.outputDir = objects.directoryProperty();
        this.sarifFileName = objects.property(String.class);
    }

    public DirectoryProperty getOutputDir() {
        return outputDir;
    }

    public Property<String> getSarifFileName() {
        return sarifFileName;
    }
}
