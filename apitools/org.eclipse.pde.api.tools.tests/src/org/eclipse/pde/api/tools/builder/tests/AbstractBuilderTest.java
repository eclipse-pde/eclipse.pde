package org.eclipse.pde.api.tools.builder.tests;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileConstants;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.tests.util.Util;

public class AbstractBuilderTest extends TestCase {

	protected static boolean DEBUG = false;
	protected static TestingEnvironment env = null;
	public static final int F_1_3 = 0x01;
	public static final int F_1_4 = 0x02;
	public static final int F_1_5 = 0x04;

	public static final int F_1_6 = 0x08;

	public static final int F_1_7 = 0x10;

	private static int possibleComplianceLevels = -1;

	// Summary display		
	// Used by AbstractRegressionTest for javac comparison tests
	protected static Map TESTS_COUNTERS = new HashMap();

	/**
	 * Build a test suite made of test suites for all possible running VM compliances .
	 * 
	 * @see #buildUniqueComplianceTestSuite(Class, long) for test suite children content.
	 * 
	 * @param evaluationTestClass The main test suite to build.
	 * @return built test suite (see {@link TestSuite}
	 */
	public static Test buildAllCompliancesTestSuite(Class evaluationTestClass) {
		TestSuite suite = new TestSuite(evaluationTestClass.getName());
		int complianceLevels = AbstractBuilderTest.getPossibleComplianceLevels();
		if ((complianceLevels & AbstractBuilderTest.F_1_3) != 0) {
			suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_3));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_4) != 0) {
			suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_4));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_5) != 0) {
			suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_5));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_6) != 0) {
			suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_6));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_7) != 0) {
			suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_7));
		}
		return suite;
	}

	/**
	 * Build a test suite made of test suites for all possible running VM compliances .
	 * 
	 * @see #buildComplianceTestSuite(List, Class, long) for test suite children content.
	 * 
	 * @param testSuiteClass The main test suite to build.
	 * @param setupClass The compiler setup to class to use to bundle given tets suites tests.
	 * @param testClasses The list of test suites to include in main test suite.
	 * @return built test suite (see {@link TestSuite}
	 */
	public static Test buildAllCompliancesTestSuite(Class testSuiteClass, Class setupClass, List testClasses) {
		TestSuite suite = new TestSuite(testSuiteClass.getName());
		int complianceLevels = AbstractBuilderTest.getPossibleComplianceLevels();
		if ((complianceLevels & AbstractBuilderTest.F_1_3) != 0) {
			suite.addTest(buildComplianceTestSuite(testClasses, setupClass, ClassFileConstants.JDK1_3));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_4) != 0) {
			suite.addTest(buildComplianceTestSuite(testClasses, setupClass, ClassFileConstants.JDK1_4));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_5) != 0) {
			suite.addTest(buildComplianceTestSuite(testClasses, setupClass, ClassFileConstants.JDK1_5));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_6) != 0) {
			suite.addTest(buildComplianceTestSuite(testClasses, setupClass, ClassFileConstants.JDK1_6));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_7) != 0) {
			suite.addTest(buildComplianceTestSuite(testClasses, setupClass, ClassFileConstants.JDK1_7));
		}
		return suite;
	}

	/**
	 * Build a test suite for a compliance and a list of test suites.
	 * Children of returned test suite are setup test suites (see {@link BuilderTestSetup}).
	 * Name of returned suite is the given compliance level.
	 * 
	 * @param complianceLevel The compliance level used for this test suite.
	 * @param testClasses The list of test suites to include in main test suite.
	 * @return built test suite (see {@link TestSuite}
	 */
	private static Test buildComplianceTestSuite(List testClasses, Class setupClass, long complianceLevel) {
		TestSuite complianceSuite = new TestSuite();
		for (int i=0, m=testClasses.size(); i<m ; i++) {
			Class testClass = (Class)testClasses.get(i);
			TestSuite suite = new TestSuite(testClass.getName());
			List tests = buildTestsList(testClass);
			for (int index=0, size=tests.size(); index<size; index++) {
				suite.addTest((Test)tests.get(index));
			}
			complianceSuite.addTest(suite);
		}
	
		// call the setup constructor with the suite and compliance level
		try {
			Constructor constructor = setupClass.getConstructor(new Class[]{Test.class, long.class});
			Test setUp = (Test)constructor.newInstance(new Object[]{complianceSuite, new Long(complianceLevel)});
			return setUp;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
	
		return null;
	}

	/**
	 * Build a test suite for a compliance and a list of test suites.
	 * Returned test suite has only one child: {@link RegressionTestSetup} test suite.
	 * Name of returned suite is the given compliance level.
	 * 
	 * @see #buildComplianceTestSuite(List, Class, long) for child test suite content.
	 * 
	 * @param complianceLevel The compliance level used for this test suite.
	 * @param testClasses The list of test suites to include in main test suite.
	 * @return built test suite (see {@link TestSuite}
	 */
	public static Test buildComplianceTestSuite(long complianceLevel, List testClasses) {
		return buildComplianceTestSuite(testClasses, BuilderTestSetup.class, complianceLevel);
	}

	/**
	 * Build a regression test setup suite for a minimal compliance and a test suite to run.
	 * Returned test suite has only one child: {@link RegressionTestSetup} test suite.
	 * Name of returned suite is the name of given test suite class.
	 * The test suite will be run iff the compliance is at least the specified one.
	 * 
	 * @param minimalCompliance The unqie compliance level used for this test suite.
	 * @param evaluationTestClass The test suite to run.
	 * @return built test suite (see {@link TestSuite}
	 */
	public static Test buildMinimalComplianceTestSuite(Class evaluationTestClass, int minimalCompliance) {
		TestSuite suite = new TestSuite(evaluationTestClass.getName());
		int complianceLevels = AbstractBuilderTest.getPossibleComplianceLevels();
		int level13 = complianceLevels & AbstractBuilderTest.F_1_3;
		if (level13 != 0) {
			if (level13 < minimalCompliance) {
				System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_3)+"!");
			} else {
				suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_3));
			}
		}
		int level14 = complianceLevels & AbstractBuilderTest.F_1_4;
		if (level14 != 0) {
			if (level14 < minimalCompliance) {
				System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_4)+"!");
			} else {
				suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_4));
			}
		}
		int level15 = complianceLevels & AbstractBuilderTest.F_1_5;
		if (level15 != 0) {
			if (level15 < minimalCompliance) {
				System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_5)+"!");
			} else {
				suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_5));
			}
		}
		int level16 = complianceLevels & AbstractBuilderTest.F_1_6;
		if (level16 != 0) {
			if (level16 < minimalCompliance) {
				System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_6)+"!");
			} else {
				suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_6));
			}
		}
		int level17 = complianceLevels & AbstractBuilderTest.F_1_7;
		if (level17 != 0) {
			if (level17 < minimalCompliance) {
				System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+CompilerOptions.versionFromJdkLevel(ClassFileConstants.JDK1_7)+"!");
			} else {
				suite.addTest(buildUniqueComplianceTestSuite(evaluationTestClass, ClassFileConstants.JDK1_7));
			}
		}
		return suite;
	}

	public static Test buildTestSuite(Class evaluationTestClass) {
		if (TESTS_PREFIX != null || TESTS_NAMES != null || TESTS_NUMBERS!=null || TESTS_RANGE !=null) {
			return buildTestSuite(evaluationTestClass, highestComplianceLevels());
		}
		return setupSuite(evaluationTestClass);
	}

	public static Test buildTestSuite(Class evaluationTestClass, long complianceLevel) {
		TestSuite suite = new TestSuite(CompilerOptions.versionFromJdkLevel(complianceLevel));
		List tests = buildTestsList(evaluationTestClass);
		for (int index=0, size=tests.size(); index<size; index++) {
			suite.addTest((Test)tests.get(index));
		}
		TestSuite test = new TestSuite(evaluationTestClass.getName());
		test.addTest(new BuilderTestSetup(suite, complianceLevel));
		String className = evaluationTestClass.getName();
		Integer testsNb;
		int newTestsNb = test.countTestCases();
		if ((testsNb = (Integer) TESTS_COUNTERS.get(className)) != null)
			newTestsNb += testsNb.intValue();
		TESTS_COUNTERS.put(className, new Integer(newTestsNb));
		return test;
	}

	/**
	 * Build a regression test setup suite for a compliance and a test suite to run.
	 * Returned test suite has only one child: {@link RegressionTestSetup} test suite.
	 * Name of returned suite is the name of given test suite class.
	 * 
	 * @param uniqueCompliance The unique compliance level used for this test suite.
	 * @param evaluationTestClass The test suite to run.
	 * @return built test suite (see {@link TestSuite}
	 */
	public static Test buildUniqueComplianceTestSuite(Class evaluationTestClass, long uniqueCompliance) {
		long highestLevel = highestComplianceLevels();
		if (highestLevel < uniqueCompliance) {
			System.err.println("Cannot run "+evaluationTestClass.getName()+" at compliance "+highestLevel+"!");
			return new TestSuite();
		}
		TestSuite complianceSuite = new TestSuite(CompilerOptions.versionFromJdkLevel(uniqueCompliance));
		List tests = buildTestsList(evaluationTestClass);
		for (int index=0, size=tests.size(); index<size; index++) {
			complianceSuite.addTest((Test)tests.get(index));
		}
		TestSuite suite = new TestSuite(evaluationTestClass.getName());
		suite.addTest(new BuilderTestSetup(complianceSuite, uniqueCompliance));
		return suite;
	}

	private static Class[] getAllTestClasses() {
		Class[] classes = new Class[] {
				// add all builder classes tests
		};

		if ((AbstractBuilderTest.getPossibleComplianceLevels()  & AbstractBuilderTest.F_1_5) != 0) {
			// add tests case that require 1.5
		}
		return classes;
	}

	/*
	 * Returns the possible compliance levels this VM instance can run.
	 */
	public static int getPossibleComplianceLevels() {
		if (possibleComplianceLevels == -1) {
			String compliance = System.getProperty("compliance");
			if (compliance != null) {
				if (CompilerOptions.VERSION_1_3.equals(compliance)) {
					possibleComplianceLevels = F_1_3;
				} else if (CompilerOptions.VERSION_1_4.equals(compliance)) {
					possibleComplianceLevels = F_1_4;
				} else if (CompilerOptions.VERSION_1_5.equals(compliance)) {
					possibleComplianceLevels = F_1_5;
				} else if (CompilerOptions.VERSION_1_6.equals(compliance)) {
					possibleComplianceLevels = F_1_6;
				} else if (CompilerOptions.VERSION_1_7.equals(compliance)) {
					possibleComplianceLevels = F_1_7;
				} else {
					System.out.println("Invalid compliance specified (" + compliance + ")");
					System.out.print("Use one of ");
					System.out.print(CompilerOptions.VERSION_1_3 + ", ");
					System.out.print(CompilerOptions.VERSION_1_4 + ", ");
					System.out.print(CompilerOptions.VERSION_1_5 + ", ");
					System.out.print(CompilerOptions.VERSION_1_6 + ", ");
					System.out.println(CompilerOptions.VERSION_1_7);
					System.out.println("Defaulting to all possible compliances");
				}
			}
			if (possibleComplianceLevels == -1) {
				possibleComplianceLevels = F_1_3;
				String specVersion = System.getProperty("java.specification.version");
				boolean canRun1_4 = !"1.0".equals(specVersion)
					&& !CompilerOptions.VERSION_1_1.equals(specVersion)
					&& !CompilerOptions.VERSION_1_2.equals(specVersion)
					&& !CompilerOptions.VERSION_1_3.equals(specVersion);
				if (canRun1_4) {
					possibleComplianceLevels |= F_1_4;
				}
				boolean canRun1_5 = canRun1_4 && !CompilerOptions.VERSION_1_4.equals(specVersion);
				if (canRun1_5) {
					possibleComplianceLevels |= F_1_5;
				}
				boolean canRun1_6 = canRun1_5 && !CompilerOptions.VERSION_1_5.equals(specVersion);
				if (canRun1_6) {
					possibleComplianceLevels |= F_1_6;
				}
				boolean canRun1_7 = canRun1_6 && !CompilerOptions.VERSION_1_6.equals(specVersion);
				if (canRun1_7) {
					possibleComplianceLevels |= F_1_7;
				}
			}
		}
		return possibleComplianceLevels;
	}

	/*
	 * Returns the highest compliance level this VM instance can run.
	 */
	public static long highestComplianceLevels() {
		int complianceLevels = AbstractBuilderTest.getPossibleComplianceLevels();
		if ((complianceLevels & AbstractBuilderTest.F_1_7) != 0) {
			return ClassFileConstants.JDK1_7;
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_6) != 0) {
			return ClassFileConstants.JDK1_6;
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_5) != 0) {
			return ClassFileConstants.JDK1_5;
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_4) != 0) {
			return ClassFileConstants.JDK1_4;
		}
		return ClassFileConstants.JDK1_3;
	}

	public static boolean isJRELevel(int compliance) {
		return (AbstractBuilderTest.getPossibleComplianceLevels() & compliance) != 0;
	}
	
	public static Test setupSuite(Class clazz) {
		ArrayList testClasses = new ArrayList();
		testClasses.add(clazz);
		return suite(clazz.getName(), BuilderTestSetup.class, testClasses);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AbstractBuilderTest.class.getName());

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

	/*
	 * Returns a test suite including the tests defined by the given classes for all possible complianceLevels
	 * and using the given setup class (CompilerTestSetup or a subclass)
	 */
	public static Test suite(String suiteName, Class setupClass, ArrayList testClasses) {
		TestSuite all = new TestSuite(suiteName);
		int complianceLevels = AbstractBuilderTest.getPossibleComplianceLevels();
		if ((complianceLevels & AbstractBuilderTest.F_1_3) != 0) {
			all.addTest(suiteForComplianceLevel(ClassFileConstants.JDK1_3, setupClass, testClasses));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_4) != 0) {
			all.addTest(suiteForComplianceLevel(ClassFileConstants.JDK1_4, setupClass, testClasses));
		}
		if ((complianceLevels & AbstractBuilderTest.F_1_5) != 0) {
			all.addTest(suiteForComplianceLevel(ClassFileConstants.JDK1_5, setupClass, testClasses));
		}
		return all;
	}

	/*
	 * Returns a test suite including the tests defined by the given classes for the given complianceLevel 
	 * (see AbstractCompilerTest for valid values) and using the given setup class (CompilerTestSetup or a subclass)
	 */
	public static Test suiteForComplianceLevel(long complianceLevel, Class setupClass, ArrayList testClasses) {
		TestSuite suite;
		Class testClass;
		if (testClasses.size() == 1) {
			suite = new TestSuite(testClass = (Class)testClasses.get(0), CompilerOptions.versionFromJdkLevel(complianceLevel));
			TESTS_COUNTERS.put(testClass.getName(), new Integer(suite.countTestCases()));
		} else {
			suite = new TestSuite(CompilerOptions.versionFromJdkLevel(complianceLevel));
			for (int i = 0, length = testClasses.size(); i < length; i++) {
				TestSuite innerSuite = new TestSuite(testClass = (Class)testClasses.get(i));
				TESTS_COUNTERS.put(testClass.getName(), new Integer(innerSuite.countTestCases()));
				suite.addTest(innerSuite);
			}
		}

		// call the setup constructor with the suite and compliance level
		try {
			Constructor constructor = setupClass.getConstructor(new Class[]{Test.class, String.class});
			Test setUp = (Test)constructor.newInstance(new Object[]{suite, CompilerOptions.versionFromJdkLevel(complianceLevel)});
			return setUp;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.getTargetException().printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected long complianceLevel;
	
	protected EfficiencyCompilerRequestor debugRequestor = null;
	// Output files management
	protected IPath outputRootDirectoryPath = new Path(Util.getOutputDirectory());

	protected File outputTestDirectory;
	
	public AbstractBuilderTest(String name) {
		super(name);
	}


	/**
	 * Concatenate and sort all problems for given root paths.
	 *
	 * @param roots The path to get the problems
	 * @return All sorted problems of all given path
	 */
	IMarker[] allSortedMarkers(IPath[] roots) {
		IMarker[] allMarkers = null;
		for (int i = 0, max=roots.length; i<max; i++) {
			IMarker[] problems = env.getMarkersFor(roots[i]);
			int length = problems.length;
			if (problems.length != 0) {
				if (allMarkers == null) {
					allMarkers = problems;
				} else {
					int all = allMarkers.length;
					System.arraycopy(allMarkers, 0, allMarkers = new IMarker[all+length], 0, all);
					System.arraycopy(problems, 0, allMarkers , all, length);
				}
			}
		}
		if (allMarkers != null) {
			Arrays.sort(allMarkers);
		}
		return allMarkers;
	}
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
	protected void cleanBuild() {
		debugRequestor.clearResult();
		debugRequestor.activate();
		env.cleanBuild();
		debugRequestor.deactivate();
	}

	/**
	 * Create a test specific output directory as a subdirectory of 
	 * outputRootDirectory, given a subdirectory path. The whole 
	 * subtree is created as needed. outputTestDirectoryPath is 
	 * modified according to the latest call to this method.
	 * @param suffixPath a valid relative path for the subdirectory
	 */
	protected void createOutputTestDirectory(String suffixPath) {
		this.outputTestDirectory =  new File(this.outputRootDirectoryPath.toFile(), suffixPath);
		if (!this.outputTestDirectory.exists()) {
			this.outputTestDirectory.mkdirs();
		}
	}

	/** Verifies that given classes have been compiled.
	 */
	protected void expectingCompiledClasses(String[] expected) {
		String[] actual = debugRequestor.getCompiledClasses();
		org.eclipse.jdt.internal.core.util.Util.sort(actual);
		org.eclipse.jdt.internal.core.util.Util.sort(expected);
		expectingCompiling(actual, expected, "unexpected recompiled units"); //$NON-NLS-1$
	}

	private void expectingCompiling(String[] actual, String[] expected, String message) {
		if (DEBUG)
			for (int i = 0; i < actual.length; i++)
				System.out.println(actual[i]);

		StringBuffer actualBuffer = new StringBuffer("{"); //$NON-NLS-1$
		for (int i = 0; i < actual.length; i++) {
			if (i > 0)
				actualBuffer.append(","); //$NON-NLS-1$
			actualBuffer.append(actual[i]);
		}
		actualBuffer.append('}');
		StringBuffer expectedBuffer = new StringBuffer("{"); //$NON-NLS-1$
		for (int i = 0; i < expected.length; i++) {
			if (i > 0)
				expectedBuffer.append(","); //$NON-NLS-1$
			expectedBuffer.append(expected[i]);
		}
		expectedBuffer.append('}');
		assertEquals(message, expectedBuffer.toString(), actualBuffer.toString());
	}

	/** Verifies that given classes have been compiled in the specified order.
	 */
	protected void expectingCompilingOrder(String[] expected) {
		expectingCompiling(debugRequestor.getCompiledClasses(), expected, "unexpected compiling order"); //$NON-NLS-1$
	}

	/**
	 * Verifies that the given element has the expected problems.
	 */
	protected void expectingMarkersFor(IPath root, List expected) {
		expectingMarkersFor(new IPath[] { root }, expected);
	}

	/** Verifies that the given element has problems.
	 */
	protected void expectingMarkersFor(IPath root, String expected) {
		expectingMarkersFor(new IPath[] { root }, expected);
	}

	/**
	 * Verifies that the given elements have the expected problems.
	 */
	protected void expectingMarkersFor(IPath[] roots, List expected) {
		IMarker[] allProblems = allSortedMarkers(roots);
		assertEquals("Invalid problem(s)!!!", arrayToString(expected.toArray()), arrayToString(allProblems));
	}
	
	/** Verifies that the given elements have problems.
	 */
	protected void expectingMarkersFor(IPath[] roots, String expected) {
		IMarker[] problems = allSortedMarkers(roots);
		assertEquals("Invalid problem(s)!!!", expected, arrayToString(problems)); //$NON-NLS-1$
	}

	/** Verifies that the given element has no problems.
	 */
	protected void expectingNoMarkersFor(IPath root) {
		expectingNoMarkersFor(new IPath[] { root });
	}

	/** Verifies that the given elements have no problems.
	 */
	protected void expectingNoMarkersFor(IPath[] roots) {
		StringBuffer buffer = new StringBuffer();
		IMarker[] allProblems = allSortedMarkers(roots);
		if (allProblems != null) {
			for (int i=0, length=allProblems.length; i<length; i++) {
				buffer.append(allProblems[i]+"\n");
			}
		}
		String actual = buffer.toString();
		assertEquals("Unexpected problem(s)!!!", "", actual); //$NON-NLS-1$
	}

	/** Verifies that given element is not present.
	 */
	protected void expectingNoPresenceOf(IPath path) {
		expectingNoPresenceOf(new IPath[] { path });
	}

	/** Verifies that given elements are not present.
	 */
	protected void expectingNoPresenceOf(IPath[] paths) {
		IPath wRoot = env.getWorkspaceRootPath();

		for (int i = 0; i < paths.length; i++)
			assertTrue(paths[i] + " is present", !wRoot.append(paths[i]).toFile().exists()); //$NON-NLS-1$
	}

	/** Verifies that the workspace has no problems.
	 */
	protected void expectingNoProblems() {
		expectingNoMarkersFor(env.getWorkspaceRootPath());
	}

	/** Verifies that the given element has problems and
	 * only the given element.
	 */
	protected void expectingOnlyMarkersFor(IPath expected) {
		expectingOnlyMarkersFor(new IPath[] { expected });
	}

	/** Verifies that the given elements have problems and
	 * only the given elements.
	 */
	protected void expectingOnlyMarkersFor(IPath[] expected) {
		if (DEBUG)
			printProblems();

		IMarker[] rootMarkers = env.getMarkers();
		Hashtable actual = new Hashtable(rootMarkers.length * 2 + 1);
		for (int i = 0; i < rootMarkers.length; i++) {
			IPath culprit = rootMarkers[i].getResource().getFullPath();
			actual.put(culprit, culprit);
		}

		for (int i = 0; i < expected.length; i++)
			if (!actual.containsKey(expected[i]))
				assertTrue("missing expected problem with " + expected[i].toString(), false); //$NON-NLS-1$

		if (actual.size() > expected.length) {
			for (Enumeration e = actual.elements(); e.hasMoreElements();) {
				IPath path = (IPath) e.nextElement();
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

	/** Verifies that the given element has a specific problem and
	 * only the given problem.
	 */
	protected void expectingOnlySpecificMarkerFor(IPath root, IMarker marker) {
		expectingOnlySpecificMarkersFor(root, new IMarker[] { marker });
	}

	/** Verifies that the given element has specifics problems and
	 * only the given problems.
	 */
	protected void expectingOnlySpecificMarkersFor(IPath root, IMarker[] expectedMarkers) {
		if (DEBUG)
			printMarkersFor(root);

		IMarker[] rootProblems = env.getMarkersFor(root);

		for (int i = 0; i < expectedMarkers.length; i++) {
			IMarker expectedProblem = expectedMarkers[i];
			boolean found = false;
			for (int j = 0; j < rootProblems.length; j++) {
				if(expectedProblem.equals(rootProblems[j])) {
					found = true;
					rootProblems[j] = null;
					break;
				}
			}
			if (!found) {
				printMarkersFor(root);
			}
			assertTrue("problem not found: " + expectedProblem.toString(), found); //$NON-NLS-1$
		}
		for (int i = 0; i < rootProblems.length; i++) {
			if(rootProblems[i] != null) {
				printMarkersFor(root);
				assertTrue("unexpected problem: " + rootProblems[i].toString(), false); //$NON-NLS-1$
			}
		}
	}

	/** Verifies that given element is not present.
	 */
	protected void expectingPresenceOf(IPath path) {
		expectingPresenceOf(new IPath[] { path });
	}

	/** Verifies that given elements are not present.
	 */
	protected void expectingPresenceOf(IPath[] paths) {
		IPath wRoot = env.getWorkspaceRootPath();

		for (int i = 0; i < paths.length; i++)
			assertTrue(paths[i] + " is not present", wRoot.append(paths[i]).toFile().exists()); //$NON-NLS-1$
	}

	/** Verifies that the given element has a specific problem.
	 */
	protected void expectingSpecificMarkerFor(IPath root, IMarker problem) {
		expectingSpecificMarkersFor(root, new IMarker[] { problem });
	}

	/** Verifies that the given element has specific problems.
	 */
	protected void expectingSpecificMarkersFor(IPath root, IMarker[] problems) {
		if (DEBUG)
			printMarkersFor(root);

		IMarker[] rootMarkers = env.getMarkersFor(root);
		next : for (int i = 0; i < problems.length; i++) {
			IMarker problem = problems[i];
			for (int j = 0; j < rootMarkers.length; j++) {
				IMarker rootProblem = rootMarkers[j];
				if (rootProblem != null) {
					if (problem.equals(rootProblem)) {
						rootMarkers[j] = null;
						continue next;
					}
				}
			}
			/*
			for (int j = 0; j < rootProblems.length; j++) {
				Problem pb = rootProblems[j];
				if (pb != null) {
					System.out.print("got pb:		new Problem(\"" + pb.getLocation() + "\", \"" + pb.getMessage() + "\", \"" + pb.getResourcePath() + "\"");
					System.out.print(", " + pb.getStart() + ", " + pb.getEnd() +  ", " + pb.getCategoryId()+  ", " + pb.getSeverity());
					System.out.println(")");
				}
			}
			*/
			System.out.println("--------------------------------------------------------------------------------");
			System.out.println("Missing problem while running test "+getName()+":");
			System.out.println("	- expected : " + problem);
			System.out.println("	- current: " + arrayToString(rootMarkers));
			assumeTrue("missing expected problem: " + problem, false);
		}
	}

	/** 
	 * Verifies that the given classes and no others have been compiled, 
	 * but permits the classes to have been compiled more than once.
	 */
	protected void expectingUniqueCompiledClasses(String[] expected) {
		String[] actual = debugRequestor.getCompiledClasses();
		org.eclipse.jdt.internal.core.util.Util.sort(actual);
		// Eliminate duplicate entries
		int dups = 0;
		for (int i = 0; i < actual.length - 1; ++i) {
			if (actual[i + 1].equals(actual[i])) {
				++dups;
				actual[i] = null;
			}
		}
		String[] uniqueActual = new String[actual.length - dups];
		for (int i = 0, j = 0; i < actual.length; ++i) {
			if (actual[i] != null) {
				uniqueActual[j++] = actual[i];
			}
		}
		org.eclipse.jdt.internal.core.util.Util.sort(expected);
		expectingCompiling(uniqueActual, expected, "unexpected compiled units"); //$NON-NLS-1$
	}

	/** Batch builds the workspace.
	 */
	protected void fullBuild() {
		debugRequestor.clearResult();
		debugRequestor.activate();
		env.fullBuild();
		debugRequestor.deactivate();
	}

	/** Batch builds the given project.
	 */
	protected void fullBuild(IPath projectPath) {
		debugRequestor.clearResult();
		debugRequestor.activate();
		env.fullBuild(projectPath);
		debugRequestor.deactivate();
	}

	protected Map getCompilerOptions() {
		Map options = new CompilerOptions().getMap();
		options.put(CompilerOptions.OPTION_ReportUnusedLocal, CompilerOptions.IGNORE);
		if (this.complianceLevel == ClassFileConstants.JDK1_3) {
			options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_3);
			options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_3);
			options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_1);
		} else if (this.complianceLevel == ClassFileConstants.JDK1_4) {
			options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_4);
			options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_4);
			options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_4);
		} else if (this.complianceLevel == ClassFileConstants.JDK1_5) {
			options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_5);
			options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_5);
			options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_5);
		} else if (this.complianceLevel == ClassFileConstants.JDK1_6) {
			options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_6);
			options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_6);
			options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_6);
		} else if (this.complianceLevel == ClassFileConstants.JDK1_7) {
			options.put(CompilerOptions.OPTION_Compliance, CompilerOptions.VERSION_1_7);
			options.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_7);
			options.put(CompilerOptions.OPTION_TargetPlatform, CompilerOptions.VERSION_1_7);
		}
		return options;
	}

	public String getName() {
		String name = super.getName();
		if (this.complianceLevel != 0) {
			name = name + " - " + CompilerOptions.versionFromJdkLevel(this.complianceLevel);
		}
		return name;
	}

	/** Incrementally builds the workspace.
	 */
	protected void incrementalBuild() {
		debugRequestor.clearResult();
		debugRequestor.activate();
		env.incrementalBuild();
		debugRequestor.deactivate();
	}

	/** Incrementally builds the given project.
	 */
	protected void incrementalBuild(IPath projectPath) {
		debugRequestor.clearResult();
		debugRequestor.activate();
		env.incrementalBuild(projectPath);
		debugRequestor.deactivate();
	}

	public void initialize(BuilderTestSetup setUp) {
		this.complianceLevel = setUp.complianceLevel;
	}

	protected void printMarkersFor(IPath root) {
		printMarkersFor(new IPath[] { root });
	}

	protected void printMarkersFor(IPath[] roots) {
		for (int i = 0; i < roots.length; i++) {
			IPath path = roots[i];

			/* get the leaf problems for this type */
			IMarker[] problems = env.getMarkersFor(path);
			System.out.println(arrayToString(problems));
			System.out.println();
		}
	}

	protected void printProblems() {
		printMarkersFor(env.getWorkspaceRootPath());
	}
	/** Sets up this test.
	 */
	protected void setUp() throws Exception {
		super.setUp();

		debugRequestor = new EfficiencyCompilerRequestor();
		Compiler.DebugRequestor = debugRequestor;
		if (env == null) {
			env = new TestingEnvironment();
			env.openEmptyWorkspace();
		}
		env.resetWorkspace();

	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		env.resetWorkspace();
		JavaCore.setOptions(JavaCore.getDefaultOptions());
		super.tearDown();
	}

	protected String testName() {
		return super.getName();
	}

	/*
	 * Write given source test files in current output sub-directory.
	 * Use test name for this sub-directory name (ie. test001, test002, etc...)
	 */
	protected void writeFiles(String[] testFiles) {
		createOutputTestDirectory(testName());

		// Write each given test file
		for (int i = 0, length = testFiles.length; i < length; ) {
			String fileName = testFiles[i++];
			String contents = testFiles[i++];
			File file = new File(this.outputTestDirectory, fileName);
			if (fileName.lastIndexOf('/') >= 0) {
				File dir = file.getParentFile();
				if (!dir.exists()) {
					dir.mkdirs();
				}
			}
			Util.writeToFile(contents, file.getPath());
		}
	}
}
