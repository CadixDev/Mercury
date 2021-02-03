/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import test.ObfClass;

class JavadocTest {

    /**
     * The {@link ObfClass core}.
     */
    private final ObfClass core = new ObfClass();

    /**
     * Gets the name of the core.
     *
     * @return The name
     * @see ObfClass#name()
     */
    public String getName() {
        return this.core.name();
    }

    /**
     * {@link test}
     */
    void testNonsense1() {
    }

    /**
     * {@link test.test}
     */
    void testNonsense2() {
    }

}
