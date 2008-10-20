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
package org.eclipse.pde.api.tools.comparator.tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

public abstract class DeltaTestSetup extends TestCase {
	private static final String TESTS_DELTAS_NAME = "tests-deltas";
	
	protected static final String BUNDLE_NAME = "deltatest";

	private static final String WORKSPACE_NAME = "tests_deltas_workspace";

	private static IPath WORKSPACE_ROOT;

	private static final String BEFORE = "before";
	private static final String AFTER = "after";
	
	private static final IDelta[] EMPTY_CHILDREN = new IDelta[0];

	private static final boolean DEBUG = false;

	static {
		WORKSPACE_ROOT = TestSuiteHelper.getPluginDirectoryPath().append(WORKSPACE_NAME);
	}

	protected void setUp() throws Exception {
		super.setUp();
		// create workspace root
		new File(WORKSPACE_ROOT.toOSString()).mkdirs();
	}
	
	protected void tearDown() throws Exception {
		// remove workspace root
		assertTrue(TestSuiteHelper.delete(new File(WORKSPACE_ROOT.toOSString())));
		super.tearDown();
	}

	public DeltaTestSetup(String name) {
		super(name);
	}

	public abstract String getTestRoot();

	/**
	 * The test name must be the folder name inside the tests-deltas resource folder
	 * <code>name</code> represents either "before" or "after"
	 * 
	 * @param testName the given test name
	 * @param name the given state name
	 */
	private void deployBundle(String testName, String name) {
		String[] sourceFilePaths = new String[] {
				TestSuiteHelper.getPluginDirectoryPath().append(TESTS_DELTAS_NAME).append(getTestRoot()).append(testName).append(name).toOSString()
		};
		IPath destinationPath = WORKSPACE_ROOT.append(name).append(BUNDLE_NAME);
		String[] compilerOptions = TestSuiteHelper.COMPILER_OPTIONS;
		assertTrue(TestSuiteHelper.compile(sourceFilePaths, destinationPath.toOSString(), compilerOptions));
		
		// copy the MANIFEST in the workspace folder
		copyResources(testName, name, destinationPath.toOSString());
	}

	/**
	 * @param testName the given test name
	 * @param name either BEFORE or AFTER
	 * @param destination where to put the resources
	 */
	private void copyResources(String testName, String name, String destination) {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append(TESTS_DELTAS_NAME).append("resources");
		File file = path.toFile();
		File dest = new File(destination);
		TestSuiteHelper.copy(file, dest);

		// check if there is specific local resources to copy
		path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append(TESTS_DELTAS_NAME).append(getTestRoot()).append(testName).append("resources").append(name);
		file = path.toFile();
		if (file.exists()) {
			TestSuiteHelper.copy(file, dest);
			return;
		}

		// check if there is a global local resources to copy
		path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append(TESTS_DELTAS_NAME).append(getTestRoot()).append(testName).append("resources");
		file = path.toFile();
		if (file.exists()) {
			TestSuiteHelper.copy(file, dest);
		}
	}
	
	protected IApiBaseline getBeforeState() {
		IApiBaseline state = null;
		try {
			state = TestSuiteHelper.createTestingProfile(getBaseLineFolder(BEFORE));
		} catch (CoreException e) {
			e.printStackTrace();
			assertTrue("Should not happen", false);
		}
		return state;
	}
	
	protected IApiBaseline getAfterState() {
		IApiBaseline state = null;
		try {
			state = TestSuiteHelper.createTestingProfile(getBaseLineFolder(AFTER));
		} catch (CoreException e) {
			e.printStackTrace();
			assertTrue("Should not happen", false);
		}
		return state;
	}

	private IPath getBaseLineFolder(String name) {
		return new Path(WORKSPACE_NAME).append(name);
	}

	/**
	 * Sort the resulting deltas based first on their kind and then on their flags
	 *
	 * @param delta the given delta
	 * @return the sorted list of children if any, an empty array otherwise
	 */
	protected IDelta[] collectLeaves(IDelta delta) {
		assertTrue("Should not be NO_DELTA", delta != ApiComparator.NO_DELTA);
		List<IDelta> leaves = new ArrayList<IDelta>();
		collect0(delta, leaves);
		int size = leaves.size();
		if (size == 0) return EMPTY_CHILDREN;

		IDelta[] result = new IDelta[size];
		leaves.toArray(result);
		Arrays.sort(result, new Comparator<IDelta>() {
			public int compare(IDelta delta, IDelta delta2) {
				int kind = delta.getKind();
				int kind2 = delta2.getKind();
				if (kind == kind2) {
					int flags = delta.getFlags();
					int flags2 = delta2.getFlags();
					if (flags == flags2) {
						return delta.getKey().compareTo(delta2.getKey());
					}
					return flags - flags2;
				}
				return kind - kind2;
			}
		});
		String unknownMessageStart = BuilderMessages.ApiProblemFactory_problem_message_not_found;
		unknownMessageStart = unknownMessageStart.substring(0, unknownMessageStart.lastIndexOf('{'));
		for (int i = 0, max = result.length; i < max; i++) {
			IDelta leafDelta = result[i];
			String message = leafDelta.getMessage();
			assertNotNull("No message", message);
			if (DEBUG) {
				System.out.println("message : " + message);
			}
			assertFalse("Should not be an unknown message : " + leafDelta, message.startsWith(unknownMessageStart));
		}
		return result;
	}

	private void collect0(IDelta delta, List<IDelta> collect) {
		IDelta[] children = delta.getChildren();
		int length = children.length;
		if (length != 0) {
			for (int i = 0; i < length; i++) {
				collect0(children[i], collect);
			}
		} else {
			collect.add(delta);
		}
	}
	
	protected void deployBundles(String testName) {
		deployBundle(testName, BEFORE);
		deployBundle(testName, AFTER);
	}
}
