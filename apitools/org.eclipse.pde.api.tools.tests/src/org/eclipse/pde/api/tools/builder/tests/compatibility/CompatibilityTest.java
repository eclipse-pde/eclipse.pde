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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.ApiTestsPlugin;

/**
 * Base class for binary compatibility tests
 * 
 * @since 1.0
 */
public abstract class CompatibilityTest extends ApiBuilderTest {	

	/**
	 * 
	 */
	private static final String BASELINE = "baseline";

	/**
	 * Constructor
	 * @param name
	 */
	public CompatibilityTest(String name) {
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
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(true);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(false);
		enableVersionNumberOptions(false);
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			BundleCompatibilityTests.class,
			AnnotationCompatibilityTests.class,
			InterfaceCompatibilityTests.class,
			EnumCompatibilityTests.class,
			ClassCompatibilityTests.class,
			FieldCompatibilityTests.class,
			MethodCompatibilityTests.class,
			ConstructorCompatibilityTests.class,
			SinceTagTest.class,
			VersionTest.class
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
		TestSuite suite = new TestSuite(CompatibilityTest.class.getName());
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
		createExistingProjects(BASELINE, true);
		IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();
		IApiProfile baseline = manager.getDefaultApiProfile();
		if (baseline == null) {
			// create the API baseline
			IApiProfile profile = manager.getWorkspaceProfile();
			IProject[] projects = getEnv().getWorkspace().getRoot().getProjects();
			IPath baselineLocation = ApiTestsPlugin.getDefault().getStateLocation().append(BASELINE);
			for (int i = 0; i < projects.length; i++) {
				exportApiComponent(
						projects[i],
						profile.getApiComponent(projects[i].getName()), 
						baselineLocation);
			}
			baseline = Factory.newApiProfile("API-baseline");
			IApiComponent[] components = new IApiComponent[projects.length];
			for (int i = 0; i < projects.length; i++) {
				IProject project = projects[i];
				IPath location = baselineLocation.append(project.getName());
				components[i] = baseline.newApiComponent(location.toOSString());
			}
			baseline.addApiComponents(components);
			manager.addApiProfile(baseline);
			manager.setDefaultApiProfile(baseline.getName());
		}
	}	
	
	/**
	 * Exports the project as an API component to be used in an API baseline.
	 * 
	 * @param project project to export
	 * @param apiComponent associated API component from the workspace profile
	 * @param baselineLocation local file system directory to host exported component
	 */
	private void exportApiComponent(IProject project, IApiComponent apiComponent, IPath baselineLocation) throws Exception {
		File root = baselineLocation.toFile();
		File componentDir = new File(root, project.getName());
		componentDir.mkdirs();
		IResource[] members = project.members();
		// copy root files and manifest
		for (int i = 0; i < members.length; i++) {
			IResource res = members[i];
			if (res.getType() == IResource.FILE) {
				copyFile(componentDir, (IFile)res);
			} else if (res.getType() == IResource.FOLDER) {
				if (res.getName().equals("META-INF")) {
					File manDir = new File(componentDir, "META-INF");
					manDir.mkdirs();
					copyFile(manDir, ((IFolder)res).getFile("MANIFEST.MF"));
				}
			}
		}
		// copy over .class files
		IFolder output = project.getFolder("bin");
		copyFolder(output, componentDir);
		// API Description
		ApiSettingsXmlVisitor visitor = new ApiSettingsXmlVisitor(apiComponent);
		apiComponent.getApiDescription().accept(visitor);
		String xml = visitor.getXML();
		File desc = new File(componentDir, ".api_description");
		desc.createNewFile();
		FileOutputStream stream = new FileOutputStream(desc);
		stream.write(xml.getBytes("UTF-8"));
		stream.close();
	}
	
	/**
	 * Copy the folder contents to the local file system.
	 * 
	 * @param folder workspace folder
	 * @param dir local directory
	 */
	private void copyFolder(IFolder folder, File dir) throws Exception {
		IResource[] members = folder.members();
		for (int i = 0; i < members.length; i++) {
			IResource res = members[i];
			if (res.getType() == IResource.FILE) {
				IFile file = (IFile) res;
				copyFile(dir, file);
			} else {
				IFolder nested = (IFolder) res;
				File next = new File(dir, nested.getName());
				next.mkdirs();
				copyFolder(nested, next);
			}
		}
	}
	
	/**
	 * Copies the given file to the given directory.
	 * 
	 * @param dir
	 * @param file
	 */
	private void copyFile(File dir, IFile file) throws Exception {
		File local = new File(dir, file.getName());
		local.createNewFile();
		FileOutputStream stream = new FileOutputStream(local);
		InputStream contents = file.getContents();
		byte[] bytes = Util.getInputStreamAsByteArray(contents, -1);
		stream.write(bytes);
		contents.close();
		stream.close();
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
