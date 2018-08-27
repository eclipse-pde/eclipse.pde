/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;

import junit.framework.Test;

/**
 * Tests that the builder correctly finds manifest version problems
 *
 * @since 1.0
 */
public class VersionTest extends CompatibilityTest {

	/**
	 * Workspace relative path classes in bundle/project A
	 */
	protected static IPath WORKSPACE_CLASSES_PACKAGE_A = new Path("bundle.a/src/a/version"); //$NON-NLS-1$
	protected static IPath WORKSPACE_CLASSES_PACKAGE_INTERNAL = new Path("bundle.a/src/a/version/internal"); //$NON-NLS-1$

	protected static IPath MANIFEST_PATH = new Path("bundle.a").append(JarFile.MANIFEST_NAME); //$NON-NLS-1$

	/**
	 * Package prefix for test classes
	 */
	protected static String PACKAGE_PREFIX = "a.version."; //$NON-NLS-1$

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(VersionTest.class);
	}

	public VersionTest(String name) {
		super(name);
	}

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(true);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("version"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IDelta.MAJOR_VERSION, IApiProblem.NO_FLAGS);
	}

	@Override
	protected String getTestingProjectName() {
		return "enumcompat"; //$NON-NLS-1$
	}

	/**
	 * Tests API addition (minor version change)
	 */
	private void xAddApi(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("AddApi.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("BreakApi.java"); //$NON-NLS-1$
		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
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
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("StableApi.java"); //$NON-NLS-1$
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
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("the instance pref node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
				ApiPlugin.VALUE_ERROR);
		inode.flush();

		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { "1.1.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);

		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists()); //$NON-NLS-1$
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "1.1.0"); //$NON-NLS-1$ //$NON-NLS-2$
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
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The instance pref node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
				ApiPlugin.VALUE_ERROR);
		inode.flush();

		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { "2.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);

		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists()); //$NON-NLS-1$
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "2.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
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
	 * Tests ignore minor version increment with no API addition
	 */
	private void xIgnoreFalseMinorInc(boolean incremental) throws Exception {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("the instance pref node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
				ApiPlugin.VALUE_ENABLED);
		inode.flush();


		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists()); //$NON-NLS-1$
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "1.1.0"); //$NON-NLS-1$ //$NON-NLS-2$
		getEnv().addFile(MANIFEST_PATH.removeLastSegments(1), MANIFEST_PATH.lastSegment(), content);

		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(MANIFEST_PATH, null);
		assertEquals("No problem expected", 0, problems.length); //$NON-NLS-1$
	}

	public void testIgnoreFalseMinorIncI() throws Exception {
		xIgnoreFalseMinorInc(true);
	}

	public void testIgnoreFalseMinorIncF() throws Exception {
		xIgnoreFalseMinorInc(false);
	}

	/**
	 * Tests ignore major version increment with no API breakage
	 */
	private void xIgnoreFalseMajorInc(boolean incremental) throws Exception {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The instance pref node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
				ApiPlugin.VALUE_ENABLED);
		inode.flush();

		// update manifest minor version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists()); //$NON-NLS-1$
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "2.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
		getEnv().addFile(MANIFEST_PATH.removeLastSegments(1), MANIFEST_PATH.lastSegment(), content);

		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(MANIFEST_PATH, null);
		assertEquals("No problem expected", 0, problems.length); //$NON-NLS-1$
	}

	public void testIgnoreFalseMajorIncI() throws Exception {
		xIgnoreFalseMajorInc(true);
	}

	public void testIgnoreFalseMajorIncF() throws Exception {
		xIgnoreFalseMajorInc(false);
	}

	/**
	 * Tests removing a non-API class
	 */
	private void xRemoveInternalClass(boolean incremental) throws Exception {
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_INTERNAL.append("RemoveInternalClass.java"); //$NON-NLS-1$
		// no problems expected
		performDeletionCompatibilityTest(filePath, incremental);
	}

	public void testRemoveInternalClassI() throws Exception {
		xRemoveInternalClass(true);
	}

	public void testRemoveInternalClassF() throws Exception {
		xRemoveInternalClass(false);
	}

	public void testBreakApiRegardlessOfMajorVersionI() throws Exception {
		xRegardlessMajorInc(true);
	}

	public void testBreakApiRegardlessOfMajorVersionF() throws Exception {
		xRegardlessMajorInc(false);
	}

	/**
	 * Tests API breakage still reported when major version increment but
	 * preference set to warn of breakage regardless of major version change is
	 * set.
	 */
	private void xRegardlessMajorInc(boolean incremental) throws Exception {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		assertNotNull("The instance pref node must exist", inode); //$NON-NLS-1$
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
				ApiPlugin.VALUE_ENABLED);
		inode.put(IApiProblemTypes.REPORT_API_BREAKAGE_WHEN_MAJOR_VERSION_INCREMENTED, ApiPlugin.VALUE_ENABLED);
		inode.flush();

		int[] ids = new int[] { ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.REMOVED, IDelta.METHOD) };
		setExpectedProblemIds(ids);
		String[][] args = new String[1][];
		args[0] = new String[] { PACKAGE_PREFIX + "BreakApi", "method()" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);

		// break the API be removing a method
		IPath filePath = WORKSPACE_CLASSES_PACKAGE_A.append("BreakApi.java"); //$NON-NLS-1$
		updateWorkspaceFile(filePath, getUpdateFilePath(filePath.lastSegment()));

		// update manifest major version
		IFile file = getEnv().getWorkspace().getRoot().getFile(MANIFEST_PATH);
		assertTrue("Missing manifest", file.exists()); //$NON-NLS-1$
		String content = Util.getFileContentAsString(file.getLocation().toFile());
		content = content.replace("1.0.0", "2.0.0"); //$NON-NLS-1$ //$NON-NLS-2$
		getEnv().addFile(MANIFEST_PATH.removeLastSegments(1), MANIFEST_PATH.lastSegment(), content);

		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblemsFor(filePath, null);
		assertProblems(problems);
	}
}
