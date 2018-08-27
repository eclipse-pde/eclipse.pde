/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.builder.tests.ApiTestingEnvironment;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.ApiTestsPlugin;

import junit.framework.Test;

/**
 * Tests for splitting plug-ins across releases
 *
 * @since 1.0
 */
public class BundleMergeSplitTests extends ApiBuilderTest {

	static {
		// TESTS_NUMBERS = new int[] { 9 };
	}
	private static final String API_BASELINE = "API-baseline"; //$NON-NLS-1$
	/**
	 * Workspace relative path
	 */
	protected static String WORKSPACE_ROOT = "mergesplit"; //$NON-NLS-1$

	/**
	 *
	 */
	public static final String WORKSPACE_PROFILE = "post-split"; //$NON-NLS-1$

	public static final String BASELINE = "pre-split"; //$NON-NLS-1$

	IApiBaseline baseline;

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public BundleMergeSplitTests(String name) {
		super(name);
	}

	@Override
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableUnsupportedAnnotationOptions(false);
		enableBaselineOptions(true);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(true);
		enableUsageOptions(false);
		enableVersionNumberOptions(true);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(BundleMergeSplitTests.class);
	}

	/*
	 * Ensure a baseline has been created to compare against.
	 * 
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		ApiTestingEnvironment env = getEnv();
		if (env != null) {
			env.setRevert(true);
		}
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		manager.setDefaultApiBaseline(null);
		manager.removeApiBaseline(API_BASELINE);
		this.baseline.dispose();
		this.baseline = null;
		IApiBaseline wsbaseline = manager.getWorkspaceBaseline();
		if (wsbaseline != null) {
			wsbaseline.dispose();
		}
		IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
		for (int i = 0, length = projects.length; i < length; i++) {
			getEnv().removeProject(projects[i].getFullPath());
		}
		super.tearDown();
		getEnv().setRevert(false);
	}

	/**
	 * Tests that merging a plug-in is compatible with previous release.
	 *
	 * @throws Exception
	 */
	public void test001() throws Exception {
		// setup the environment
		setupTest("test1"); //$NON-NLS-1$
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release.
	 *
	 * @throws Exception
	 */
	public void test002() throws Exception {
		// setup the environment
		setupTest("test2"); //$NON-NLS-1$
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Remove a re-exported type
	 *
	 * @throws Exception
	 */
	public void test003() throws Exception {
		// setup the environment
		setupTest("test3"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[] { "a.b.c.ClassB", "a.b.c_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Adding a re-exported type
	 *
	 * @throws Exception
	 */
	public void test004() throws Exception {
		// setup the environment
		setupTest("test4"); //$NON-NLS-1$
		// no problem expected
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Remove a re-exported type
	 *
	 * @throws Exception
	 */
	public void test005() throws Exception {
		// setup the environment
		setupTest("test5"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[] { "a.b.c.ClassB", "a.b.c_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Remove a re-exported type
	 *
	 * @throws Exception
	 */
	public void test006() throws Exception {
		// setup the environment
		setupTest("test6"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.REEXPORTED_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[] { "a.b.c.ClassB", "a.b.c_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Adding a type
	 *
	 * @throws Exception
	 */
	public void test007() throws Exception {
		// setup the environment
		setupTest("test7"); //$NON-NLS-1$
		// no problem expected
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Remove a re-exported type
	 *
	 * @throws Exception
	 */
	public void test008() throws Exception {
		// setup the environment
		setupTest("test8"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[] { "a.b.c.ClassB", "a.b.c_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release. Remove a re-exported type
	 *
	 * @throws Exception
	 */
	public void test009() throws Exception {
		// setup the environment
		setupTest("test9"); //$NON-NLS-1$
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.API_COMPONENT_ELEMENT_TYPE, IDelta.REMOVED, IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.TYPE, IDelta.REMOVED, IDelta.API_TYPE),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, IElementDescriptor.RESOURCE, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS) };
		setExpectedProblemIds(ids);
		String[][] args = new String[4][];
		args[0] = new String[] { "d.e.f.ClassD", "a.b.c_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[1] = new String[] { "d.e.f.ClassD", "a.b.c.core_1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[2] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		args[3] = new String[] { "1.0.0", "1.0.0" }; //$NON-NLS-1$ //$NON-NLS-2$
		setExpectedMessageArgs(args);
		performMergeSplit();
	}

	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible
	 * with previous release when package changes name, and old types subclass
	 * new types. There should be no since tags errors in this case since the
	 * new types are in new packages and should contain the since tag
	 * corresponding to the new bundle. Since the new bundle is not in the
	 * baseline, the tags cannot be validated.
	 *
	 * @throws Exception
	 */
	public void test010() throws Exception {
		// setup the environment
		setupTest("test10"); //$NON-NLS-1$
		performMergeSplit();
		// no problems should appear in an incremental build either
		IProject project = getEnv().getProject("a.b.c.core"); //$NON-NLS-1$
		IFile file = project.getFile(new Path("src").append("a").append("b").append("c").append("core").append("ClassD.java")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		IPath replacement = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(WORKSPACE_ROOT).append("test10").append("post-changes").append("ClassD.java"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		updateWorkspaceFile(file.getFullPath(), replacement);
		incrementalBuild();
		assertProblems(getEnv().getProblems());
	}

	private void performMergeSplit() throws CoreException {
		cleanBuild();
		fullBuild();
		boolean errors = false;
		int attempts = 1;
		// for some reason we get JDT build errors sometimes... so try again
		// until there are none
		do {
			errors = false;
			IMarker[] jdtMarkers = getEnv().getAllJDTMarkers(getEnv().getWorkspaceRootPath());
			int length = jdtMarkers.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					if (jdtMarkers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR) {
						errors = true;
						break;
					}
				}
			}
			if (errors) {
				attempts++;
				cleanBuild();
				fullBuild();
			}
		} while (errors && attempts < 20);

		expectingNoJDTProblems();
		// problems are now reported on the types from the fragment
		// https://bugs.eclipse.org/bugs/show_bug.cgi?id=289640
		/*
		 * IPath manifestPath = new
		 * Path("a.b.c").append("META-INF").append("MANIFEST.MF");
		 */
		ApiProblem[] problems = getEnv().getProblems();/*
														 * For(manifestPath,
														 * null);
														 */
		assertProblems(problems);
	}

	private void setupTest(String testName) throws Exception {
		// build the baseline if not present
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		String referenceBaselineLocation = getReferenceBaselineLocation(testName);

		// import baseline projects
		createExistingProjects(referenceBaselineLocation, false, true, false);
		cleanBuild();
		fullBuild();
		// create the API baseline
		IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
		int length = projects.length;
		IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append(referenceBaselineLocation);
		for (int i = 0; i < length; i++) {
			IProject currentProject = projects[i];
			IApiComponent component = manager.getWorkspaceComponent(currentProject.getName());
			assertNotNull("The project was not found in the workspace baseline: " + currentProject.getName(), component); //$NON-NLS-1$
			exportApiComponent(currentProject, component, baselineLocation);
		}
		this.baseline = ApiModelFactory.newApiBaseline(API_BASELINE);
		IApiComponent[] components = new IApiComponent[length];
		for (int i = 0; i < length; i++) {
			IProject project = projects[i];
			IPath location = baselineLocation.append(project.getName());
			components[i] = ApiModelFactory.newApiComponent(this.baseline, location.toOSString());
		}
		this.baseline.addApiComponents(components);
		manager.addApiBaseline(this.baseline);
		manager.setDefaultApiBaseline(this.baseline.getName());
		// delete the projects
		for (int i = 0; i < length; i++) {
			getEnv().removeProject(projects[i].getFullPath());
		}
		// populate the workspace with initial plug-ins/projects
		String currentBaselineLocation = getCurrentBaselineLocation(testName);
		createExistingProjects(currentBaselineLocation, false, true, false);
	}

	private String getCurrentBaselineLocation(String testName) {
		return WORKSPACE_ROOT + File.separator + testName + File.separator + WORKSPACE_PROFILE;
	}

	private String getReferenceBaselineLocation(String testName) {
		return WORKSPACE_ROOT + File.separator + testName + File.separator + BASELINE;
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	@Override
	protected IPath getTestSourcePath() {
		return new Path(""); //$NON-NLS-1$
	}

	@Override
	protected String getTestingProjectName() {
		return "bundlemergesplit"; //$NON-NLS-1$
	}

}
