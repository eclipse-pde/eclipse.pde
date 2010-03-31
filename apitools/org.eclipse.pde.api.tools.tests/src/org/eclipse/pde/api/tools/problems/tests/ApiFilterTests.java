/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;

import junit.framework.TestCase;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter} class
 * 
 * @since 1.0.1
 */
public class ApiFilterTests extends TestCase {

	/**
	 * Tests the {@link ApiProblemFilter#toString()} method
	 */
	public void testToString() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("", null, null, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING);
		ApiProblemFilter filter = new ApiProblemFilter("comp.id", problem, null);
		assertNotNull("The toString should not return null", filter.toString());
	}
	
	/**
	 * Tests the {@link ApiProblemFilter#equals(Object)} method
	 */
	public void testEquals() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("path", new String[] {"one"}, new String[] {"one"}, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING);
		IApiProblemFilter filter1 = new ApiProblemFilter("comp.id", problem, null);
		IApiProblemFilter filter2 = new ApiProblemFilter("comp.id", problem, null);
		assertEquals("the filters should be equal", filter1, filter2);
		assertEquals("the filters should be equal", filter2, filter1);
		assertEquals("the filter should be equal to the problem", filter1, problem);
		assertEquals("the filter should be equal to the problem", filter2, problem);
		assertTrue("The filter should not be equal to the Object", !filter1.equals(new Object()));
		assertTrue("The filter should not be equal to the Object", !new Object().equals(filter1));
		filter1 = new ApiProblemFilter(null, problem, null);
		filter2 = new ApiProblemFilter(null, problem, null);
		assertEquals("the filters should be equal", filter1, filter2);
		assertEquals("the filters should be equal", filter2, filter1);
	}
	
	/**
	 * Tests the {@link ApiProblemFilter#clone()} method
	 */
	public void testClone() {
		IApiProblem problem = ApiProblemFactory.newApiBaselineProblem("path", new String[] {"one"}, new String[] {"one"}, IElementDescriptor.RESOURCE, IApiProblem.API_BASELINE_MISSING);
		ApiProblemFilter filter1 = new ApiProblemFilter("comp.id", problem, null);
		IApiProblemFilter filter2 = (IApiProblemFilter) filter1.clone();
		assertEquals("the filters should be equal", filter1, filter2);
		assertEquals("the filters should be equal", filter2, filter1);
		assertEquals("the filter should be equal to the problem", filter1, problem);
		assertEquals("the filter should be equal to the problem", filter2, problem);
	}
}
