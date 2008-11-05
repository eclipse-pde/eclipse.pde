package org.eclipse.pde.api.tools.builder.tests.tags;

import junit.framework.Test;

import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.pde.api.tools.builder.tests.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

/**
 * Tests invalid duplicate tags placed on members
 * 
 * @since 1.0.0
 */
public class InvalidDuplicateTagsTests extends TagTest {

	private int fPid = -1;
	
	/**
	 * Constructor
	 * @param name
	 */
	public InvalidDuplicateTagsTests(String name) {
		super(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		return fPid;
	}

	/**
	 * Must be called before a call {@link #getDefaultProblemId()}
	 * @param element
	 * @param kind
	 */
	private void setProblemId(int element, int kind) {
		fPid = ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_USAGE, 
				element, 
				kind, 
				IApiProblem.NO_FLAGS);
	}
	
	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidDuplicateTagsTests.class);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTests#getTestSourcePath()
	 */
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("duplicates");
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestCompliance()
	 */
	protected String getTestCompliance() {
		return CompilerOptions.VERSION_1_5;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#assertProblems(org.eclipse.pde.api.tools.builder.tests.ApiProblem[])
	 */
	protected void assertProblems(ApiProblem[] problems) {
		String message = null;
		for(int i = 0; i < problems.length; i++) {
			message = problems[i].getMessage();
			assertTrue("The problem message is not correct: "+message, message.endsWith("already defined on this element"));
		}
	}
	
	/**
	 * Tests a class that has duplicate tags is properly detected using an incremental build 
	 */
	public void testClassWithDuplicateTagsI() {
		x1(true);
	}
	
	/**
	 * Tests a class that has duplicate tags is properly detected using a full build
	 */
	public void testClassWithDuplicateTagsF() {
		x1(false);
	}
	
	private void x1(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, 
				"test1", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that an interface with duplicate tags is properly using an incremental build
	 */
	public void testInterfaceWithDuplicateTagsI() {
		x2(true);
	}
	
	/**
	 * Tests that an interface with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceWithDuplicateTagsF() {
		x2(false);
	}
	
	private void x2(boolean inc) {
		setProblemId(IElementDescriptor.TYPE, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, 
				"test2", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a class field with duplicate tags is properly detected using an incremental build
	 */
	public void testClassFieldWithDuplicateTagsI() {
		x3(true);
	}
	
	/**
	 * Tests that a class field with duplicate tags is properly detected using a full build
	 */
	public void testClassFieldWithDuplicateTagsF() {
		x3(false);
	}
	
	private void x3(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, 
				"test3", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that an interface field with duplicate tags is properly detected using an incremental build
	 */
	public void testInterfaceFieldWithDuplicateTagsI() {
		x4(true);
	}
	
	/**
	 * Tests that an interface field with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceFieldWithDuplicateTagsF() {
		x4(false);
	}
	
	private void x4(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(3));
		deployTagTest(TESTING_PACKAGE, 
				"test4", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that an enum field with duplicate tags is properly detected using an incremental build
	 */
	public void testEnumFieldWithDuplicateTagsI() {
		x5(true);
	}
	
	/**
	 * Tests that an enum field with duplicate tags is properly detected using a full build
	 */
	public void testEnumFieldWithDuplicateTagsF() {
		x5(false);
	}
	
	private void x5(boolean inc) {
		setProblemId(IElementDescriptor.FIELD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(4));
		deployTagTest(TESTING_PACKAGE, 
				"test5", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that a class method with duplicate tags is properly detected using an incremental build
	 */
	public void testClassMethoddWithDuplicateTagsI() {
		x6(true);
	}
	
	/**
	 * Tests that a class method with duplicate tags is properly detected using a full build
	 */
	public void testClassMethodWithDuplicateTagsF() {
		x6(false);
	}
	
	private void x6(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, 
				"test6", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using an incremental build
	 */
	public void testInterfaceMethoddWithDuplicateTagsI() {
		x7(true);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using a full build
	 */
	public void testInterfaceMethodWithDuplicateTagsF() {
		x7(false);
	}
	
	private void x7(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, 
				"test7", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
	
	/**
	 * Tests that an enum method with duplicate tags is properly detected using an incremental build
	 */
	public void testEnumMethodWithDuplicateTagsI() {
		x8(true);
	}
	
	/**
	 * Tests that an interface method with duplicate tags is properly detected using a full build
	 */
	public void testEnumMethodWithDuplicateTagsF() {
		x8(false);
	}
	
	private void x8(boolean inc) {
		setProblemId(IElementDescriptor.METHOD, IApiProblem.DUPLICATE_TAG_USE);
		setExpectedProblemIds(getDefaultProblemSet(6));
		deployTagTest(TESTING_PACKAGE, 
				"test8", 
				true, 
				(inc ? IncrementalProjectBuilder.INCREMENTAL_BUILD : IncrementalProjectBuilder.FULL_BUILD), 
				true);
	}
}
