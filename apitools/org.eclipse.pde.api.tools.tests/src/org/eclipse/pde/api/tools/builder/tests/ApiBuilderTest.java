/*******************************************************************************
 * Copyright (c) 2008, 2019 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 525701
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.tests.builder.BuilderTests;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.annotations.AnnotationTest;
import org.eclipse.pde.api.tools.builder.tests.compatibility.CompatibilityTest;
import org.eclipse.pde.api.tools.builder.tests.leak.LeakTest;
import org.eclipse.pde.api.tools.builder.tests.tags.TagTest;
import org.eclipse.pde.api.tools.builder.tests.usage.Java7UsageTest;
import org.eclipse.pde.api.tools.builder.tests.usage.Java8UsageTest;
import org.eclipse.pde.api.tools.builder.tests.usage.UsageTest;
import org.eclipse.pde.api.tools.internal.ApiDescriptionXmlCreator;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.junit.BeforeClass;
import org.osgi.service.prefs.BackingStoreException;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Base class for API builder tests
 *
 * @since 1.0
 */
public abstract class ApiBuilderTest extends BuilderTests {
	/**
	 * Debug flag
	 */
	protected static boolean DEBUG = false;

	public static final String TEST_SOURCE_ROOT = "test-builder"; //$NON-NLS-1$
	public static final String BASELINE = "baseline"; //$NON-NLS-1$
	public static final String JAVA_EXTENSION = ".java"; //$NON-NLS-1$
	public static final String SRC_ROOT = "src"; //$NON-NLS-1$
	public static final String BIN_ROOT = "bin"; //$NON-NLS-1$
	protected final int[] NO_PROBLEM_IDS = new int[0];

	/**
	 * Describes a line number mapped to the problem id with the given args we
	 * expect to see there
	 */
	protected class LineMapping {
		private int linenumber = 0;
		private int problemid = 0;
		private String message = null;

		public LineMapping(int linenumber, int problemid, String[] messageargs) {
			this.linenumber = linenumber;
			this.problemid = problemid;
			this.message = ApiProblemFactory.getLocalizedMessage(ApiProblemFactory.getProblemMessageId(this.problemid), messageargs);
		}

		public LineMapping(ApiProblem problem) {
			this.linenumber = problem.getLineNumber();
			this.problemid = problem.getProblemId();
			this.message = problem.getMessage();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LineMapping) {
				LineMapping lm = (LineMapping) obj;
				return lm.linenumber == this.linenumber && lm.problemid == this.problemid && (this.message == null ? lm.message == null : this.message.equals(lm.message));
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return this.linenumber | this.problemid | (this.message == null ? 0 : this.message.hashCode());
		}

		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Line mapping: "); //$NON-NLS-1$
			buffer.append("[line ").append(this.linenumber).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("[problemid: ").append(problemid).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			if (this.message != null) {
				buffer.append("[message: ").append(this.message).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				buffer.append("[no message]"); //$NON-NLS-1$
			}
			return super.toString();
		}
	}

	private int[] fProblems = null;
	private String[][] fMessageArgs = null;
	private LineMapping[] fLineMappings = null;

	/**
	 * Constructor
	 *
	 * @param name
	 */
	public ApiBuilderTest(String name) {
		super(name);
	}

	/**
	 * @return the testing environment cast the the one we want
	 */
	protected ApiTestingEnvironment getEnv() {
		return (ApiTestingEnvironment) env;
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ApiTestingEnvironment.setTargetPlatform();
	}

	/**
	 * Verifies that the workspace has no problems.
	 */
	@Override
	protected void expectingNoProblems() {
		expectingNoProblemsFor(getEnv().getWorkspaceRootPath());
	}

	/**
	 * Verifies that the given element has no problems.
	 */
	@Override
	protected void expectingNoProblemsFor(IPath root) {
		expectingNoProblemsFor(new IPath[] { root });
	}

	/**
	 * Asserts that there are no compilation problems in the environment
	 *
	 * @throws CoreException
	 */
	protected void expectingNoJDTProblems() throws CoreException {
		expectingNoJDTProblemsFor(getEnv().getWorkspaceRootPath());
	}

	/**
	 * Asserts that there are no compilation problems on the given resource path
	 *
	 * @param resource
	 * @throws CoreException
	 */
	protected void expectingNoJDTProblemsFor(IPath resource) throws CoreException {
		IMarker[] jdtMarkers = getEnv().getAllJDTMarkers(resource);
		int length = jdtMarkers.length;
		if (length != 0) {
			boolean condition = false;
			String cause = "No marker message"; //$NON-NLS-1$
			for (int i = 0; i < length; i++) {
				condition = condition || jdtMarkers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR;
				if (condition) {
					cause = (String) jdtMarkers[i].getAttribute(IMarker.MESSAGE);
					System.err.println("Unexpected JDT Marker in " + jdtMarkers[i].getResource().getFullPath()); //$NON-NLS-1$
					System.err.println(cause);
				}
			}
			if (condition) {
				/*
				 * We are about to fail, log some extra information for easier
				 * debugging of the fail.
				 */
				logProjectInfos(getName() + " is about to fail, logging extra infos for resource " + resource); //$NON-NLS-1$
			}
			assertFalse("Should not be a JDT error: " + cause, condition); //$NON-NLS-1$
		}
	}

	/**
	 * Verifies that the given elements have no problems.
	 */
	@Override
	protected void expectingNoProblemsFor(IPath[] roots) {
		StringBuilder buffer = new StringBuilder();
		ApiProblem[] problems = allSortedApiProblems(roots);
		if (problems != null) {
			for (ApiProblem problem : problems) {
				buffer.append(problem + "\n"); //$NON-NLS-1$
			}
		}
		String actual = buffer.toString();
		assumeEquals("Unexpected problem(s)!!!", "", actual); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Verifies that the given element has problems and only the given element.
	 */
	@Override
	protected void expectingOnlyProblemsFor(IPath expected) {
		expectingOnlyProblemsFor(new IPath[] { expected });
	}

	/**
	 * Creates a set of the default problem ids of the given count
	 *
	 * @param numproblems
	 * @return the set of default problem ids, or an empty set.
	 */
	protected int[] getDefaultProblemIdSet(int numproblems) {
		if (numproblems < 0) {
			return NO_PROBLEM_IDS;
		}
		int[] set = new int[numproblems];
		for (int i = 0; i < numproblems; i++) {
			set[i] = getDefaultProblemId();
		}
		return set;
	}

	/**
	 * Verifies that the given elements have problems and only the given
	 * elements.
	 */
	@Override
	protected void expectingOnlyProblemsFor(IPath[] expected) {
		if (DEBUG) {
			printProblems();
		}
		IMarker[] rootProblems = getEnv().getMarkers();
		Hashtable<IPath, IPath> actual = new Hashtable<>(rootProblems.length * 2 + 1);
		for (IMarker rootProblem : rootProblems) {
			IPath culprit = rootProblem.getResource().getFullPath();
			actual.put(culprit, culprit);
		}

		for (IPath element : expected) {
			if (!actual.containsKey(element)) {
				assertTrue("missing expected problem with " + element.toString(), false); //$NON-NLS-1$
			}
		}

		if (actual.size() > expected.length) {
			for (Enumeration<IPath> e = actual.elements(); e.hasMoreElements();) {
				IPath path = e.nextElement();
				boolean found = false;
				for (IPath element : expected) {
					if (path.equals(element)) {
						found = true;
						break;
					}
				}
				if (!found) {
					assertTrue("unexpected problem(s) with " + path.toString(), false); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Creates the workspace by importing projects from the 'projectsdir'
	 * directory. All projects in the given directory will try to be imported
	 * into the workspace. The given 'projectsdir' is assumed to be a child path
	 * of the test source path (the test-builder folder in the test workspace).
	 *
	 * This is the initial state of the workspace.
	 *
	 * @param projectsdir the directory to load projects from
	 * @param buildimmediately if a build should be run immediately following
	 *            the import
	 * @param importfiles
	 * @param usetestcompliance
	 * @throws Exception
	 */
	protected void createExistingProjects(String projectsdir, boolean buildimmediately, boolean importfiles, boolean usetestcompliance) throws Exception {
		IPath path = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(projectsdir);
		File dir = path.toFile();
		assertTrue("Test data directory does not exist: " + path.toOSString(), dir.exists()); //$NON-NLS-1$
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isDirectory() && !file.getName().equals("CVS")) { //$NON-NLS-1$
				createExistingProject(file, importfiles, usetestcompliance);
			}
		}
		if (buildimmediately) {
			fullBuild();
		}
	}

	/**
	 * Exports the project as an API component to be used in an API baseline.
	 *
	 * @param project project to export
	 * @param apiComponent associated API component from the workspace profile
	 * @param baselineLocation local file system directory to host exported
	 *            component
	 */
	protected void exportApiComponent(IProject project, IApiComponent apiComponent, IPath baselineLocation) throws Exception {
		File root = baselineLocation.toFile();
		File componentDir = new File(root, project.getName());
		componentDir.mkdirs();
		IResource[] members = project.members();
		// copy root files and manifest
		for (IResource res : members) {
			if (res.getType() == IResource.FILE) {
				FileUtils.copyFile(componentDir, (IFile) res);
			} else if (res.getType() == IResource.FOLDER) {
				if (res.getName().equals("META-INF")) { //$NON-NLS-1$
					File manDir = new File(componentDir, "META-INF"); //$NON-NLS-1$
					manDir.mkdirs();
					FileUtils.copyFile(manDir, ((IFolder) res).getFile("MANIFEST.MF")); //$NON-NLS-1$
				}
			}
		}
		// copy over .class files
		IFolder output = project.getFolder("bin"); //$NON-NLS-1$
		FileUtils.copyFolder(output, componentDir);
		// API Description
		ApiDescriptionXmlCreator visitor = new ApiDescriptionXmlCreator(apiComponent);
		apiComponent.getApiDescription().accept(visitor, null);
		String xml = visitor.getXML();
		File desc = new File(componentDir, ".api_description"); //$NON-NLS-1$
		desc.createNewFile();
		try (FileOutputStream stream = new FileOutputStream(desc)) {
			stream.write(xml.getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * Create the project described in record. If it is successful return true.
	 *
	 * @param projectDir directory containing existing project
	 * @param importfiles
	 * @param usetestcompliance
	 */
	protected void createExistingProject(File projectDir, boolean importfiles, boolean usetestcompliance) throws Exception {
		String projectName = projectDir.getName();
		final IWorkspace workspace = getEnv().getWorkspace();
		IPath ppath = getEnv().addProject(projectName, usetestcompliance ? getTestCompliance() : null);
		IProject project = getEnv().getProject(ppath);
		IProjectDescription description = workspace.newProjectDescription(projectName);
		IPath locationPath = new Path(projectDir.getAbsolutePath());
		description.setLocation(locationPath);

		URI locationURI = description.getLocationURI();
		// if location is null, project already exists in this location or
		// some error condition occurred.
		assertNotNull("project description location is null", locationURI); //$NON-NLS-1$

		IProjectDescription desc = workspace.newProjectDescription(projectName);
		desc.setBuildSpec(description.getBuildSpec());
		desc.setComment(description.getComment());
		desc.setDynamicReferences(description.getDynamicReferences());
		desc.setNatureIds(description.getNatureIds());
		desc.setReferencedProjects(description.getReferencedProjects());
		description = desc;

		project.setDescription(description, new NullProgressMonitor());
		project.open(null);

		// only import the files if we want them
		if (importfiles) {
			// import operation to import project files
			File importSource = new File(locationURI);
			List<?> filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);
			for (Iterator<?> iterator = filesToImport.iterator(); iterator.hasNext();) {
				if (((File) iterator.next()).getName().equals("CVS")) { //$NON-NLS-1$
					iterator.remove();
				}
			}
			ImportOperation operation = new ImportOperation(project.getFullPath(), importSource, FileSystemStructureProvider.INSTANCE, pathString -> IOverwriteQuery.ALL, filesToImport);
			operation.setOverwriteResources(true);
			operation.setCreateContainerStructure(false);
			operation.run(new NullProgressMonitor());
		}

		// force the use of the test compliance
		if (usetestcompliance) {
			getEnv().setProjectCompliance(getEnv().getJavaProject(ppath), getTestCompliance());
		}
	}

	/**
	 * @return the default compiler compliance to use for the test
	 */
	protected String getTestCompliance() {
		return JavaCore.VERSION_1_4;
	}

	/**
	 * Method that can be overridden for custom assertion of the problems after
	 * the build
	 *
	 * @param problems the complete listing of problems from the testing
	 *            environment
	 */
	protected void assertProblems(ApiProblem[] problems) {
		int[] expectedProblemIds = getExpectedProblemIds();
		int length = problems.length;
		if (expectedProblemIds.length != length) {
			for (int i = 0; i < length; i++) {
				System.err.println(problems[i]);
			}
		}
		assertEquals("Wrong number of problems", expectedProblemIds.length, length); //$NON-NLS-1$
		String[][] args = getExpectedMessageArgs();
		if (args != null) {
			// compare messages
			ArrayList<String> messages = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				messages.add(problems[i].getMessage());
			}
			for (int i = 0; i < expectedProblemIds.length; i++) {
				String[] messageArgs = args[i];
				int messageId = ApiProblemFactory.getProblemMessageId(expectedProblemIds[i]);
				String message = ApiProblemFactory.getLocalizedMessage(messageId, messageArgs);
				assertTrue("Missing expected problem: " + message, messages.remove(message)); //$NON-NLS-1$
			}
			if (messages.size() > 0) {
				StringBuilder buffer = new StringBuilder();
				buffer.append('[');
				for (String problem : messages) {
					buffer.append(problem).append(',');
				}
				buffer.append(']');
				fail("There was no problems that matched the arguments: " + buffer.toString()); //$NON-NLS-1$
			}
		} else {
			// compare id's
			ArrayList<Integer> messages = new ArrayList<>();
			for (int i = 0; i < length; i++) {
				messages.add(Integer.valueOf(problems[i].getProblemId()));
			}
			for (int expectedProblemId : expectedProblemIds) {
				assertTrue("Missing expected problem: " + expectedProblemId, messages.remove(Integer.valueOf(expectedProblemId))); //$NON-NLS-1$
			}
		}
		if (fLineMappings != null) {
			ArrayList<LineMapping> mappings = new ArrayList<>(Arrays.asList(fLineMappings));
			for (ApiProblem problem : problems) {
				assertTrue("Missing expected problem line mapping: " + problem, mappings.remove(new LineMapping(problem))); //$NON-NLS-1$
			}
			if (mappings.size() > 0) {
				StringBuilder buffer = new StringBuilder();
				buffer.append('[');
				for (LineMapping mapping : mappings) {
					buffer.append(mapping).append(',');
				}
				buffer.append(']');
				fail("There was no problems that matched the line mappings: " + buffer.toString()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Sets the ids of the problems you expect to see from deploying a builder
	 * test
	 *
	 * @param problemids
	 */
	protected void setExpectedProblemIds(int[] problemids) {
		fProblems = problemids;
	}

	/**
	 * Sets the line mappings that problems are expected on
	 *
	 * @param linenumbers
	 */
	protected void setExpectedLineMappings(LineMapping[] linemappings) {
		fLineMappings = linemappings;
	}

	/**
	 * Sets the message arguments for corresponding problem ids.
	 *
	 * @param messageArgs message arguments - an array of String for each
	 *            expected problem.
	 */
	protected void setExpectedMessageArgs(String[][] messageArgs) {
		fMessageArgs = messageArgs;
	}

	/**
	 * @return the name of the testing project for the implementing test suite
	 */
	protected abstract String getTestingProjectName();

	/**
	 * @return the default problem id for the given test
	 */
	protected abstract int getDefaultProblemId();

	/**
	 * @return the ids of the
	 *         {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem}
	 *         we are expecting to find after a build.
	 *
	 *         This method is consulted for every call to a deploy* method where
	 *         a builder test is run.
	 *
	 *         The returned array from this method is used to make sure that
	 *         expected problems (kind and count) appear after a build
	 */
	protected int[] getExpectedProblemIds() {
		if (fProblems == null) {
			return NO_PROBLEM_IDS;
		}
		return fProblems;
	}

	/**
	 * Returns the expected message arguments corresponding to expected problem
	 * ids, or <code>null</code> if unspecified.
	 *
	 * @return message arguments for each expected problem or <code>null</code>
	 *         if unspecified
	 */
	protected String[][] getExpectedMessageArgs() {
		return fMessageArgs;
	}

	/**
	 * Verifies that the given element has a specific problem and only the given
	 * problem.
	 */
	protected void expectingOnlySpecificProblemFor(IPath root, int problemid) {
		expectingOnlySpecificProblemsFor(root, new int[] { problemid });
	}

	/**
	 * Returns the problem id from the marker
	 *
	 * @param marker
	 * @return the problem id from the marker or -1 if there isn't one set on
	 *         the marker
	 */
	protected int getProblemId(IMarker marker) {
		if (marker == null) {
			return -1;
		}
		return marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
	}

	/**
	 * Verifies that the given element has specifics problems and only the given
	 * problems.
	 */
	protected void expectingOnlySpecificProblemsFor(final IPath root, final int[] problemids) {
		if (DEBUG) {
			printProblemsFor(root);
		}
		IMarker[] markers = getEnv().getMarkersFor(root);
		for (int problemid : problemids) {
			boolean found = false;
			for (int j = 0; j < markers.length; j++) {
				if (getProblemId(markers[j]) == problemid) {
					found = true;
					markers[j] = null;
					break;
				}
			}
			if (!found) {
				printProblemsFor(root);
			}
			assertTrue("problem not found: " + problemid, found); //$NON-NLS-1$
		}
		for (IMarker marker : markers) {
			if (marker != null) {
				printProblemsFor(root);
				assertTrue("unexpected problem: " + marker.toString(), false); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Verifies that the given element has problems.
	 */
	@Override
	protected void expectingProblemsFor(IPath root, String expected) {
		expectingProblemsFor(new IPath[] { root }, expected);
	}

	/**
	 * Verifies that the given elements have problems.
	 */
	@Override
	protected void expectingProblemsFor(IPath[] roots, String expected) {
		ApiProblem[] problems = allSortedApiProblems(roots);
		assumeEquals("Invalid problem(s)!!!", expected, arrayToString(problems)); //$NON-NLS-1$
	}

	/**
	 * Verifies that the given elements have the expected problems.
	 */
	@Override
	protected void expectingProblemsFor(IPath[] roots, List expected) {
		ApiProblem[] problems = allSortedApiProblems(roots);
		assumeEquals("Invalid problem(s)!!!", arrayToString(expected.toArray()), arrayToString(problems)); //$NON-NLS-1$
	}

	/**
	 * Concatenate and sort all problems for given root paths.
	 *
	 * @param roots The path to get the problems
	 * @return All sorted problems of all given path
	 */
	protected ApiProblem[] allSortedApiProblems(IPath[] roots) {
		ApiProblem[] allProblems = null;
		ApiProblem[] problems = null;
		for (IPath root : roots) {
			problems = (ApiProblem[]) getEnv().getProblemsFor(root);
			int length = problems.length;
			if (problems.length != 0) {
				if (allProblems == null) {
					allProblems = problems;
				} else {
					int all = allProblems.length;
					System.arraycopy(allProblems, 0, allProblems = new ApiProblem[all + length], 0, all);
					System.arraycopy(problems, 0, allProblems, all, length);
				}
			}
		}
		if (allProblems != null) {
			Arrays.sort(allProblems);
		}
		return allProblems;
	}

	/**
	 * Verifies that the given element has a specific problem.
	 */
	protected void expectingSpecificProblemFor(IPath root, int problemid) {
		expectingSpecificProblemsFor(root, new int[] { problemid });
	}

	/**
	 * Verifies that the given element has specific problems.
	 */
	protected void expectingSpecificProblemsFor(IPath root, int[] problemids) {
		if (DEBUG) {
			printProblemsFor(root);
		}
		IMarker[] markers = getEnv().getMarkersFor(root);
		IMarker marker = null;
		next: for (int i = 0; i < problemids.length; i++) {
			for (int j = 0; j < markers.length; j++) {
				marker = markers[j];
				if (marker != null) {
					if (problemids[i] == getProblemId(marker)) {
						markers[j] = null;
						continue next;
					}
				}
			}
			System.out.println("--------------------------------------------------------------------------------"); //$NON-NLS-1$
			System.out.println("Missing problem while running test " + getName() + ":"); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("	- expected : " + problemids[i]); //$NON-NLS-1$
			System.out.println("	- current: " + arrayToString(markers)); //$NON-NLS-1$
			assumeTrue("missing expected problem: " + problemids[i], false); //$NON-NLS-1$
		}
	}

	/**
	 * Prints all of the problems in the current test workspace
	 */
	@Override
	protected void printProblems() {
		printProblemsFor(getEnv().getWorkspaceRootPath());
	}

	/**
	 * Prints all of the problems from the current root to infinite children
	 *
	 * @param root
	 */
	@Override
	protected void printProblemsFor(IPath root) {
		printProblemsFor(new IPath[] { root });
	}

	/**
	 * Prints all of the problems from each of the roots to infinite children
	 *
	 * @param roots
	 */
	@Override
	protected void printProblemsFor(IPath[] roots) {
		for (IPath root : roots) {
			/* get the leaf problems for this type */
			System.out.println(arrayToString(getEnv().getProblemsFor(root)));
			System.out.println();
		}
	}

	/**
	 * Takes each element of the array and calls toString on it to put an array
	 * together as a string
	 *
	 * @param array
	 * @return
	 */
	@Override
	protected String arrayToString(Object[] array) {
		StringBuilder buffer = new StringBuilder();
		int length = array == null ? 0 : array.length;
		if (length == 0) {
			buffer.append("No problem found"); //$NON-NLS-1$
		} else {
			for (int i = 0; i < length; i++) {
				if (array[i] != null) {
					if (i > 0) {
						buffer.append('\n');
					}
					buffer.append(array[i].toString());
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * @return the source path from the test-builder test source root to find
	 *         the test source in
	 */
	protected abstract IPath getTestSourcePath();

	/**
	 * Sets the current builder options to use for the current test. Default is
	 * all set to their default values
	 */
	protected void setBuilderOptions() {
		resetBuilderOptions();
	}

	/**
	 * Resets all of the builder options to their defaults after each test run
	 */
	protected void resetBuilderOptions() {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		// usage
		inode.put(IApiProblemTypes.ILLEGAL_EXTEND, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.ILLEGAL_INSTANTIATE, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.ILLEGAL_REFERENCE, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.ILLEGAL_OVERRIDE, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.LEAK_EXTEND, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.LEAK_FIELD_DECL, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.LEAK_IMPLEMENT, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.LEAK_METHOD_PARAM, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.INVALID_JAVADOC_TAG, ApiPlugin.VALUE_IGNORE);
		inode.put(IApiProblemTypes.INVALID_ANNOTATION, ApiPlugin.VALUE_IGNORE);
		inode.put(IApiProblemTypes.UNUSED_PROBLEM_FILTERS, ApiPlugin.VALUE_WARNING);

		// compatibilities
		for (String allCompatibilityKey : ApiPlugin.AllCompatibilityKeys) {
			inode.put(allCompatibilityKey, ApiPlugin.VALUE_ERROR);
		}

		// version management
		inode.put(IApiProblemTypes.MISSING_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.MALFORMED_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE,
				ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE,
				ApiPlugin.VALUE_WARNING);
		inode.put(IApiProblemTypes.CHANGED_EXECUTION_ENV, ApiPlugin.VALUE_ERROR);

		inode.put(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, ApiPlugin.VALUE_WARNING);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables all of the usage problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableUsageOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		// usage
		inode.put(IApiProblemTypes.ILLEGAL_EXTEND, value);
		inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, value);
		inode.put(IApiProblemTypes.ILLEGAL_INSTANTIATE, value);
		inode.put(IApiProblemTypes.ILLEGAL_REFERENCE, value);
		inode.put(IApiProblemTypes.ILLEGAL_OVERRIDE, value);
		inode.put(IApiProblemTypes.UNUSED_PROBLEM_FILTERS, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables all of the leak problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableLeakOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.LEAK_EXTEND, value);
		inode.put(IApiProblemTypes.LEAK_FIELD_DECL, value);
		inode.put(IApiProblemTypes.LEAK_IMPLEMENT, value);
		inode.put(IApiProblemTypes.LEAK_METHOD_PARAM, value);
		inode.put(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Deletes the workspace file at the specified location (full path).
	 *
	 * @param workspaceLocation
	 */
	protected void deleteWorkspaceFile(IPath workspaceLocation, boolean recorddeletion) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertTrue("Workspace file does not exist: " + workspaceLocation.toString(), file.exists()); //$NON-NLS-1$
		file.delete(true, null);
		if (recorddeletion) {
			getEnv().removed(workspaceLocation);
		}
	}

	/**
	 * Returns a path in the local file system to an updated file based on this
	 * tests source path and filename.
	 *
	 * @param filename name of file to update
	 * @return path to the file in the local file system
	 */
	protected IPath getUpdateFilePath(String filename) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath()).append(filename);
	}

	/**
	 * Updates the contents of a workspace file at the specified location (full
	 * path), with the contents of a local file at the given replacement
	 * location (absolute path).
	 *
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	protected void createWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertFalse("Workspace file should not exist: " + workspaceLocation.toString(), file.exists()); //$NON-NLS-1$
		File replacement = replacementLocation.toFile();
		assertTrue("Replacement file does not exist: " + replacementLocation.toOSString(), replacement.exists()); //$NON-NLS-1$
		try (FileInputStream stream = new FileInputStream(replacement)) {
			file.create(stream, true, null);
		}
		getEnv().added(workspaceLocation);
	}

	/**
	 * Updates the contents of a workspace file at the specified location (full
	 * path), with the contents of a local file at the given replacement
	 * location (absolute path).
	 *
	 * @param workspaceLocation
	 * @param replacementLocation
	 */
	protected void updateWorkspaceFile(IPath workspaceLocation, IPath replacementLocation) throws Exception {
		IFile file = getEnv().getWorkspace().getRoot().getFile(workspaceLocation);
		assertTrue("Workspace file does not exist: " + workspaceLocation.toString(), file.exists()); //$NON-NLS-1$
		File replacement = replacementLocation.toFile();
		assertTrue("Replacement file does not exist: " + replacementLocation.toOSString(), replacement.exists()); //$NON-NLS-1$
		try (FileInputStream stream = new FileInputStream(replacement)) {
			file.setContents(stream, true, false, null);
		}
		getEnv().changed(workspaceLocation);
	}

	/**
	 * Enables or disables the unsupported Javadoc tag problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableUnsupportedTagOptions(boolean enabled) {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.INVALID_JAVADOC_TAG, enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables the unsupported annotation problems for the builder
	 *
	 * @param enabled
	 * @since 1.0.400
	 */
	protected void enableUnsupportedAnnotationOptions(boolean enabled) {
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.INVALID_ANNOTATION, enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables all of the compatibility problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableCompatibilityOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		for (String allCompatibilityKey : ApiPlugin.AllCompatibilityKeys) {
			inode.put(allCompatibilityKey, value);
		}
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables all of the since tag problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableSinceTagOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.MISSING_SINCE_TAG, value);
		inode.put(IApiProblemTypes.MALFORMED_SINCE_TAG, value);
		inode.put(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables all of the version number problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error' or
	 *            'Enabled', false sets the options to 'Ignore' or 'Disabled'
	 */
	protected void enableVersionNumberOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		String value2 = enabled ? ApiPlugin.VALUE_ENABLED : ApiPlugin.VALUE_DISABLED;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, value);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MINOR_WITHOUT_API_CHANGE, value2);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_REPORT_MAJOR_WITHOUT_BREAKING_CHANGE, value2);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables the API baseline problems for the builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableBaselineOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Enables or disables the External Depencency breakage problems for the
	 * builder
	 *
	 * @param enabled if true the builder options are set to 'Error', false sets
	 *            the options to 'Ignore'
	 */
	protected void enableExternalDependencyCheckOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.API_USE_SCAN_TYPE_SEVERITY, value);
		inode.put(IApiProblemTypes.API_USE_SCAN_METHOD_SEVERITY, value);
		inode.put(IApiProblemTypes.API_USE_SCAN_FIELD_SEVERITY, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Sets up this test.
	 */
	@Override
	protected void setUp() throws Exception {
		if (env == null) {
			env = new ApiTestingEnvironment();
			env.openEmptyWorkspace();
			env.setAutoBuilding(false);
		}
		setBuilderOptions();
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		resetBuilderOptions();
		fProblems = null;
		fMessageArgs = null;
		this.debugRequestor.clearResult();
		super.tearDown();
	}

	/**
	 * @return all of the child test classes of this class
	 */
	private static Class<?>[] getAllTestClasses() {
		ArrayList<Class<?>> classes = new ArrayList<>();
		classes.add(CompatibilityTest.class);
		classes.add(UsageTest.class);
		classes.add(LeakTest.class);
		classes.add(TagTest.class);
		classes.add(AnnotationTest.class);
		if (ProjectUtils.isJava7Compatible()) {
			classes.add(Java7UsageTest.class);
		}
		if (ProjectUtils.isJava8Compatible()) {
			classes.add(Java8UsageTest.class);
		}
		return classes.toArray(new Class[classes.size()]);
	}

	/**
	 * Collects tests from the getAllTestClasses() method into the given suite
	 *
	 * @param suite
	 */
	private static void collectTests(TestSuite suite) {
		// Hack to load all classes before computing their suite of test cases
		// this allow to reset test cases subsets while running all Builder
		// tests...
		Class<?>[] classes = getAllTestClasses();

		// Reset forgotten subsets of tests
		TestCase.TESTS_PREFIX = null;
		TestCase.TESTS_NAMES = null;
		TestCase.TESTS_NUMBERS = null;
		TestCase.TESTS_RANGE = null;
		TestCase.RUN_ONLY_ID = null;

		/* tests */
		for (Class<?> clazz : classes) {
			Method suiteMethod;
			try {
				suiteMethod = clazz.getDeclaredMethod("suite"); //$NON-NLS-1$
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				continue;
			}
			Object test;
			try {
				test = suiteMethod.invoke(clazz);
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
	 * loads builder tests
	 *
	 * @return
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(ApiBuilderTest.class.getName());
		collectTests(suite);
		return suite;
	}

	private static void logProjectInfos(String message) {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		logProjectInfos(message, IStatus.ERROR, Arrays.asList(projects));
	}

	/**
	 * Logs the classpath of each accessible project of the specified projects,
	 * as well as all markers in the workspace.
	 *
	 * @param message
	 *            a prefix for the logged message
	 * @param severity
	 *            the severity of the logged entry
	 * @param projectNames
	 *            the names of the projects for which to log classpaths
	 */
	protected static void logProjectInfos(String message, String[] projectNames) {
		List<IProject> projects = new ArrayList<>();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String projectName : projectNames) {
			IProject project = root.getProject(projectName);
			projects.add(project);
		}
		logProjectInfos(message, IStatus.INFO, projects);
	}

	/**
	 * Logs the classpath of each accessible project of the specified projects,
	 * as well as all markers in the workspace.
	 *
	 * @param message
	 *            a prefix for the logged message
	 * @param severity
	 *            the severity of the logged entry
	 * @param projects
	 *            the projects for which to log classpaths
	 */
	protected static void logProjectInfos(String message, int severity, List<IProject> projects) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		ILog log = PDECore.getDefault().getLog();
		try {
			IMarker[] markers = workspaceRoot.findMarkers(null, true, IResource.DEPTH_INFINITE);

			String infosContent = String.join(System.lineSeparator(), message, toString(projects), toString(markers));

			IStatus infos = new Status(severity, PDECore.PLUGIN_ID, infosContent, new Exception());
			log.log(infos);
		} catch (Exception e) {
			IStatus error = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, "error occurred while logging extra info", e); //$NON-NLS-1$
			log.log(error);
		}
	}

	private static String toString(List<IProject> projects) throws Exception {
		StringBuilder contents = new StringBuilder();
		contents.append("Listing " + projects.size() + " projects:"); //$NON-NLS-1$ //$NON-NLS-2$
		for (IProject project : projects) {
			contents.append(System.lineSeparator());
			contents.append("    name: " + project.getName()); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    location: " + project.getLocation()); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    is accessible: " + project.isAccessible()); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    is open: " + project.isOpen()); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			if (project.hasNature(JavaCore.NATURE_ID) && project.isAccessible()) {
				IJavaProject javaProject = JavaCore.create(project);
				boolean ignoreUnresolvedEntry = true;
				IClasspathEntry[] projectClassPath = javaProject.getResolvedClasspath(ignoreUnresolvedEntry);
				contents.append(toString(projectClassPath));

			}
		}
		return contents.toString();
	}

	private static String toString(IClasspathEntry[] classpathEntries) {
		StringBuilder contents = new StringBuilder();
		contents.append("Listing " + classpathEntries.length + " classpath entries:"); //$NON-NLS-1$ //$NON-NLS-2$
		contents.append(System.lineSeparator());
		for (IClasspathEntry classpathEntry : classpathEntries) {
			contents.append(classpathEntry);
			contents.append(System.lineSeparator());
		}
		return contents.toString();
	}

	private static String toString(IMarker[] markers) throws CoreException {
		StringBuilder contents = new StringBuilder();
		contents.append("Listing " + markers.length + " markers:"); //$NON-NLS-1$ //$NON-NLS-2$
		for (IMarker marker : markers) {
			contents.append(System.lineSeparator());
			contents.append("    message: " + marker.getAttribute(IMarker.MESSAGE)); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    severity: " + marker.getAttribute(IMarker.SEVERITY)); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    line number: " + marker.getAttribute(IMarker.LINE_NUMBER)); //$NON-NLS-1$
			contents.append(System.lineSeparator());
			contents.append("    resource: " + marker.getResource().getFullPath()); //$NON-NLS-1$
		}
		return contents.toString();
	}
}
