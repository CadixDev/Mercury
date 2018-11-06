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

import static org.cadixdev.mercury.Mercury.JAVA_EXTENSION;

import org.cadixdev.mercury.jdt.rewrite.imports.ImportRewrite;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

public final class RewriteContext extends SourceContext {

    private TextEdit edit;
    private ASTRewrite rewrite;
    private ImportRewrite importRewrite;

    RewriteContext(Mercury mercury, Path sourceFile, CompilationUnit compilationUnit, String primaryType) {
        super(mercury, sourceFile, compilationUnit, primaryType);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName != null ? packageName : "";
    }

    public void setPrimaryType(String primaryType) {
        this.primaryType = Objects.requireNonNull(primaryType, "primaryType");
    }

    public Optional<ASTRewrite> getASTRewrite() {
        return Optional.ofNullable(this.rewrite);
    }

    public ASTRewrite createASTRewrite() {
        if (this.rewrite == null) {
            this.rewrite = ASTRewrite.create(getCompilationUnit().getAST());
        }
        return this.rewrite;
    }

    public ImportRewrite createImportRewrite() {
        if (this.importRewrite == null) {
            this.importRewrite = ImportRewrite.create(getCompilationUnit(), true);
        }
        return this.importRewrite;
    }

    public void addEdit(TextEdit edit) {
        if (this.edit == null) {
            this.edit = new MultiTextEdit();
        }

        this.edit.addChild(Objects.requireNonNull(edit, "edit"));
    }

    private TextEdit rewrite() throws CoreException, IOException {
        if (this.rewrite == null && this.importRewrite == null && this.edit == null) {
            return null;
        }

        TextEdit edit = null;
        if (this.rewrite != null) {
            edit = this.rewrite.rewriteAST(loadDocument(), null);
        }

        if (this.importRewrite != null) {
            edit = combineEdit(edit, this.importRewrite.rewriteImports(loadDocument(), null));
        }

        return combineEdit(edit, this.edit);
    }

    private static TextEdit combineEdit(TextEdit before, TextEdit edit) {
        if (before == null) {
            return edit;
        }
        if (edit != null) {
            before.addChild(edit);
        }
        return before;
    }

    @Override
    void process(List<SourceProcessor> processors) throws Exception {
        super.process(processors);

        Path outputDir = getMercury().getOutputDir();

        String path = this.primaryType + JAVA_EXTENSION;
        if (!this.packageName.isEmpty()) {
            StringJoiner joiner = new StringJoiner(outputDir.getFileSystem().getSeparator());

            for (String part : this.packageName.split("\\.")) {
                joiner.add(part);
            }
            joiner.add(path);

            path = joiner.toString();
        }

        Path outputFile = outputDir.resolve(path);
        Files.createDirectories(outputFile.getParent());

        TextEdit edit = rewrite();
        if (edit == null) {
            // Copy original source file
            Files.copy(getSourceFile(), outputFile, StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        // Save the rewritten source file
        Document document = loadDocument();
        edit.apply(document, TextEdit.NONE);

        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(outputFile), getMercury().getEncoding())) {
            writer.write(document.get());
        }
    }

}
