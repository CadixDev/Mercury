/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.minecrell.mercury;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jface.text.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class SourceContext {

    private final Mercury mercury;

    private final Path sourceFile;
    private final CompilationUnit compilationUnit;

    String packageName;
    String primaryType;

    private Document document;

    SourceContext(Mercury mercury, Path sourceFile, CompilationUnit compilationUnit, String primaryType) {
        this.mercury = mercury;
        this.sourceFile = sourceFile;
        this.compilationUnit = compilationUnit;

        PackageDeclaration packageDeclaration = compilationUnit.getPackage();
        this.packageName = packageDeclaration != null ? packageDeclaration.getName().getFullyQualifiedName() : "";
        this.primaryType = primaryType;
    }

    public final Mercury getMercury() {
        return this.mercury;
    }

    public final Path getSourceFile() {
        return this.sourceFile;
    }

    public final CompilationUnit getCompilationUnit() {
        return this.compilationUnit;
    }

    public final String getPackageName() {
        return this.packageName;
    }

    public final String getPrimaryType() {
        return this.primaryType;
    }

    public final String getQualifiedPrimaryType() {
        if (this.packageName.isEmpty()) {
            return this.primaryType;
        } else {
            return this.packageName + '.' + this.primaryType;
        }
    }

    public final Document loadDocument() throws IOException {
        if (this.document == null) {
            this.document = new Document(new String(Files.readAllBytes(this.sourceFile), StandardCharsets.UTF_8));
        }
        return this.document;
    }

    void process(List<SourceProcessor> processors) throws Exception {
        for (SourceProcessor processor : processors) {
            processor.process(this);
        }
    }

}
