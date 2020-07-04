/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.util;

import org.cadixdev.mercury.SourceContext;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Javadoc;

/**
 * Utility for checking gracefully, based on configuration.
 *
 * @author Jamie Mansfield
 */
public final class GracefulCheck {

    public static boolean checkGracefully(final SourceContext ctx, final ITypeBinding binding) {
        return ctx.getMercury().isGracefulClasspathChecks() && binding.getBinaryName() == null;
    }

    public static boolean isJavadoc(ASTNode node) {
        for (ASTNode current = node; current != null; current = current.getParent()) {
            if (current instanceof Javadoc) {
                return true;
            }
        }

        return false;
    }

    private GracefulCheck() {
    }

}
