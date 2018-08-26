/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.minecrell.mercury.extra;

import static net.minecrell.mercury.util.BombeBindings.convertSignature;
import static net.minecrell.mercury.util.BombeBindings.isPackagePrivate;

import me.jamiemansfield.bombe.analysis.InheritanceProvider;
import me.jamiemansfield.bombe.type.signature.MethodSignature;
import me.jamiemansfield.lorenz.MappingSet;
import me.jamiemansfield.lorenz.model.ClassMapping;
import net.minecrell.at.AccessChange;
import net.minecrell.at.AccessTransform;
import net.minecrell.at.AccessTransformSet;
import net.minecrell.at.ModifierChange;
import net.minecrell.mercury.SourceContext;
import net.minecrell.mercury.SourceProcessor;
import net.minecrell.mercury.analysis.MercuryInheritanceProvider;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SimpleName;

import java.util.Objects;

/**
 * Generates access transformers for fields/method that would no longer be
 * accessible when moving classes to other packages.
 */
public final class AccessAnalyzerProcessor implements SourceProcessor {

    private final AccessTransformSet ats;
    private final MappingSet mappings;

    private AccessAnalyzerProcessor(AccessTransformSet ats, MappingSet mappings) {
        this.ats = Objects.requireNonNull(ats, "ats");
        this.mappings = Objects.requireNonNull(mappings, "mappings");
    }

    @Override
    public int getFlags() {
        return FLAG_RESOLVE_BINDINGS;
    }

    @Override
    public void process(SourceContext context) {
        context.getCompilationUnit().accept(new Visitor(context, this.ats, this.mappings));
    }

    private static class Visitor extends ASTVisitor {

        private static final AccessTransform TRANSFORM = AccessTransform.of(AccessChange.PUBLIC, ModifierChange.NONE);

        private final AccessTransformSet ats;
        private final MappingSet mappings;
        private final InheritanceProvider inheritanceProvider;
        private String newPackage;

        private Visitor(SourceContext context, AccessTransformSet ats, MappingSet mappings) {
            this.ats = ats;
            this.mappings = mappings;
            this.inheritanceProvider = MercuryInheritanceProvider.get(context.getMercury());

            this.newPackage = this.mappings.getTopLevelClassMapping(context.getQualifiedPrimaryType())
                    .map(primary -> primary.getDeobfuscatedPackage().replace('/', '.'))
                    .orElse(context.getPackageName());
        }

        private static ITypeBinding resolveBinding(ASTNode node) {
            if (node instanceof AbstractTypeDeclaration) {
                return ((AbstractTypeDeclaration) node).resolveBinding();
            }

            if (node.getNodeType() == ASTNode.ANONYMOUS_CLASS_DECLARATION) {
                return ((AnonymousClassDeclaration) node).resolveBinding();
            }

            return null;
        }

        private static boolean inheritsBinding(ASTNode node, ITypeBinding declaringClass) {
            while (node != null) {
                ITypeBinding parentBinding = resolveBinding(node);
                if (parentBinding != null && parentBinding.isAssignmentCompatible(declaringClass)) {
                    return true;
                }

                node = node.getParent();
            }

            return false;
        }


        private boolean needsTransform(SimpleName node, IBinding binding, ITypeBinding declaringClass) {
            if (declaringClass == null) {
                return false;
            }

            int modifiers = binding.getModifiers();
            if (Modifier.isProtected(modifiers)) {
                if (inheritsBinding(node, declaringClass)) {
                    return false;
                }
            } else if (!isPackagePrivate(modifiers)) {
                return false;
            }

            ClassMapping<?> mapping = this.mappings.getClassMapping(declaringClass.getBinaryName()).orElse(null);

            String packageName;
            if (mapping != null) {
                mapping.complete(this.inheritanceProvider, declaringClass);
                packageName = mapping.getDeobfuscatedPackage().replace('/', '.');
            } else {
                packageName = declaringClass.getPackage().getName();
            }

            return !packageName.equals(this.newPackage);
        }

        private void analyze(SimpleName node, ITypeBinding binding) {
            if (needsTransform(node, binding, binding)) {
                this.ats.getOrCreateClass(binding.getBinaryName()).merge(TRANSFORM);
            }
        }

        private void analyze(SimpleName node, IMethodBinding binding) {
            ITypeBinding declaringClass = binding.getDeclaringClass();
            if (needsTransform(node, binding, declaringClass)) {
                MethodSignature signature = convertSignature(binding);
                this.ats.getOrCreateClass(declaringClass.getBinaryName()).mergeMethod(signature, TRANSFORM);
            }
        }

        private void analyze(SimpleName node, IVariableBinding binding) {
            if (!binding.isField()) {
                return;
            }

            ITypeBinding declaringClass = binding.getDeclaringClass();
            if (needsTransform(node, binding, declaringClass)) {
                this.ats.getOrCreateClass(declaringClass.getBinaryName()).mergeField(binding.getName(), TRANSFORM);
            }
        }

        @Override
        public boolean visit(SimpleName node) {
            IBinding binding = node.resolveBinding();
            if (binding == null) {
                return true;
            }

            switch (binding.getKind()) {
                case IBinding.TYPE:
                    analyze(node, ((ITypeBinding) binding).getErasure());
                    break;
                case IBinding.METHOD:
                    analyze(node, ((IMethodBinding) binding).getMethodDeclaration());
                    break;
                case IBinding.VARIABLE:
                    analyze(node, ((IVariableBinding) binding).getVariableDeclaration());
                    break;
            }

            return true;
        }

    }

}
