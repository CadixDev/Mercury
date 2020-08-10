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

public class Test {
    public void testMethod() {
        new Object() { // Test$1 - this is the normal case
            public void name(int first, int second) {}
            public int field; // This comes from Test$2
        };
        new Object() { // Test$2 - here we match based on Test$3 (what this class maps to)
            public void name(boolean first, boolean second) {}
        };
        new Object() { // Test$3 - here we match based on Test$2 (which this class maps from)
            public void name(double first, double second) {}
        };
    }
}
