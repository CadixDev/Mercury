/*
 * Copyright (c) 2018 Cadix Development (https://www.cadixdev.org)
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import java.util.jar.Attributes;

public abstract class NameQualifiedTest {

  public abstract void testQualifiedToUnqualified(final @NonNull Core clazz);

  public abstract void testInnerClass(final Attributes.@NonNull Name name);

  public abstract void testQualifiedInner(final java.nio.file.WatchEvent.@NonNull Kind<?> event)

  public abstract void testFullyQualifed(final java.net.@NonNull URL url);

}
