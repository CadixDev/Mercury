/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FileASTRequestor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class Mercury {

    public static final String JAVA_EXTENSION = ".java";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private String sourceCompatibility = JavaCore.VERSION_1_8;
    private Charset encoding = StandardCharsets.UTF_8;
    /**
     * Mercury will by default crash if a none-complete classpath is supplied, though
     * you absolutely <em>should</em> supply a full classpath - we can handle it
     * gracefully.
     */
    private boolean gracefulClasspathChecks = false;

    private final List<Path> classPath = new ArrayList<>();
    private final List<Path> sourcePath = new ArrayList<>();

    private final Map<Object, Object> context = new HashMap<>();
    private Path sourceDir;
    private Path outputDir;

    private final List<SourceProcessor> processors = new ArrayList<>();

    private final FileASTRequestor requestor = new Requestor();

    public String getSourceCompatibility() {
        return this.sourceCompatibility;
    }

    public void setSourceCompatibility(String sourceCompatibility) {
        this.sourceCompatibility = Objects.requireNonNull(sourceCompatibility, "sourceCompatibility");
    }

    public Charset getEncoding() {
        return this.encoding;
    }

    public void setEncoding(Charset encoding) {
        this.encoding = Objects.requireNonNull(encoding, "encoding");
    }

    public boolean isGracefulClasspathChecks() {
        return this.gracefulClasspathChecks;
    }

    public void setGracefulClasspathChecks(final boolean enable) {
        this.gracefulClasspathChecks = enable;
    }

    public List<Path> getClassPath() {
        return this.classPath;
    }

    public List<Path> getSourcePath() {
        return this.sourcePath;
    }

    public List<SourceProcessor> getProcessors() {
        return this.processors;
    }

    public Path getSourceDir() {
        return this.sourceDir;
    }

    public Path getOutputDir() {
        return this.outputDir;
    }

    public Map<Object, Object> getContext() {
        return this.context;
    }

    public Optional<ITypeBinding> createTypeBinding(String className) {
        if (isAnonymousOrLocalType(className)) {
            // TODO: Anonymous or local types are currently not supported
            // Eclipse uses source lines in their binding keys that are impossible
            // to know in advance. Since it may return incorrect results, abort early.
            return Optional.empty();
        }

        IBinding binding = this.requestor.createBindings(new String[]{'L' + className.replace('.', '/') + ';'})[0];
        return binding != null && binding.getKind() == IBinding.TYPE ? Optional.of((ITypeBinding) binding) : Optional.empty();
    }

    private static boolean isAnonymousOrLocalType(String className) {
        int i = className.indexOf('$') + 1;
        while (i > 0 && i < className.length()) {
            if (Character.isDigit(className.charAt(i))) {
                return true;
            }
            i = className.indexOf('$', i) + 1;
        }
        return false;
    }

    public void process(Path sourceDir) throws Exception {
        if (this.sourceDir != null) {
            throw new IllegalStateException("Instance is currently processing: " + this.sourceDir);
        }

        try {
            this.sourceDir = Objects.requireNonNull(sourceDir, "sourceDir");
            run();
        } finally {
            cleanup();
        }
    }

    public void rewrite(Path sourceDir, Path outputDir) throws Exception {
        if (this.sourceDir != null) {
            throw new IllegalStateException("Instance is currently processing: " + this.sourceDir);
        }

        try {
            this.sourceDir = Objects.requireNonNull(sourceDir, "sourceDir");
            this.outputDir = Objects.requireNonNull(outputDir, "outputDir");
            run();
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        this.sourceDir = null;
        this.outputDir = null;
        this.context.clear();
    }

    private void run() throws Exception {
        ASTParser parser = ASTParser.newParser(AST.JLS10);

        // Set Java version
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(this.sourceCompatibility, options);
        parser.setCompilerOptions(options);

        // Collect processor flags
        int flags = 0;
        for (SourceProcessor processor : this.processors) {
            flags |= processor.getFlags();
        }

        if ((flags & SourceProcessor.FLAG_RESOLVE_BINDINGS) != 0) {
            // Resolve references
            parser.setResolveBindings(true);
            parser.setBindingsRecovery(true);
        }

        // Set environment
        String[] sourcePath = toArray(this.sourcePath.stream());
        parser.setEnvironment(toArray(this.classPath.stream()), sourcePath, getEncodings(sourcePath), true);

        // Walk directory to find source files
        String[] sourceFiles = toArray(Files.walk(this.sourceDir, FileVisitOption.FOLLOW_LINKS)
                .filter(p -> p.getFileName() != null && p.getFileName().toString().endsWith(JAVA_EXTENSION)));

        for (SourceProcessor processor : this.processors) {
            processor.initialize(this);
        }

        // Parse source files
        parser.createASTs(sourceFiles, getEncodings(sourceFiles), EMPTY_STRING_ARRAY, this.requestor, null);

        for (SourceProcessor processor : this.processors) {
            processor.finish(this);
        }
    }

    private SourceContext createContext(String sourceFilePath, CompilationUnit ast) {
        Path sourceFile = Paths.get(sourceFilePath);
        String fileName = sourceFile.getFileName().toString();
        String primaryType = fileName.substring(0, fileName.length() - JAVA_EXTENSION.length());

        if (this.outputDir != null) {
            return new RewriteContext(this, sourceFile, ast, primaryType);
        } else {
            return new SourceContext(this, sourceFile, ast, primaryType);
        }
    }

    void accept(String sourceFilePath, CompilationUnit ast) {
        SourceContext context = createContext(sourceFilePath, ast);

        try {
            context.process(this.processors);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process: " + sourceFilePath, e);
        }
    }

    private static String[] toArray(Stream<Path> stream) {
        return stream.map(Path::toString).toArray(String[]::new);
    }

    // Assume that all files use the same encoding
    private String[] getEncodings(String[] files) {
        if (files.length == 0) {
            return EMPTY_STRING_ARRAY;
        }

        String[] encodings = new String[files.length];
        Arrays.fill(encodings, this.encoding.name());
        return encodings;
    }

    private class Requestor extends FileASTRequestor {

        @Override
        public void acceptAST(String sourceFilePath, CompilationUnit ast) {
            accept(sourceFilePath, ast);
        }

    }

}
