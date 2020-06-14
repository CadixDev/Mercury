/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury;

public interface SourceProcessor {

    int FLAG_RESOLVE_BINDINGS = 1 << 0;

    default int getFlags() {
        return 0;
    }

    default void initialize(Mercury mercury) throws Exception {
    }

    void process(SourceContext context) throws Exception;

    default void finish(Mercury mercury) throws Exception {
    }

}
