/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import java.util.function.Consumer;

public class b {
    public static void main(final String[] args) {
        using("Demo", (var name) -> {
        });
    }

    private static void using(final String name, final Consumer<String> consumer) {
        consumer.accept(name);
    }
}
