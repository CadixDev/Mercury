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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.cadixdev.bombe.util.ByteStreams;
import org.cadixdev.lorenz.MappingSet;
import org.cadixdev.lorenz.io.MappingFormats;
import org.cadixdev.lorenz.io.MappingsReader;
import org.cadixdev.mercury.Mercury;
import org.cadixdev.mercury.remapper.MercuryRemapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RemapperTest {

    private final String name;
    private final Consumer<Mercury> mercuryConfigure;

    private final Path dir;
    private final MappingSet mappings;

    private final Map<String, String> expected = new HashMap<>();

    public RemapperTest(final String name) throws IOException {
        this(name, mercury -> {});
    }

    public RemapperTest(final String name, final Consumer<Mercury> mercuryConfigure) throws IOException {
        this.name = name;
        this.mercuryConfigure = mercuryConfigure;

        // Create temporary directory, as Mercury needs to operate on the actual file
        // system.
        this.dir = Files.createTempDirectory("mercury-test");
        Files.createDirectories(this.dir.resolve("a"));
        Files.createDirectories(this.dir.resolve("b"));

        // Read test mappings
        this.mappings = MappingSet.create();
        try (final MappingsReader reader = MappingFormats.byId("jam")
                .createReader(RemapperTest.class.getResourceAsStream("/" + this.name + "/test.jam"))) {
            reader.read(this.mappings);
        }
    }

    public RemapperTest copy(final String file) throws IOException {
        final Path path = this.dir.resolve("a").resolve(file + ".java");

        // Make sure the parent directory exists
        Files.createDirectories(path.getParent());

        // Copy the file to the file system
        Files.copy(
                RemapperTest.class.getResourceAsStream("/" + this.name + "/a/" + file + ".java"),
                path,
                StandardCopyOption.REPLACE_EXISTING
        );

        // Finally verify the file exists, to prevent issues later on
        assertTrue(Files.exists(path), file + " failed to copy!");

        return this;
    }

    public RemapperTest register(final String a, final String b) throws IOException {
        // Copy to a
        this.copy(a);

        // Register test
        this.expected.put(a + ".java", b + ".java");

        return this;
    }

    public void test() throws Exception {
        final Path in = this.dir.resolve("a");
        final Path out = this.dir.resolve("b");

        final Mercury mercury = new Mercury();
        this.mercuryConfigure.accept(mercury);
        mercury.getProcessors().add(MercuryRemapper.create(this.mappings));
        mercury.rewrite(in, out);

        for (final String file : this.expected.values()) {
            final Path path = out.resolve(file);

            // First check the path exists
            assertTrue(Files.exists(path), file + " doesn't exists!");

            // Check the file matches the expected output
            final String expected;
            try (final InputStream is = RemapperTest.class.getResourceAsStream("/" + this.name + "/b/" + file)) {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteStreams.copy(is, baos);
                expected = baos.toString();
            }
            final String actual = new String(Files.readAllBytes(path));
            assertEquals(expected, actual, "Remapped code for " + file + " does not match expected");
        }
    }

}
