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
package org.eclipse.pde.api.tools.builder.tests.usage;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.tests.junit.extension.TestCase;
import org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Tests usage scanning in source
 * @since 1.0.0 
 */
public abstract class UsageTest extends ApiBuilderTest {

	protected static final String TESTING_PACKAGE = "x.y.z";
	protected static final String REPLACEMENT_PACKAGE = "x.y.z.replace";
	protected static final String REF_PROJECT_NAME = "refproject";
	protected static final String TESTING_PROJECT = "usagetests"; 
	protected static final String INNER_NAME1 = "inner";
	protected static final String OUTER_NAME = "outer";
	protected static final String INNER_NAME2 = "inner2";
	protected static final String OUTER_INAME = "Iouter";
	
	/**
	 * Constructor
	 */
	public UsageTest(String name) {
		super(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#setBuilderOptions()
	 */
	protected void setBuilderOptions() {
		enableUnsupportedTagOptions(false);
		enableBaselineOptions(false);
		enableCompatibilityOptions(false);
		enableLeakOptions(false);
		enableSinceTagOptions(false);
		enableUsageOptions(true);
		enableVersionNumberOptions(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return new Path("usage");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestingProjectName()
	 */
	protected String getTestingProjectName() {
		return "usagetests";
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		TestSuite suite = new TestSuite(UsageTest.class.getName());
		collectTests(suite);
		return suite;
	}
	
	/**
	 * Deploys a usage test
	 * @param typename
	 * @param inc
	 */
	protected void deployTest(String typename, boolean inc) {
		deployUsageTest(TESTING_PACKAGE, 
				typename, 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				getExpectedProblemIds().length > 0);
	}

	protected void deployReplacementTest(IPath before, IPath after, IPath filterpath, String sourcename, boolean inc) {
		try {
			getEnv().setAutoBuilding(false);
			IProject project = getEnv().getProject(getTestingProjectName());
			assertNotNull("the testing project "+getTestingProjectName()+" must be in the workspace", project);
			IPath settings = assertSettingsFolder(project);
			getEnv().addFile(settings, filterpath.lastSegment(), Util.getFileContentAsString(filterpath.toFile()));
			assertSource(before, project, TESTING_PACKAGE, sourcename);
			doBuild((inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), null);
			replaceSource(after, project, TESTING_PACKAGE, sourcename);
			doBuild((inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), null);
			IJavaProject jproject = getEnv().getJavaProject(getTestingProjectName());
			IPath sourcepath = project.getFullPath();
			if(after != null) {
				IType type = jproject.findType(TESTING_PACKAGE, sourcename);
				assertNotNull("The type "+sourcename+" from package "+TESTING_PACKAGE+" must exist", type);
				sourcepath = type.getPath();
			}
			if(getExpectedProblemIds().length > 0) {
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
		finally {
			getEnv().setAutoBuilding(true);
		}
	}
	
	protected void replaceSource(IPath sourcepath, IProject project, String packagename, String sourcename) {
		IPath ppath = project.getFullPath();
		assertTrue("The path for '"+project.getName()+"' must exist", !ppath.isEmpty());
		IPath frpath = getEnv().getPackageFragmentRootPath(ppath, SRC_ROOT);
		assertTrue("The path for '"+SRC_ROOT+"' must exist", !frpath.isEmpty());
		IPath packpath = getEnv().getPackagePath(frpath, packagename);
		assertTrue("The path for '"+packagename+"' must exist", !packpath.isEmpty());
		if(sourcepath == null) {
			//delete source requested
			getEnv().removeClass(packpath, sourcename);
		}
		else {
			String contents = getSourceContents(sourcepath, sourcename);
			assertNotNull("the source contents for '"+sourcename+"' must exist", contents);
			IPath cpath = getEnv().addClass(packpath, sourcename, contents);
			assertTrue("The path for '"+sourcename+"' must exist", !cpath.isEmpty());
		}
	}
	
	/**
	 * Ensures that the .settings folder is available
	 * @param project
	 * @throws CoreException
	 */
	protected IPath assertSettingsFolder(IProject project) throws CoreException {
		IFolder folder = project.getFolder(".settings");
		assertNotNull("the settings folder must exist", folder);
		if(!folder.isAccessible()) {
			folder.create(true, true, null); 
		}
		assertTrue("the .settings folder must be accessible", folder.isAccessible());
		return folder.getFullPath();
	}
	
	/**
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		createExistingProjects("usageprojects", false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	@Override
	protected void assertProblems(ApiProblem[] problems) {
		int[] pids = getExpectedProblemIds();
		assertEquals("The number of problems should match the number of specified problem ids", pids.length, problems.length);
		String[][] margs = getExpectedMessageArgs();
		if(margs != null) {
			ArrayList<String[]> args = new ArrayList<String[]>(Arrays.asList(margs));
			String message = null;
			int messageid = -1;
			loop: for(int i = 0; i < problems.length; i++) {
				for(Iterator<String[]> iter = args.iterator(); iter.hasNext();) {
					messageid = ApiProblemFactory.getProblemMessageId(problems[i].getProblemId());
					message = ApiProblemFactory.getLocalizedMessage(messageid, iter.next());
					if(problems[i].getMessage().equals(message)) {
						iter.remove();
						continue loop;
					}
				}
			}
			if(args.size() > 0) {
				fail("There was no problem that matched the arguments: "+Arrays.toString(args.iterator().next()));
			}
		}
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
	 * @return all of the child test classes of this class
	 */
	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
				FieldUsageTests.class,
				MethodUsageTests.class,
				ConstructorUsageTests.class,
				ClassUsageTests.class,
				InterfaceUsageTests.class,
				UnusedApiProblemFilterTests.class
		};
		return classes;
	}
}
