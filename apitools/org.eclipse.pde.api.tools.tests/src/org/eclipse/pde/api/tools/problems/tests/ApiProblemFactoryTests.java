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
package org.eclipse.pde.api.tools.problems.tests;

import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

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
	 * Tests that creating an {@link IApiProblem} does not fail
	 */
	public void testCreateProblem() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, 
				IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, 
				IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}

	/**
	 * Tests creating a new {@link IApiProblem} using the usage specialized factory method
	 */
	public void tesCreateUsageProblem() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiUsageProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests creating a new {@link IApiProblem} using the since tag specialized factory method
	 */
	public void testCreateSincetagProblem() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem(null, null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests creating a new {@link IApiProblem} using the version number specialized factory method
	 */
	public void testCreateVersionProblem() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem(null, null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("path", null, new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests that we can get the correct kind from the preference key
	 */
	public void testGetKindFromPref() {
		int kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ILLEGAL_OVERRIDE);
		assertTrue("the kind should be illegal override", kind == IApiProblem.ILLEGAL_OVERRIDE);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ILLEGAL_EXTEND);
		assertTrue("the kind should be illegal extend", kind == IApiProblem.ILLEGAL_EXTEND);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ILLEGAL_REFERENCE);
		assertTrue("the kind should be illegal reference", kind == IApiProblem.ILLEGAL_REFERENCE);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ILLEGAL_IMPLEMENT);
		assertTrue("the kind should be illegal implement", kind == IApiProblem.ILLEGAL_IMPLEMENT);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ILLEGAL_INSTANTIATE);
		assertTrue("the kind should be illegal instantiate", kind == IApiProblem.ILLEGAL_INSTANTIATE);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ANNOTATION_CHANGED_TYPE_CONVERSION);
		assertTrue("the kind should be CHANGED", kind == IDelta.CHANGED);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.TYPE_PARAMETER_ADDED_CLASS_BOUND);
		assertTrue("the kind should be ADDED", kind == IDelta.ADDED);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND);
		assertTrue("the kind should be REMOVED", kind == IDelta.REMOVED);
	}
	
	/**
	 * Test getting version number problem messages
	 */
	public void testGetVersionMessages() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem("", null, 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.TYPE, IApiProblem.MAJOR_VERSION_CHANGE);
		assertNotNull("there should be a new problem created", problem);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("", null, 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.TYPE, IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE);
		assertNotNull("there should be a new problem created", problem);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("", null, 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.TYPE, IApiProblem.MINOR_VERSION_CHANGE);
		assertNotNull("there should be a new problem created", problem);
		validateProblem(2, problem);
	}
	
	private void validateProblem(int argumentsSize, IApiProblem apiProblem) {
		String message = apiProblem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be correct", message.startsWith(this.fDefaultMessage));
		assertEquals("Wrong argument size", argumentsSize, apiProblem.getMessageArguments().length);
	}

	/**
	 * Tests getting API usage problem messages
	 */
	public void testGetUsageMessages() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", null, 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_EXTEND);
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, 
				new String[] {"foo", "bar"}, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_OVERRIDE);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiUsageProblem("", null, 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_INSTANTIATE);
		validateProblem(1, problem);
		problem = ApiProblemFactory.newApiProblem("", null, 
				new String[] {"foo", "bar"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE,IElementDescriptor.TYPE, IApiProblem.ILLEGAL_REFERENCE, IApiProblem.METHOD);
		validateProblem(2, problem);
	}
	
	/**
	 * Tests getting (some of) the binary messages
	 */
	public void testGetBinaryMessages() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, new String[] {"X", "X()"}, null, null, -1, -1, -1,  
				IApiProblem.CATEGORY_COMPATIBILITY, IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.CONSTRUCTOR);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiProblem(null, null, new String[] {"X", "foo()"}, null, null, -1, -1, -1,  
				IApiProblem.CATEGORY_COMPATIBILITY, IDelta.INTERFACE_ELEMENT_TYPE, IDelta.ADDED, IDelta.METHOD);
		validateProblem(2, problem);
	}
	
	/**
	 * Tests getting since tag problem messages
	 */
	public void testGetSinceTagMessages() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem("", 
				null, new String[] {"A", "B", "C"}, null, null, -1, -1, -1,  IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_INVALID);
		validateProblem(3, problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("", 
				null, new String[] {"A", "B"}, null, null, -1, -1, -1,  IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_MALFORMED);
		validateProblem(2, problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("", 
				null, new String[] {"A"}, null, null, -1, -1, -1,  IElementDescriptor.RESOURCE, IApiProblem.SINCE_TAG_MISSING);
		validateProblem(1, problem);
	}
	
	/**
	 * Tests that the custom message for a constructor parameter can be acquired
	 */
	public void testGetLeakConstructorParamMessage() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", 
				null, new String[] {"fooconstructor"}, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.API_LEAK, IApiProblem.LEAK_CONSTRUCTOR_PARAMETER);
		validateProblem(1, problem);
	}
	
	/**
	 * Tests the {@link ApiProblemFactory#newApiComponentResolutionProblem(String, String[], String[], Object[], int, int)} method
	 */
	public void testCreateComponentresolutionProblem() {
		IApiProblem problem = ApiProblemFactory.newApiComponentResolutionProblem("", 
				null, null, null, IElementDescriptor.COMPONENT, IApiProblem.API_COMPONENT_RESOLUTION);
		assertNotNull("there should be a new problem created", problem);
		validateProblem(0, problem);
	}
	
	/**
	 * Tests outliers of the {@link ApiProblemFactory#getProblemMessageId(int, int, int, int)} method
	 */
	public void testGetProblemMessageId() {
		int id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_API_BASELINE, -1, -1, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_VERSION, -1, -1, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, -1, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_IMPLEMENT, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_EXTEND, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.ILLEGAL_REFERENCE, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.API_LEAK, -1);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, -1);
		assertEquals("The returned id should be 36", 36, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.METHOD);
		assertEquals("The returned id should be 33", 33, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.CONSTRUCTOR_METHOD);
		assertEquals("The returned id should be 34", 34, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_USAGE, -1, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.FIELD);
		assertEquals("The returned id should be 35", 35, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.ADDED, IDelta.FIELD);
		assertEquals("The returned id should be 39", 39, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, IDelta.ANNOTATION_ELEMENT_TYPE, IDelta.ADDED, IDelta.METHOD);
		assertEquals("The returned id should be 43", 43, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.ADDED, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertEquals("The returned id should be 18", 18, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.ADDED, 0);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.CHANGED, 0);
		assertEquals("The returned id should be 0", 0, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_COMPATIBILITY, -1, IDelta.REMOVED, IDelta.TYPE_ARGUMENTS);
		assertEquals("The returned id should be 103", 103, id);
		id = ApiProblemFactory.getProblemMessageId(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION, -1, IApiProblem.API_COMPONENT_RESOLUTION, -1);
		assertEquals("The returned id should be 99", 99, id);
	}
	
	/**
	 * Tests the {@link ApiProblemFactory#getProblemSeverityId(IApiProblem)}
	 */
	public void testGetProblemSeverityId() {
		IApiProblem problem = ApiProblemFactory.newApiComponentResolutionProblem("", null, null, null, IElementDescriptor.COMPONENT, IApiProblem.API_COMPONENT_RESOLUTION);
		assertEquals("the problem id should be: "+IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT, IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT, ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiComponentResolutionProblem("", null, null, null, IElementDescriptor.COMPONENT, 0);
		assertNull("the id must be null", ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiBaselineProblem("", null, null, IElementDescriptor.RESOURCE, 0);
		assertNull("the id must be null", ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiSinceTagProblem("", null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, 0);
		assertNull("the id must be null", ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.API_LEAK);
		assertNull("the id must be null", ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES);
		assertEquals("the problem id should be: "+IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, ApiProblemFactory.getProblemSeverityId(problem));
		problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, null, -1, -1, -1, IElementDescriptor.TYPE, 0);
		assertNull("the id must be null", ApiProblemFactory.getProblemSeverityId(problem));
	}
	
	/**
	 * Tests the {@link ApiProblemFactory#getProblemKindFromPref(String)} method
	 */
	public void testGetProblemKindFromPref() {
		int kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES);
		assertEquals("the kind should be: "+IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES, kind);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.UNUSED_PROBLEM_FILTERS);
		assertEquals("the kind should be: "+IApiProblem.UNUSED_PROBLEM_FILTERS, IApiProblem.UNUSED_PROBLEM_FILTERS, kind);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.MISSING_SINCE_TAG);
		assertEquals("the kind should be: "+IApiProblem.SINCE_TAG_MISSING, IApiProblem.SINCE_TAG_MISSING, kind);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.MALFORMED_SINCE_TAG);
		assertEquals("the kind should be: "+IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.SINCE_TAG_MALFORMED, kind);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.INVALID_SINCE_TAG_VERSION);
		assertEquals("the kind should be: "+IApiProblem.SINCE_TAG_INVALID, IApiProblem.SINCE_TAG_INVALID, kind);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.REPORT_RESOLUTION_ERRORS_API_COMPONENT);
		assertEquals("the kind should be: "+IApiProblem.API_COMPONENT_RESOLUTION, IApiProblem.API_COMPONENT_RESOLUTION, kind);
		kind = ApiProblemFactory.getProblemKindFromPref("test");
		assertEquals("the kind should be 0", 0, kind);
	}
}
