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

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests aspects of a {@link ApiProblem} and {@link ApiProblemFactory}
 * 
 * @since 1.0.0
 */
public class ApiProblemTests extends AbstractApiTest {

	
	/**
	 * Tests that two problems are equal (when they are known to be)
	 */
	public void testProblemsEqual() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertEquals("the two problems should be equal", problem, problem2);
	}
	
	/**
	 * Tests that two problems are not equal (when they are known not to be)
	 */
	public void testProblemsNotEqual() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
	}
	
	/**
	 * Tests that a problem category is encoded and decoded properly from a problem id
	 */
	public void testGetCategory() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the category should be CATEGORY_BINARY", IApiProblem.CATEGORY_BINARY, problem.getCategory());
	}
	
	/**
	 * Tests that a problem id is encoded properly
	 */
	public void testGetId() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the problemids should match", problem.getId(), ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS));
	}
	
	/**
	 * Tests that a problem kind is encoded and decoded properly from a problem id
	 */
	public void testGetKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the kind should be ILLEGAL_IMPLEMENT", problem.getKind(), IApiProblem.ILLEGAL_IMPLEMENT);
	}
	
	/**
	 * Tests that problem flags are encoded and decoded properly
	 */
	public void testGetFlags() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the kind should be ANNOTATION_DEFAULT_VALUE", problem.getFlags(), IDelta.ANNOTATION_DEFAULT_VALUE);
	}
	
	/**
	 * Tests that a problem element kind is encoded and decoded properly
	 */
	public void testGetElementKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the element kind should be T_FIELD", problem.getElementKind(), IElementDescriptor.T_FIELD);
	}
	
	/**
	 * Tests getting the resource path attribute
	 */
	public void testGetResourcePath() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("there should be a path set on the problem", problem.getResourcePath());
	}
	
	/**
	 * Tests getting a localized message for the problem
	 */
	public void testGetMessage() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("the message should be null", problem.getMessage());
		assertTrue("the not found message should be displayed", problem.getMessage().startsWith("Message not found for problem id: "));
	}	
	
	/**
	 * Tests getting the charstart attribute
	 */
	public void testGetCharStart() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, 57, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the charstart should be 57", problem.getCharStart(), 57);
	}
	
	/**
	 * Tests getting the charend attribute
	 */
	public void testGetCharEnd() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, 57, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the charend should be 57", problem.getCharEnd(), 57);
	}
	
	/**
	 * Tests getting the line number attribute
	 */
	public void testGetLineNumber() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, 57, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the line number should be 57", problem.getLineNumber(), 57);
	}
	
	/**
	 * Tests that passing in null for problem arguments will return an empty array
	 */
	public void testGetExtraArgumentNamesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeIds());
		assertTrue("the argument ids array type should be string", problem.getExtraMarkerAttributeIds() instanceof String[]);
	}
	
	/**
	 * Tests that passing in null for problem arguments will return an empty array
	 */
	public void testGetExtraArgumentValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeValues());
		assertTrue("the arguments array type should be object", problem.getExtraMarkerAttributeValues() instanceof Object[]);
	}
	
	/**
	 * Tests that non-null argument names and null values will return an empty array 
	 * from a call to getExtramarkerAttributeIds (we have to have matching arrays for valid returns)
	 */
	public void tesGetExtraArgumentIdsNotNullValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1", "test2", "test3"}, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("argument ids should not be null", problem.getExtraMarkerAttributeIds());
		assertTrue("the argument ids array type should be string", problem.getExtraMarkerAttributeIds() instanceof String[]);
		assertTrue("argument size should be 0", problem.getExtraMarkerAttributeIds().length == 0);
	}
	
	/**
	 * Tests that non-null argument values and null ids will return an empty array 
	 * from a call to getExtraMarkerAttributeValues (we have to have matching arrays for valid returns)
	 */
	public void tesGetExtraArgumentNamesNotNullIdsNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, new String[] {"test1", "test2", "test3"}, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("arguments should not be null", problem.getExtraMarkerAttributeValues());
		assertTrue("the arguments array type should be object", problem.getExtraMarkerAttributeValues() instanceof Object[]);
		assertTrue("argument size should be 0", problem.getExtraMarkerAttributeValues().length == 0);
	}

	/**
	 * Tests that non-null argument values and non-null ids will return an empty array 
	 * from a call to getExtraMarkerAttributeValues when the arrays are not the same size (we have to have matching arrays for valid returns)
	 */
	public void tesGetExtraArgumentsNotSameSize() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"id1", "id2"}, new String[] {"test1", "test2", "test3"}, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("argument ids should not be null", problem.getExtraMarkerAttributeValues());
		assertNotNull("argument values should not be null", problem.getExtraMarkerAttributeValues());
		assertTrue("the argument ids array type should be string", problem.getExtraMarkerAttributeIds() instanceof String[]);
		assertTrue("the arguments array type should be object", problem.getExtraMarkerAttributeValues() instanceof Object[]);
		assertTrue("argument size should be 0", problem.getExtraMarkerAttributeIds().length == 0);
		assertTrue("argument size should be 0", problem.getExtraMarkerAttributeValues().length == 0);
	}
	
	/**
	 * Tests that non-null argument values and non-null ids will return an the passed in arrays
	 * from a call to getExtraMarkerAttributeValues when the arrays are the same size (we have to have matching arrays for valid returns)
	 */
	public void tesGetExtraArgumentsSameSize() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"id1", "id2", "id3"}, new String[] {"value1", "value2", "value3"}, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("argument ids should not be null", problem.getExtraMarkerAttributeValues());
		assertNotNull("argument values should not be null", problem.getExtraMarkerAttributeValues());
		assertTrue("the argument ids array type should be string", problem.getExtraMarkerAttributeIds() instanceof String[]);
		assertTrue("the arguments array type should be object", problem.getExtraMarkerAttributeValues() instanceof Object[]);
		assertTrue("argument size should be 3", problem.getExtraMarkerAttributeIds().length == 3);
		assertTrue("argument size should be 3", problem.getExtraMarkerAttributeValues().length == 3);
	}
	
	/**
	 * Tests that passing in null for message arguments will return an empty array
	 */
	public void testGetMessageArgumentsNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getMessageArguments());
		assertTrue("the arguments array type should be string", problem.getMessageArguments() instanceof String[]);
	}
	
	/**
	 * Tests that non-null message arguments passed a retrievable
	 */
	public void testGetMessageArgumentsNotNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), new String[] {"test1", "test2", "test3"}, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("arguments should not be null", problem.getMessageArguments());
		assertTrue("the arguments array type should be string", problem.getMessageArguments() instanceof String[]);
		assertTrue("argument size should be 3", problem.getMessageArguments().length == 3);
	}
	
	/**
	 * Tests that toString does not return null
	 */
	public void testToString() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), new String[] {"test1, test2, test3"}, null, null, -1, -1, -1, 2, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("there should be a string", problem.toString());
	}
	
	/**
	 * Tests getting the severity attribute
	 */
	public void testGetSeverity() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), new String[] {"test1, test2, test3"}, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the severity should be ERROR", IMarker.SEVERITY_ERROR, problem.getSeverity());
	}
	
	/**
	 * Regression test for the hash code of an {@link IApiProblem}.
	 */
	public void testGetHashCode() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), new String[] {"test1, test2, test3"}, null, null, 2, 2, 2, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_BINARY, IElementDescriptor.T_FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the hashcode should be equal to the sum of: id, resourcepath.hashCode", 
				problem.hashCode(), (problem.getId() + problem.getResourcePath().hashCode()));
	}
}
