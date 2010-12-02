/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.builder.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.tests.builder.BuilderTests;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.compatibility.CompatibilityTest;
import org.eclipse.pde.api.tools.builder.tests.leak.LeakTest;
import org.eclipse.pde.api.tools.builder.tests.tags.TagTest;
import org.eclipse.pde.api.tools.builder.tests.usage.UsageTest;
import org.eclipse.pde.api.tools.internal.ApiDescriptionXmlCreator;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.osgi.service.prefs.BackingStoreException;

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
	
	public static final String TEST_SOURCE_ROOT = "test-builder";
	public static final String BASELINE = "baseline";
	public static final String JAVA_EXTENSION = ".java";
	public static final String SRC_ROOT = "src";
	public static final String BIN_ROOT = "bin";
	protected final int[] NO_PROBLEM_IDS = new int[0];
	
	/**
	 * Describes a line number mapped to the problem id with the given args we expect to see there
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
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			if(obj instanceof LineMapping) {
				LineMapping lm = (LineMapping) obj;
				return lm.linenumber == this.linenumber &&
						lm.problemid == this.problemid &&
						(this.message == null ? lm.message == null : this.message.equals(lm.message));
			}
			return super.equals(obj);
		}
		/**
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return this.linenumber | this.problemid | (this.message == null ? 0 : this.message.hashCode());
		}
		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Line mapping: ");
			buffer.append("[line ").append(this.linenumber).append("]");
			buffer.append("[problemid: ").append(problemid).append("]");
			if(this.message != null) {
				buffer.append("[message: ").append(this.message).append("]");
			}
			else {
				buffer.append("[no message]");
			}
			return super.toString();
		}
	}
	
	private int[] fProblems = null;
	private String[][] fMessageArgs = null;
	private LineMapping[] fLineMappings = null;
	
	/**
	 * Constructor
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
	
	/** 
	 * Verifies that the workspace has no problems.
	 */
	protected void expectingNoProblems() {
		expectingNoProblemsFor(getEnv().getWorkspaceRootPath());
	}

	/** 
	 * Verifies that the given element has no problems.
	 */
	protected void expectingNoProblemsFor(IPath root) {
		expectingNoProblemsFor(new IPath[] { root });
	}

	/**
	 * Asserts that there are no compilation problems in the environment
	 * @throws CoreException
	 */
	protected void expectingNoJDTProblems() throws CoreException {
		expectingNoJDTProblemsFor(getEnv().getWorkspaceRootPath());
	}
	
	/**
	 * Asserts that there are no compilation problems on the given resource path
	 * @param resource
	 * @throws CoreException
	 */
	protected void expectingNoJDTProblemsFor(IPath resource) throws CoreException {
		IMarker[] jdtMarkers = getEnv().getAllJDTMarkers(resource);
		int length = jdtMarkers.length;
		if (length != 0) {
			boolean condition = false;
			for (int i = 0; i < length; i++) {
				condition = condition || jdtMarkers[i].getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING) == IMarker.SEVERITY_ERROR;
				if (condition) {
					System.err.println(jdtMarkers[i].getAttribute(IMarker.MESSAGE));
				}
			}
			assertFalse("Should not be a JDT error", condition);
		}
	}
	
	/** 
	 * Verifies that the given elements have no problems.
	 */
	protected void expectingNoProblemsFor(IPath[] roots) {
		StringBuffer buffer = new StringBuffer();
		ApiProblem[] problems = allSortedApiProblems(roots);
		if (problems != null) {
			for (int i = 0, length = problems.length; i<length; i++) {
				buffer.append(problems[i]+"\n");
			}
		}
		String actual = buffer.toString();
		assumeEquals("Unexpected problem(s)!!!", "", actual); //$NON-NLS-1$
	}

	/** 
	 * Verifies that the given element has problems and
	 * only the given element.
	 */
	protected void expectingOnlyProblemsFor(IPath expected) {
		expectingOnlyProblemsFor(new IPath[] { expected });
	}

	/**
	 * Creates a set of the default problem ids of the given count
	 * @param numproblems
	 * @return the set of default problem ids, or an empty set.
	 */
	protected int[] getDefaultProblemIdSet(int numproblems) {
		if(numproblems < 0) {
			return NO_PROBLEM_IDS;
		}
		int[] set = new int[numproblems];
		for(int i = 0; i < numproblems; i++) {
			set[i] = getDefaultProblemId();
		}
		return set;
	}
	
	/** 
	 * Verifies that the given elements have problems and
	 * only the given elements.
	 */
	protected void expectingOnlyProblemsFor(IPath[] expected) {
		if (DEBUG) {
			printProblems();
		}
		IMarker[] rootProblems = getEnv().getMarkers();
		Hashtable<IPath, IPath> actual = new Hashtable<IPath, IPath>(rootProblems.length * 2 + 1);
		for (int i = 0; i < rootProblems.length; i++) {
			IPath culprit = rootProblems[i].getResource().getFullPath();
			actual.put(culprit, culprit);
		}

		for (int i = 0; i < expected.length; i++)
			if (!actual.containsKey(expected[i]))
				assertTrue("missing expected problem with " + expected[i].toString(), false); //$NON-NLS-1$

		if (actual.size() > expected.length) {
			for (Enumeration<IPath> e = actual.elements(); e.hasMoreElements();) {
				IPath path = e.nextElement();
				boolean found = false;
				for (int i = 0; i < expected.length; ++i) {
					if (path.equals(expected[i])) {
						found = true;
						break;
					}
				}
				if (!found)
					assertTrue("unexpected problem(s) with " + path.toString(), false); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Creates the workspace by importing projects from the 'projectsdir' directory. All projects in the given directory 
	 * will try to be imported into the workspace. The given 'projectsdir' is assumed to be a child path
	 * of the test source path (the test-builder folder in the test workspace).
	 * 
	 * This is the initial state of the workspace.
	 *  
	 * @param projectsdir the directory to load projects from
	 * @param buildimmediately if a build should be run immediately following the import
	 * @param importfiles
	 * @param usetestcompliance
	 * @throws Exception
	 */
	protected void createExistingProjects(String projectsdir, boolean buildimmediately, boolean importfiles, boolean usetestcompliance) throws Exception {
		IPath path = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(projectsdir);
		File dir = path.toFile();
		assertTrue("Test data directory does not exist: " + path.toOSString(), dir.exists());
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory() && !file.getName().equals("CVS")) {
				createExistingProject(file, importfiles, usetestcompliance);
			}
		}
		if(buildimmediately) {
			fullBuild();
		}
	}
	
	/**
	 * Exports the project as an API component to be used in an API baseline.
	 * 
	 * @param project project to export
	 * @param apiComponent associated API component from the workspace profile
	 * @param baselineLocation local file system directory to host exported component
	 */
	protected void exportApiComponent(IProject project, IApiComponent apiComponent, IPath baselineLocation) throws Exception {
		File root = baselineLocation.toFile();
		File componentDir = new File(root, project.getName());
		componentDir.mkdirs();
		IResource[] members = project.members();
		// copy root files and manifest
		for (int i = 0; i < members.length; i++) {
			IResource res = members[i];
			if (res.getType() == IResource.FILE) {
				FileUtils.copyFile(componentDir, (IFile)res);
			} else if (res.getType() == IResource.FOLDER) {
				if (res.getName().equals("META-INF")) {
					File manDir = new File(componentDir, "META-INF");
					manDir.mkdirs();
					FileUtils.copyFile(manDir, ((IFolder)res).getFile("MANIFEST.MF"));
				}
			}
		}
		// copy over .class files
		IFolder output = project.getFolder("bin");
		FileUtils.copyFolder(output, componentDir);
		// API Description
		ApiDescriptionXmlCreator visitor = new ApiDescriptionXmlCreator(apiComponent);
		apiComponent.getApiDescription().accept(visitor, null);
		String xml = visitor.getXML();
		File desc = new File(componentDir, ".api_description");
		desc.createNewFile();
		FileOutputStream stream = new FileOutputStream(desc);
		stream.write(xml.getBytes("UTF-8"));
		stream.close();
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
		IPath ppath = getEnv().addProject(projectName, getTestCompliance());
		IProject project = getEnv().getProject(ppath);
		IProjectDescription description = workspace.newProjectDescription(projectName);
		IPath locationPath = new Path(projectDir.getAbsolutePath());
		description.setLocation(locationPath);

		URI locationURI = description.getLocationURI();
		// if location is null, project already exists in this location or
		// some error condition occurred.
		assertNotNull("project description location is null", locationURI);
		
		IProjectDescription desc = workspace.newProjectDescription(projectName);
		desc.setBuildSpec(description.getBuildSpec());
		desc.setComment(description.getComment());
		desc.setDynamicReferences(description.getDynamicReferences());
		desc.setNatureIds(description.getNatureIds());
		desc.setReferencedProjects(description.getReferencedProjects());
		description = desc;

		project.setDescription(description, new NullProgressMonitor());
		project.open(null);
		
		//only import the files if we want them
		if(importfiles) {
			// import operation to import project files
			File importSource = new File(locationURI);
			List filesToImport = FileSystemStructureProvider.INSTANCE.getChildren(importSource);
			for (Iterator iterator = filesToImport.iterator(); iterator.hasNext();) {
				if(((File)iterator.next()).getName().equals("CVS")) {
					iterator.remove();
				}
			}
			ImportOperation operation = new ImportOperation(project.getFullPath(), importSource,
					FileSystemStructureProvider.INSTANCE, new IOverwriteQuery() {
						public String queryOverwrite(String pathString) {
							return IOverwriteQuery.ALL;
						}
					}, filesToImport);
			operation.setOverwriteResources(true);
			operation.setCreateContainerStructure(false);
			operation.run(new NullProgressMonitor());
		}
		
		//force the use of the test compliance
		if(usetestcompliance) {
			getEnv().setProjectCompliance(getEnv().getJavaProject(ppath), getTestCompliance());
		}
	}
	
	/**
	 * @return the default compiler compliance to use for the test
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_4;
	}
	
	/**
	 * Method that can be overridden for custom assertion of the problems after the build
	 * @param problems the complete listing of problems from the testing environment
	 */
	protected void assertProblems(ApiProblem[] problems) {
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
			ArrayList<String> messages = new ArrayList<String>();
			for (int i = 0; i < length; i++) {
				messages.add(problems[i].getMessage());
			}
			for (int i = 0; i < expectedProblemIds.length; i++) {
				String[] messageArgs = args[i];
				int messageId = ApiProblemFactory.getProblemMessageId(expectedProblemIds[i]);
				String message = ApiProblemFactory.getLocalizedMessage(messageId, messageArgs);
				assertTrue("Missing expected problem: " + message, messages.remove(message));
			}
			if(messages.size() > 0) {
				StringBuffer buffer = new StringBuffer();
				buffer.append('[');
				for(String problem: messages) {
					buffer.append(problem).append(',');
				}
				buffer.append(']');
				fail("There was no problems that matched the arguments: "+buffer.toString());
			}
		} else {
			// compare id's
			ArrayList<Integer> messages = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				messages.add(new Integer(problems[i].getProblemId()));
			}
			for (int i = 0; i < expectedProblemIds.length; i++) {
				assertTrue("Missing expected problem: " + expectedProblemIds[i], messages.remove(new Integer(expectedProblemIds[i])));
			}
		}
		if(fLineMappings != null) {
			ArrayList<LineMapping> mappings = new ArrayList(Arrays.asList(fLineMappings));
			for(int i = 0; i < problems.length; i++) {
				assertTrue("Missing expected problem line mapping: "+problems[i], mappings.remove(new LineMapping(problems[i])));
			}
			if(mappings.size() > 0) {
				StringBuffer buffer = new StringBuffer();
				buffer.append('[');
				for(LineMapping mapping: mappings) {
					buffer.append(mapping).append(',');
				}
				buffer.append(']');
				fail("There was no problems that matched the line mappings: "+buffer.toString());
			}
		}
	}
	
	/**
	 * Sets the ids of the problems you expect to see from deploying a builder test
	 * @param problemids
	 */
	protected void setExpectedProblemIds(int[] problemids) {
		fProblems = problemids;
	}
	
	/**
	 * Sets the line mappings that problems are expected on
	 * @param linenumbers
	 */
	protected void setExpectedLineMappings(LineMapping[] linemappings) {
		fLineMappings = linemappings;
	}
	
	/**
	 * Sets the message arguments for corresponding problem ids.
	 * 
	 * @param messageArgs message arguments - an array of String for each expected problem.
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
	 * @return the ids of the {@link org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem} we are
	 * expecting to find after a build. 
	 * 
	 * This method is consulted for every call to a deploy* method where a builder test is run.
	 * 
	 * The returned array from this method is used to make sure that expected problems (kind and count) appear after a build
	 */
	protected int[] getExpectedProblemIds() {
		if(fProblems == null) {
			return NO_PROBLEM_IDS;
		}
		return fProblems;
	}
	
	/**
	 * Returns the expected message arguments corresponding to expected problem ids, 
	 * or <code>null</code> if unspecified.
	 * 
	 * @return message arguments for each expected problem or <code>null</code> if unspecified
	 */
	protected String[][] getExpectedMessageArgs() {
		return fMessageArgs;
	}
	
	/** 
	 * Verifies that the given element has a specific problem and
	 * only the given problem.
	 */
	protected void expectingOnlySpecificProblemFor(IPath root, int problemid) {
		expectingOnlySpecificProblemsFor(root, new int[] { problemid });
	}

	/**
	 * Returns the problem id from the marker
	 * @param marker
	 * @return the problem id from the marker or -1 if there isn't one set on the marker
	 */
	protected int getProblemId(IMarker marker) {
		if(marker == null) {
			return -1;
		}
		return marker.getAttribute(IApiMarkerConstants.MARKER_ATTR_PROBLEM_ID, -1);
	}
	
	/** 
	 * Verifies that the given element has specifics problems and
	 * only the given problems.
	 */
	protected void expectingOnlySpecificProblemsFor(final IPath root, final int[] problemids) {
		if (DEBUG) {
			printProblemsFor(root);
		}
		IMarker[] markers = getEnv().getMarkersFor(root);
		for (int i = 0; i < problemids.length; i++) {
			boolean found = false;
			for (int j = 0; j < markers.length; j++) {
				if(getProblemId(markers[j]) == problemids[i]) {
					found = true;
					markers[j] = null;
					break;
				}
			}
			if (!found) {
				printProblemsFor(root);
			}
			assertTrue("problem not found: " + problemids[i], found); //$NON-NLS-1$
		}
		for (int i = 0; i < markers.length; i++) {
			if(markers[i] != null) {
				printProblemsFor(root);
				assertTrue("unexpected problem: " + markers[i].toString(), false); //$NON-NLS-1$
			}
		}
	}

	/** 
	 * Verifies that the given element has problems.
	 */
	protected void expectingProblemsFor(IPath root, String expected) {
		expectingProblemsFor(new IPath[] { root }, expected);
	}

	/** 
	 * Verifies that the given elements have problems.
	 */
	protected void expectingProblemsFor(IPath[] roots, String expected) {
		ApiProblem[] problems = allSortedApiProblems(roots);
		assumeEquals("Invalid problem(s)!!!", expected, arrayToString(problems)); //$NON-NLS-1$
	}

	/**
	 * Verifies that the given elements have the expected problems.
	 */
	protected void expectingProblemsFor(IPath[] roots, List expected) {
		ApiProblem[] problems = allSortedApiProblems(roots);
		assumeEquals("Invalid problem(s)!!!", arrayToString(expected.toArray()), arrayToString(problems));
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
		for (int i = 0, max=roots.length; i<max; i++) {
			problems = (ApiProblem[]) getEnv().getProblemsFor(roots[i]);
			int length = problems.length;
			if (problems.length != 0) {
				if (allProblems == null) {
					allProblems = problems;
				} else {
					int all = allProblems.length;
					System.arraycopy(allProblems, 0, allProblems = new ApiProblem[all+length], 0, all);
					System.arraycopy(problems, 0, allProblems , all, length);
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
		next : for (int i = 0; i < problemids.length; i++) {
			for (int j = 0; j < markers.length; j++) {
				marker = markers[j];
				if (marker != null) {
					if (problemids[i] == getProblemId(marker)) {
						markers[j] = null;
						continue next;
					}
				}
			}
			System.out.println("--------------------------------------------------------------------------------");
			System.out.println("Missing problem while running test "+getName()+":");
			System.out.println("	- expected : " + problemids[i]);
			System.out.println("	- current: " + arrayToString(markers));
			assumeTrue("missing expected problem: " + problemids[i], false);
		}
	}

	/**
	 * Prints all of the problems in the current test workspace
	 */
	protected void printProblems() {
		printProblemsFor(getEnv().getWorkspaceRootPath());
	}

	/**
	 * Prints all of the problems from the current root to infinite children
	 * @param root
	 */
	protected void printProblemsFor(IPath root) {
		printProblemsFor(new IPath[] { root });
	}

	/**
	 * Prints all of the problems from each of the roots to infinite children
	 * @param roots
	 */
	protected void printProblemsFor(IPath[] roots) {
		for (int i = 0; i < roots.length; i++) {
			/* get the leaf problems for this type */
			System.out.println(arrayToString(getEnv().getProblemsFor(roots[i])));
			System.out.println();
		}
	}

	/**
	 * Takes each element of the array and calls toString on it to put an array together as a string
	 * @param array
	 * @return
	 */
	protected String arrayToString(Object[] array) {
		StringBuffer buffer = new StringBuffer();
		int length = array == null ? 0 : array.length;
		if (length == 0) {
			buffer.append("No problem found");
		} else {
			for (int i = 0; i < length; i++) {
				if (array[i] != null) {
					if (i > 0) buffer.append('\n');
					buffer.append(array[i].toString());
				}
			}
		}
		return buffer.toString();
	}

	/**
	 * @return the source path from the test-builder test source root to find the test source in
	 */
	protected abstract IPath getTestSourcePath();
	
	/**
	 * Sets the current builder options to use for the current test.
	 * Default is all set to their default values
	 */
	protected void setBuilderOptions() {
		resetBuilderOptions();
	}
	
	/**
	 * Resets all of the builder options to their defaults after each test run
	 */
	protected void resetBuilderOptions() {
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
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
		inode.put(IApiProblemTypes.UNUSED_PROBLEM_FILTERS, ApiPlugin.VALUE_WARNING);
		
		// compatibilities
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			inode.put(ApiPlugin.AllCompatibilityKeys[i], ApiPlugin.VALUE_ERROR);
		}
	
		// version management
		inode.put(IApiProblemTypes.MISSING_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.MALFORMED_SINCE_TAG, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, ApiPlugin.VALUE_ERROR);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, ApiPlugin.VALUE_DISABLED);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, ApiPlugin.VALUE_DISABLED);
		
		inode.put(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, ApiPlugin.VALUE_WARNING);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Enables or disables all of the usage problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableUsageOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
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
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableLeakOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
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
		assertTrue("Workspace file does not exist: " + workspaceLocation.toString(), file.exists());
		file.delete(true, null);
		if(recorddeletion) {
			getEnv().removed(workspaceLocation);
		}
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
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(replacement);
			file.create(stream, true, null);
		} finally {
			if(stream != null) {
				stream.close();
			}
		}
		getEnv().added(workspaceLocation);
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
		FileInputStream stream = null;
		try {
			stream = new FileInputStream(replacement);
			file.setContents(stream, true, false, null);
		}
		finally {
			if(stream != null) {
				stream.close();
			}
		}
		getEnv().changed(workspaceLocation);
	}
	
	
	/**
	 * Enables or disables the unsupported Javadoc tag problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableUnsupportedTagOptions(boolean enabled) {
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.INVALID_JAVADOC_TAG, enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Enables or disables all of the compatibility problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableCompatibilityOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			inode.put(ApiPlugin.AllCompatibilityKeys[i], value);
		}
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Enables or disables all of the since tag problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableSinceTagOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
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
	 * @param enabled if true the builder options are set to 'Error' or 'Enabled', false sets the 
	 * options to 'Ignore' or 'Disabled'
	 */
	protected void enableVersionNumberOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		String value2 = enabled ? ApiPlugin.VALUE_ENABLED : ApiPlugin.VALUE_DISABLED;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, value);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, value2);
		inode.put(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, value2);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Enables or disables the API baseline problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableBaselineOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
		inode.put(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, value);
		try {
			inode.flush();
		} catch (BackingStoreException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Enables or disables the External Depencency breakage problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableExternalDependencyCheckOptions(boolean enabled) {
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);		
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
	protected void setUp() throws Exception {
		if (env == null) {
			env = new ApiTestingEnvironment();
			env.openEmptyWorkspace();
			env.setAutoBuilding(false);
		}
		setBuilderOptions();
		super.setUp();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.core.tests.builder.BuilderTests#tearDown()
	 */
	protected void tearDown() throws Exception {
		resetBuilderOptions();
		fProblems = null;
		fMessageArgs = null;
		super.tearDown();
	}
	
	/**
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			CompatibilityTest.class,
			UsageTest.class,	
			LeakTest.class,
			TagTest.class
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
				test = suiteMethod.invoke(clazz, new Object[0]);
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
	 * @return
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(ApiBuilderTest.class.getName());
		collectTests(suite);
		return suite;
	}
}
