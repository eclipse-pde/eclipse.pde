/*******************************************************************************
 * Copyright (c) Mar 26, 2014 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.junit.AfterClass;
import org.junit.Test;

/**
 * Tests reading JJava 8 classfiles and extracting specific references
 *
 * @since 1.0.400
 */
public class Java8ClassfileScannerTests extends ScannerTest {

	private static IPath WORKSPACE_ROOT = TestSuiteHelper.getPluginDirectoryPath().append("test_classes_workspace_java8"); //$NON-NLS-1$
	private static IPath ROOT_PATH = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("invokedynamic"); //$NON-NLS-1$ //$NON-NLS-2$

	@Override
	protected IPath getWorkspaceRoot() {
		return WORKSPACE_ROOT;
	}

	@Override
	protected IPath getSourcePath() {
		return ROOT_PATH;
	}

	@Override
	protected String getPackageName() {
		return "invokedynamic"; //$NON-NLS-1$
	}

	@Override
	protected boolean doCompile() {
		boolean result = true;
		String[] sourceFilePaths = new String[] { ROOT_PATH.toOSString() };
		result &= TestSuiteHelper.compile(sourceFilePaths, WORKSPACE_ROOT.toOSString(), TestSuiteHelper.getCompilerOptions());
		assertTrue("working directory should compile", result);  //$NON-NLS-1$
		return true;
	}

	/**
	 * Tests getting an invoke dynamic ref for a static method ref
	 *
	 * @throws Exception
	 */
	@Test
	public void testStaticMethodRef() throws Exception {
		List<IReference> refs = getRefSet("test1"); //$NON-NLS-1$
		IReference ref = findMemberReference("invokedynamic.test1", "m1", "invokedynamic.test1$MR", "mrCompare", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("There should be a ref for invokedynamic.test1$MR#mrCompare", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests getting an invoke dynamic ref for an instance method ref
	 *
	 * @throws Exception
	 */
	@Test
	public void testInstanceMethodRef() throws Exception {
		List<IReference> refs = getRefSet("test2"); //$NON-NLS-1$
		IReference ref = findMemberReference("invokedynamic.test2", "m1", "invokedynamic.test2$MR", "mrCompare", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("There should be a ref for invokedynamic.test2$MR#mrCompare", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests an invoke dynamic reference to an instance method of an arbitrary
	 * object
	 *
	 * @throws Exception
	 */
	@Test
	public void testArbitraryObjectMethodRef() throws Exception {
		List<IReference> refs = getRefSet("test3"); //$NON-NLS-1$
		IReference ref = findMemberReference("invokedynamic.test3", "m1", "java.lang.String", "compareToIgnoreCase", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("There should be a ref for String#compareToIgnoreCase", ref != null); //$NON-NLS-1$
	}

	/**
	 * Tests an invoke dynamic reference to a constructor method ref
	 *
	 * @throws Exception
	 */
	@Test
	public void testConstructorMethodRef() throws Exception {
		List<IReference> refs = getRefSet("test4"); //$NON-NLS-1$
		IReference ref = findMemberReference("invokedynamic.test4", "m1", "java.util.HashSet", "<init>", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("There should be a ref for HashSet#<init>", ref != null); //$NON-NLS-1$
	}

	/**
	 * Cleans up after the tests are done. This must be the last test run
	 *
	 * @throws Exception
	 */
	@AfterClass
	public static void testCleanup() throws Exception {
		cleanUp();
		// remove workspace root
		assertTrue(TestSuiteHelper.delete(new File(WORKSPACE_ROOT.toOSString())));
	}
}
