/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
	 * Tests that a fully qualified type has its package removed correctly
	 */
	public void testResolvePackageNameFullyQualifiedType() {
		String pname = Util.getPackageName("a.b.c.Type");
		assertEquals("The package name should be 'a.b.c'", "a.b.c", pname);
	}
	
	/**
	 * Tests that passing in a non-fully qualified type returns the empty package
	 */
	public void testResolvePackageNameTypeNoQualification() {
		String pname = Util.getPackageName("Type");
		assertEquals("the default package should be returned", "", pname);
	}
	
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

	/**
	 * Test convert(String descriptor) method
	 */
	public void testConvertDescriptor() {
		assertEquals("Wrong conversion", "(QObject;QException;)V", Util.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"));
		assertEquals("Wrong conversion", "(QObject;QException;)QException;", Util.dequalifySignature("(Ljava/lang/Object;Ljava/lang/Exception;)Ljava/lang/Exception;"));
		assertEquals("Wrong conversion", "(IJCQObject;IJCQException;IJC)I", Util.dequalifySignature("(IJCLjava/lang/Object;IJCLjava/lang/Exception;IJC)I"));
		assertEquals("Wrong conversion", "([IJC[[[QObject;IJCQException;IJC)I", Util.dequalifySignature("([IJC[[[Ljava/lang/Object;IJCLjava/lang/Exception;IJC)I"));
		assertEquals("Wrong conversion", "(QObject;QException;)V", Util.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)V"));
		assertEquals("Wrong conversion", "(QObject;QException;)QException;", Util.dequalifySignature("(Ljava.lang.Object;Ljava.lang.Exception;)Ljava.lang.Exception;"));
		assertEquals("Wrong conversion", "(IJCQObject;IJCQException;IJC)I", Util.dequalifySignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertEquals("Wrong conversion", "([IJC[[[QObject;IJCQException;IJC)I", Util.dequalifySignature("([IJC[[[Ljava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertEquals("Wrong conversion", "(QList;)QList;", Util.dequalifySignature("(Ljava.util.List;)Ljava.util.List;"));
		assertEquals("wrong conversion", "(QList;)QList;", Util.dequalifySignature("(QList;)QList;"));
		assertEquals("wrong converstion", "(QLanguage;)V;", Util.dequalifySignature("(Lfoo.test.Language;)V;"));
		assertEquals("wrong converstion", "(QJokes;)V;", Util.dequalifySignature("(Lfoo.test.Jokes;)V;"));
		assertEquals("wrong conversion", "(QDiff;)Z", Util.dequalifySignature("(LDiff;)Z"));
	}
	
	/**
	 * Tests the isQualifiedSignature method
	 */
	public void testIsQualified() {
		assertTrue("should return true", Util.isQualifiedSignature("(Ljava/lang/Object;Ljava/lang/Exception;)V"));
		assertTrue("should return false", !Util.isQualifiedSignature("(IJCQObject;IJCQException;IJC)I"));
		assertTrue("should return true", Util.isQualifiedSignature("(IJCLjava.lang.Object;IJCLjava.lang.Exception;IJC)I"));
		assertTrue("should return false", !Util.isQualifiedSignature("([IJC[[[QObject;IJCQException;IJC)I"));
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
	}
}
