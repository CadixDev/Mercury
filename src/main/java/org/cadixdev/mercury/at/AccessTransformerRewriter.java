/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.at;

import static org.cadixdev.mercury.util.BombeBindings.convertSignature;

import org.cadixdev.at.AccessChange;
import org.cadixdev.at.AccessTransform;
import org.cadixdev.at.AccessTransformSet;
import org.cadixdev.at.ModifierChange;
import org.cadixdev.bombe.analysis.InheritanceProvider;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.mercury.RewriteContext;
import org.cadixdev.mercury.SourceRewriter;
import org.cadixdev.mercury.analysis.MercuryInheritanceProvider;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;

import java.util.List;

public final class AccessTransformerRewriter implements SourceRewriter {

    public static SourceRewriter create(AccessTransformSet ats) {
        return new AccessTransformerRewriter(ats);
    }

    private final AccessTransformSet ats;

    private AccessTransformerRewriter(AccessTransformSet ats) {
        this.ats = ats;
    }

    @Override
    public int getFlags() {
        return FLAG_RESOLVE_BINDINGS;
    }

    @Override
    public void rewrite(RewriteContext context) {
        context.getCompilationUnit().accept(new Visitor(context, this.ats));
    }

    private static class Visitor extends ASTVisitor {

        private final RewriteContext context;
        private final AccessTransformSet ats;
        private final InheritanceProvider inheritanceProvider;

        private Visitor(RewriteContext context, AccessTransformSet ats) {
            this.context = context;
            this.ats = ats;
            this.inheritanceProvider = MercuryInheritanceProvider.get(context.getMercury());
        }

        private void transform(BodyDeclaration declaration, AccessTransform transform) {
            if (transform.isEmpty()) {
                return;
            }

            Modifier accessModifier = null;

            AccessChange accessChange = transform.getAccess();
            ModifierChange finalChange = transform.getFinal();

            @SuppressWarnings("unchecked")
            List<IExtendedModifier> modifiers = declaration.modifiers();
            for (IExtendedModifier em : modifiers) {
                if (!em.isModifier()) {
                    continue;
                }

                Modifier m = (Modifier) em;
                int modifier = m.getKeyword().toFlagValue();
                switch (modifier) {
                    case Modifier.PUBLIC:
                    case Modifier.PROTECTED:
                    case Modifier.PRIVATE:
                        switch (accessChange) {
                            case NONE:
                                accessModifier = m;
                                continue;
                            case PACKAGE_PRIVATE:
                                this.context.createASTRewrite().remove(m, null);
                                accessChange = AccessChange.NONE;
                                continue;
                            default:
                                this.context.createASTRewrite().set(m, Modifier.KEYWORD_PROPERTY,
                                        Modifier.ModifierKeyword.fromFlagValue(accessChange.getModifier()), null);
                                accessModifier = m;
                                accessChange = AccessChange.NONE;
                                continue;
                        }
                    case Modifier.FINAL:
                        switch (finalChange) {
                            case REMOVE:
                                this.context.createASTRewrite().remove(m, null);
                                // fallthrough
                            case ADD:
                                finalChange = ModifierChange.NONE;
                                continue;
                            default:
                                continue;
                        }
                }
            }

            if (accessChange == AccessChange.NONE && finalChange == ModifierChange.NONE) {
                return;
            }

            ListRewrite rewrite = this.context.createASTRewrite().getListRewrite(declaration, declaration.getModifiersProperty());
            if (accessChange != AccessChange.NONE) {
                accessModifier = declaration.getAST().newModifier(Modifier.ModifierKeyword.fromFlagValue(accessChange.getModifier()));
                rewrite.insertFirst(accessModifier, null);
            }

            if (finalChange != ModifierChange.NONE) {
                Modifier finalModifier = declaration.getAST().newModifier(Modifier.ModifierKeyword.FINAL_KEYWORD);
                if (accessModifier != null) {
                    rewrite.insertAfter(finalModifier, accessModifier, null);
                } else {
                    rewrite.insertFirst(finalModifier, null);
                }
            }
        }

        private AccessTransformSet.Class findClass(ITypeBinding declaringClass) {
            if (declaringClass == null) {
                return null;
            }

            return this.ats.getClass(declaringClass.getBinaryName()).orElse(null);
        }

        private void visitDeclaration(AbstractTypeDeclaration declaration) {
            AccessTransformSet.Class classSet = findClass(declaration.resolveBinding());
            if (classSet != null) {
                transform(declaration, classSet.get());
            }
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            visitDeclaration(node);
            return true;
        }

        @Override
        public boolean visit(EnumDeclaration node) {
            visitDeclaration(node);
            return true;
        }

        @Override
        public boolean visit(AnnotationTypeDeclaration node) {
            visitDeclaration(node);
            return true;
        }

        @Override
        public boolean visit(FieldDeclaration node) {
            AccessTransform transform = AccessTransform.EMPTY;

            @SuppressWarnings("unchecked")
            List<VariableDeclarationFragment> fragments = node.fragments();
            for (VariableDeclarationFragment fragment : fragments) {
                IVariableBinding binding = fragment.resolveBinding();

                AccessTransformSet.Class classSet = findClass(binding.getDeclaringClass());
                if (classSet != null) {
                    transform = transform.merge(classSet.getField(binding.getName()));
                }
            }

            transform(node, transform);
            return true;
        }

        @Override
        public boolean visit(MethodDeclaration node) {
            IMethodBinding binding = node.resolveBinding();
            ITypeBinding declaringClass = binding.getDeclaringClass();
            if (declaringClass == null) {
                return true;
            }

            AccessTransformSet.Class classSet = this.ats.getOrCreateClass(declaringClass.getBinaryName());
            classSet.complete(this.inheritanceProvider, declaringClass);

            MethodSignature signature = convertSignature(binding);
            transform(node, classSet.getMethod(signature));

            return true;
        }

    }

}
