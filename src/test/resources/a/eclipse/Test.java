/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package eclipse;

import java.util.function.Supplier;

// Eclipse Bug 564263
public class Test {
    private String text;

    public class LambaError {
        public LambaError(Supplier<String> message) {}
    }

    public class Broken extends LambaError {
        public Broken(String message) {
            super(() -> Test.this.text);
        }
    }
}
