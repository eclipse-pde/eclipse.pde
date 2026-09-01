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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests aspects of a {@link ApiProblem} and {@link ApiProblemFactory}
 *
 * @since 1.0.0
 */
public class ApiProblemTests extends AbstractApiTest {

	/**
	 * Tests that two problems are equal (when they are known to be)
	 */
	@Test
	public void testProblemsEqual() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertTrue(problem.equals(problem2), "the two problems should be equal"); //$NON-NLS-1$
		assertTrue(problem2.equals(problem), "the two problems should be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal (when they are known not to be)
	 */
	@Test
	public void testProblemsNotEqual() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal if one has a resource path and the
	 * the other does not
	 */
	@Test
	public void testProblemsNotEqualMissingResourcePath() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z/").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z/").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal if their resource paths differ but
	 * are not null
	 */
	@Test
	public void testProblemsNotEqualDifferentPaths() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/yy/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal if their type names differ because
	 * one is null
	 */
	@Test
	public void testNotEqualDifferingTypeNames() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), "x.y.z.foo", null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal if the message arguments are not
	 * equals
	 */
	@Test
	public void testNotEqualDifferingMessageArguments() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "one" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "one" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that two problems are not equal if the message arguments are not
	 * equals
	 */
	@Test
	public void testNotEqualDifferingMessageArgumentsNumber() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "one" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		IApiProblem problem2 = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "one", "two" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull(problem2, "there should have been a new problem created"); //$NON-NLS-1$
		assertFalse(problem.equals(problem2), "the two problems should not be equal"); //$NON-NLS-1$
		assertFalse(problem2.equals(problem), "the two problems should not be equal"); //$NON-NLS-1$
	}

	/**
	 * Tests that an object other than an {@link IApiProblem} is not equal
	 */
	@Test
	public void testNotEqualDifferentObjects() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		Assertions.assertNotEquals(problem, new String("API Problem"), "the two problems should not be equal"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that a problem category is encoded and decoded properly from a
	 * problem id
	 */
	@Test
	public void testGetCategory() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(IApiProblem.CATEGORY_COMPATIBILITY, problem.getCategory(), "the category should be CATEGORY_BINARY"); //$NON-NLS-1$
	}

	/**
	 * Tests that a problem id is encoded properly
	 */
	@Test
	public void testGetId() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getId(), ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS), "the problemids should match"); //$NON-NLS-1$
	}

	/**
	 * Tests that a problem kind is encoded and decoded properly from a problem
	 * id
	 */
	@Test
	public void testGetKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getKind(), IApiProblem.ILLEGAL_IMPLEMENT, "the kind should be ILLEGAL_IMPLEMENT"); //$NON-NLS-1$
	}

	/**
	 * Tests that problem flags are encoded and decoded properly
	 */
	@Test
	public void testGetFlags() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getFlags(), IDelta.ANNOTATION_DEFAULT_VALUE, "the kind should be ANNOTATION_DEFAULT_VALUE"); //$NON-NLS-1$
	}

	/**
	 * Tests that a problem element kind is encoded and decoded properly
	 */
	@Test
	public void testGetElementKind() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getElementKind(), IElementDescriptor.FIELD, "the element kind should be T_FIELD"); //$NON-NLS-1$
	}

	/**
	 * Tests getting the resource path attribute
	 */
	@Test
	public void testGetResourcePath() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getResourcePath(), "there should be a path set on the problem"); //$NON-NLS-1$
	}

	/**
	 * Tests getting a localized message for the problem
	 */
	@Test
	public void testGetMessage() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getMessage(), "the message should be null"); //$NON-NLS-1$
		assertTrue(problem.getMessage().startsWith("Message not found for id: "), "the not found message should be displayed"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests getting the charstart attribute
	 */
	@Test
	public void testGetCharStart() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, 57, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getCharStart(), 57, "the charstart should be 57"); //$NON-NLS-1$
	}

	/**
	 * Tests getting the charend attribute
	 */
	@Test
	public void testGetCharEnd() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, 57, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getCharEnd(), 57, "the charend should be 57"); //$NON-NLS-1$
	}

	/**
	 * Tests getting the line number attribute
	 */
	@Test
	public void testGetLineNumber() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, 57, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.getLineNumber(), 57, "the line number should be 57"); //$NON-NLS-1$
	}

	/**
	 * Tests that passing in null for problem arguments will return an empty
	 * array
	 */
	@Test
	public void testGetExtraArgumentNamesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeIds(), "null passed in should return an emtpy array"); //$NON-NLS-1$
	}

	/**
	 * Tests that getting the extra argument ids will return an empty array when
	 * the number of ids does not match the number of arguments
	 */
	@Test
	public void testGetExtraArgumentIdsNotNullNotEqualLength() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), //$NON-NLS-1$
				null, null, new String[] { "one" }, //$NON-NLS-1$
				new String[] { "one", "two" }, //$NON-NLS-1$ //$NON-NLS-2$
				-1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeIds(), "null passed in should return an emtpy array"); //$NON-NLS-1$
		String[] args = problem.getExtraMarkerAttributeIds();
		assertNotNull(args, "the argument ids array type should not be null"); //$NON-NLS-1$
		assertEquals(0, args.length, "there should be no arguments returned"); //$NON-NLS-1$
	}

	/**
	 * Tests that getting the extra argument values will return an empty array
	 * when the number of ids does not match the number of arguments
	 */
	@Test
	public void testGetExtraArgumentValuesNotNullNotEqualLength() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), //$NON-NLS-1$
				null, null, new String[] { "one" }, //$NON-NLS-1$
				new String[] { "one", "two" }, //$NON-NLS-1$ //$NON-NLS-2$
				-1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE);
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeIds(), "null passed in should return an emtpy array"); //$NON-NLS-1$
		Object[] args = problem.getExtraMarkerAttributeValues();
		assertNotNull(args, "the argument ids array type should not be null"); //$NON-NLS-1$
		assertEquals(0, args.length, "there should be no arguments returned"); //$NON-NLS-1$
	}

	/**
	 * Tests that passing in null for problem arguments will return an empty
	 * array
	 */
	@Test
	public void testGetExtraArgumentValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "null passed in should return an emtpy array"); //$NON-NLS-1$
	}

	/**
	 * Tests that non-null argument names and null values will return an empty
	 * array from a call to getExtramarkerAttributeIds (we have to have matching
	 * arrays for valid returns)
	 */
	@Test
	public void tesGetExtraArgumentIdsNotNullValuesNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, new String[] { "test1", "test2", "test3" }, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeIds(), "argument ids should not be null"); //$NON-NLS-1$
		assertEquals(0, problem.getExtraMarkerAttributeIds().length, "argument size should be 0"); //$NON-NLS-1$
	}

	/**
	 * Tests that non-null argument values and null ids will return an empty
	 * array from a call to getExtraMarkerAttributeValues (we have to have
	 * matching arrays for valid returns)
	 */
	@Test
	public void tesGetExtraArgumentNamesNotNullIdsNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, new String[] { "test1", "test2", "test3" }, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "arguments should not be null"); //$NON-NLS-1$
		assertEquals(0, problem.getExtraMarkerAttributeValues().length, "argument size should be 0"); //$NON-NLS-1$
	}

	/**
	 * Tests that non-null argument values and non-null ids will return an empty
	 * array from a call to getExtraMarkerAttributeValues when the arrays are
	 * not the same size (we have to have matching arrays for valid returns)
	 */
	@Test
	public void tesGetExtraArgumentsNotSameSize() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, new String[] { "id1", "id2" }, new String[] { "test1", "test2", "test3" }, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "argument ids should not be null"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "argument values should not be null"); //$NON-NLS-1$
		assertEquals(0, problem.getExtraMarkerAttributeIds().length, "argument size should be 0"); //$NON-NLS-1$
		assertEquals(0, problem.getExtraMarkerAttributeValues().length, "argument size should be 0"); //$NON-NLS-1$
	}

	/**
	 * Tests that non-null argument values and non-null ids will return an the
	 * passed in arrays from a call to getExtraMarkerAttributeValues when the
	 * arrays are the same size (we have to have matching arrays for valid
	 * returns)
	 */
	@Test
	public void tesGetExtraArgumentsSameSize() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, new String[] { "id1", "id2", "id3" }, new String[] { "value1", "value2", "value3" }, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "argument ids should not be null"); //$NON-NLS-1$
		assertNotNull(problem.getExtraMarkerAttributeValues(), "argument values should not be null"); //$NON-NLS-1$
		assertEquals(3, problem.getExtraMarkerAttributeIds().length, "argument size should be 3"); //$NON-NLS-1$
		assertEquals(3, problem.getExtraMarkerAttributeValues().length, "argument size should be 3"); //$NON-NLS-1$
	}

	/**
	 * Tests that passing in null for message arguments will return an empty
	 * array
	 */
	@Test
	public void testGetMessageArgumentsNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getMessageArguments(), "null passed in should return an emtpy array"); //$NON-NLS-1$
	}

	/**
	 * Tests that non-null message arguments passed a retrievable
	 */
	@Test
	public void testGetMessageArgumentsNotNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "test1", "test2", "test3" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.getMessageArguments(), "arguments should not be null"); //$NON-NLS-1$
		assertEquals(3, problem.getMessageArguments().length, "argument size should be 3"); //$NON-NLS-1$
	}

	/**
	 * Tests that toString does not return null
	 */
	@Test
	public void testToString() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "test1, test2, test3" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.toString(), "there should be a string"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(null, null, new String[] { "test1, test2, test3" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertNotNull(problem.toString(), "there should be a string"); //$NON-NLS-1$
	}

	/**
	 * Tests getting the severity attribute
	 */
	@Test
	public void testGetSeverity() {
		if (ApiPlugin.isRunningInFramework()) {
			IEclipsePreferences inode = InstanceScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
			assertNotNull(inode, "The instance preference node must exist"); //$NON-NLS-1$
			inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_IGNORE);
			IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "test1, test2, test3" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
			assertEquals(IMarker.SEVERITY_INFO, problem.getSeverity(), "the severity should be IGNORE"); //$NON-NLS-1$
			IEclipsePreferences dnode = DefaultScope.INSTANCE.getNode(ApiPlugin.PLUGIN_ID);
			assertNotNull(dnode, "the default pref node must exist"); //$NON-NLS-1$
			inode.put(IApiProblemTypes.ILLEGAL_IMPLEMENT, dnode.get(IApiProblemTypes.ILLEGAL_IMPLEMENT, ApiPlugin.VALUE_WARNING));
		} else {
			IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "test1, test2, test3" }, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS); //$NON-NLS-1$ //$NON-NLS-2$
			assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
			assertEquals(IMarker.SEVERITY_WARNING, problem.getSeverity(), "the severity should be WARNING (no framework running)"); //$NON-NLS-1$
		}
	}

	/**
	 * Tests the {@link ApiProblem#getDescriptorKind(int)} method
	 */
	@Test
	public void testGetDescriptorKind() {
		assertEquals("PACKAGE", ApiProblem.getDescriptorKind(IElementDescriptor.PACKAGE), "the kind should be 'PACKAGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("REFERENCE_TYPE", ApiProblem.getDescriptorKind(IElementDescriptor.TYPE), "the kind should be 'REFERENCE_TYPE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("METHOD", ApiProblem.getDescriptorKind(IElementDescriptor.METHOD), "the kind should be 'METHOD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("FIELD", ApiProblem.getDescriptorKind(IElementDescriptor.FIELD), "the kind should be 'FIELD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("RESOURCE", ApiProblem.getDescriptorKind(IElementDescriptor.RESOURCE), "the kind should be 'RESOURCE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_ELEMENT_KIND, ApiProblem.getDescriptorKind(-1), "the kind should be 'UNKOWN_ELEMENT_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getTagsProblemKindName(int)} method
	 */
	@Test
	public void testGetTagsProblemKindName() {
		assertEquals("INVALID_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_INVALID), "the tag problem kind should be 'INVALID_SINCE_TAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MALFORMED_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_MALFORMED), "the tag problem kind should be 'MALFORMED_SINCE_TAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MISSING_SINCE_TAGS", ApiProblem.getTagsProblemKindName(IApiProblem.SINCE_TAG_MISSING), "the tag problem kind should be 'MISSING_SINCE_TAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getTagsProblemKindName(-1), "the tag problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getUsageProblemKindName(int)} method
	 */
	@Test
	public void testGetUsageProblemKindName() {
		assertEquals("ILLEGAL_EXTEND", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_EXTEND), "the usage problem kind should be 'ILLEGAL_EXTEND'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("ILLEGAL_IMPLEMENT", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_IMPLEMENT), "the usage problem kind should be 'ILLEGAL_IMPLEMENT'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("ILLEGAL_INSTANTIATE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_INSTANTIATE), "the usage problem kind should be 'ILLEGAL_INSTANTIATE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("ILLEGAL_OVERRIDE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_OVERRIDE), "the usage problem kind should be 'ILLEGAL_OVERRIDE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("ILLEGAL_REFERENCE", ApiProblem.getUsageProblemKindName(IApiProblem.ILLEGAL_REFERENCE), "the usage problem kind should be 'ILLEGAL_REFERENCE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("API_LEAK", ApiProblem.getUsageProblemKindName(IApiProblem.API_LEAK), "the usage problem kind should be 'API_LEAK'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("UNSUPPORTED_TAG_USE", ApiProblem.getUsageProblemKindName(IApiProblem.UNSUPPORTED_TAG_USE), "the usage problem kind should be 'UNSUPPORTED_TAG_USE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("INVALID_REFERENCE_IN_SYSTEM_LIBRARIES", ApiProblem.getUsageProblemKindName(IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES), "the usage problem kind should be 'INVALID_REFERENCE_IN_SYSTEM_LIBRARIES'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("UNUSED_PROBLEM_FILTERS", ApiProblem.getUsageProblemKindName(IApiProblem.UNUSED_PROBLEM_FILTERS), "the usage problem kind should be 'UNUSED_PROBLEM_FILTERS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getUsageProblemKindName(-1), "the usage problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getVersionProblemKindName(int)} method
	 */
	@Test
	public void testGetVersionProblemKindName() {
		assertEquals("MINOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.MINOR_VERSION_CHANGE), "the version problem kind should be 'MINOR_VERSION_CHANGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MAJOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.MAJOR_VERSION_CHANGE), "the version problem kind should be 'MAJOR_VERSION_CHANGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MAJOR_VERSION_CHANGE_NO_BREAKAGE", ApiProblem.getVersionProblemKindName(IApiProblem.MAJOR_VERSION_CHANGE_NO_BREAKAGE), "the version problem kind should be 'MAJOR_VERSION_CHANGE_NO_BREAKAGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MINOR_VERSION_CHANGE_NO_NEW_API", ApiProblem.getVersionProblemKindName(IApiProblem.MINOR_VERSION_CHANGE_NO_NEW_API), "the version problem kind should be 'MINOR_VERSION_CHANGE_NO_NEW_API'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("REEXPORTED_MAJOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.REEXPORTED_MAJOR_VERSION_CHANGE), "the version problem kind should be 'REEXPORTED_MAJOR_VERSION_CHANGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("REEXPORTED_MINOR_VERSION_CHANGE", ApiProblem.getVersionProblemKindName(IApiProblem.REEXPORTED_MINOR_VERSION_CHANGE), "the version problem kind should be 'MINOR_VERSION_CHANGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getVersionProblemKindName(-1), "the version problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the
	 * {@link ApiProblem#getApiComponentResolutionProblemKindName(int)} method
	 */
	@Test
	public void testGetApiComponentResolutionProblemKindName() {
		assertEquals("API_COMPONENT_RESOLUTION", ApiProblem.getApiComponentResolutionProblemKindName(IApiProblem.API_COMPONENT_RESOLUTION), "the component resolution problem kind should be 'API_COMPONENT_RESOLUTION'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getApiComponentResolutionProblemKindName(-1), "the component resolution problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getApiBaselineProblemKindName(int)} method
	 */
	@Test
	public void testGetApiBaselineProblemKindName() {
		assertEquals("API_BASELINE_MISSING", ApiProblem.getApiBaselineProblemKindName(IApiProblem.API_BASELINE_MISSING), "the baseline problem kind should be 'API_BASELINE_MISSING'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getApiBaselineProblemKindName(-1), "the baseline problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getProblemKind(int, int)} method
	 */
	@Test
	public void testGetProblemKind() {
		assertEquals("API_COMPONENT_RESOLUTION", ApiProblem.getProblemKind(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION, IApiProblem.API_COMPONENT_RESOLUTION), "the problem kind should be 'API_COMPONENT_RESOLUTION'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("API_BASELINE_MISSING", ApiProblem.getProblemKind(IApiProblem.CATEGORY_API_BASELINE, IApiProblem.API_BASELINE_MISSING), "the problem kind should be 'API_BASELINE_MISSING'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("INVALID_SINCE_TAGS", ApiProblem.getProblemKind(IApiProblem.CATEGORY_SINCETAGS, IApiProblem.SINCE_TAG_INVALID), "the problem kind should be 'INVALID_SINCE_TAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("ILLEGAL_EXTEND", ApiProblem.getProblemKind(IApiProblem.CATEGORY_USAGE, IApiProblem.ILLEGAL_EXTEND), "the problem kind should be 'ILLEGAL_EXTEND'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("MINOR_VERSION_CHANGE", ApiProblem.getProblemKind(IApiProblem.CATEGORY_VERSION, IApiProblem.MINOR_VERSION_CHANGE), "the problem kind should be 'MINOR_VERSION_CHANGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getProblemKind(-1, -1), "the problem kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getProblemFlagsName(int, int)} method
	 */
	@Test
	public void testGetProblemFlagsName() {
		assertEquals("LEAK_CONSTRUCTOR_PARAMETER", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_CONSTRUCTOR_PARAMETER), "the problem flags kind should be 'LEAK_CONSTRUCTOR_PARAMETER'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("LEAK_EXTENDS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_EXTENDS), "the problem flags kind should be 'LEAK_EXTENDS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("LEAK_FIELD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_FIELD), "the problem flags kind should be 'LEAK_FIELD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("LEAK_IMPLEMENTS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_IMPLEMENTS), "the problem flags kind should be 'LEAK_IMPLEMENTS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("LEAK_METHOD_PARAMETER", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_METHOD_PARAMETER), "the problem flags kind should be 'LEAK_METHOD_PARAMETER'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("LEAK_RETURN_TYPE", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.LEAK_RETURN_TYPE), "the problem flags kind should be 'LEAK_RETURN_TYPE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("CONSTRUCTOR_METHOD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.CONSTRUCTOR_METHOD), "the problem flags kind should be 'CONSTRUCTOR_METHOD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("NO_FLAGS", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.NO_FLAGS), "the problem flags kind should be 'NO_FLAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("INDIRECT_REFERENCE", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.INDIRECT_REFERENCE), "the problem flags kind should be 'INDIRECT_REFERENCE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("METHOD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.METHOD), "the problem flags kind should be 'METHOD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("FIELD", ApiProblem.getProblemFlagsName(IApiProblem.CATEGORY_USAGE, IApiProblem.FIELD), "the problem flags kind should be 'FIELD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_FLAGS, ApiProblem.getProblemFlagsName(-1, -1), "the problem flags kind should be 'UNKNOWN_FLAGS'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getProblemElementKind(int, int)} method
	 */
	@Test
	public void testGetProblemElementKind() {
		assertEquals("METHOD", ApiProblem.getProblemElementKind(IApiProblem.CATEGORY_USAGE, IElementDescriptor.METHOD), "the problem element kind should be 'METHOD'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(Util.UNKNOWN_KIND, ApiProblem.getProblemElementKind(-1, -1), "the problem element kind should be 'UNKNOWN_KIND'"); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblem#getProblemCategory(int)} method
	 */
	@Test
	public void testgetProblemCategory() {
		assertEquals("API_BASELINE", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_API_BASELINE), "the problem category kind should be 'API_BASELINE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("API_COMPONENT_RESOLUTION", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_API_COMPONENT_RESOLUTION), "the problem category kind should be 'API_COMPONENT_RESOLUTION'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("COMPATIBILITY", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_COMPATIBILITY), "the problem category kind should be 'COMPATIBILITY'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("SINCETAGS", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_SINCETAGS), "the problem category kind should be 'SINCETAGS'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("USAGE", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_USAGE), "the problem category kind should be 'USAGE'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("VERSION", ApiProblem.getProblemCategory(IApiProblem.CATEGORY_VERSION), "the problem category kind should be 'VERSION'"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("UNKNOWN_CATEGORY", ApiProblem.getProblemCategory(-1), "the problem category kind should be 'UNKNOWN_CATEGORY'"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Regression test for the hash code of an {@link IApiProblem}.
	 */
	@Test
	public void testGetHashCode() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(IPath.fromOSString("x/y/z").toPortableString(), null, new String[] { "test1, test2, test3" }, null, null, 2, 2, 2, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.hashCode(), (problem.getId() + problem.getResourcePath().hashCode()
						+ Objects.hash("test1, test2, test3")), "the hashcode should be equal to the sum of: id, resourcepath.hashCode"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Regression test for the hash code of an {@link IApiProblem}.
	 */
	@Test
	public void testGetHashCodeResourcePathNull() {
		IApiProblem problem = ApiProblemFactory.newApiProblem(null, null, new String[] { "test1, test2, test3" }, null, null, 2, 2, 2, IApiProblem.CATEGORY_COMPATIBILITY, IElementDescriptor.FIELD, IApiProblem.ILLEGAL_IMPLEMENT, IDelta.ANNOTATION_DEFAULT_VALUE); //$NON-NLS-1$
		assertNotNull(problem, "there should have been a new problem created"); //$NON-NLS-1$
		assertEquals(problem.hashCode(), (problem.getId() + 0 + Objects.hash("test1, test2, test3")), "the hashcode should be equal to the sum of: id, resourcepath.hashCode"); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
