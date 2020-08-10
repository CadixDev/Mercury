/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package anon;

public class Anon {
    public void testMethod() {
        new Object() { // Test$1 - this is the normal case
            public void remapped(int first, int second) {}
            public int abc; // This comes from Test$2
        };
        new Object() { // Test$2 - here we match based on Test$3 (what this class maps to)
            public void remapped(boolean first, boolean second) {}
        };
        new Object() { // Test$3 - here we match based on Test$2 (which this class maps from)
            public void remapped(double first, double second) {}
        };
    }
}
