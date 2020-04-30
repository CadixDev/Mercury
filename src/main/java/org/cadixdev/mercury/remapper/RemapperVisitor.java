/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.remapper;

import static org.cadixdev.mercury.util.BombeBindings.isPackagePrivate;

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.Mapping;
import org.cadixdev.lorenz.model.TopLevelClassMapping;
import org.cadixdev.mercury.RewriteContext;
import org.cadixdev.mercury.jdt.rewrite.imports.ImportRewrite;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class RemapperVisitor extends SimpleRemapperVisitor {

    private final ImportRewrite importRewrite;
    private final Deque<ImportContext> importStack = new ArrayDeque<>();

    RemapperVisitor(RewriteContext context, MappingSet mappings) {
        super(context, mappings);

        this.importRewrite = context.createImportRewrite();
        importRewrite.setUseContextToFilterImplicitImports(true);

        TopLevelClassMapping primary = mappings.getTopLevelClassMapping(context.getQualifiedPrimaryType()).orElse(null);
        if (primary != null) {
            context.setPackageName(primary.getDeobfuscatedPackage().replace('/', '.'));
            this.importRewrite.setImplicitPackageName(context.getPackageName());

            String simpleDeobfuscatedName = primary.getSimpleDeobfuscatedName();
            context.setPrimaryType(simpleDeobfuscatedName);

            List<String> implicitTypes = new ArrayList<>();
            String simpleObfuscatedName = primary.getSimpleObfuscatedName();

            @SuppressWarnings("unchecked")
            List<AbstractTypeDeclaration> types = context.getCompilationUnit().types();
            for (AbstractTypeDeclaration type : types) {
                String name = type.getName().getIdentifier();
                if (name.equals(simpleObfuscatedName)) {
                    implicitTypes.add(simpleDeobfuscatedName);
                } else {
                    implicitTypes.add(mappings.getTopLevelClassMapping(context.getPackageName() + '.' + name)
                        .map(Mapping::getSimpleDeobfuscatedName)
                        .orElse(name));
                }
            }
            this.importRewrite.setImplicitTypes(implicitTypes);
        }
    }

    private void remapType(SimpleName node, ITypeBinding binding) {
        if (binding.isTypeVariable()) {
            return;
        }

        ClassMapping<?, ?> mapping = this.mappings.computeClassMapping(binding.getBinaryName()).orElse(null);

        if (node.getParent() instanceof AbstractTypeDeclaration || binding.isLocal()) {
            if (mapping != null) {
                updateIdentifier(node, mapping.getSimpleDeobfuscatedName());
            }
            return;
        }

        String qualifiedName = (mapping != null ? mapping.getFullDeobfuscatedName().replace('/', '.') : binding.getBinaryName()).replace('$', '.');
        String newName = this.importRewrite.addImport(qualifiedName, this.importStack.peek());

        if (!node.getIdentifier().equals(newName)) {
            if (newName.indexOf('.') == -1) {
                this.context.createASTRewrite().set(node, SimpleName.IDENTIFIER_PROPERTY, newName, null);
            } else {
                // Qualified name
                this.context.createASTRewrite().replace(node, node.getAST().newName(newName), null);
            }
        }
    }

    private void remapQualifiedType(QualifiedName node, ITypeBinding binding) {
        String binaryName = binding.getBinaryName();
        TopLevelClassMapping mapping = this.mappings.getTopLevelClassMapping(binaryName).orElse(null);

        if (mapping == null) {
            return;
        }

        String newName = mapping.getDeobfuscatedName().replace('/', '.');
        if (binaryName.equals(newName)) {
            return;
        }

        this.context.createASTRewrite().replace(node, node.getAST().newName(newName), null);
    }

    private void remapInnerType(QualifiedName qualifiedName, ITypeBinding outerClass) {
        ClassMapping<?, ?> outerClassMapping = this.mappings.computeClassMapping(outerClass.getBinaryName()).orElse(null);
        if (outerClassMapping == null) {
            return;
        }

        SimpleName node = qualifiedName.getName();
        InnerClassMapping mapping = outerClassMapping.getInnerClassMapping(node.getIdentifier()).orElse(null);
        if (mapping == null) {
            return;
        }

        updateIdentifier(node, mapping.getDeobfuscatedName());
    }

    @Override
    protected void visit(SimpleName node, IBinding binding) {
        switch (binding.getKind()) {
            case IBinding.TYPE:
                remapType(node, (ITypeBinding) binding);
                break;
            case IBinding.METHOD:
            case IBinding.VARIABLE:
                super.visit(node, binding);
                break;
            case IBinding.PACKAGE:
                // This is ignored because it should be covered by separate handling
                // of QualifiedName (for full-qualified class references),
                // PackageDeclaration and ImportDeclaration
            default:
                throw new IllegalStateException("Unhandled binding: " + binding.getClass().getSimpleName() + " (" + binding.getKind() + ')');
        }
    }

    @Override
    public boolean visit(QualifiedName node) {
        IBinding binding = node.resolveBinding();
        if (binding.getKind() != IBinding.TYPE) {
            // Unpack the qualified name and remap method/field and type separately
            return true;
        }

        Name qualifier = node.getQualifier();
        IBinding qualifierBinding = qualifier.resolveBinding();
        switch (qualifierBinding.getKind()) {
            case IBinding.PACKAGE:
                // Remap full qualified type
                remapQualifiedType(node, (ITypeBinding) binding);
                break;
            case IBinding.TYPE:
                // Remap inner type separately
                remapInnerType(node, (ITypeBinding) qualifierBinding);

                // Remap the qualifier
                qualifier.accept(this);
                break;
            default:
                throw new IllegalStateException("Unexpected qualifier binding: " + binding.getClass().getSimpleName() + " (" + binding.getKind() + ')');
        }

        return false;
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        String currentPackage = node.getName().getFullyQualifiedName();

        if (this.context.getPackageName().length() == 0) {
           this.context.createASTRewrite().remove(node, null); // no package name -> no package declaration!
        } else if (!currentPackage.equals(this.context.getPackageName())) {
            this.context.createASTRewrite().replace(node.getName(), node.getAST().newName(this.context.getPackageName()), null);
        }

        return false;
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        if (node.isStatic()) {
            // Remap class/member reference separately
            return true;
        }

        IBinding binding = node.resolveBinding();
        if (binding != null) {
            switch (binding.getKind()) {
                case IBinding.TYPE:
                    ITypeBinding typeBinding = (ITypeBinding) binding;
                    String name = typeBinding.getBinaryName();
                    ClassMapping<?, ?> mapping = this.mappings.computeClassMapping(name).orElse(null);
                    if (mapping != null && !name.equals(mapping.getFullDeobfuscatedName().replace('/', '.'))) {
                        this.importRewrite.removeImport(typeBinding.getQualifiedName());
                    }

                    break;
            }
        }
        return false;
    }

    private void pushImportContext(ITypeBinding binding) {
        ImportContext context = new ImportContext(this.importRewrite.getDefaultImportRewriteContext(), this.importStack.peek());
        collectImportContext(context, binding);
        this.importStack.push(context);
    }

    private void collectImportContext(ImportContext context, ITypeBinding binding) {
        if (binding == null) {
            return;
        }

        // Names from inner classes
        for (ITypeBinding inner : binding.getDeclaredTypes()) {
            int modifiers = inner.getModifiers();
            if (Modifier.isPrivate(modifiers)) {
                // Inner type must be declared in this compilation unit
                if (this.context.getCompilationUnit().findDeclaringNode(inner) == null) {
                    continue;
                }
            }

            ClassMapping<?, ?> mapping = this.mappings.getClassMapping(inner.getBinaryName()).orElse(null);

            if (isPackagePrivate(modifiers)) {
                // Must come from the same package
                String packageName = mapping != null ? mapping.getDeobfuscatedPackage() : inner.getPackage().getName();
                if (packageName.equals(this.context.getPackageName())) {
                    continue;
                }
            }

            String simpleName;
            String qualifiedName;
            if (mapping != null) {
                simpleName = mapping.getSimpleDeobfuscatedName();
                qualifiedName = mapping.getFullDeobfuscatedName().replace('/', '.').replace('$', '.');
            } else {
                simpleName = inner.getName();
                qualifiedName = inner.getBinaryName().replace('$', '.');
            }

            if (!context.conflicts.contains(simpleName)) {
                String current = context.implicit.putIfAbsent(simpleName, qualifiedName);
                if (current != null && !current.equals(qualifiedName)) {
                    context.implicit.remove(simpleName);
                    context.conflicts.add(simpleName);
                }
            }
        }

        // Inherited names
        collectImportContext(context, binding.getSuperclass());
        for (ITypeBinding parent : binding.getInterfaces()) {
            collectImportContext(context, parent);
        }
    }

    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        pushImportContext(node.resolveBinding());
        return true;
    }

    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        pushImportContext(node.resolveBinding());
        return true;
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        pushImportContext(node.resolveBinding());
        return true;
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        pushImportContext(node.resolveBinding());
        return true;
    }

    @Override
    public void endVisit(AnnotationTypeDeclaration node) {
        this.importStack.pop();
    }

    @Override
    public void endVisit(AnonymousClassDeclaration node) {
        this.importStack.pop();
    }

    @Override
    public void endVisit(EnumDeclaration node) {
        this.importStack.pop();
    }

    @Override
    public void endVisit(TypeDeclaration node) {
        this.importStack.pop();
    }

    private static class ImportContext extends ImportRewrite.ImportRewriteContext {
        private final ImportRewrite.ImportRewriteContext defaultContext;
        final Map<String, String> implicit;
        final Set<String> conflicts;

        ImportContext(ImportRewrite.ImportRewriteContext defaultContext, ImportContext parent) {
            this.defaultContext = defaultContext;
            if (parent != null) {
                this.implicit = new HashMap<>(parent.implicit);
                this.conflicts = new HashSet<>(parent.conflicts);
            } else {
                this.implicit = new HashMap<>();
                this.conflicts = new HashSet<>();
            }
        }

        @Override
        public int findInContext(String qualifier, String name, int kind) {
            int result = this.defaultContext.findInContext(qualifier, name, kind);
            if (result != RES_NAME_UNKNOWN) {
                return result;
            }

            if (kind == KIND_TYPE) {
                String current = implicit.get(name);
                if (current != null) {
                    return current.equals(qualifier + '.' + name) ? RES_NAME_FOUND : RES_NAME_CONFLICT;
                }

                if (conflicts.contains(name)) {
                    return RES_NAME_CONFLICT;  // TODO
                }
            }

            return RES_NAME_UNKNOWN;
        }
    }

}
