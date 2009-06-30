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
package org.eclipse.pde.api.tools.builder.tests;

import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.builder.BaseApiAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.BuildContext;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.model.tests.TestSuiteHelper;

/**
 * Tests the base analyzer without OSGi present
 * 
 * @since 1.0
 */
public class OSGiLessAnalysisTests extends TestCase {
	
	public void testAnalyzer() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("baseline", new Path("test-analyzer-1"));
		IApiBaseline current = TestSuiteHelper.createTestingBaseline("current", new Path("test-analyzer-2"));
		BaseApiAnalyzer analyzer = new BaseApiAnalyzer();
		IApiComponent component = current.getApiComponent("test.bundle.a");
		assertNotNull("Missing API component test.bundle.a", component);
		analyzer.analyzeComponent(null, null, null, baseline, component, new BuildContext(), new NullProgressMonitor());
		IApiProblem[] problems = analyzer.getProblems();
		Set<Integer> expectedIds = new HashSet<Integer>();
		expectedIds.add(ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, 
																		IDelta.FIELD_ELEMENT_TYPE, 
																		IDelta.CHANGED, 
																		IDelta.DECREASE_ACCESS));
		expectedIds.add(ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, 
																		IDelta.CLASS_ELEMENT_TYPE, 
																		IDelta.REMOVED, 
																		IDelta.METHOD));
		expectedIds.add(ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_COMPATIBILITY, 
																		IDelta.INTERFACE_ELEMENT_TYPE, 
																		IDelta.ADDED, 
																		IDelta.METHOD));
		expectedIds.add(ApiProblemFactory.createProblemId(IApiProblem.CATEGORY_VERSION, 
																		IElementDescriptor.RESOURCE, 
																		IApiProblem.MAJOR_VERSION_CHANGE, 
																		IApiProblem.NO_FLAGS));
		assertEquals("Wrong number of problems", 4, problems.length);
		for (int i = 0; i < problems.length; i++) {
			expectedIds.remove(new Integer(problems[i].getId()));
		}
		assertTrue("Did not find expected problems", expectedIds.isEmpty());
		baseline.dispose();
		current.dispose();
	}
}
