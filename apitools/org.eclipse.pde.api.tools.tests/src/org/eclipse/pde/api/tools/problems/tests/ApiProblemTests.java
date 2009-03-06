/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.api.tools.internal.problems.ApiProblem;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Util;
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
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should be equal", problem.equals(problem2));
		assertTrue("the two problems should be equal", problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal (when they are known not to be)
	 */
	public void testProblemsNotEqual() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal if one has a resource path and the the other does not
	 */
	public void testProblemsNotEqualMissingResourcePath() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z/").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal if their resource paths differ but are not null
	 */
	public void testProblemsNotEqualDifferentPaths() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/yy/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal if their type names differ because one is null
	 */
	public void testNotEqualDifferingTypeNames() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), "x.y.z.foo", null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal if the message arguments are not equals
	 */
	public void testNotEqualDifferingMessageArguments() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"one"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that two problems are not equal if the message arguments are not equals
	 */
	public void testNotEqualDifferingMessageArgumentsNumber() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"one"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"one", "two"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem2);
		assertTrue("the two problems should not be equal", !problem.equals(problem2));
		assertTrue("the two problems should not be equal", !problem2.equals(problem));
	}
	
	/**
	 * Tests that an object other than an {@link IApiProblem} is not equal
	 */
	public void testNotEqualDifferentObjects() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertTrue("the two problems should not be equal", !problem.equals(new String("Api Problem")));
	}
	
	/**
	 * Tests that a problem category is encoded and decoded properly from a problem id
	 */
	public void testGetCategory() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the category should be CATEGORY_BINARY", IApiProblem.CATEGORY_COMPATIBILITY, problem.getCategory());
	}
	
	/**
	 * Tests that a problem id is encoded properly
	 */
	public void testGetId() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the problemids should match", problem.getId(), ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS));
	}
	
	/**
	 * Tests that a problem kind is encoded and decoded properly from a problem id
	 */
	public void testGetKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the kind should be ILLEGAL_IMPLEMENT", problem.getKind(), IApiProblem.ILLEGAL_IMPLEMENT);
	}
	
	/**
	 * Tests that problem flags are encoded and decoded properly
	 */
	public void testGetFlags() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the kind should be ANNOTATION_DEFAULT_VALUE", problem.getFlags(), IDelta.ANNOTATION_DEFAULT_VALUE);
	}
	
	/**
	 * Tests that a problem element kind is encoded and decoded properly
	 */
	public void testGetElementKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the element kind should be T_FIELD", problem.getElementKind(), IElementDescriptor.FIELD);
	}
	
	/**
	 * Tests getting the resource path attribute
	 */
	public void testGetResourcePath() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("there should be a path set on the problem", problem.getResourcePath());
	}
	
	/**
	 * Tests getting a localized message for the problem
	 */
	public void testGetMessage() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("the message should be null", problem.getMessage());
		assertTrue("the not found message should be displayed", problem.getMessage().startsWith("Message not found for id: "));
	}	
	
	/**
	 * Tests getting the charstart attribute
	 */
	public void testGetCharStart() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, 57, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the charstart should be 57", problem.getCharStart(), 57);
	}
	
	/**
	 * Tests getting the charend attribute
	 */
	public void testGetCharEnd() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, 57, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the charend should be 57", problem.getCharEnd(), 57);
	}
	
	/**
	 * Tests getting the line number attribute
	 */
	public void testGetLineNumber() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, 57, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the line number should be 57", problem.getLineNumber(), 57);
	}
	
	/**
	 * Tests that passing in null for problem arguments will return an empty array
	 */
	public void testGetExtraArgumentNamesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeIds());
		assertTrue("the argument ids array type should be string", problem.getExtraMarkerAttributeIds() instanceof String[]);
	}
	
	/**
	 * Tests that getting the extra argument ids will return an empty array when the number of ids
	 * does not match the number of arguments
	 */
	public void testGetExtraArgumentIdsNotNullNotEqualLength() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(
				new Path("x/y/z").toPortableString(), 
				null, 
				null, 
				new String[] {"one"}, 
				new String[] {"one", "two"}, 
				-1, 
				-1, 
				-1, 
				IApiProblem.CATEGORY_COMPATIBILITY, 
				IElementDescriptor.FIELD, 
				IApiProblem.ILLEGAL_IMPLEMENT, 
				IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeIds());
		String[] args = problem.getExtraMarkerAttributeIds();
		assertTrue("the argument ids array type should be string", args instanceof String[]);
		assertTrue("there should be no arguments returned", args.length == 0);
	}
	
	/**
	 * Tests that getting the extra argument values will return an empty array when the number of ids
	 * does not match the number of arguments
	 */
	public void testGetExtraArgumentValuesNotNullNotEqualLength() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(
				new Path("x/y/z").toPortableString(), 
				null, 
				null, 
				new String[] {"one"}, 
				new String[] {"one", "two"}, 
				-1, 
				-1, 
				-1, 
				IApiProblem.CATEGORY_COMPATIBILITY, 
				IElementDescriptor.FIELD, 
				IApiProblem.ILLEGAL_IMPLEMENT, 
				IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeIds());
		Object[] args = problem.getExtraMarkerAttributeValues();
		assertTrue("the argument ids array type should be string", args instanceof Object[]);
		assertTrue("there should be no arguments returned", args.length == 0);
	}
	
	/**
	 * Tests that passing in null for problem arguments will return an empty array
	 */
	public void testGetExtraArgumentValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getExtraMarkerAttributeValues());
		assertTrue("the arguments array type should be object", problem.getExtraMarkerAttributeValues() instanceof Object[]);
	}
	
	/**
	 * Tests that non-null argument names and null values will return an empty array 
	 * from a call to getExtramarkerAttributeIds (we have to have matching arrays for valid returns)
	 */
	public void tesGetExtraArgumentIdsNotNullValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, new String[] {"test1", "test2", "test3"}, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
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
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, new String[] {"test1", "test2", "test3"}, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
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
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, new String[] {"id1", "id2"}, new String[] {"test1", "test2", "test3"}, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
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
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, new String[] {"id1", "id2", "id3"}, new String[] {"value1", "value2", "value3"}, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
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
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("null passed in should return an emtpy array", problem.getMessageArguments());
		assertTrue("the arguments array type should be string", problem.getMessageArguments() instanceof String[]);
	}
	
	/**
	 * Tests that non-null message arguments passed a retrievable
	 */
	public void testGetMessageArgumentsNotNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1", "test2", "test3"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("arguments should not be null", problem.getMessageArguments());
		assertTrue("the arguments array type should be string", problem.getMessageArguments() instanceof String[]);
		assertTrue("argument size should be 3", problem.getMessageArguments().length == 3);
	}
	
	/**
	 * Tests that toString does not return null
	 */
	public void testToString() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1, test2, test3"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertNotNull("there should be a string", problem.toString());
	}
	
	/**
	 * Tests getting the severity attribute
	 */
	public void testGetSeverity() {
		if(ApiPlugin.isRunningInFramework()) {
			IEclipsePreferences inode = new InstanceScope().getNode(ApiPlugin.PLUGIN_ID);
			assertNotNull("The instance preference node must exist", inode);
			inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_IGNORE);
			IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1, test2, test3"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
			assertNotNull("there should have been a new problem created", problem);
			assertEquals("the severity should be IGNORE", IMarker.SEVERITY_INFO, problem.getSeverity());
			IEclipsePreferences dnode = new DefaultScope().getNode(ApiPlugin.PLUGIN_ID);
			assertNotNull("the default pref node must exist", dnode);
			inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, dnode.get(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_WARNING));
		}
		else {
			IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1, test2, test3"}, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
			assertNotNull("there should have been a new problem created", problem);
			assertEquals("the severity should be WARNING (no framework running)", IMarker.SEVERITY_WARNING, problem.getSeverity());
		}
	}
	
	/**
	 * Tests the {@link ApiProblem#getDescriptorKind(int)} method
	 */
	public void testGetDescriptorKind() {
		assertEquals("the kind should be 'PACKAGE'", "PACKAGE", ApiProblem.getDescriptorKind(IElementDescriptor.PACKAGE));
		assertEquals("the kind should be 'REFERENCE_TYPE'", "REFERENCE_TYPE", ApiProblem.getDescriptorKind(IElementDescriptor.TYPE));
		assertEquals("the kind should be 'METHOD'", "METHOD", ApiProblem.getDescriptorKind(IElementDescriptor.METHOD));
		assertEquals("the kind should be 'FIELD'", "FIELD", ApiProblem.getDescriptorKind(IElementDescriptor.FIELD));
		assertEquals("the kind should be 'RESOURCE'", "RESOURCE", ApiProblem.getDescriptorKind(IElementDescriptor.RESOURCE));
		assertEquals("the kind should be 'UNKOWN_ELEMENT_KIND'", Util.UNKNOWN_ELEMENT_KIND, ApiProblem.getDescriptorKind(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getTagsProblemKindName(int)} method
	 */
	public void testGetTagsProblemKindName() {
		assertEquals("the tag problem kind should be 'INVALID_SINCE_TAGS'", "INVALID_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_INVALID));
		assertEquals("the tag problem kind should be 'MALFORMED_SINCE_TAGS'", "MALFORMED_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_MALFORMED));
		assertEquals("the tag problem kind should be 'MISSING_SINCE_TAGS'", "MISSING_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_MISSING));
		assertEquals("the tag problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getTagsProblemKindName(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getUsageProblemKindName(int)} method
	 */
	public void testGetUsageProblemKindName() {
		assertEquals("the usage problem kind should be 'ILLEGAL_EXTEND'", "ILLEGAL_EXTEND", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_EXTEND));
		assertEquals("the usage problem kind should be 'ILLEGAL_IMPLEMENT'", "ILLEGAL_IMPLEMENT", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_IMPLEMENT));
		assertEquals("the usage problem kind should be 'ILLEGAL_INSTANTIATE'", "ILLEGAL_INSTANTIATE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_INSTANTIATE));
		assertEquals("the usage problem kind should be 'ILLEGAL_OVERRIDE'", "ILLEGAL_OVERRIDE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_OVERRIDE));
		assertEquals("the usage problem kind should be 'ILLEGAL_REFERENCE'", "ILLEGAL_REFERENCE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_REFERENCE));
		assertEquals("the usage problem kind should be 'API_LEAK'", "API_LEAK", ApiProblem.getUsageProblemKindName(IApiProblem.API_LEAK));
		assertEquals("the usage problem kind should be 'UNSUPPORTED_TAG_USE'", "UNSUPPORTED_TAG_USE", ApiProblem.getUsageProblemKindName(IApiProblem.UNSUPPORTED_TAG_USE));
		assertEquals("the usage problem kind should be 'INVALID_REFERENCE_IN_SYSTEM_LIBRARIES'", "INVALID_REFERENCE_IN_SYSTEM_LIBRARIES", ApiProblem.getUsageProblemKindName(IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES));
		assertEquals("the usage problem kind should be 'UNUSED_PROBLEM_FILTERS'", "UNUSED_PROBLEM_FILTERS", ApiProblem.getUsageProblemKindName(IApiProblem.UNUSED_PROBLEM_FILTERS));
		assertEquals("the usage problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getUsageProblemKindName(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getVersionProblemKindName(int)} method
	 */
	public void testGetVersionProblemKindName() {
		assertEquals("the version problem kind should be 'MINOR_VERSION_CHANGE'", "MINOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.MINOR_VERSION_CHANGE));
		assertEquals("the version problem kind should be 'MAJOR_VERSION_CHANGE'", "MAJOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.MAJOR_VERSION_CHANGE));
		assertEquals("the version problem kind should be 'MAJOR_VERSION_CHANGE_NO_BREAKAGE'", "MAJOR_VERSION_CHANGE_NO_BREAKAGE", ApiProblem.getVersionProblemKindName(IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE));
		assertEquals("the version problem kind should be 'MINOR_VERSION_CHANGE_NO_NEW_API'", "MINOR_VERSION_CHANGE_NO_NEW_API", ApiProblem.getVersionProblemKindName(IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API));
		assertEquals("the version problem kind should be 'REEXPORTED_MAJOR_VERSION_CHANGE'", "REEXPORTED_MAJOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE));
		assertEquals("the version problem kind should be 'MINOR_VERSION_CHANGE'", "REEXPORTED_MINOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE));
		assertEquals("the version problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getVersionProblemKindName(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getApiComponentResolutionProblemKindName(int)} method
	 */
	public void testGetApiComponentResolutionProblemKindName() {
		assertEquals("the component resolution problem kind should be 'API_COMPONENT_RESOLUTION'", "API_COMPONENT_RESOLUTION", ApiProblem.getApiComponentResolutionProblemKindName(IApiProblem.API_COMPONENT_RESOLUTION));
		assertEquals("the component resolution problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getApiComponentResolutionProblemKindName(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getApiBaselineProblemKindName(int)} method
	 */
	public void testGetApiBaselineProblemKindName() {
		assertEquals("the baseline problem kind should be 'API_BASELINE_MISSING'", "API_BASELINE_MISSING", ApiProblem.getApiBaselineProblemKindName(IApiProblem.API_BASELINE_MISSING));
		assertEquals("the baseline problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getApiBaselineProblemKindName(-1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getProblemKind(int, int)} method 
	 */
	public void testGetProblemKind() {
		assertEquals("the problem kind should be 'API_COMPONENT_RESOLUTION'", "API_COMPONENT_RESOLUTION", ApiProblem.getProblemKind(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION, IApiProblem.API_COMPONENT_RESOLUTION));
		assertEquals("the problem kind should be 'API_BASELINE_MISSING'", "API_BASELINE_MISSING", ApiProblem.getProblemKind(IApiProblem.CATEGORY_API_BASELINE, IApiProblem.API_BASELINE_MISSING));
		assertEquals("the problem kind should be 'INVALID_SINCE_TAGS'", "INVALID_SINCE_TAGS", ApiProblem.getProblemKind(IApiProblem.CATEGORY_SINCETAGS, IApiProblem.SINCE_TAG_INVALID));
		assertEquals("the problem kind should be 'ILLEGAL_EXTEND'", "ILLEGAL_EXTEND", ApiProblem.getProblemKind(IApiProblem.CATEGORY_USAGE, IApiProblem.ILLEGAL_EXTEND));
		assertEquals("the problem kind should be 'MINOR_VERSION_CHANGE'", "MINOR_VERSION_CHANGE", ApiProblem.getProblemKind(IApiProblem.CATEGORY_VERSION, IApiProblem.MINOR_VERSION_CHANGE));
		assertEquals("the problem kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getProblemKind(-1, -1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getProblemFlagsName(int, int)} method
	 */
	public void testGetProblemFlagsName() {
		assertEquals("the problem flags kind should be 'LEAK_CONSTRUCTOR_PARAMETER'", "LEAK_CONSTRUCTOR_PARAMETER", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_CONSTRUCTOR_PARAMETER));
		assertEquals("the problem flags kind should be 'LEAK_EXTENDS'", "LEAK_EXTENDS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_EXTENDS));
		assertEquals("the problem flags kind should be 'LEAK_FIELD'", "LEAK_FIELD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_FIELD));
		assertEquals("the problem flags kind should be 'LEAK_IMPLEMENTS'", "LEAK_IMPLEMENTS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_IMPLEMENTS));
		assertEquals("the problem flags kind should be 'LEAK_METHOD_PARAMETER'", "LEAK_METHOD_PARAMETER", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_METHOD_PARAMETER));
		assertEquals("the problem flags kind should be 'LEAK_RETURN_TYPE'", "LEAK_RETURN_TYPE", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_RETURN_TYPE));
		assertEquals("the problem flags kind should be 'CONSTRUCTOR_METHOD'", "CONSTRUCTOR_METHOD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.CONSTRUCTOR_METHOD));
		assertEquals("the problem flags kind should be 'NO_FLAGS'", "NO_FLAGS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.NO_FLAGS));
		assertEquals("the problem flags kind should be 'INDIRECT_REFERENCE'", "INDIRECT_REFERENCE", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.INDIRECT_REFERENCE));
		assertEquals("the problem flags kind should be 'METHOD'", "METHOD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.METHOD));
		assertEquals("the problem flags kind should be 'FIELD'", "FIELD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.FIELD));
		assertEquals("the problem flags kind should be 'UNKNOWN_FLAGS'", Util.UNKNOWN_FLAGS, ApiProblem.getProblemFlagsName(-1, -1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getProblemElementKind(int, int)} method
	 */
	public void testGetProblemElementKind() {
		assertEquals("the problem element kind should be 'METHOD'", "METHOD", ApiProblem.getProblemElementKind(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD));
		assertEquals("the problem element kind should be 'UNKNOWN_KIND'", Util.UNKNOWN_KIND, ApiProblem.getProblemElementKind(-1, -1));
	}
	
	/**
	 * Tests the {@link ApiProblem#getProblemCategory(int)} method
	 */
	public void testgetProblemCategory() {
		assertEquals("the problem category kind should be 'API_BASELINE'", "API_BASELINE", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_API_BASELINE));
		assertEquals("the problem category kind should be 'API_COMPONENT_RESOLUTION'", "API_COMPONENT_RESOLUTION", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION));
		assertEquals("the problem category kind should be 'COMPATIBILITY'", "COMPATIBILITY", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_COMPATIBILITY));
		assertEquals("the problem category kind should be 'SINCETAGS'", "SINCETAGS", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_SINCETAGS));
		assertEquals("the problem category kind should be 'USAGE'", "USAGE", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_USAGE));
		assertEquals("the problem category kind should be 'VERSION'", "VERSION", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_VERSION));
		assertEquals("the problem category kind should be 'UNKNOWN_CATEGORY'", "UNKNOWN_CATEGORY", ApiProblem.getProblemCategory(-1));
	}
	
	/**
	 * Regression test for the hash code of an {@link IApiProblem}.
	 */
	public void testGetHashCode() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(new Path("x/y/z").toPortableString(), null, new String[] {"test1, test2, test3"}, null, null, 2, 2, 2, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the hashcode should be equal to the sum of: id, resourcepath.hashCode", 
				problem.hashCode(), (problem.getId()
						+ problem.getResourcePath().hashCode()
						+ argumentsHashcode(new String[] {"test1, test2, test3"})));
	}
	
	/**
	 * Regression test for the hash code of an {@link IApiProblem}.
	 */
	public void testGetHashCodeResourcePathNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, new String[] {"test1, test2, test3"}, null, null, 2, 2, 2, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull("there should have been a new problem created", problem);
		assertEquals("the hashcode should be equal to the sum of: id, resourcepath.hashCode", 
				problem.hashCode(), (problem.getId()
						+ 0
						+ argumentsHashcode(new String[] {"test1, test2, test3"})));
	}
	
	/**
	 * Helper method to get a hash code for problem arguments
	 * @param arguments
	 * @return
	 */
	private int argumentsHashcode(String[] arguments) {
		if(arguments == null) {
			return 0;
		}
		int hashcode = 0;
		for(int i = 0; i < arguments.length; i++) {
			hashcode += arguments[i].hashCode();
		}
		return hashcode;
	}
}
