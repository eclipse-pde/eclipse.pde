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
package org.eclipse.pde.api.tools.problems.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.junit.Test;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter} class
 *
 * @since 1.0.1
 */
public class ApiFilterTests {

	/**
	 * Tests the {@link ApiProblemFilter#toString()} method
	 */
	@Test
	public void testToString() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("", null, null, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING); //$NON-NLS-1$
		ApiProblemFilter filter = new ApiProblemFilter("comp.id", problem, null); //$NON-NLS-1$
		assertNotNull("The toString should not return null", filter.toString()); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblemFilter#equals(Object)} method
	 */
	@Test
	public void testEquals() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("path", new String[] {"one"}, new String[] {"one"}, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IApiProblemFilter filter1 = new ApiProblemFilter("comp.id", problem, null); //$NON-NLS-1$
		IApiProblemFilter filter2 = new ApiProblemFilter("comp.id", problem, null); //$NON-NLS-1$
		assertEquals("the filters should be equal", filter1, filter2); //$NON-NLS-1$
		assertEquals("the filters should be equal", filter2, filter1); //$NON-NLS-1$
		assertEquals("the filter should be equal to the problem", filter1, problem); //$NON-NLS-1$
		assertEquals("the filter should be equal to the problem", filter2, problem); //$NON-NLS-1$
		assertFalse("The filter should not be equal to the Object", filter1.equals(new Object())); //$NON-NLS-1$
		assertFalse("The filter should not be equal to the Object", new Object().equals(filter1)); //$NON-NLS-1$
		filter1 = new ApiProblemFilter(null, problem, null);
		filter2 = new ApiProblemFilter(null, problem, null);
		assertEquals("the filters should be equal", filter1, filter2); //$NON-NLS-1$
		assertEquals("the filters should be equal", filter2, filter1); //$NON-NLS-1$
	}

	/**
	 * Tests the {@link ApiProblemFilter#clone()} method
	 */
	@Test
	public void testClone() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("path", new String[] {"one"}, new String[] {"one"}, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ApiProblemFilter filter1 = new ApiProblemFilter("comp.id", problem, null); //$NON-NLS-1$
		IApiProblemFilter filter2 = (IApiProblemFilter) filter1.clone();
		assertEquals("the filters should be equal", filter1, filter2); //$NON-NLS-1$
		assertEquals("the filters should be equal", filter2, filter1); //$NON-NLS-1$
		assertEquals("the filter should be equal to the problem", filter1, problem); //$NON-NLS-1$
		assertEquals("the filter should be equal to the problem", filter2, problem); //$NON-NLS-1$
	}
}
