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
package org.eclipse.pde.api.tools.builder.tests.usage;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

import junit.framework.Test;

/**
 * These tests are incremental builder tests that add / remove / change
 * restrictions on elements known to be used by other bundles
 * and ensure problems are updated accordingly on dependent types
 *
 * @since 1.0.1
 */
public class DependentUsageTests extends UsageTest {

	static final String WITHOUTTAG = "withouttag"; //$NON-NLS-1$
	static final String WITHTAG = "withtag"; //$NON-NLS-1$
	static final IPath C_PATH = new Path("/refproject/src/c/"); //$NON-NLS-1$
	static final IPath F_PATH = new Path("/refproject/src/f/"); //$NON-NLS-1$
	static final IPath I_PATH = new Path("/refproject/src/i/"); //$NON-NLS-1$
	static final IPath M_PATH = new Path("/refproject/src/m/"); //$NON-NLS-1$
	static final IPath XYZ_PATH = new Path("/usagetests/src/x/y/z/"); //$NON-NLS-1$
	static final IPath MPPATH = new Path("/refproject/src/pack/multi/part"); //$NON-NLS-1$

	public DependentUsageTests(String name) {
		super(name);
	}

	public static Test suite() {
		return buildTestSuite(DependentUsageTests.class);
	}

	@Override
	protected int getDefaultProblemId() {
		return 0;
	}

	protected IPath getTestSourcePath(String path) {
		return super.getTestSourcePath().append("dependent").append(path); //$NON-NLS-1$
	}

	/**
	 * Returns the type name from the {@link #WITHTAG} location in a test folder
	 * @param test the test context
	 * @param context the sub folder of the test where the type name should be looked up
	 * @param typename the type name to append to the path
	 * @return the path to the given type name for the {@link #WITHTAG} folder in the given test folder
	 */
	protected IPath getReplacementType(String test, String context, String typename) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath(test)).append(context).append(typename);
	}

	/**
	 * Returns the {@link IPath} to the *.java file to deploy
	 * @param test
	 * @return the path to the test source given the test name
	 */
	protected IPath getTestSource(String test) {
		return TestSuiteHelper.getPluginDirectoryPath().append(TEST_SOURCE_ROOT).append(getTestSourcePath(test)).append(test).addFileExtension("java"); //$NON-NLS-1$
	}

	/**
	 * Deploys the test
	 * @param test the name of the test
	 * @param usepath the local workspace path to place the referencing type in
	 * @param refpath the workspace relative path for the ref type to be added to
	 * @param refname the '/' qualified name of the reference type to update
	 * @param addtag if the the restricted tag is to be added or removed for the test
	 * @throws Exception if something bad happens
	 */
	protected void deployTest(String test, IPath usepath, IPath refpath, String refname, boolean addtag) throws Exception {
		try {
			getEnv().setAutoBuilding(false);
			//copy source files and build
			IPath updatepath = refpath.append(refname);
			IPath path = (addtag ? getReplacementType(test, WITHOUTTAG, refname) : getReplacementType(test, WITHTAG, refname));
			//create the referenced file
			createWorkspaceFile(updatepath, path);
			//create the referencing file
			createWorkspaceFile(usepath.append(test+".java"), getTestSource(test)); //$NON-NLS-1$
			fullBuild();
			expectingNoJDTProblems();
			if(addtag) {
				expectingNoProblems();
			}
			else {
				assertProblems(getEnv().getProblems());
			}
			//do replacement with the alternate and build
			path = (addtag ? getReplacementType(test, WITHTAG, refname) : getReplacementType(test, WITHOUTTAG, refname));
			updateWorkspaceFile(updatepath, path);
			incrementalBuild();
			expectingNoJDTProblems();
			if(addtag) {
				assertProblems(getEnv().getProblems());
			}
			else {
				expectingNoProblems();
			}
		}
		finally {
			getEnv().setAutoBuilding(true);
		}
	}

	/**
	 * Tests adding an @noextend restriction to a class known to be used
	 * by another bundle
	 *
	 * Uses test1.java and classref.java
	 */
	public void testAddExtendRestriction() throws Exception {
		test1(true);
	}

	/**
	 * Tests removing an @noextend restriction from a class known
	 * to be used by another bundle
	 *
	 * Uses test1.java and classref.java
	 */
	public void testRemoveExtendRestriction() throws Exception {
		test1(false);
	}

	private void test1(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {{"classref", "test1"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployTest("test1", XYZ_PATH, C_PATH, "classref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @noimplement restriction to an interface known
	 * to be used by another bundle
	 *
	 * Uses test2.java and interref.java
	 */
	public void testAddImplementsRestriction() throws Exception {
		test2(true);
	}

	/**
	 * Tests removing an @noimplement restriction from an interface known
	 * to be used by another bundle
	 *
	 * Uses test2.java and interref.java
	 */
	public void testRemoveImplementRestriction() throws Exception {
		test2(false);
	}

	private void test2(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {{"interref", "test2"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployTest("test2", XYZ_PATH, I_PATH, "interref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @noinstantiate restriction to a class known to be used
	 * by another bundle
	 *
	 * Uses test3.java and classref.java
	 */
	public void testAddInstantiateRestriction() throws Exception {
		test3(true);
	}

	/**
	 * Tests removing an @noinstantiate restriction from a class known to be used by
	 * another bundle
	 *
	 * Uses test3.java and classref.java
	 */
	public void testRemoveInstantiateRestriction() throws Exception {
		test3(false);
	}

	private void test3(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {{"classref", "test3"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployTest("test3", XYZ_PATH, C_PATH, "classref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @noreference restriction to a constructor known to be called
	 * by another bundle
	 *
	 * Uses test4.java and constref.java
	 */
	public void testAddReferenceConstructorRestriction() throws Exception {
		test4(true);
	}

	/**
	 * Tests removing an @noreference restriction to a constructor known to be called
	 * by another bundle
	 *
	 * Uses test4.java and constref.java
	 */
	public void testRemoveReferenceConstructorRestriction() throws Exception {
		test4(false);
	}

	private void test4(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.CONSTRUCTOR_METHOD)
		});
		setExpectedMessageArgs(new String[][] {{"constref()", "test4"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployTest("test4", XYZ_PATH, M_PATH, "constref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @noreference restriction to a field known
	 * to be used by another bundle
	 *
	 * Uses test5.java and fieldref.java
	 */
	public void testAddReferenceFieldRestriction() throws Exception {
		test5(true);
	}

	/**
	 * Tests removing an @noreference restriction to a field
	 * known to be used by another bundle
	 *
	 * Uses test5.java and fieldref.java
	 */
	public void testRemoveReferenceFieldRestriction() throws Exception {
		test5(false);
	}

	private void test5(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.FIELD)
		});
		setExpectedMessageArgs(new String[][] {{"fieldref", "test5", "number"}}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		deployTest("test5", XYZ_PATH, F_PATH, "fieldref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @noreference restriction to method
	 * known to be used by another bundle
	 *
	 * Uses test6.java and methodref.java
	 */
	public void testAddReferenceMethodRestriction() throws Exception {
		test6(true);
	}

	/**
	 * Tests removing an @noreference restriction to a method
	 * known to be used by another bundle
	 *
	 * Uses test6.java and methodref.java
	 */
	public void testRemoveReferenceMethodRestriction() throws Exception {
		test6(false);
	}

	private void test6(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD)
		});
		setExpectedMessageArgs(new String[][] {{"methodref", "test6", "m1()"}}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		deployTest("test6", XYZ_PATH, M_PATH, "methodref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding an @nooverride restriction to a method
	 * known to be used by another bundle
	 *
	 * Uses test7.java and methodref.java
	 */
	public void testAddOverrideRestriction() throws Exception {
		test7(true);
	}

	/**
	 * Tests removing @nooverride restriction to a method
	 * known to be used by another bundle
	 *
	 * Uses test7.java and methodref.java
	 */
	public void testRemoveOverrideRestriction() throws Exception {
		test7(false);
	}

	private void test7(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {{"methodref", "test7", "m1()"}}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		deployTest("test7", XYZ_PATH, M_PATH, "methodref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding @noextend restriction to an interface
	 * known to be used by another bundle
	 *
	 * Uses test8.java and interref.java
	 */
	public void testAddExtendInterfaceRestriction() throws Exception {
		test8(true);
	}

	/**
	 * Tests adding @noextend restriction to an interface
	 * known to be used by another bundle
	 *
	 * Uses test8.java and interref.java
	 */
	public void testRemoveExtendInterfaceRestriction() throws Exception {
		test8(false);
	}

	private void test8(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {{"interref", "test8"}}); //$NON-NLS-1$ //$NON-NLS-2$
		deployTest("test8", XYZ_PATH, I_PATH, "interref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding @noextend AND @noinstantiate tags to a class known to be used
	 * by another bundle
	 *
	 *  Uses test9.java and classref.java
	 */
	public void testAddExtendInstantiateRestriction() throws Exception {
		test9(true);
	}

	/**
	 * Tests removing @noextend AND @noinstantiate tags to a class known to be used
	 * by another bundle
	 *
	 * Uses test9.java and classref.java
	 */
	public void testRemoveExtendInstantiateRestriction() throws Exception {
		test9(false);
	}

	private void test9(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {
					{"classref", "test9"}, //$NON-NLS-1$ //$NON-NLS-2$
					{"classref", "test9"} //$NON-NLS-1$ //$NON-NLS-2$
				});
		deployTest("test9", XYZ_PATH, C_PATH, "classref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests adding @noextend AND @noimplement tags to an interface known to be used
	 * by another bundle
	 *
	 *  Uses test10.java and interref.java
	 */
	public void testAddExtendImplementRestriction() throws Exception {
		test10(true);
	}

	/**
	 * Tests removing @noextend AND @noimplement tags to an interface known to be used
	 * by another bundle
	 *
	 * Uses test10.java and interref.java
	 */
	public void testRemoveExtendImplementRestriction() throws Exception {
		test10(false);
	}

	private void test10(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS),
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {
					{"interref", "test10"}, //$NON-NLS-1$ //$NON-NLS-2$
					{"interref", "clazz"} //$NON-NLS-1$ //$NON-NLS-2$
				});
		deployTest("test10", XYZ_PATH, I_PATH, "interref.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * tests adding an @noextend restriction to a type in a multi-part package name
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=296375
	 *
	 * @throws Exception
	 */
	public void testAddExtendRestrictionMultiPartPackageName() throws Exception {
		test11(true);
	}

	/**
	 * tests removing an @noextend restriction to a type in a multi-part package name
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=296375
	 *
	 * @throws Exception
	 */
	public void testRemoveExtendRestrictionMultiPartPackageName() throws Exception {
		test11(false);
	}

	private void test11(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {
				{"mpClassRef", "test11"} //$NON-NLS-1$ //$NON-NLS-2$
			});
		deployTest("test11", XYZ_PATH, MPPATH, "mpClassRef.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * tests adding an @noinstantiate restriction to a type in a multi-part package name
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=296375
	 *
	 * @throws Exception
	 */
	public void testAddInstantiateRestrictionMultiPartPackageName() throws Exception {
		test12(true);
	}

	/**
	 * tests removing an @noinstantiate restriction to a type in a multi-part package name
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=296375
	 *
	 * @throws Exception
	 */
	public void testRemoveInstantiateRestrictionMultiPartPackageName() throws Exception {
		test12(false);
	}

	private void test12(boolean addtag) throws Exception {
		setExpectedProblemIds(new int[] {
				ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE, IApiProblem.NO_FLAGS)
		});
		setExpectedMessageArgs(new String[][] {
				{"mpClassRef", "test12"} //$NON-NLS-1$ //$NON-NLS-2$
			});
		deployTest("test12", XYZ_PATH, MPPATH, "mpClassRef.java", addtag); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
