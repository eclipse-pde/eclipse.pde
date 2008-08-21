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
package org.eclipse.pde.api.tools.builder.tests;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.tests.builder.BuilderTests;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.compatibility.CompatibilityTest;
import org.eclipse.pde.api.tools.builder.tests.leak.LeakTest;
import org.eclipse.pde.api.tools.builder.tests.tags.TagTest;
import org.eclipse.pde.api.tools.builder.tests.usage.UsageTest;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;

/**
 * Base class for API builder tests
 */
public abstract class ApiBuilderTest extends BuilderTests {
	/**
	 * Debug flag
	 */
	protected static boolean DEBUG = false;
	
	public static final String TEST_SOURCE_ROOT = "test-builder";
	public static final String JAVA_EXTENSION = ".java";
	public static final String SRC_ROOT = "src";
	public static final String BIN_ROOT = "bin";
	protected final int[] NO_PROBLEM_IDS = new int[0];
	
	private int[] fProblems = null;
	private String[][] fMessageArgs = null;
	
	/**
	 * Constructor
	 * @param name
	 */
	public ApiBuilderTest(String name) {
		super(name);
	}
	
	/**
	 * Returns the contents of the source file in the given category with the given name
	 * @param srcpath the path to the folder containing the test source
	 * @param srcname the name of the test (which is the name of the file)
	 * @return the contents of the source file as a string, or <code>null</code>
	 */
	protected String getSourceContents(IPath srcpath, String srcname) {
		String contents = null;
		IPath path = TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(srcpath).append(srcname+JAVA_EXTENSION);
		File file = path.toFile();
		if(file.exists()) {
			contents = Util.getFileContentAsString(file);
		}
		return contents;
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
	 * Sets up the project for a given test using the specified source.
	 * 
	 * @param sourcename the name of the source file to create in the project
	 * @param packagename the name of the package to create in the project in the default 'src' package
	 * fragment root
	 * 
	 * @return the path to the new project
	 */
	protected IPath assertProject(String sourcename, String packagename) throws JavaModelException {
		IPath ppath = getEnv().addProject(getTestingProjectName(), getTestCompliance());
		assertTrue("The path for '"+getTestingProjectName()+"' must exist", !ppath.isEmpty());
		IPath frpath = getEnv().addPackageFragmentRoot(ppath, SRC_ROOT);
		assertTrue("The path for '"+SRC_ROOT+"' must exist", !frpath.isEmpty());
		IPath packpath = getEnv().addPackage(frpath, packagename);
		assertTrue("The path for '"+packagename+"' must exist", !packpath.isEmpty());
		String contents = getSourceContents(getTestSourcePath(), sourcename);
		assertNotNull("the source contents for '"+sourcename+"' must exist", contents);
		IPath cpath = getEnv().addClass(packpath, sourcename, contents);
		assertTrue("The path for '"+sourcename+"' must exist", !cpath.isEmpty());
		return ppath;
	}
	
	/**
	 * Deploys a full build test for API Javadoc tags using the given source file in the specified package,
	 * looking for problems specified from {@link #getExpectedProblemIds()()}
	 * @param packagename
	 * @param sourcename
	 * @param expectingproblems
	 * @param buildtype the type of build to perform. One of:
	 * <ol>
	 * <li>IncrementalProjectBuilder#FULL_BUILD</li>
	 * <li>IncrementalProjectBuilder#INCREMENTAL_BUILD</li>
	 * <li>IncrementalProjectBuilder#CLEAN_BUILD</li>
	 * </ol>
	 * @param buildworkspace true if the workspace should be built, false if the created project should be built
	 */
	protected void deployTagTest(String packagename, String sourcename, boolean expectingproblems, int buildtype, boolean buildworkspace) {
		try {
			IPath path = assertProject(sourcename, packagename);
			doBuild(buildtype, (buildworkspace ? null : path));
			IJavaProject jproject = getEnv().getJavaProject(path);
			IType type = jproject.findType(packagename, sourcename);
			assertNotNull("The type "+sourcename+" from package "+packagename+" must exist", type);
			IPath sourcepath = type.getPath();
			if(expectingproblems) {
				expectingOnlySpecificProblemsFor(sourcepath, getExpectedProblemIds());
				assertProblems(getEnv().getProblems());
			}
			else {
				expectingNoProblemsFor(sourcepath);
			}
		}
		catch(Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Sets up the project for a given test using the specified source.
	 * The listing of source names and package names must be equal in size, as each source name will be
	 * placed in the the corresponding package listed in packagenames
	 * 
	 * @param sourcenames listing of source names to deploy in the test project
	 * @param packagenames listing of package name to deploy in the 'src' root of the project
	 * @param internalpackages listing of the name of packages to make internal in the testing project (set x-internal to true)
	 * 
	 * @return the path to the new project
	 */
	protected IPath assertProject(String[] sourcenames, String[] packagenames, String[] internalpackages) throws JavaModelException, CoreException {
		assertTrue("source and package name lists must be the same size", sourcenames.length == packagenames.length);
		IPath ppath = getEnv().addProject(getTestingProjectName(), getTestCompliance());
		assertTrue("The path for '"+getTestingProjectName()+"' must exist", !ppath.isEmpty());
		IPath frpath = getEnv().addPackageFragmentRoot(ppath, SRC_ROOT);
		assertTrue("The path for '"+SRC_ROOT+"' must exist", !frpath.isEmpty());
		IProject project = getEnv().getProject(ppath);
		for(int i = 0; i < sourcenames.length; i++) {
			IPath packpath = getEnv().addPackage(frpath, packagenames[i]);
			assertTrue("The path for '"+packagenames[i]+"' must exist", !packpath.isEmpty());
			String contents = getSourceContents(getTestSourcePath(), sourcenames[i]);
			assertNotNull("the source contents for '"+sourcenames[i]+"' must exist", contents);
			IPath cpath = getEnv().addClass(packpath, sourcenames[i], contents);
			assertTrue("The path for '"+sourcenames[i]+"' must exist", !cpath.isEmpty());
			ProjectUtils.addExportedPackage(project, packagenames[i], false, null);
		}
		for(int i = 0; i < internalpackages.length; i++) {
			IPackageFragment pack = getEnv().getJavaProject(ppath).findPackageFragment(getEnv().getPackagePath(frpath, internalpackages[i]));
			if(pack != null) {
				ProjectUtils.addExportedPackage(project, internalpackages[i], true, null);
			}
		}
		return ppath;
	}
	
	/**
	 * Performs the specified type of build on the given path, or the workspace if the path is <code>null</code>
	 * @param type the type of build. One of:
	 * <ol>
	 * <li>IncrementalProjectBuilder#FULL_BUILD</li>
	 * <li>IncrementalProjectBuilder#INCREMENTAL_BUILD</li>
	 * <li>IncrementalProjectBuilder#CLEAN_BUILD</li>
	 * </ol>
	 * @param path the path of the project to build or <code>null</code> if the workspace should be built
	 */
	private void doBuild(int type, IPath path) {
		switch(type) {
			case IncrementalProjectBuilder.FULL_BUILD: {
				if(path == null) {
					fullBuild();
				}
				else {
					fullBuild(path);
				}
				break;
			}
			case IncrementalProjectBuilder.INCREMENTAL_BUILD: {
				if(path == null) {
					incrementalBuild();
				}
				else {
					incrementalBuild(path);
				}
				break;
			}
			case IncrementalProjectBuilder.CLEAN_BUILD: {
				cleanBuild();
				break;
			}
		}
	}
	
	/**
	 * Deploys a full build with the given package and source names, where: 
	 * <ol>
	 * <li>the listing of internal package names will set all those packages that exist to be x-internal=true in the manifest</li>
	 * <li>the listing of fully qualified type names will each be checked for set expected problem id</li>
	 * <li>all other packages specified in packagenames that do not appear in internalpnames will be set to exported</li>
	 * </ol>
	 * @param packagenames the names of the packages to create in the testing project
	 * @param sourcenames the names of the source files to create in the testing project. Each source will be placed in the 
	 * corresponding package from the packagnames array, i.e. sourcenames[0] will be placed in packagenames[0]
	 * @param internalpnames the names of packages to mark as x-internal=true in the manifest of the project
	 * @param expectingproblemson the fully qualified names of the types we are expecting to see problems on
	 * @param expectingproblems the problem ids we expect to see on each of the types specified in the expectingproblemson array
	 * @param buildtype the type of build to run. One of:
	 * <ol>
	 * <li>IncrementalProjectBuilder#FULL_BUILD</li>
	 * <li>IncrementalProjectBuilder#INCREMENTAL_BUILD</li>
	 * <li>IncrementalProjectBuilder#CLEAN_BUILD</li>
	 * </ol>
	 * @param buildworkspace true if the entire workspace should be built, false if only the created project should be built
	 */
	protected void deployLeakTest(String[] packagenames, String[] sourcenames, String[] internalpnames, String[] expectingproblemson, boolean expectingproblems, int buildtype, boolean buildworkspace) {
		try {
			IPath path = assertProject(sourcenames, packagenames, internalpnames);
			doBuild(buildtype, (buildworkspace ? null : path));
			if(expectingproblems || expectingproblemson != null) {
				IJavaProject jproject = getEnv().getJavaProject(path);
				for(int i = 0; i < expectingproblemson.length; i++) {
					IType type = jproject.findType(expectingproblemson[i]);
					assertNotNull("The type "+expectingproblemson[i]+" must exist", type);
					expectingOnlySpecificProblemsFor(type.getPath(), getExpectedProblemIds());
				}
				assertProblems(getEnv().getProblems());
			}
			else {
				expectingNoProblems();
			}
		}
		catch(Exception e) {
			fail(e.getMessage());
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
	protected void assertProblems(ApiProblem[] problems) {}
	
	/**
	 * Sets the ids of the problems you expect to see from deploying a builder test
	 * @param problemids
	 */
	protected void setExpectedProblemIds(int[] problemids) {
		fProblems = problemids;
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
		for (int i = 0; i < length; i++) {
			if (array[i] != null) {
				if (i > 0) buffer.append('\n');
				buffer.append(array[i].toString());
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
	private void resetBuilderOptions() {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		// usage
		prefs.setToDefault(IApiProblemTypes.ILLEGAL_EXTEND);
		prefs.setToDefault(IApiProblemTypes.ILLEGAL_IMPLEMENT);
		prefs.setToDefault(IApiProblemTypes.ILLEGAL_INSTANTIATE);
		prefs.setToDefault(IApiProblemTypes.ILLEGAL_REFERENCE);
		prefs.setToDefault(IApiProblemTypes.ILLEGAL_OVERRIDE);
		prefs.setToDefault(IApiProblemTypes.LEAK_EXTEND);
		prefs.setToDefault(IApiProblemTypes.LEAK_FIELD_DECL);
		prefs.setToDefault(IApiProblemTypes.LEAK_IMPLEMENT);
		prefs.setToDefault(IApiProblemTypes.LEAK_METHOD_PARAM);
		prefs.setToDefault(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE);
		prefs.setToDefault(IApiProblemTypes.INVALID_JAVADOC_TAG);
		
		// compatibilities
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			prefs.setToDefault(ApiPlugin.AllCompatibilityKeys[i]);
		}
	
		// version management
		prefs.setToDefault(IApiProblemTypes.MISSING_SINCE_TAG);
		prefs.setToDefault(IApiProblemTypes.MALFORMED_SINCE_TAG);
		prefs.setToDefault(IApiProblemTypes.INVALID_SINCE_TAG_VERSION);
		prefs.setToDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION);
		prefs.setToDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE);
		prefs.setToDefault(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE);
		
		prefs.setToDefault(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables all of the usage problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableUsageOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		prefs.setValue(IApiProblemTypes.ILLEGAL_EXTEND, value);
		prefs.setValue(IApiProblemTypes.ILLEGAL_IMPLEMENT, value);
		prefs.setValue(IApiProblemTypes.ILLEGAL_INSTANTIATE, value);
		prefs.setValue(IApiProblemTypes.ILLEGAL_REFERENCE, value);
		prefs.setValue(IApiProblemTypes.ILLEGAL_OVERRIDE, value);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables all of the leak problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableLeakOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		prefs.setValue(IApiProblemTypes.LEAK_EXTEND, value);
		prefs.setValue(IApiProblemTypes.LEAK_FIELD_DECL, value);
		prefs.setValue(IApiProblemTypes.LEAK_IMPLEMENT, value);
		prefs.setValue(IApiProblemTypes.LEAK_METHOD_PARAM, value);
		prefs.setValue(IApiProblemTypes.LEAK_METHOD_RETURN_TYPE, value);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables the unsupported Javadoc tag problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableUnsupportedTagOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		prefs.setValue(IApiProblemTypes.INVALID_JAVADOC_TAG, enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables all of the compatibility problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableCompatibilityOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		for (int i = 0, max = ApiPlugin.AllCompatibilityKeys.length; i < max; i++) {
			prefs.setValue(ApiPlugin.AllCompatibilityKeys[i], value);
		}
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables all of the since tag problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableSinceTagOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		prefs.setValue(IApiProblemTypes.MISSING_SINCE_TAG, value);
		prefs.setValue(IApiProblemTypes.MALFORMED_SINCE_TAG, value);
		prefs.setValue(IApiProblemTypes.INVALID_SINCE_TAG_VERSION, value);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables all of the version number problems for the builder
	 * @param enabled if true the builder options are set to 'Error' or 'Enabled', false sets the 
	 * options to 'Ignore' or 'Disabled'
	 */
	protected void enableVersionNumberOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		String value2 = enabled ? ApiPlugin.VALUE_ENABLED : ApiPlugin.VALUE_DISABLED;
		prefs.setValue(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION, value);
		prefs.setValue(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MINOR_WITHOUT_API_CHANGE, value2);
		prefs.setValue(IApiProblemTypes.INCOMPATIBLE_API_COMPONENT_VERSION_INCLUDE_INCLUDE_MAJOR_WITHOUT_BREAKING_CHANGE, value2);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/**
	 * Enables or disables the API baseline problems for the builder
	 * @param enabled if true the builder options are set to 'Error', false sets the 
	 * options to 'Ignore'
	 */
	protected void enableBaselineOptions(boolean enabled) {
		Preferences prefs = ApiPlugin.getDefault().getPluginPreferences();
		String value = enabled ? ApiPlugin.VALUE_ERROR : ApiPlugin.VALUE_IGNORE;
		prefs.setValue(IApiProblemTypes.MISSING_DEFAULT_API_BASELINE, value);
		ApiPlugin.getDefault().savePluginPreferences();
	}
	
	/** 
	 * Sets up this test.
	 */
	protected void setUp() throws Exception {
		if (env == null) {
			env = new ApiTestingEnvironment();
			env.openEmptyWorkspace();
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
			VersionNumberingTests.class,
			BaselineProblemTests.class,
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
