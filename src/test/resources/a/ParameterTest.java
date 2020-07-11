/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import java.util.function.BiFunction;
import java.util.function.Function;

import test.ObfClass;

class ParameterTest<T> {

    public void simpleNoopTest(int var0) {}

    public T simpleTestWithGenerics(T var0) { return var0; }

    /**
     * @param var0
     */
    public void simpleTestWithJavadoc(ObfClass var0) {}

    public void simpleTest(int var0) {
        var0++;

        if (var0 < 0) {
            simpleTest(var0);
        }
    }

    public void simpleTest(ObfClass var0) {
        var0.toString();
    }

    // Epic multiply by 10
    public int advancedTest(String var0) {
        Function<String, Integer> function = s -> { return Integer.parseInt(var0 + s); };
        return function.apply("0");
    }

    public String advancedTest0(int i, int b) {
        BiFunction<Integer, Integer, Integer> function = new BiFunction<Integer, Integer, Integer>() {
            final int ii = i;

            @Override
            public Integer apply(Integer integer, Integer integer2) {
                return Integer.toString(integer, integer2);
            }
        };

        return function.apply(i, b);
    }
}
