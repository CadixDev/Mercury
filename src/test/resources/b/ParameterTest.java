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

class ParameterTest<T> {

    public void simpleNoopTest(int i) {}

    public T simpleTestWithGenerics(T t) { return t; }

    /**
     * @param core
     */
    public void simpleTestWithJavadoc(Core core) {}

    public void simpleTest(int i) {
        i++;

        if (i < 0) {
            simpleTest(i);
        }
    }

    public void simpleTest(Core core) {
        core.toString();
    }

    // Epic multiply by 10
    public int advancedTest(String number) {
        Function<String, Integer> function = s -> { return Integer.parseInt(number + s); };
        return function.apply("0");
    }

    public String advancedTest(int number, int radix) {
        BiFunction<Integer, Integer, Integer> function = new BiFunction<Integer, Integer, Integer>() {
            final int ii = number;

            @Override
            public Integer apply(Integer integer, Integer integer2) {
                return Integer.toString(integer, integer2);
            }
        };

        return function.apply(number, radix);
    }
}
