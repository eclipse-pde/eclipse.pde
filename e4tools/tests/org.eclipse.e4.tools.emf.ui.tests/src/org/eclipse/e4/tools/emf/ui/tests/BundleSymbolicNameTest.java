/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 ******************************************************************************/

package org.eclipse.e4.tools.emf.ui.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.e4.tools.emf.ui.common.BundleSymbolicName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class BundleSymbolicNameTest {

	private static String parse(String manifest) throws IOException {
		return BundleSymbolicName.parse(new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testPlainName() throws IOException {
		assertEquals("org.example", parse("Manifest-Version: 1.0\nBundle-SymbolicName: org.example\n")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDirectivesAreIgnored() throws IOException {
		assertEquals("org.example", //$NON-NLS-1$
				parse("Manifest-Version: 1.0\nBundle-SymbolicName: org.example;singleton:=true\n")); //$NON-NLS-1$
	}

	@Test
	public void testWrappedHeader() throws IOException {
		assertEquals("org.example.with.a.long.name", //$NON-NLS-1$
				parse("Manifest-Version: 1.0\nBundle-SymbolicName: org.example.with.a.long.name;\n singleton:=true\n")); //$NON-NLS-1$
		assertEquals("org.example.with.a.long.name", //$NON-NLS-1$
				parse("Manifest-Version: 1.0\nBundle-SymbolicName: org.example.with.a.\n long.name;singleton:=true\n")); //$NON-NLS-1$
	}

	@Test
	public void testLastLineWithoutNewline() throws IOException {
		assertEquals("org.example", parse("Manifest-Version: 1.0\nBundle-SymbolicName: org.example;singleton:=true")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSimilarHeadersAreNotMistaken() throws IOException {
		assertEquals("org.example", //$NON-NLS-1$
				parse("Manifest-Version: 1.0\nBundle-SymbolicNameAlias: other\nBundle-SymbolicName: org.example\n")); //$NON-NLS-1$
	}

	@Test
	public void testMissingHeader() throws IOException {
		assertNull(parse("Manifest-Version: 1.0\nBundle-Name: Example\n")); //$NON-NLS-1$
	}

	@Test
	public void testFromHeader() {
		assertEquals("a.b", BundleSymbolicName.fromHeader("a.b;singleton:=true")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("a.b", BundleSymbolicName.fromHeader(" a.b ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(BundleSymbolicName.fromHeader(null));
	}

	@Test
	public void testBundleDirectory(@TempDir Path dir) throws IOException {
		assertNull(BundleSymbolicName.of(dir.toFile()));

		Path metaInf = Files.createDirectories(dir.resolve("META-INF")); //$NON-NLS-1$
		Files.writeString(metaInf.resolve("MANIFEST.MF"), //$NON-NLS-1$
				"Manifest-Version: 1.0\nBundle-SymbolicName: org.example;\n singleton:=true\n"); //$NON-NLS-1$
		assertEquals("org.example", BundleSymbolicName.of(dir.toFile())); //$NON-NLS-1$
		assertNull(BundleSymbolicName.of(new File(dir.toFile(), "missing"))); //$NON-NLS-1$
	}
}
