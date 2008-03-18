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

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

import com.ibm.icu.text.MessageFormat;

/**
 * Tests various aspects of the {@link ApiProblemFactory}
 * 
 * @since 1.0.0
 */
public class ApiProblemFactoryTests extends AbstractApiTest {

	String fDefaultMessage = MessageFormat.format("Message not found for problem id:", new String[0]);
	
	/**
	 * Tests that creating an {@link IApiProblem} does not fail
	 */
	public void testCreateProblem() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 
				 IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiProblem("path", new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_METHOD, IApiProblem.ILLEGAL_OVERRIDE, IApiProblem.NO_FLAGS);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests creating a new {@link IApiProblem} using the usage specialized factory method
	 */
	public void tesCreateUsageProblem() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem(null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiUsageProblem("path", new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests creating a new {@link IApiProblem} using the since tag specialized factory method
	 */
	public void testCreateSincetagProblem() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem(null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiSinceTagProblem("path", new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with non-null attributes", problem);
	}
	
	/**
	 * Tests creating a new {@link IApiProblem} using the version number specialized factory method
	 */
	public void testCreateVersionProblem() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem(null, null, null, null, -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		assertNotNull("a new problem should have been created with null attributes", problem);
		problem = ApiProblemFactory.newApiVersionNumberProblem("path", new String[0], new String[0], new Object[0], -1, -1, -1, 
				 IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
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
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ANNOTATION_CHANGED_CONTRACTED_SUPERINTERFACES_SET);
		assertTrue("the kind should be CHANGED", kind == IDelta.CHANGED);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ANNOTATION_ADDED_CLASS_BOUND);
		assertTrue("the kind should be ADDED", kind == IDelta.ADDED);
		kind = ApiProblemFactory.getProblemKindFromPref(IApiProblemTypes.ANNOTATION_REMOVED_INTERFACE_BOUND);
		assertTrue("the kind should be REMOVED", kind == IDelta.REMOVED);
	}
	
	/**
	 * Test getting version number problem messages
	 */
	public void testGetVersionMessages() {
		IApiProblem problem = ApiProblemFactory.newApiVersionNumberProblem("", 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.MAJOR_VERSION_CHANGE);
		assertNotNull("there should be a new problem created", problem);
		String message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("The major version should be incremented in version 1, since API breakage occurred since version 2", new String[0])));
		problem = ApiProblemFactory.newApiVersionNumberProblem("", 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE);
		assertNotNull("there should be a new problem created", problem);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("The major version should be identical in version 1, since no API breakage occurred since version 2", new String[0])));
		problem = ApiProblemFactory.newApiVersionNumberProblem("", 
				new String[] {"1", "2"}, null, null, -1, -1, -1,  IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.MINOR_VERSION_CHANGE);
		assertNotNull("there should be a new problem created", problem);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("The minor version should be incremented in version 1, since new APIs have been added since version 2", new String[0])));
	}
	
	/**
	 * Tests getting API usage problem messages
	 */
	public void testGetUsageMessages() {
		IApiProblem problem = ApiProblemFactory.newApiUsageProblem("", 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_EXTEND);
		String message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Illegally extends foo", new String[0])));
		problem = ApiProblemFactory.newApiUsageProblem("", 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Illegally implements foo", new String[0])));
		problem = ApiProblemFactory.newApiUsageProblem("", 
				new String[] {"foo", "bar"}, null, null, -1, -1, -1, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_OVERRIDE);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Illegally overrides foo.bar", new String[0])));
		problem = ApiProblemFactory.newApiUsageProblem("", 
				new String[] {"foo"}, null, null, -1, -1, -1, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_INSTANTIATE);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Illegally instantiates foo", new String[0])));
		problem = ApiProblemFactory.newApiUsageProblem("", 
				new String[] {"foo", "bar"}, null, null, -1, -1, -1, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_REFERENCE);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Illegally references foo.bar", new String[0])));
	}
	
	/**
	 * Tests getting (some of) the binary messages
	 */
	public void testGetBinaryMessages() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1,  
				IApiProblem.CATEGORY_BINARY, IDelta.CLASS_ELEMENT_TYPE, IDelta.ADDED, IDelta.CONSTRUCTOR);
		String message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Added a constructor", new String[0])));
		problem = ApiProblemFactory.newApiProblem(null, new String[] {"foo()"}, null, null, -1, -1, -1,  
				IApiProblem.CATEGORY_BINARY, IDelta.INTERFACE_ELEMENT_TYPE, IDelta.ADDED, IDelta.METHOD);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Added method foo() in an interface that is intended to be implemented", new String[0])));
	}
	
	/**
	 * Tests getting since tag problem messages
	 */
	public void testGetSinceTagMessages() {
		IApiProblem problem = ApiProblemFactory.newApiSinceTagProblem("", 
				new String[] {"A", "B"}, null, null, -1, -1, -1,  IElementDescriptor.T_RESOURCE, IApiProblem.SINCE_TAG_INVALID);
		String message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Invalid @since tag: A; the expected @since tag value is B", new String[0])));
		problem = ApiProblemFactory.newApiSinceTagProblem("", 
				new String[] {"A"}, null, null, -1, -1, -1,  IElementDescriptor.T_RESOURCE, IApiProblem.SINCE_TAG_MALFORMED);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Invalid @since tag: A; the @since tag can only have two fragments", new String[0])));
		problem = ApiProblemFactory.newApiSinceTagProblem("", 
				new String[0], null, null, -1, -1, -1,  IElementDescriptor.T_RESOURCE, IApiProblem.SINCE_TAG_MISSING);
		message = problem.getMessage();
		assertNotNull("the message should not be null", message);
		assertFalse("the message should be found", message.equals(fDefaultMessage));
		assertTrue("the message should be correct", message.equals(MessageFormat.format("Missing @since tag", new String[0])));
	}
}
