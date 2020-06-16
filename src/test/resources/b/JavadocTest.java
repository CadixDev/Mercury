/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

class JavadocTest {

    /**
     * The {@link Core core}.
     */
    private final Core core = new Core();

    /**
     * Gets the name of the core.
     *
     * @return The name
     * @see Core#firstName()
     */
    public String getName() {
        return this.core.firstName();
    }

}
