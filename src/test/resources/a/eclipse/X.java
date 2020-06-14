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

import java.util.function.Consumer;

// Eclipse Bug 511958
public class X {

    private final String text = "Bug?";

    public static void main(String[] args) {
        new X().doIt();
    }

    private void doIt() {
        new Sub();
    }

    private class Super<T> {

        public Super(String s) {
        }

        public Super(Consumer<T> consumer) {
        }

    }

    private class Sub extends Super<String> {

        public Sub() {
            super(s -> System.out.println(text));
        }

    }

}
