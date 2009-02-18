/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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

/**
 * Tests for splitting plug-ins across releases
 * 
 * @since 1.0
 */
public class BundleMergeSplitTests extends ApiBuilderTest {

	static {
//		TESTS_NUMBERS = new int[] { 9 };
	}
	private static final String API_BASELINE = "API-baseline";
	/**
	 * Workspace relative path
	 */
	protected static String WORKSPACE_ROOT = "mergesplit";

	/**
	 * 
	 */
	public static final String WORKSPACE_PROFILE = "post-split";
	
	public static final String BASELINE = "pre-split";

	IApiBaseline profile;
	IApiBaseline baseline;

	/**
	 * Constructor
	 * @param name
	 */
	public BundleMergeSplitTests(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(true);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(true);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(BundleMergeSplitTests.class);
	}
	
	/* (non-Javadoc)
	 * 
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
		this.profile.dispose();
		this.baseline = null;
		this.profile = null;
		IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
		for (int i = 0, length = projects.length; i < length; i++) {
			getEnv().removeProject(projects[i].getFullPath());
		}
		super.tearDown();
		getEnv().setRevert(false);
	}
	/**
	 * Updates the contents of a workspace file at the specified location (full path),
	 * with the contents of a local file at the given replacement location (absolute path).
	 * 
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	protected void updateWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertTrue("Workspace file does not exist: " + workspaceLocation.toString(), file.exists());
		File replacement = replacementLocation.toFile();
		assertTrue("Replacement file does not exist: " + replacementLocation.toOSString(), replacement.exists());
		FileInputStream stream = new FileInputStream(replacement);
		file.setContents(stream, false, true, null);
		stream.close();
		getEnv().changed(workspaceLocation);
	}
	
	/**
	 * Updates the contents of a workspace file at the specified location (full path),
	 * with the contents of a local file at the given replacement location (absolute path).
	 * 
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	protected void createWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertFalse("Workspace file should not exist: " + workspaceLocation.toString(), file.exists());
		File replacement = replacementLocation.toFile();
		assertTrue("Replacement file does not exist: " + replacementLocation.toOSString(), replacement.exists());
		FileInputStream stream = new FileInputStream(replacement);
		file.create(stream, false, null);
		stream.close();
		getEnv().added(workspaceLocation);
	}
	
	/**
	 * Deletes the workspace file at the specified location (full path).
	 * 
	 * @param workspaceLocation
	 */
	protected void deleteWorkspaceFile(IPath workspaceLocation) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertTrue("Workspace file does not exist: " + workspaceLocation.toString(), file.exists());
		file.delete(false, null);
		getEnv().removed(workspaceLocation);
	}	
	
	/**
	 * Returns a path in the local file system to an updated file based on this tests source path
	 * and filename.
	 * 
	 * @param filename name of file to update
	 * @return path to the file in the local file system
	 */
	protected IPath getUpdateFilePath(String filename) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(filename);
	}
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is updated with a corresponding file from test data. A build is performed
	 * and problems are compared against the expected problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
			updateWorkspaceFile(
					workspaceFile,
					getUpdateFilePath(workspaceFile.lastSegment()));
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			IMarker[] jdtMarkers = getEnv().getAllJDTMarkers(workspaceFile);
			int length = jdtMarkers.length;
			if (length != 0) {
				for (int i = 0; i < length; i++) {
					boolean condition = jdtMarkers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR;
					if (condition) {
						System.err.println("workspace file : " + workspaceFile.toOSString());
						System.err.println(jdtMarkers[i].getAttribute(IMarker.MESSAGE));
					}
					assertFalse("Should not be a JDT error", condition);
				}
			}
			ApiProblem[] problems = getEnv().getProblemsFor(workspaceFile, null);
			assertProblems(problems);
	}
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is updated with a corresponding file from test data. A build is performed
	 * and problems are compared against the expected problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performVersionTest(IPath workspaceFile, boolean incremental) throws Exception {
			updateWorkspaceFile(
					workspaceFile,
					getUpdateFilePath(workspaceFile.lastSegment()));
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			ApiProblem[] problems = getEnv().getProblemsFor(new Path(workspaceFile.segment(0)).append(JarFile.MANIFEST_NAME), null);
			assertProblems(problems);
	}	
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is deleted. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performDeletionCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
			deleteWorkspaceFile(workspaceFile);
			if (incremental) {
				incrementalBuild();
			} else {
				fullBuild();
			}
			ApiProblem[] problems = getEnv().getProblems();
			assertProblems(problems);
	}	
	
	/**
	 * Performs a compatibility test. The workspace file at the specified (full workspace path)
	 * location is created. A build is performed and problems are compared against the expected
	 * problems for the associated resource.
	 * 
	 * @param workspaceFile file to update
	 * @param incremental whether to perform an incremental (<code>true</code>) or
	 * 	full (<code>false</code>) build
	 * @throws Exception
	 */
	protected void performCreationCompatibilityTest(IPath workspaceFile, boolean incremental) throws Exception {
		createWorkspaceFile(
				workspaceFile,
				getUpdateFilePath(workspaceFile.lastSegment()));
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		ApiProblem[] problems = getEnv().getProblems();
		assertProblems(problems);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	@Override
	protected void assertProblems(ApiProblem[] problems) {
		super.assertProblems(problems);
		int[] expectedProblemIds = getExpectedProblemIds();
		int length = problems.length;
		if (expectedProblemIds.length != length) {
			for (int i = 0; i < length; i++) {
				System.err.println(problems[i]);
			}
		}
		assertEquals("Wrong number of problems", expectedProblemIds.length, length);
		String[][] args = getExpectedMessageArgs();
		if (args != null) {
			// compare messages
			Set<String> set = new HashSet<String>();
			for (int i = 0; i < length; i++) {
				set.add(problems[i].getMessage());
			}
			for (int i = 0; i < expectedProblemIds.length; i++) {
				String[] messageArgs = args[i];
				int messageId = ApiProblemFactory.getProblemMessageId(expectedProblemIds[i]);
				String message = ApiProblemFactory.getLocalizedMessage(messageId, messageArgs);
				assertTrue("Missing expected problem: " + message, set.remove(message));
			}
		} else {
			// compare id's
			Set<Integer> set = new HashSet<Integer>();
			for (int i = 0; i < length; i++) {
				set.add(new Integer(problems[i].getProblemId()));
			}
			for (int i = 0; i < expectedProblemIds.length; i++) {
				assertTrue("Missing expected problem: " + expectedProblemIds[i], set.remove(new Integer(expectedProblemIds[i])));
			}
		}
	}
	
	/**
	 * Tests that merging a plug-in is compatible with
	 * previous release.
	 * 
	 * @throws Exception
	 */
	public void test001() throws Exception {
		// setup the environment
		setupTest("test1");
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * 
	 * @throws Exception
	 */
	public void test002() throws Exception {
		// setup the environment
		setupTest("test2");
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Remove a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test003() throws Exception {
		// setup the environment
		setupTest("test3");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{"a.b.c.ClassB", "a.b.c_1.0.0"};
		args[1] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Adding a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test004() throws Exception {
		// setup the environment
		setupTest("test4");
		// no problem expected
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Remove a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test005() throws Exception {
		// setup the environment
		setupTest("test5");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{"a.b.c.ClassB", "a.b.c_1.0.0"};
		args[1] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Remove a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test006() throws Exception {
		// setup the environment
		setupTest("test6");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.REEXPORTED_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{"a.b.c.ClassB", "a.b.c_1.0.0"};
		args[1] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Adding a type
	 * 
	 * @throws Exception
	 */
	public void test007() throws Exception {
		// setup the environment
		setupTest("test7");
		// no problem expected
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Remove a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test008() throws Exception {
		// setup the environment
		setupTest("test8");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{"a.b.c.ClassB", "a.b.c_1.0.0"};
		args[1] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performMergeSplit(false);
	}
	/**
	 * Tests that splitting a plug-in and re-exporting the base is compatible with
	 * previous release.
	 * Remove a re-exported type
	 * 
	 * @throws Exception
	 */
	public void test009() throws Exception {
		// setup the environment
		setupTest("test9");
		int[] ids = new int[] {
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_COMPATIBILITY,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.REMOVED,
						IDelta.REEXPORTED_API_TYPE),
				ApiProblemFactory.createProblemId(
						IApiProblem.CATEGORY_VERSION,
						IElementDescriptor.RESOURCE,
						IApiProblem.MAJOR_VERSION_CHANGE,
						IApiProblem.NO_FLAGS)
		};
		setExpectedProblemIds(ids);
		String[][] args = new String[2][];
		args[0] = new String[]{"d.e.f.ClassD", "a.b.c_1.0.0"};
		args[1] = new String[]{"1.0.0", "1.0.0"};
		setExpectedMessageArgs(args);
		performMergeSplit(false);
	}
	private void performMergeSplit(boolean incremental) throws CoreException {
		if (incremental) {
			incrementalBuild();
		} else {
			fullBuild();
		}
		IMarker[] jdtMarkers = getEnv().getAllJDTMarkers(ResourcesPlugin.getWorkspace().getRoot().getLocation());
		int length = jdtMarkers.length;
		if (length != 0) {
			for (int i = 0; i < length; i++) {
				boolean condition = jdtMarkers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR;
				if (condition) {
					System.err.println(jdtMarkers[i].getAttribute(IMarker.MESSAGE));
				}
				assertFalse("Should not be a JDT error", condition);
			}
		}
		IPath manifestPath = new Path("a.b.c").append("META-INF").append("MANIFEST.MF");
		ApiProblem[] problems = getEnv().getProblemsFor(manifestPath, null);
		assertProblems(problems);
	}
	private void setupTest(String testName) throws Exception {
		// build the baseline if not present
		IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();
		String referenceBaselineLocation = getReferenceBaselineLocation(testName);

		// import baseline projects
		createExistingProjects(referenceBaselineLocation, true, true, false);
		// create the API baseline
		this.profile = manager.getWorkspaceBaseline();
		IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
		int length = projects.length;
		IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append(referenceBaselineLocation);
		for (int i = 0; i < length; i++) {
			IProject currentProject = projects[i];
			exportApiComponent(
					currentProject,
					this.profile.getApiComponent(currentProject.getName()), 
					baselineLocation);
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
		createExistingProjects(currentBaselineLocation, true, true, false);
	}

	private String getCurrentBaselineLocation(String testName) {
		return WORKSPACE_ROOT + File.separator + testName + File.separator + WORKSPACE_PROFILE;
	}

	private String getReferenceBaselineLocation(String testName) {
		return WORKSPACE_ROOT + File.separator + testName + File.separator +  BASELINE;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestSourcePath()
	 */
	@Override
	protected IPath getTestSourcePath() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestingProjectName()
	 */
	@Override
	protected String getTestingProjectName() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
