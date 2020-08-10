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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

class RemappingTests {

    // Mercury contains the following tests:
    // 1. Simple remaps
    //    This test is used to verify that Mercury can remap simple things:
    //      - Mercury can remap simple classes, fields, and methods
    //      - Mercury will remove package declarations when remapping to the
    //        root package (GH-11)
    //      - Mercury will remap Javadoc references (GH-6)
    // 2. Method overriding and generics
    //    This test is used to verify that Mercury can handle child classes
    //    overriding methods from their parents:
    //      - Mercury will remap methods with their return type raised (GH-14) (currently disabled)
    //      - Mercury can handle generic return types, and parameters (GH-8) (currently disabled)
    // 3. Eclipse Bugs
    //      - https://bugs.eclipse.org/bugs/show_bug.cgi?id=511958 (currently disabled)
    //      - https://bugs.eclipse.org/bugs/show_bug.cgi?id=564263 (currently disabled)
    // 4. Anonymous class remapping
    //    This test verifies we can handle remapping cases for different anonymous class remapping
    //    combinations (GH-31).

    @Test
    void remap() throws Exception {
        final Path tempDir = Files.createTempDirectory("mercury-test");
        final Path in = tempDir.resolve("a");
        final Path out = tempDir.resolve("b");
        Files.createDirectories(in);
        Files.createDirectories(out);

        // Copy our test classes to the temporary directory
        // - Test 1
        this.copy(in, "test/ObfClass.java");
        this.copy(in, "JavadocTest.java");
        // - Test 2
        //this.copy(in, "OverrideChild.java");
        //this.copy(in, "OverrideParent.java");
        // - Test 3
        //this.copy(in, "eclipse/X.java");
        //this.copy(in, "eclipse/Test.java");
        // - Test 4
        this.copy(in, "anon/Test.java");

        // Load our test mappings
        final MappingSet mappings = MappingSet.create();
        try (final MappingsReader reader = MappingFormats.TSRG
                .createReader(RemappingTests.class.getResourceAsStream("/test.tsrg"))) {
            reader.read(mappings);
        }

        // Run Mercury
        final Mercury mercury = new Mercury();
        mercury.getProcessors().add(MercuryRemapper.create(mappings));
        mercury.rewrite(in, out);

        // Check that the output is as expected
        // - Test 1
        this.verify(out, "Core.java");
        this.verify(out, "JavadocTest.java");
        // - Test 2
        //this.verify(out, "OverrideChild.java");
        //this.verify(out, "OverrideParent.java");
        // - Test 3
        //this.verify(out, "eclipse/X.java");
        //this.verify(out, "eclipse/Test.java");
        // - Test 4
        this.verify(out, "anon/Anon.java");

        // Delete the directory
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    void copy(final Path dir, final String file) throws IOException {
        final Path path = dir.resolve(file);

        // Make sure the parent directory exists
        Files.createDirectories(path.getParent());

        // Copy the file to the file system
        Files.copy(
                RemappingTests.class.getResourceAsStream("/a/" + file),
                path,
                StandardCopyOption.REPLACE_EXISTING
        );

        // Finally verify the file exists, to prevent issues later on
        assertTrue(Files.exists(path), file + " failed to copy!");
    }

    void verify(final Path dir, final String file) throws IOException {
        final Path path = dir.resolve(file);

        // First check the path exists
        assertTrue(Files.exists(path), file + " doesn't exists!");

        // Check the file matches the expected output
        final String expected;
        try (final InputStream in = RemappingTests.class.getResourceAsStream("/b/" + file)) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ByteStreams.copy(in, baos);
            expected = baos.toString();
        }
        final String actual = new String(Files.readAllBytes(path));
        assertEquals(expected, actual, "Remapped code for " + file + " does not match expected");
    }

}
