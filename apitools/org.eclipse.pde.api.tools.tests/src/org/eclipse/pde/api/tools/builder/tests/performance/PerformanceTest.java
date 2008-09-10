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
package org.eclipse.pde.api.tools.builder.tests.performance;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.jdt.core.tests.util.Util;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.ApiTestsPlugin;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;

/**
 * Base class for performance tests
 * 
 * @since 1.0
 */
public abstract class PerformanceTest extends ApiBuilderTest {	

	/**
	 * Constructor
	 * @param name
	 */
	public PerformanceTest(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("compat");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(true);
		enableBaselineOptions(true);
		enableCompatibilityOptions(true);
		enableLeakOptions(true);
		enableSinceTagOptions(true);
		enableUsageOptions(true);
		enableVersionNumberOptions(true);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			FullSourceBuildTests.class
		};
		return classes;
	}
	
	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder tests...
		Class[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (int i = 0, length = classes.length; i < length; i++) {
			Class clazz = classes[i];
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite", new Class[0]);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(null, new Object[0]);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				continue;
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				continue;
			}
			suite.addTest((Test) test);
		}
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(PerformanceTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/* (non-Javadoc)
	 * 
	 * Ensure a baseline has been created to compare against.
	 * 
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// populate the workspace with initial plug-ins/projects
		createInitialWorkspace();
		IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();
		IApiProfile perfline = manager.getApiProfile("perf-baseline");
		if (perfline == null) {
			// create the API baseline
			IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append("perf-baseline");
			long start = System.currentTimeMillis();
			String fullSourceZipPath = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("perf").append("bin-baseline.zip").toOSString();
			System.out.println("Unzipping "+fullSourceZipPath);
			System.out.print("	in "+baselineLocation.toOSString()+"...");
			Util.unzip(fullSourceZipPath, baselineLocation.toOSString());
			System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");	
				
			perfline = Factory.newApiProfile("perf-baseline");
			File[] files = baselineLocation.toFile().listFiles();
			IApiComponent[] components = new IApiComponent[files.length];
			for (int i = 0; i < files.length; i++) {
				IPath location = baselineLocation.append(files[i].getName());
				components[i] = perfline.newApiComponent(location.toOSString());
			}
			perfline.addApiComponents(components);
			manager.addApiProfile(perfline);
			manager.setDefaultApiProfile(perfline.getName());			
		}
		IApiProfile baseline = manager.getDefaultApiProfile();
		if (baseline != perfline) {
			manager.setDefaultApiProfile(perfline.getName());
		}
	}	
	
	/**
	 * Creates the workspace by importing projects from the source zip. This is the 
	 * initial state of the workspace.
	 *  
	 * @throws Exception
	 */
	protected void createInitialWorkspace() throws Exception {
		// Get wksp info
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = ApiTestsPlugin.getDefault().getStateLocation().append("workspace");
		location.toFile().mkdirs();
		String targetWorkspacePath = location.toOSString();

		// Modify resources workspace preferences to avoid disturbing tests while running them
		IEclipsePreferences resourcesPreferences = new InstanceScope().getNode(ResourcesPlugin.PI_RESOURCES);
		resourcesPreferences.put(ResourcesPlugin.PREF_AUTO_REFRESH, "false");
		workspace.getDescription().setSnapshotInterval(Long.MAX_VALUE);
		workspace.getDescription().setAutoBuilding(false);

		// Get projects directories
		long start = System.currentTimeMillis();
		String fullSourceZipPath = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append("perf").append("source-ws.zip").toOSString();
		System.out.println("Unzipping "+fullSourceZipPath);
		System.out.print("	in "+targetWorkspacePath+"...");
		Util.unzip(fullSourceZipPath, targetWorkspacePath);
		System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");
		
		start = System.currentTimeMillis();
		System.out.println("Importing projects... ");
		File root = location.toFile();
		File[] projects = root.listFiles();
		for (int i = 0; i < projects.length; i++) {
			System.out.println("\t" + projects[i].getName());
			createExistingProject(projects[i]);
		}
		System.out.println(" done in "+(System.currentTimeMillis()-start)+"ms.");
		
	}
	
	/**
	 * Creates a project contained in the given directory.
	 * 
	 * @param projectDir directory containing existing project
	 */
	private void createExistingProject(File projectDir) throws Exception {
		String projectName = projectDir.getName();
		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		final IProject project = workspace.getRoot().getProject(projectName);
		IProjectDescription description = workspace.newProjectDescription(projectName);
		IPath locationPath = new Path(projectDir.getAbsolutePath());
		description.setLocation(locationPath);
		
		// import from file system
		File importSource = null;
		// import project from location copying files - use default project
		// location for this workspace
		URI locationURI = description.getLocationURI();
		// if location is null, project already exists in this location or
		// some error condition occured.
		assertNotNull("project description location is null", locationURI);
		importSource = new File(locationURI);
		IProjectDescription desc = workspace.newProjectDescription(projectName);
		desc.setBuildSpec(description.getBuildSpec());
		desc.setComment(description.getComment());
		desc.setDynamicReferences(description.getDynamicReferences());
		desc.setNatureIds(description.getNatureIds());
		desc.setReferencedProjects(description.getReferencedProjects());
		description = desc;

		project.create(description, null);
		project.open(null);

		// import operation to import project files
		List filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);
		ImportOperation operation = new ImportOperation(
				project.getFullPath(), importSource,
				FileSystemStructureProvider.INSTANCE, new IOverwriteQuery() {
					public String queryOverwrite(String pathString) {
						return IOverwriteQuery.ALL;
					}
				}, filesToImport);
		operation.setOverwriteResources(true);
		operation.setCreateContainerStructure(false);
		operation.run(new NullProgressMonitor());
	}	

	/**
	 * Creates projects in the given archive file.
	 * 
	 * @param zip Archive file
	 */
	protected void createExistingProjects(ZipFile zip) throws Exception {
		IImportStructureProvider provider = new ZipFileStructureProvider(zip);
		ImportOperation operation = new ImportOperation(
				Path.ROOT, zip,
				provider, new IOverwriteQuery() {
					public String queryOverwrite(String pathString) {
						return IOverwriteQuery.ALL;
					}
				}, provider.getChildren(zip));
		operation.setOverwriteResources(true);
		operation.setCreateContainerStructure(false);
		operation.run(new NullProgressMonitor());
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
}
