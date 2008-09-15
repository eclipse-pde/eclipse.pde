/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import java.util.jar.JarFile;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Tests that the builder correctly finds manifest version problems
 * 
 * @since 1.0
 */
public class VersionTest extends CompatibilityTest {
	
	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/version");
	protected static IPath WORKSPACE_CLASSES_PACKAGE_INTERNAL = new Path("bundle.a/src/a/version/internal");
	
	protected static IPath MANIFEST_PATH = new Path("bundle.a").append(JarFile.MANIFEST_NAME);

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.version.";
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(VersionTest.class);
	}
	
	/**
	 * Constructor
	 * @param name
	 */
	public VersionTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("version");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_VERSION,
				IElementDescriptor.T_RESOURCE,
				IDelta.MAJOR_VERSION,
				IApiProblem.NO_FLAGS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "enumcompat";
	}

	/**
	 * Tests API addition (minor version change)
	 */
	private void xAddApi(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddApi.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.T_RESOURCE,
						IApiProblem.MINOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performVersionTest(filePath, incremental);
	}
	
	public void testAddApiI() throws Exception {
		xAddApi(true);
	}	
	
	public void testAddApiF() throws Exception {
		xAddApi(false);
	}
	
	/**
	 * Tests API breakage (major version change)
	 */
	private void xBreakApi(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("BreakApi.java");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.T_RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performVersionTest(filePath, incremental);
	}
	
	public void testBreakApiI() throws Exception {
		xBreakApi(true);
	}	
	
	public void testBreakApiF() throws Exception {
		xBreakApi(false);
	}	
	
	/**
	 * Tests API stability (no change)
	 */
	private void xStableApi(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("StableApi.java");
		// no problems
		performVersionTest(filePath, incremental);
	}
	
	public void testStableApiI() throws Exception {
		xStableApi(true);
	}	
	
	public void testStableApiF() throws Exception {
		xStableApi(false);
	}
	
	/**
	 * Tests unneeded minor version increment with no API addition
	 */
	private void xFalseMinorInc(boolean incremental) throws Exception {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		prefs.setValue(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, ApiPlugin.VALUE_ENABLED);
		ApiPlugin.getDefault().savePluginPreferences();
		
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.T_RESOURCE,
						IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API,
						IApiProblem.NO_FLAGS)
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"1.1.0", "1.0.0"};
		setExpectedMessageArgs(args);
		
		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists());
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "1.1.0");
		getEnv().addFile(MANIFEST_PATH.removeLastSegments(1), MANIFEST_PATH.lastSegment(), content);
		
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(MANIFEST_PATH, null);
		assertProblems(problems);
	}
	
	public void testFalseMinorIncI() throws Exception {
		xFalseMinorInc(true);
	}	
	
	public void testFalseMinorIncF() throws Exception {
		xFalseMinorInc(false);
	}
	
	/**
	 * Tests unneeded major version increment with no API breakage
	 */
	private void xFalseMajorInc(boolean incremental) throws Exception {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		prefs.setValue(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, ApiPlugin.VALUE_ENABLED);
		ApiPlugin.getDefault().savePluginPreferences();
		
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.T_RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE,
						IApiProblem.NO_FLAGS)
			};
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[]{"2.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		
		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists());
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "2.0.0");
		getEnv().addFile(MANIFEST_PATH.removeLastSegments(1), MANIFEST_PATH.lastSegment(), content);
		
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(MANIFEST_PATH, null);
		assertProblems(problems);
	}
	
	public void testFalseMajorIncI() throws Exception {
		xFalseMajorInc(true);
	}	
	
	public void testFalseMajorIncF() throws Exception {
		xFalseMajorInc(false);
	}
	
	/**
	 * Tests removing a non-API class
	 */
	private void xRemoveInternalClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_INTERNAL.append("RemoveInternalClass.java");
		// no problems expected
		performDeletionCompatibilityTest(filePath, incremental);
	}
	
	public void testRemoveInternalClassI() throws Exception {
		// TODO: this test should be uncommented when bug is fixed
		//xRemoveInternalClass(true);
	}	
	
	public void testRemoveInternalClassF() throws Exception {
		xRemoveInternalClass(false);
	}	
}
