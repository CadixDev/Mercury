/*
 * Copyright (c) 2018 Minecrell (https://github.com/Minecrell)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.minecrell.mercury.util;

import me.jamiemansfield.bombe.type.ArrayType;
import me.jamiemansfield.bombe.type.FieldType;
import me.jamiemansfield.bombe.type.MethodDescriptor;
import me.jamiemansfield.bombe.type.ObjectType;
import me.jamiemansfield.bombe.type.PrimitiveType;
import me.jamiemansfield.bombe.type.Type;
import me.jamiemansfield.bombe.type.signature.FieldSignature;
import me.jamiemansfield.bombe.type.signature.MethodSignature;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Modifier;

import java.util.ArrayList;
import java.util.List;

public final class BombeBindings {

    private BombeBindings() {
    }

    public static boolean isPackagePrivate(int modifiers) {
        return (modifiers & (Modifier.PUBLIC | Modifier.PROTECTED | Modifier.PRIVATE)) == 0;
    }

    public static Type convertType(ITypeBinding binding) {
        if (binding.isPrimitive()) {
            return PrimitiveType.getFromKey(binding.getBinaryName().charAt(0));
        }

        if (binding.isArray()) {
            return new ArrayType(binding.getDimensions(), (FieldType) convertType(binding.getElementType()));
        }

        return new ObjectType(binding.getErasure().getBinaryName());
    }

    public static MethodSignature convertSignature(IMethodBinding binding) {
        ITypeBinding[] parameterBindings = binding.getParameterTypes();
        List<FieldType> parameters = new ArrayList<>(parameterBindings.length);

        for (ITypeBinding parameterBinding : parameterBindings) {
            parameters.add((FieldType) convertType(parameterBinding));
        }

        return new MethodSignature(binding.getName(), new MethodDescriptor(parameters, convertType(binding.getReturnType())));
    }

    public static FieldSignature convertSignature(IVariableBinding binding) {
        return new FieldSignature(binding.getName(), (FieldType) convertType(binding.getType()));
    }

}
