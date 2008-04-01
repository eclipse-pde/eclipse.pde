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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.tests.builder.AbstractMethodTests;
import org.eclipse.jdt.core.tests.builder.BasicBuildTests;
import org.eclipse.jdt.core.tests.builder.BuilderTests;
import org.eclipse.jdt.core.tests.builder.BuildpathTests;
import org.eclipse.jdt.core.tests.builder.CopyResourceTests;
import org.eclipse.jdt.core.tests.builder.DependencyTests;
import org.eclipse.jdt.core.tests.builder.EfficiencyTests;
import org.eclipse.jdt.core.tests.builder.ErrorsTests;
import org.eclipse.jdt.core.tests.builder.ExecutionTests;
import org.eclipse.jdt.core.tests.builder.GetResourcesTests;
import org.eclipse.jdt.core.tests.builder.IncrementalTests;
import org.eclipse.jdt.core.tests.builder.MultiProjectTests;
import org.eclipse.jdt.core.tests.builder.MultiSourceFolderAndOutputFolderTests;
import org.eclipse.jdt.core.tests.builder.OutputFolderTests;
import org.eclipse.jdt.core.tests.builder.PackageTests;
import org.eclipse.jdt.core.tests.builder.StaticFinalTests;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;

/**
 * Base class for API builder tests
 */
@SuppressWarnings("unchecked")
public class ApiBuilderTests extends BuilderTests {
	/**
	 * Debug flag
	 */
	protected static boolean DEBUG = false;
	
	/**
	 * Constructor
	 * @param name
	 */
	public ApiBuilderTests(String name) {
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
	 * Sets up this test.
	 */
	protected void setUp() throws Exception {
		if (env == null) {
			env = new ApiTestingEnvironment();
			env.openEmptyWorkspace();
		}
		env.resetWorkspace();
		super.setUp();

	}
	
	/**
	 * @return all of the child test classes of this class
	 * TODO fill in with new API tools tests
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
			UsageLeakTests.class,	
				
			AbstractMethodTests.class,
			BasicBuildTests.class,
			BuildpathTests.class,
			CopyResourceTests.class,
			DependencyTests.class,
			ErrorsTests.class,
			EfficiencyTests.class,
			ExecutionTests.class,
			IncrementalTests.class,
			MultiProjectTests.class,
			MultiSourceFolderAndOutputFolderTests.class,
			OutputFolderTests.class,
			PackageTests.class,
			StaticFinalTests.class,
			GetResourcesTests.class,
		};
		return classes;
	}

	/**
	 * loads builder tests
	 * @return
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(ApiBuilderTests.class.getName());

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

		return suite;
	}
}
