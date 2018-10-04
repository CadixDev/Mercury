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

public interface SourceRewriter extends SourceProcessor {

    void rewrite(RewriteContext context) throws Exception;

    @Override
    default void process(SourceContext context) throws Exception {
        if (context instanceof RewriteContext) {
            rewrite((RewriteContext) context);
        } else {
            throw new IllegalArgumentException("Cannot rewrite without RewriteContext");
        }
    }

}
