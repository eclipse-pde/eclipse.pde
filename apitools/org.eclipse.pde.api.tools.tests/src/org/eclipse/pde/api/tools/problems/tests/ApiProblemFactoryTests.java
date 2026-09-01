/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.problems.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.junit.jupiter.api.Test;

/**
 * Tests various aspects of the {@link ApiProblemFactory}
 *
 * @since 1.0.0
 */
public class ApiProblemFactoryTests extends AbstractApiTest {

	String fDefaultMessage = null;
	{
		String unknownMessage = BuilderMessages.ApiProblemFactory_problem_message_not_found;
		fDefaultMessage = unknownMessage.substring(0, unknownMessage.lastIndexOf('{'));
	}

	/**
	 * Tests that the hashcodes from an {@link IApiProblem} and an
	 * {@link IApiProblemFilter} handle are the same.
	 *
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=404173"
	 * @since 1.0.400
	 */
	@Test
	public void testGethashcode1() throws Exception {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", "x.y.z.myclazz", //$NON-NLS-1$ //$NON-NLS-2$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND); //$NON-NLS-1$
		ApiProblemFilter filter = (ApiProblemFilter) ApiProblemFactory.newProblemFilter("mycomp", problem, "test comment"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(problem.hashCode() == ApiProblemFactory.getProblemHashcode(filter.getHandle()), "The hashcodes must be identical"); //$NON-NLS-1$
	}

	/**
	 * Tests that the hashcodes from an {@link IApiProblem} and an
	 * {@link IApiProblemFilter} handle are the same.
	 *
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=404173"
	 * @since 1.0.400
	 */
	@Test
	public void testGethashcode2() throws Exception {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", null, //$NON-NLS-1$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND); //$NON-NLS-1$
		ApiProblemFilter filter = (ApiProblemFilter) ApiProblemFactory.newProblemFilter("mycomp", problem, "test comment"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(problem.hashCode() == ApiProblemFactory.getProblemHashcode(filter.getHandle()), "The hashcodes must be identical"); //$NON-NLS-1$
	}

	/**
	 * Tests that the hashcodes from an {@link IApiProblem} and an
	 * {@link IApiProblemFilter} handle are the same. <br>
	 * <br>
	 * This test is expected to not be equal since you could never have a class
	 * named <code>null</code>
	 *
	 * @see "https://bugs.eclipse.org/bugs/show_bug.cgi?id=404173"
	 * @since 1.0.400
	 */
	@Test
	public void testGethashcode3() throws Exception {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", "null", //$NON-NLS-1$ //$NON-NLS-2$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND); //$NON-NLS-1$
		ApiProblemFilter filter = (ApiProblemFilter) ApiProblemFactory.newProblemFilter("mycomp", problem, "test comment"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(problem.hashCode() != ApiProblemFactory.getProblemHashcode(filter.getHandle()), "The hashcodes must be identical"); //$NON-NLS-1$
	}

	/**
	 * Tests that creating an {@link IApiProblem} does not fail
	 */
	@Test
	public void testCreateProblem() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "a new problem should have been created with null attributes"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, //$NON-NLS-1$
				IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "a new problem should have been created with non-null attributes"); //$NON-NLS-1$
	}

	/**
	 * Tests creating a new {@link IApiProblem} using the usage specialized
	 * factory method
	 */
	@Test
	public void tesCreateUsageProblem() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with null attributes"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiUsageProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, //$NON-NLS-1$
				IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with non-null attributes"); //$NON-NLS-1$
	}

	/**
	 * Tests creating a new {@link IApiProblem} using the since tag specialized
	 * factory method
	 */
	@Test
	public void testCreateSincetagProblem() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with null attributes"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiSinceTagProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, //$NON-NLS-1$
				IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with non-null attributes"); //$NON-NLS-1$
	}

	/**
	 * Tests creating a new {@link IApiProblem} using the version number
	 * specialized factory method
	 */
	@Test
	public void testCreateVersionProblem() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with null attributes"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiVersionNumberProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, //$NON-NLS-1$
				IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull(problem, "a new problem should have been created with non-null attributes"); //$NON-NLS-1$
	}

	/**
	 * Tests the new
	 * {@link ApiProblemFactory#newFatalProblem(String, String[], int)} method
	 *
	 * @since 1.1
	 */
	@Test
	public void testCreateFatalProblem() {
		IApiProblem problem = ApiProblemFactory.newFatalProblem(null, null, IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM);
		assertNotNull(problem, "a new problem should have been created with null attributes"); //$NON-NLS-1$
		assertTrue(ApiProblemFactory.getProblemCategory(problem.getId()) == IApiProblem.CATEGORY_FATAL_PROBLEM, "The category must be CATEGORY_FATAL_PROBLEM"); //$NON-NLS-1$
		assertTrue(ApiProblemFactory.getProblemElementKind(problem.getId()) == IElementDescriptor.RESOURCE, "The element kind must be RESOURCE"); //$NON-NLS-1$
		assertTrue(ApiProblemFactory.getProblemKind(problem.getId()) == IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM, "The element kind must be FATAL_JDT_BUILDPATH_PROBLEM"); //$NON-NLS-1$
	}

	/**
	 * Test getting version number problem messages
	 */
	@Test
	public void testGetVersionMessages() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem("", null, //$NON-NLS-1$
				new String[] { "1", "2" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.MAJOR_VERSION_CHANGE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should be a new problem created"); //$NON-NLS-1$
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("", null, //$NON-NLS-1$
				new String[] { "1", "2" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should be a new problem created"); //$NON-NLS-1$
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("", null, //$NON-NLS-1$
				new String[] { "1", "2" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.MINOR_VERSION_CHANGE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should be a new problem created"); //$NON-NLS-1$
		validateProblem(2, problem);
	}

	private void validateProblem(int argumentsSize, IApiProblem apiProblem) {
		String message = apiProblem.getMessage();
		assertNotNull(message, "the message should not be null"); //$NON-NLS-1$
		assertFalse(message.startsWith(this.fDefaultMessage), "the message should be correct"); //$NON-NLS-1$
		assertEquals(argumentsSize, apiProblem.getMessageArguments().length, "Wrong argument size"); //$NON-NLS-1$
	}

	/**
	 * Tests getting API usage problem messages
	 */
	@Test
	public void testGetUsageMessages() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", null, //$NON-NLS-1$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND); //$NON-NLS-1$
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, //$NON-NLS-1$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT); //$NON-NLS-1$
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, //$NON-NLS-1$
				new String[] { "foo", "bar" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_OVERRIDE); //$NON-NLS-1$ //$NON-NLS-2$
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, //$NON-NLS-1$
				new String[] { "foo" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE); //$NON-NLS-1$
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiProblem("", null, //$NON-NLS-1$
				new String[] { "foo", "bar" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD); //$NON-NLS-1$ //$NON-NLS-2$
		validateProblem(2, problem);
	}

	/**
	 * Tests getting (some of) the binary messages
	 */
	@Test
	public void testGetBinaryMessages() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, new String[] {
				"X", "X()" }, null, null, -1, -1, -1, //$NON-NLS-1$ //$NON-NLS-2$
				IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.CONSTRUCTOR);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiProblem(null, null, new String[] {
				"X", "foo()" }, null, null, -1, -1, -1, //$NON-NLS-1$ //$NON-NLS-2$
				IApiProblem.CATEGORY_COMPATIBILITY, IDelta.INTERFACE_ELEMENT_TYPE, IDelta.ADDED, IDelta.METHOD);
		validateProblem(2, problem);
	}

	/**
	 * Tests getting since tag problem messages
	 */
	@Test
	public void testGetSinceTagMessages() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem("", //$NON-NLS-1$
				null, new String[] { "A", "B", "C" }, null, null, -1, -1, -1, IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_INVALID); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		validateProblem(3, problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("", //$NON-NLS-1$
				null, new String[] { "A", "B" }, null, null, -1, -1, -1, IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_MALFORMED); //$NON-NLS-1$ //$NON-NLS-2$
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("", //$NON-NLS-1$
				null, new String[] { "A" }, null, null, -1, -1, -1, IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_MISSING); //$NON-NLS-1$
		validateProblem(1, problem);
	}

	/**
	 * Tests that the custom message for a constructor parameter can be acquired
	 */
	@Test
	public void testGetLeakConstructorParamMessage() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", //$NON-NLS-1$
				null, new String[] { "fooconstructor" }, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.API_LEAK, IApiProblem.LEAK_CONSTRUCTOR_PARAMETER); //$NON-NLS-1$
		validateProblem(1, problem);
	}

	/**
	 * Tests the
	 * {@link ApiProblemFactory#newApiComponentResolutionProblem(String, String[], String[], Object[], int, int)}
	 * method
	 */
	@Test
	public void testCreateComponentresolutionProblem() {
		IApiProblem problem = ApiProblemFactory.newApiComponentResolutionProblem("", //$NON-NLS-1$
				null, null, null, IElementDescriptor.COMPONENT, IApiProblem.API_COMPONENT_RESOLUTION);
		assertNotNull(problem, "there should be a new problem created"); //$NON-NLS-1$
		validateProblem(0, problem);
	}

	/**
	 * Tests outliers of the
	 * {@link ApiProblemFactory#getProblemMessageId(int, int, int, int)} method
	 */
	@Test
	public void testGetProblemMessageId() {
		int id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_API_BASELINE, -1, -1, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_VERSION, -1, -1, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, -1, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_IMPLEMENT, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_EXTEND, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_REFERENCE, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.API_LEAK, -1);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, -1);
		assertEquals(36, id, "The returned id should be 36"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.METHOD);
		assertEquals(33, id, "The returned id should be 33"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.CONSTRUCTOR_METHOD);
		assertEquals(34, id, "The returned id should be 34"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.FIELD);
		assertEquals(35, id, "The returned id should be 35"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD);
		assertEquals(39, id, "The returned id should be 39"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.ADDED, 0);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.CHANGED, 0);
		assertEquals(0, id, "The returned id should be 0"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.REMOVED, IDelta.TYPE_ARGUMENTS);
		assertEquals(103, id, "The returned id should be 103"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION, -1, IApiProblem.API_COMPONENT_RESOLUTION, -1);
		assertEquals(99, id, "The returned id should be 99"); //$NON-NLS-1$
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_FATAL_PROBLEM, -1, IApiProblem.FATAL_JDT_BUILDPATH_PROBLEM, -1);
		assertEquals(31, id, "The returned id should be 31"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblemFactory#getProblemSeverityId(IApiProblem)}
	 */
	@Test
	public void testGetProblemSeverityId() {
		IApiProblem problem = ApiProblemFactory.newApiComponentResolutionProblem("", null, null, null, IElementDescriptor.COMPONENT, IApiProblem.API_COMPONENT_RESOLUTION); //$NON-NLS-1$
		assertEquals(IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT, ApiProblemFactory.getProblemSeverityId(problem), "the problem id should be: " + IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiComponentResolutionProblem("", null, null, null, IElementDescriptor.COMPONENT, 0); //$NON-NLS-1$
		assertNull(ApiProblemFactory.getProblemSeverityId(problem), "the id must be null"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiBaselineProblem("", null, null, IElementDescriptor.RESOURCE, 0); //$NON-NLS-1$
		assertNull(ApiProblemFactory.getProblemSeverityId(problem), "the id must be null"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiSinceTagProblem("", null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, 0); //$NON-NLS-1$
		assertNull(ApiProblemFactory.getProblemSeverityId(problem), "the id must be null"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.API_LEAK);
		assertNull(ApiProblemFactory.getProblemSeverityId(problem), "the id must be null"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES);
		assertEquals(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, ApiProblemFactory.getProblemSeverityId(problem), "the problem id should be: " + IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, 0);
		assertNull(ApiProblemFactory.getProblemSeverityId(problem), "the id must be null"); //$NON-NLS-1$
	}
}
