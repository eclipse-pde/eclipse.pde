/*******************************************************************************
 * Copyright (c) 2020 ArSysOp and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Alexander Fedorov (ArSysOp)
 *******************************************************************************/
package org.eclipse.pde.api.tools.applications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.pde.api.tools.internal.BundleJarFiles;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.Test;

public class BundleJarFilesTest {

	@Test
	public void testMavenInput() throws Exception {
		String expected = "test.bundle.a_1.0.0.jar"; //$NON-NLS-1$
		String path = TestSuiteHelper.getPluginDirectoryPath().append("test-analyzer-1").append(expected) //$NON-NLS-1$
				.toPortableString();
		String content = new StringBuilder().append(System.lineSeparator())//
				.append("p2.eclipse-plugin:test.bundle.a:jar:1.0.0:")// //$NON-NLS-1$
				.append(path)//
				.append(System.lineSeparator()).toString();
		doTest("maven", content, expected); //$NON-NLS-1$
	}

	@Test
	public void testTychoInput() throws Exception {
		String expected = "test.bundle.a_1.0.1.jar"; //$NON-NLS-1$
		String path = TestSuiteHelper.getPluginDirectoryPath().append("test-analyzer-2").append(expected) //$NON-NLS-1$
				.toOSString();
		String content = new StringBuilder().append(System.lineSeparator())//
				.append(path)//
				.append(System.lineSeparator()).toString();
		doTest("tycho", content, expected); //$NON-NLS-1$
	}

	private void doTest(String prefix, String content, String expected) throws IOException {
		File file = File.createTempFile(prefix, "-list"); //$NON-NLS-1$
		file.deleteOnExit();
		Files.writeString(Paths.get(file.getAbsolutePath()), content);
		List<File> list = new BundleJarFiles(file).list();
		assertEquals(1, list.size());
		assertTrue(list.get(0).getAbsolutePath().endsWith(expected));
	}

}
