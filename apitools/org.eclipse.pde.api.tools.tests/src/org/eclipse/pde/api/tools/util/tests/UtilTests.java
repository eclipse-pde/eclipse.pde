/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.util.tests;

import java.io.File;
import java.io.FileFilter;

import junit.framework.TestCase;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.util.SinceTagVersion;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests the methods in our util class: {@link Util}
 * 
 * @since 1.0.0
 */
public class UtilTests extends TestCase {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test_source");
	
	/**
	 * Tests that passing in <code>null</code> to the getAllFiles(..) method
	 * will return all of the files in that directory
	 */
	public void getAllFilesNullFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists());
		assertTrue("The source location should be a directory", root.isDirectory());
		File[] files = Util.getAllFiles(root, null);
		assertTrue("There should be more than one file in the test source directory", files.length > 1);
	}
	
	/**
	 * Tests that passing an illegal argument when creating a new {@link SinceTagVersion} throws an exception
	 */
	public void testIllegalArgSinceTagVersion() {
		try {
			new SinceTagVersion(null);
			fail("creating a since tag version with a null value should have thrown an exception");
		}
		catch(IllegalArgumentException iae) {
			
		}
	}
	
	/**
	 * Tests that a filter passed to getALlFiles(..) returns only the files 
	 * it is supposed to. In this test only files that end with <code>TestClass1.java</code>
	 * pass the filter (there is only one).
	 */
	public void getAllFilesSpecificFilter() {
		File root = new File(SRC_LOC.toOSString());
		assertTrue("The test source  directory must exist", root.exists());
		assertTrue("The source location should be a directory", root.isDirectory());
		File[] files = Util.getAllFiles(root, new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.getAbsolutePath().endsWith("TestClass1.java");
			}
		});
		assertTrue("There should be only one file in the test source directory named 'TestClass1.java'", files.length == 1);
	}
	
	/**
	 * Tests that the isClassFile method works as expected when passed a valid name (*.class)
	 */
	public void testIsClassfile() {
		assertTrue("Test.class is a class file", Util.isClassFile("Test.class"));
	}
	
	/**
	 * Tests that the isClassFile method works as expected when passed an invalid name (not *.class)
	 */
	public void testIsNotClassfile() {
		assertTrue("Test.notclass is not a classfile", !Util.isClassFile("Test.notclass"));
	}
	
	/**
	 * Tests that the isArchive method works as expected when passed a valid archive 
	 * name (*.zip or *.jar)
	 */
	public void testIsArchive() {
		assertTrue("Test.zip is an archive", Util.isArchive("Test.zip"));
		assertTrue("Test.jar is an archive", Util.isArchive("Test.jar"));
	}
	
	/**
	 * Tests that the isArchive method works as expected when passed an invalid archive 
	 * name (*.notzip)
	 */
	public void testisNotArchive() {
		assertTrue("Test.notzip is not an archive", !Util.isArchive("Test.notzip"));
	}
	
	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.getFragmentNumber(String)
	 * org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	public void testGetFragmentNumber() {
		assertEquals("wrong value", 2, Util.getFragmentNumber("org.eclipse.core.filesystem 1.0"));
		assertEquals("wrong value", 2, Util.getFragmentNumber("1.0"));
		assertEquals("wrong value", 3, Util.getFragmentNumber("1.0.0"));
		assertEquals("wrong value", 1, Util.getFragmentNumber("1"));
		assertEquals("wrong value", 4, Util.getFragmentNumber("org.test 1.0.0.0"));
		try {
			Util.getFragmentNumber((String) null);
			assertTrue(false);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test "));
		assertEquals("wrong value", -1, Util.getFragmentNumber("org.test"));
	}

	/*
	 * Test org.eclipse.pde.api.tools.internal.util.Util.isGreatherVersion(String, String)
	 */
	public void testIsGreatherVersion() {
		assertEquals("wrong value", 1, 1);
	}
	
	public void testSinceTagVersion() {
		try {
			new SinceTagVersion(null);
			assertTrue("Should not reach there", false);
		} catch (IllegalArgumentException e) {
			// expected exception
		}

		SinceTagVersion sinceTagVersion = new SinceTagVersion(" org.eclipse.jdt.core 3.4. test plugin");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", " org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " test plugin", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("org.eclipse.jdt.core 3.4.0.qualifier postfix");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "org.eclipse.jdt.core ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0");
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.");
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3");
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertNull("wrong postfix string", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0.qualifier postfix");
		assertEquals("wrong version string", "3.4.0.qualifier", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0. postfix");
		assertEquals("wrong version string", "3.4.0.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4.0 postfix");
		assertEquals("wrong version string", "3.4.0", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4. postfix");
		assertEquals("wrong version string", "3.4.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4 postfix");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3. postfix");
		assertEquals("wrong version string", "3.", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3 postfix");
		assertEquals("wrong version string", "3", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " postfix", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("prefix");
		assertNull("wrong version string", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", "prefix", sinceTagVersion.postfixString());
		
		sinceTagVersion = new SinceTagVersion("test 3.4 protected (was added in 2.1 as private class)");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertEquals("wrong prefix string", "test ", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("3.4 protected (was added in 2.1 as private class)");
		assertEquals("wrong version string", "3.4", sinceTagVersion.getVersionString());
		assertNull("wrong prefix string", sinceTagVersion.prefixString());
		assertEquals("wrong postfix string", " protected (was added in 2.1 as private class)", sinceTagVersion.postfixString());

		sinceTagVersion = new SinceTagVersion("abc1.0");
		assertNull("Wrong version string", sinceTagVersion.getVersionString());
		assertNull("Wrong prefix string", sinceTagVersion.prefixString());
		assertNull("Wrong postfix string", sinceTagVersion.postfixString());
	}
}
