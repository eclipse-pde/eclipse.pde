/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.search.IReferenceCollection;
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
import org.eclipse.pde.api.tools.internal.util.SinceTagVersion;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.junit.Test;

/**
 * Tests the methods in our utility class: {@link Util}
 *
 * @since 1.0.0
 */
public class UtilTests {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test_source"); //$NON-NLS-1$
	static final IPath SRC_LOC_SEARCH = TestSuiteHelper.getPluginDirectoryPath().append("test-search"); //$NON-NLS-1$

	/**
	 * Tests that passing in <code>null</code> to the getAllFiles(..) method
	 * will return all of the files in that directory
	 */
	public void getAllFilesNullFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists()); //$NON-NLS-1$
		assertTrue("The source location should be a directory", root.isDirectory()); //$NON-NLS-1$
		File[] files = Util.getAllFiles(root, null);
		assertTrue("There should be more than one file in the test source directory", files.length > 1); //$NON-NLS-1$
	}

	/**
	 * Tests that passing an illegal argument when creating a new {@link SinceTagVersion} throws an exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testIllegalArgSinceTagVersion() {
		new SinceTagVersion(null);
	}

	/**
	 * Tests that a filter passed to getALlFiles(..) returns only the files
	 * it is supposed to. In this test only files that end with <code>TestClass1.java</code>
	 * pass the filter (there is only one).
	 */
	public void getAllFilesSpecificFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists()); //$NON-NLS-1$
		assertTrue("The source location should be a directory", root.isDirectory()); //$NON-NLS-1$
		File[] files = Util.getAllFiles(root, pathname -> pathname.getAbsolutePath().endsWith("TestClass1.java")); //$NON-NLS-1$
		assertEquals("There should be only one file in the test source directory named 'TestClass1.java'", files.length, //$NON-NLS-1$
				1);
	}

	/**
	 * Tests that the isClassFile method works as expected when passed a valid name (*.class)
	 */
	@Test
	public void testIsClassfile() {
		assertTrue("Test.class is a class file", Util.isClassFile("Test.class")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the isClassFile method works as expected when passed an invalid name (not *.class)
	 */
	@Test
	public void testIsNotClassfile() {
		assertFalse("Test.notclass is not a classfile", Util.isClassFile("Test.notclass")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the isArchive method works as expected when passed a valid archive
	 * name (*.zip or *.jar)
	 */
	@Test
	public void testIsArchive() {
		assertTrue("Test.zip is an archive", Util.isArchive("Test.zip")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.jar is an archive", Util.isArchive("Test.jar")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.gz")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.tgz")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the isTGZFile method works as expected
	 */
	@Test
	public void testIsTar() {
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.gz")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.TAR.GZ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.Tar.Gz")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.tar.GZ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isTGZFile("Test.TAR.gz")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.tgz")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.TGZ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isTGZFile("Test.Tgz")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the isZipJarFile method works as expected
	 */
	@Test
	public void testIsJar() {
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.zip")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.ZIP")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tar.gz is an archive", Util.isZipJarFile("Test.Zip")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.jar")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.JAR")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Test.tgz is an archive", Util.isZipJarFile("Test.Jar")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the isArchive method works as expected when passed an invalid archive
	 * name (*.notzip)
	 */
	@Test
	public void testisNotArchive() {
		assertFalse("Test.notzip is not an archive", Util.isArchive("Test.notzip")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.getFragmentNumber(String)
	 * org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	@Test
	public void testGetFragmentNumber() {
		assertEquals("wrong value", 2, Util.getFragmentNumber("org.eclipse.core.filesystem 1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong value", 2, Util.getFragmentNumber("1.0")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong value", 3, Util.getFragmentNumber("1.0.0")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong value", 1, Util.getFragmentNumber("1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong value", 4, Util.getFragmentNumber("org.test 1.0.0.0")); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			Util.getFragmentNumber((String) null);
			fail();
		} catch (IllegalArgumentException e) {
			// ignore
		}
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test ")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	@Test
	public void testIsGreatherVersion() {
		assertEquals("wrong value", 1, 1); //$NON-NLS-1$
	}

	@Test
	public void testSinceTagVersion() {
		try {
			new SinceTagVersion(null);
			fail("Should not reach there"); //$NON-NLS-1$
		} catch (IllegalArgumentException e) {
			// expected exception
		}

		SinceTagVersion sinceTagVersion = new SinceTagVersion(" org.eclipse.jdt.core 3.4. test plugin"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", " org.eclipse.jdt.core ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong postfix string", " test plugin", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0."); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4.0"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3.4.0."); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3.4."); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3."); //$NON-NLS-1$
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3"); //$NON-NLS-1$
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertNull("wrong postfix string", sinceTagVersion.postfixString()); //$NON-NLS-1$

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4.0. postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4.0 postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4. postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4 postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3. postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3 postfix"); //$NON-NLS-1$
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("prefix"); //$NON-NLS-1$
		assertNull("wrong version string", sinceTagVersion.getVersionString()); //$NON-NLS-1$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", "prefix", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("test 3.4 protected (was added in 2.1 as private class)"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", "test ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4 protected (was added in 2.1 as private class)"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("abc1.0"); //$NON-NLS-1$
		assertNull("Wrong version string", sinceTagVersion.getVersionString()); //$NON-NLS-1$
		assertNull("Wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("Wrong postfix string", "abc1.0", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("3.4, was added in 3.1 as private method"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("wrong prefix string", sinceTagVersion.prefixString()); //$NON-NLS-1$
		assertEquals("wrong postfix string", ", was added in 3.1 as private method", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$

		sinceTagVersion = new SinceTagVersion("abc1.0, was added in 3.1 as private method"); //$NON-NLS-1$
		assertEquals("wrong version string", "3.1", sinceTagVersion.getVersionString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong prefix string", "abc1.0, was added in ", sinceTagVersion.prefixString()); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("wrong postfix string", " as private method", sinceTagVersion.postfixString()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRegexExcludeList() {
		String line = "R:org\\.eclipse\\.swt[a-zA-Z_0-9\\.]*"; //$NON-NLS-1$
		class LocalApiComponent implements IApiComponent {
			String symbolicName;

			public LocalApiComponent(String symbolicName) {
				this.symbolicName = symbolicName;
			}
			@Override
			public String[] getPackageNames() throws CoreException {
				return null;
			}
			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
				return null;
			}
			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
				return null;
			}
			@Override
			public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
			}
			@Override
			public void close() throws CoreException {
			}
			@Override
			public int getContainerType() {
				return 0;
			}
			@Override
			public String getName() {
				return null;
			}
			@Override
			public int getType() {
				return 0;
			}
			@Override
			public IApiElement getParent() {
				return null;
			}
			@Override
			public IApiElement getAncestor(int ancestorType) {
				return null;
			}
			@Override
			public IApiComponent getApiComponent() {
				return null;
			}
			@Override
			public String getSymbolicName() {
				return this.symbolicName;
			}
			@Override
			public IApiDescription getApiDescription() throws CoreException {
				return null;
			}
			@Override
			public boolean hasApiDescription() {
				return false;
			}
			@Override
			public String getVersion() {
				return null;
			}
			@Override
			public String[] getExecutionEnvironments() throws CoreException {
				return null;
			}
			@Override
			public IApiTypeContainer[] getApiTypeContainers() throws CoreException {
				return null;
			}
			@Override
			public IApiTypeContainer[] getApiTypeContainers(String id) throws CoreException {
				return null;
			}
			@Override
			public IRequiredComponentDescription[] getRequiredComponents() throws CoreException {
				return null;
			}
			@Override
			public String getLocation() {
				return null;
			}
			@Override
			public boolean isSystemComponent() {
				return false;
			}
			@Override
			public boolean isSourceComponent() throws CoreException {
				return false;
			}
			@Override
			public void dispose() {
			}
			@Override
			public IApiBaseline getBaseline() throws CoreException {
				return null;
			}
			@Override
			public IApiFilterStore getFilterStore() throws CoreException {
				return null;
			}
			@Override
			public boolean isFragment() throws CoreException {
				return false;
			}
			@Override
			public IApiComponent getHost() throws CoreException {
				return null;
			}
			@Override
			public boolean hasFragments() throws CoreException {
				return false;
			}
			@Override
			public String[] getLowestEEs() throws CoreException {
				return null;
			}
			@Override
			public ResolverError[] getErrors() throws CoreException {
				return null;
			}
			@Override
			public IElementDescriptor getHandle() {
				return null;
			}
			@Override
			public IReferenceCollection getExternalDependencies() {
				return null;
			}
		}
		List<IApiComponent> allComponents = new ArrayList<>();
		String[] componentNames = new String[] {
				"org.eclipse.swt", //$NON-NLS-1$
				"org.eclipse.equinox.simpleconfigurator.manipulator", //$NON-NLS-1$
				"org.eclipse.team.ui", //$NON-NLS-1$
				"org.eclipse.ecf", //$NON-NLS-1$
				"org.eclipse.core.commands", //$NON-NLS-1$
				"org.eclipse.equinox.util", //$NON-NLS-1$
				"org.eclipse.equinox.p2.jarprocessor", //$NON-NLS-1$
				"org.eclipse.equinox.security", //$NON-NLS-1$
				"org.eclipse.sdk", //$NON-NLS-1$
				"org.eclipse.help.ui", //$NON-NLS-1$
				"org.eclipse.jdt.doc.isv", //$NON-NLS-1$
				"org.eclipse.equinox.p2.core", //$NON-NLS-1$
				"org.eclipse.debug.ui", //$NON-NLS-1$
				"org.eclipse.ui.navigator", //$NON-NLS-1$
				"javax.servlet.jsp", //$NON-NLS-1$
				"org.eclipse.ui.workbench", //$NON-NLS-1$
				"org.eclipse.equinox.event", //$NON-NLS-1$
				"org.eclipse.jdt.core", //$NON-NLS-1$
				"JavaSE-1.7", //$NON-NLS-1$
				"org.apache.commons.codec", //$NON-NLS-1$
				"org.apache.commons.logging", //$NON-NLS-1$
				"org.objectweb.asm", //$NON-NLS-1$
				"org.eclipse.core.filebuffers", //$NON-NLS-1$
				"org.eclipse.jsch.ui", //$NON-NLS-1$
				"org.eclipse.platform", //$NON-NLS-1$
				"org.eclipse.pde.ua.core", //$NON-NLS-1$
				"org.eclipse.help", //$NON-NLS-1$
				"org.eclipse.ecf.provider.filetransfer", //$NON-NLS-1$
				"org.eclipse.equinox.preferences", //$NON-NLS-1$
				"org.eclipse.equinox.p2.reconciler.dropins", //$NON-NLS-1$
				"org.eclipse.team.cvs.ui", //$NON-NLS-1$
				"org.eclipse.equinox.p2.metadata.generator", //$NON-NLS-1$
				"org.eclipse.equinox.registry", //$NON-NLS-1$
				"org.eclipse.update.ui", //$NON-NLS-1$
				"org.eclipse.swt", //$NON-NLS-1$
				"org.eclipse.ui.console", //$NON-NLS-1$
				"org.junit4", //$NON-NLS-1$
				"org.eclipse.ui.views.log", //$NON-NLS-1$
				"org.eclipse.equinox.p2.touchpoint.natives", //$NON-NLS-1$
				"org.eclipse.equinox.ds", //$NON-NLS-1$
				"org.eclipse.help.base", //$NON-NLS-1$
				"org.eclipse.equinox.frameworkadmin.equinox", //$NON-NLS-1$
				"org.eclipse.jdt", //$NON-NLS-1$
				"org.eclipse.osgi.util", //$NON-NLS-1$
				"org.sat4j.pb", //$NON-NLS-1$
				"org.hamcrest.core", //$NON-NLS-1$
				"org.eclipse.jdt.junit4.runtime", //$NON-NLS-1$
				"org.eclipse.equinox.p2.artifact.repository", //$NON-NLS-1$
				"org.eclipse.core.databinding.property", //$NON-NLS-1$
				"org.eclipse.core.databinding", //$NON-NLS-1$
				"org.eclipse.equinox.concurrent", //$NON-NLS-1$
				"org.eclipse.pde.ua.ui", //$NON-NLS-1$
				"org.eclipse.ui.navigator.resources", //$NON-NLS-1$
				"org.eclipse.equinox.http.servlet", //$NON-NLS-1$
				"org.eclipse.jsch.core", //$NON-NLS-1$
				"javax.servlet", //$NON-NLS-1$
				"org.eclipse.jface", //$NON-NLS-1$
				"org.eclipse.equinox.p2.updatesite", //$NON-NLS-1$
				"org.eclipse.jface.databinding", //$NON-NLS-1$
				"org.eclipse.ui.browser", //$NON-NLS-1$
				"org.eclipse.ui", //$NON-NLS-1$
				"org.eclipse.core.databinding.beans", //$NON-NLS-1$
				"org.eclipse.search", //$NON-NLS-1$
				"org.eclipse.equinox.jsp.jasper.registry", //$NON-NLS-1$
				"org.eclipse.jdt.debug", //$NON-NLS-1$
				"org.eclipse.ecf.provider.filetransfer.ssl", //$NON-NLS-1$
				"org.eclipse.platform.doc.isv", //$NON-NLS-1$
				"org.eclipse.pde.api.tools", //$NON-NLS-1$
				"org.eclipse.ui.ide.application", //$NON-NLS-1$
				"org.eclipse.equinox.p2.metadata", //$NON-NLS-1$
				"org.eclipse.equinox.security.win32.x86", //$NON-NLS-1$
				"org.eclipse.core.contenttype", //$NON-NLS-1$
				"org.eclipse.equinox.p2.ui.sdk", //$NON-NLS-1$
				"org.eclipse.core.resources", //$NON-NLS-1$
				"org.eclipse.pde.launching", //$NON-NLS-1$
				"org.eclipse.ui.externaltools", //$NON-NLS-1$
				"org.eclipse.cvs", //$NON-NLS-1$
				"org.eclipse.equinox.p2.repository", //$NON-NLS-1$
				"org.eclipse.core.resources.win32.x86", //$NON-NLS-1$
				"org.eclipse.pde.ui", //$NON-NLS-1$
				"org.eclipse.core.databinding.observable", //$NON-NLS-1$
				"org.eclipse.pde.doc.user", //$NON-NLS-1$
				"org.eclipse.ui.editors", //$NON-NLS-1$
				"org.eclipse.jdt.compiler.tool", //$NON-NLS-1$
				"org.eclipse.jdt.apt.ui", //$NON-NLS-1$
				"org.eclipse.rcp", //$NON-NLS-1$
				"org.eclipse.ui.presentations.r21", //$NON-NLS-1$
				"org.eclipse.pde.runtime", //$NON-NLS-1$
				"org.eclipse.equinox.security.ui", //$NON-NLS-1$
				"org.eclipse.core.jobs", //$NON-NLS-1$
				"org.eclipse.equinox.http.jetty", //$NON-NLS-1$
				"org.eclipse.pde.ds.ui", //$NON-NLS-1$
				"org.apache.lucene.analysis", //$NON-NLS-1$
				"org.eclipse.ui.views", //$NON-NLS-1$
				"org.eclipse.equinox.common", //$NON-NLS-1$
				"org.apache.lucene", //$NON-NLS-1$
				"org.eclipse.ecf.identity", //$NON-NLS-1$
				"org.eclipse.ui.workbench.texteditor", //$NON-NLS-1$
				"org.eclipse.equinox.p2.ui", //$NON-NLS-1$
				"org.eclipse.core.runtime.compatibility.auth", //$NON-NLS-1$
				"org.eclipse.ltk.core.refactoring", //$NON-NLS-1$
				"org.eclipse.ant.core", //$NON-NLS-1$
				"org.eclipse.ant.launching", //$NON-NLS-1$
				"com.jcraft.jsch", //$NON-NLS-1$
				"org.eclipse.ui.win32", //$NON-NLS-1$
				"org.eclipse.pde.core", //$NON-NLS-1$
				"org.eclipse.pde.build", //$NON-NLS-1$
				"org.eclipse.ltk.ui.refactoring", //$NON-NLS-1$
				"org.eclipse.jface.text", //$NON-NLS-1$
				"org.apache.commons.el", //$NON-NLS-1$
				"org.eclipse.compare.win32", //$NON-NLS-1$
				"org.eclipse.core.runtime", //$NON-NLS-1$
				"org.eclipse.jdt.ui", //$NON-NLS-1$
				"org.eclipse.compare", //$NON-NLS-1$
				"org.eclipse.ui.forms", //$NON-NLS-1$
				"org.eclipse.equinox.p2.extensionlocation", //$NON-NLS-1$
				"org.mortbay.jetty.util", //$NON-NLS-1$
				"org.eclipse.equinox.p2.director", //$NON-NLS-1$
				"org.eclipse.core.filesystem", //$NON-NLS-1$
				"org.eclipse.jdt.junit.core", //$NON-NLS-1$
				"org.eclipse.jdt.junit.runtime", //$NON-NLS-1$
				"org.eclipse.team.cvs.ssh2", //$NON-NLS-1$
				"org.eclipse.core.variables", //$NON-NLS-1$
				"org.eclipse.platform.doc.user", //$NON-NLS-1$
				"org.eclipse.equinox.p2.operations", //$NON-NLS-1$
				"org.eclipse.core.externaltools", //$NON-NLS-1$
				"org.eclipse.equinox.simpleconfigurator", //$NON-NLS-1$
				"org.eclipse.equinox.p2.touchpoint.eclipse", //$NON-NLS-1$
				"org.eclipse.equinox.p2.metadata.repository", //$NON-NLS-1$
				"org.eclipse.pde.ds.core", //$NON-NLS-1$
				"org.eclipse.jdt.apt.pluggable.core", //$NON-NLS-1$
				"org.eclipse.team.cvs.core", //$NON-NLS-1$
				"org.mortbay.jetty.server", //$NON-NLS-1$
				"org.eclipse.text", //$NON-NLS-1$
				"org.eclipse.jdt.compiler.apt", //$NON-NLS-1$
				"org.eclipse.equinox.p2.director.app", //$NON-NLS-1$
				"org.eclipse.jdt.debug.ui", //$NON-NLS-1$
				"org.eclipse.equinox.p2.repository.tools", //$NON-NLS-1$
				"org.apache.commons.httpclient", //$NON-NLS-1$
				"org.eclipse.equinox.p2.garbagecollector", //$NON-NLS-1$
				"org.eclipse.ui.ide", //$NON-NLS-1$
				"org.eclipse.equinox.p2.engine", //$NON-NLS-1$
				"org.apache.ant", //$NON-NLS-1$
				"org.eclipse.jdt.junit", //$NON-NLS-1$
				"org.eclipse.ecf.filetransfer", //$NON-NLS-1$
				"org.eclipse.core.filesystem.win32.x86", //$NON-NLS-1$
				"org.eclipse.core.net", //$NON-NLS-1$
				"org.eclipse.equinox.jsp.jasper", //$NON-NLS-1$
				"org.eclipse.equinox.p2.directorywatcher", //$NON-NLS-1$
				"org.eclipse.equinox.http.registry", //$NON-NLS-1$
				"org.junit", //$NON-NLS-1$
				"org.eclipse.pde.junit.runtime", //$NON-NLS-1$
				"org.eclipse.equinox.launcher", //$NON-NLS-1$
				"org.eclipse.jdt.launching", //$NON-NLS-1$
				"org.eclipse.core.expressions", //$NON-NLS-1$
				"org.eclipse.ui.intro", //$NON-NLS-1$
				"org.eclipse.team.core", //$NON-NLS-1$
				"org.eclipse.ui.intro.universal", //$NON-NLS-1$
				"org.eclipse.swt.win32.win32.x86", //$NON-NLS-1$
				"org.eclipse.osgi.services", //$NON-NLS-1$
				"org.eclipse.pde", //$NON-NLS-1$
				"org.eclipse.ui.views.properties.tabbed", //$NON-NLS-1$
				"org.eclipse.core.runtime.compatibility", //$NON-NLS-1$
				"org.eclipse.ant.ui", //$NON-NLS-1$
				"org.eclipse.ecf.provider.filetransfer.httpclient.ssl", //$NON-NLS-1$
				"org.eclipse.equinox.launcher.win32.win32.x86", //$NON-NLS-1$
				"org.eclipse.core.boot", //$NON-NLS-1$
				"org.apache.jasper", //$NON-NLS-1$
				"org.eclipse.help.webapp", //$NON-NLS-1$
				"org.sat4j.core", //$NON-NLS-1$
				"org.eclipse.pde.api.tools.ui", //$NON-NLS-1$
				"org.eclipse.equinox.p2.ui.sdk.scheduler", //$NON-NLS-1$
				"org.eclipse.debug.core", //$NON-NLS-1$
				"org.eclipse.jdt.core.manipulation", //$NON-NLS-1$
				"org.eclipse.osgi", //$NON-NLS-1$
				"org.eclipse.update.scheduler", //$NON-NLS-1$
				"org.eclipse.equinox.p2.updatechecker", //$NON-NLS-1$
				"org.eclipse.equinox.p2.console", //$NON-NLS-1$
				"org.eclipse.equinox.frameworkadmin", //$NON-NLS-1$
				"org.eclipse.compare.core", //$NON-NLS-1$
				"org.eclipse.jdt.apt.core", //$NON-NLS-1$
				"org.eclipse.help.appserver", //$NON-NLS-1$
				"org.eclipse.pde.ui.templates", //$NON-NLS-1$
				"org.eclipse.ecf.ssl", //$NON-NLS-1$
				"org.eclipse.ui.cheatsheets", //$NON-NLS-1$
				"com.ibm.icu", //$NON-NLS-1$
				"org.eclipse.core.net.win32.x86", //$NON-NLS-1$
				"org.eclipse.jdt.doc.user", //$NON-NLS-1$
				"org.eclipse.equinox.app", //$NON-NLS-1$
				"org.eclipse.ui.net", //$NON-NLS-1$
				"org.eclipse.equinox.p2.publisher", //$NON-NLS-1$
				"org.eclipse.ecf.provider.filetransfer.httpclient", //$NON-NLS-1$
		};
		for (int i = 0, max = componentNames.length; i < max; i++) {
			allComponents.add(new LocalApiComponent(componentNames[i]));
		}
		IApiComponent[] components = new IApiComponent[allComponents.size()];
		allComponents.toArray(components);
		FilteredElements excludedElements = new FilteredElements();
		try {
			Util.collectRegexIds(line, excludedElements, components, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals("Wrong size", 2, excludedElements.getPartialMatches().size()); //$NON-NLS-1$
		assertFalse("Wrong result", excludedElements.containsPartialMatch("org.eclipse.jdt.core")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Wrong result", excludedElements.containsExactMatch("org.eclipse.jdt.core")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testRegexExcludeList2() {
		FilteredElements excludedElements = new FilteredElements();
		assertEquals("Wrong size", 0, excludedElements.getPartialMatches().size()); //$NON-NLS-1$
		assertEquals("Wrong size", 0, excludedElements.getExactMatches().size()); //$NON-NLS-1$
		assertFalse("Wrong result", excludedElements.containsPartialMatch("org.eclipse.jdt.core")); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Wrong result", excludedElements.containsExactMatch("org.eclipse.jdt.core")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPluginXmlDecoding() {
		InputStream stream = UtilTests.class.getResourceAsStream("plugin.xml.zip"); //$NON-NLS-1$
		String s = null;
		try (ZipInputStream inputStream = new ZipInputStream(new BufferedInputStream(stream))) {
			ZipEntry zEntry;
			while ((zEntry = inputStream.getNextEntry()) != null) {
				// if it is empty directory, continue
				if (zEntry.isDirectory() || !zEntry.getName().endsWith(".xml")) { //$NON-NLS-1$
					continue;
				}
				s = new String(Util.getInputStreamAsCharArray(inputStream, -1, StandardCharsets.UTF_8));
			}
		} catch (IOException e) {
			// ignore
		}
		assertNotNull("Should not be null", s); //$NON-NLS-1$
		try {
			Util.parseDocument(s);
		} catch(CoreException ce) {
			fail("Should not happen"); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that the utility method for reading in include/exclude regex tests
	 * throws an exception when the file doesn't exist.
	 *
	 * The regex parsing is tested more extensively in
	 * {@link org.eclipse.pde.api.tools.search.tests.SearchEngineTests}
	 *
	 * @throws CoreException
	 */
	@Test(expected = CoreException.class)
	public void testInitializeRegexFilterList() throws CoreException {
		File bogus = new File(SRC_LOC.toFile(), "DOES_NOT_EXIST"); //$NON-NLS-1$
		Util.initializeRegexFilterList(bogus.getAbsolutePath(), null, false);
	}
}
