/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.remapper;

import static org.cadixdev.mercury.util.BombeBindings.convertSignature;

import org.cadixdev.bombe.analysis.InheritanceProvider;
import org.cadixdev.bombe.type.signature.FieldSignature;
import org.cadixdev.bombe.type.signature.MethodSignature;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.model.ClassMapping;
import org.cadixdev.lorenz.model.FieldMapping;
import org.cadixdev.lorenz.model.InnerClassMapping;
import org.cadixdev.lorenz.model.MemberMapping;
import org.cadixdev.lorenz.model.MethodMapping;
import org.cadixdev.mercury.RewriteContext;
import org.cadixdev.mercury.analysis.MercuryInheritanceProvider;
import org.cadixdev.mercury.util.GracefulCheck;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Remaps only methods and fields.
 */
class SimpleRemapperVisitor extends ASTVisitor {

    final RewriteContext context;
    final MappingSet mappings;
    private final InheritanceProvider inheritanceProvider;

    SimpleRemapperVisitor(RewriteContext context, MappingSet mappings, boolean javadoc) {
        super(javadoc);
        this.context = context;
        this.mappings = mappings;
        this.inheritanceProvider = MercuryInheritanceProvider.get(context.getMercury());
    }

    final void updateIdentifier(SimpleName node, String newName) {
        if (!node.getIdentifier().equals(newName)) {
            this.context.createASTRewrite().set(node, SimpleName.IDENTIFIER_PROPERTY, newName, null);
        }
    }

    private void remapMethod(SimpleName node, IMethodBinding binding) {
        ITypeBinding declaringClass = binding.getDeclaringClass();
        if (GracefulCheck.checkGracefully(this.context, declaringClass)) {
            return;
        }
        final ClassMapping<?, ?> classMapping = this.mappings.getOrCreateClassMapping(declaringClass.getBinaryName());

        if (binding.isConstructor()) {
            updateIdentifier(node, classMapping.getSimpleDeobfuscatedName());
        } else {
            MethodSignature bindingSignature = convertSignature(binding);
            MethodMapping mapping = findMemberMapping(bindingSignature, classMapping, ClassMapping::getMethodMapping);

            if (mapping == null) {
                classMapping.complete(this.inheritanceProvider, declaringClass);
                mapping = classMapping.getMethodMapping(bindingSignature).orElse(null);
            }

            if (mapping == null) {
                return;
            }

            updateIdentifier(node, mapping.getDeobfuscatedName());
        }
    }

    private void remapField(SimpleName node, IVariableBinding binding) {
        if (!binding.isField()) {
            if (binding.isParameter()) {
                remapParameter(node, binding);
            }

            return;
        }

        ITypeBinding declaringClass = binding.getDeclaringClass();
        if (declaringClass == null) {
            return;
        }

        ClassMapping<?, ?> classMapping = this.mappings.getClassMapping(declaringClass.getBinaryName()).orElse(null);
        if (classMapping == null) {
            return;
        }

        FieldSignature bindingSignature = convertSignature(binding);
        FieldMapping mapping = findMemberMapping(bindingSignature, classMapping, ClassMapping::computeFieldMapping);
        if (mapping == null) {
            return;
        }

        updateIdentifier(node, mapping.getDeobfuscatedName());
    }

    private <T extends MemberMapping<?, ?>, M> T findMemberMapping(
        M matcher,
        ClassMapping<?, ?> classMapping,
        BiFunction<ClassMapping<?, ?>, M, Optional<? extends T>> getMapping
    ) {
        T mapping = getMapping.apply(classMapping, matcher).orElse(null);
        if (mapping != null) {
            return mapping;
        }

        return findMemberMappingAnonClass(matcher, classMapping, getMapping);
    }

    private <T extends MemberMapping<?, ?>, M> T findMemberMappingAnonClass(
        M matcher,
        ClassMapping<?, ?> classMapping,
        BiFunction<ClassMapping<?, ?>, M, Optional<? extends T>> getMapping
    ) {
        // If neither name is different then this method won't do anything
        if (Objects.equals(classMapping.getObfuscatedName(), classMapping.getDeobfuscatedName())) {
            return null;
        }
        // Anonymous classes must be inner classes
        if (!(classMapping instanceof InnerClassMapping)) {
            return null;
        }
        // Verify this is inner class is anonymous
        if (!classMapping.getObfuscatedName().chars().allMatch(Character::isDigit)) {
            return null;
        }
        ClassMapping<?, ?> parentMapping = ((InnerClassMapping) classMapping).getParent();
        if (parentMapping == null) {
            return null;
        }

        // Find a sibling anonymous class whose obfuscated name is our deobfuscated name
        ClassMapping<?, ?> otherClassMapping = parentMapping
                .getInnerClassMapping(classMapping.getDeobfuscatedName()).orElse(null);
        if (otherClassMapping != null) {
            T mapping = getMapping.apply(otherClassMapping, matcher).orElse(null);
            if (mapping != null) {
                return mapping;
            }
        }

        // Find a sibling anonymous class whose deobfuscated name is our obfuscated name
        // We have to do something a little less direct for this case
        for (InnerClassMapping innerClassMapping : parentMapping.getInnerClassMappings()) {
            if (Objects.equals(classMapping.getObfuscatedName(), innerClassMapping.getDeobfuscatedName())) {
                otherClassMapping = innerClassMapping;
                break;
            }
        }
        if (otherClassMapping == null) {
            return null;
        }
        return getMapping.apply(otherClassMapping, matcher).orElse(null);
    }

    private void remapParameter(SimpleName node, IVariableBinding binding) {
        IMethodBinding declaringMethod = binding.getDeclaringMethod();

        if (declaringMethod == null) {
           return;
        }

        int index = -1;

        ASTNode n = context.getCompilationUnit().findDeclaringNode(declaringMethod);

        if (n instanceof MethodDeclaration) {
            MethodDeclaration methodDeclaration = (MethodDeclaration) n;

            // noinspection unchecked
            List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();

            for (int i = 0; i < parameters.size(); i++) {
                if (binding.equals(parameters.get(i).resolveBinding())) {
                    index = i;
                }
            }
        }

        if (index == -1) {
            return;
        }

        int finalIndex = index;
        this.mappings.getClassMapping(declaringMethod.getDeclaringClass().getBinaryName())
                .flatMap(classMapping -> classMapping.getMethodMapping(convertSignature(declaringMethod)))
                .flatMap(methodMapping -> methodMapping.getParameterMapping(finalIndex))
                .ifPresent(parameterMapping -> updateIdentifier(node, parameterMapping.getDeobfuscatedName()));
    }

    protected void visit(SimpleName node, IBinding binding) {
        switch (binding.getKind()) {
            case IBinding.METHOD:
                remapMethod(node, ((IMethodBinding) binding).getMethodDeclaration());
                break;
            case IBinding.VARIABLE:
                remapField(node, ((IVariableBinding) binding).getVariableDeclaration());
                break;
        }
    }

    @Override
    public final boolean visit(SimpleName node) {
        IBinding binding = node.resolveBinding();
        if (binding != null) {
            visit(node, binding);
        }
        return false;
    }

}
