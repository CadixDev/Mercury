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

import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.mercury.RewriteContext;
import org.cadixdev.mercury.SourceRewriter;

import java.util.Objects;

public final class MercuryRemapper implements SourceRewriter {

    public static SourceRewriter create(MappingSet mappings) {
        return new MercuryRemapper(mappings, false);
    }

    public static SourceRewriter createSimple(MappingSet mappings) {
        return new MercuryRemapper(mappings, true);
    }

    private final MappingSet mappings;
    private final boolean simple;

    private MercuryRemapper(MappingSet mappings, boolean simple) {
        this.mappings = Objects.requireNonNull(mappings, "mappings");
        this.simple = simple;
    }

    @Override
    public int getFlags() {
        return FLAG_RESOLVE_BINDINGS;
    }

    @Override
    public void rewrite(RewriteContext context) {
        context.getCompilationUnit().accept(
                this.simple ? new SimpleRemapperVisitor(context, this.mappings) : new RemapperVisitor(context, this.mappings));
    }

}
