/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package net.example;

import static net.example.pkg.Util.*;

import java.lang.String;
import net.example.newother.AnotherClass;
import net.example.newother.OtherClass;
import java.lang.Exception;

public class ImportTestNew {

    public void test() {
        OtherClass otherClass = new OtherClass();
        AnotherClass anotherClass = new AnotherClass();
    }

}