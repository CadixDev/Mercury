/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.minecrell.mercury.analysis;

import me.jamiemansfield.bombe.analysis.CachingInheritanceProvider;
import me.jamiemansfield.bombe.analysis.InheritanceProvider;
import me.jamiemansfield.bombe.analysis.InheritanceType;
import me.jamiemansfield.bombe.type.signature.FieldSignature;
import me.jamiemansfield.bombe.type.signature.MethodSignature;
import net.minecrell.mercury.Mercury;
import net.minecrell.mercury.util.BombeBindings;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MercuryInheritanceProvider implements InheritanceProvider {

    private final Mercury mercury;

    public static InheritanceProvider get(Mercury mercury) {
        return (InheritanceProvider) mercury.getContext().computeIfAbsent(InheritanceProvider.class,
                i -> new CachingInheritanceProvider(new MercuryInheritanceProvider(mercury)));
    }

    private MercuryInheritanceProvider(Mercury mercury) {
        this.mercury = mercury;
    }

    @Override
    public Optional<ClassInfo> provide(String klass) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Optional<ClassInfo> provide(String klass, Object context) {
        if (context instanceof ITypeBinding) {
            // Avoid looking up binding if it is provided in context
            return Optional.of(provide((ITypeBinding) context));
        } else {
            return provide(klass);
        }
    }

    public ClassInfo provide(ITypeBinding binding) {
        return new BindingClassInfo(binding).lazy();
    }

    private static class BindingClassInfo extends ClassInfo.Abstract {

        private final ITypeBinding binding;

        private BindingClassInfo(ITypeBinding binding) {
            this.binding = binding;
        }

        private static String getInternalName(ITypeBinding binding) {
            return binding.getBinaryName().replace('.', '/');
        }

        @Override
        public String getName() {
            return getInternalName(this.binding);
        }

        @Override
        public boolean isInterface() {
            return this.binding.isInterface();
        }

        @Override
        public String getSuperName() {
            ITypeBinding superClass = this.binding.getSuperclass();
            return superClass != null ? getInternalName(superClass) : "";
        }

        @Override
        public List<String> getInterfaces() {
            return Collections.unmodifiableList(Arrays.stream(this.binding.getInterfaces())
                    .map(BindingClassInfo::getInternalName)
                    .collect(Collectors.toList()));
        }

        @Override
        public Map<FieldSignature, InheritanceType> getFields() {
            return Collections.unmodifiableMap(Arrays.stream(this.binding.getDeclaredFields())
                    .collect(Collectors.toMap(BombeBindings::convertSignature, f -> InheritanceType.fromModifiers(f.getModifiers()))));
        }

        @Override
        public Map<String, InheritanceType> getFieldsByName() {
            return Collections.unmodifiableMap(Arrays.stream(this.binding.getDeclaredFields())
                    .collect(Collectors.toMap(IVariableBinding::getName, f -> InheritanceType.fromModifiers(f.getModifiers()))));
        }

        @Override
        public Map<MethodSignature, InheritanceType> getMethods() {
            return Collections.unmodifiableMap(Arrays.stream(this.binding.getDeclaredMethods())
                    .collect(Collectors.toMap(BombeBindings::convertSignature, m -> InheritanceType.fromModifiers(m.getModifiers()))));
        }

        private void provideParent(InheritanceProvider provider, ITypeBinding parent, Collection<ClassInfo> parents) {
            if (parent == null) {
                return;
            }

            ClassInfo parentInfo = provider.provide(getInternalName(parent), parent).orElse(null);
            if (parentInfo != null) {
                parentInfo.provideParents(provider, parents);
                parents.add(parentInfo);
            }
        }

        @Override
        public void provideParents(InheritanceProvider provider, Collection<ClassInfo> parents) {
            provideParent(provider, this.binding.getSuperclass(), parents);
            for (ITypeBinding iface : this.binding.getInterfaces()) {
                provideParent(provider, iface, parents);
            }
        }

    }

}
