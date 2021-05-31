/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

class OverrideChild extends OverrideParent<String> {

    @Override
    public String abc() {
        var result = "Hello, World!";
        return result;
    }

    @Override
    public String bcd() {
        return "Hello, World!";
    }

    @Override
    public void cde(final String abc) {
    }

}
