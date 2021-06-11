/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.cadixdev.mercury.test;

import org.cadixdev.mercury.Mercury;
import org.eclipse.jdt.core.JavaCore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Java11Tests {

    @Test
    @DisplayName("doesn't remove var keyword in lambda parameter (remapped type)")
    void remapVarKeywordRemappedTypeLambdaParameter() throws Exception {
        new RemapperTest("java-11", Java11Tests::configureMercury)
                .copy("a")
                .register("b", "b")
                .test();
    }
    @Test
    @DisplayName("doesn't remove var keyword in lambda parameter (not remapped type)")
    void remapVarKeywordNotRemappedTypeLambdaParameter() throws Exception {
        new RemapperTest("java-11", Java11Tests::configureMercury)
                .register("c", "c")
                .test();
    }

    static void configureMercury(final Mercury mercury) {
        mercury.setSourceCompatibility(JavaCore.VERSION_11);
    }

}
