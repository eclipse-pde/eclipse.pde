/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.FilterStore} which does not
 * require workspace resources but does not save changes.
 * 
 * @since 1.0.300
 */
public class FilterStoreTests extends AbstractApiTest {
	
	private static final IPath XML_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-xml");
	/**
	 * The symbolic name from the component that we create a filter store for 
	 */
	private static final String COMPONENT_NAME = "test";
	
	
	private FilterStore filterStore = null;
	
	private FilterStore getFilterStore() throws CoreException {
		if (filterStore == null){
			IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
			assertNotNull("the workspace profile must exist", profile);
			BundleComponent component = new BundleComponent(profile, XML_LOC.toOSString(), 0);
			assertNotNull("the component must exist", component);
			IApiFilterStore store = component.getFilterStore();
			assertTrue("the component must have a filter store that is an instance of FilterStore", store instanceof FilterStore);
			filterStore = (FilterStore)store;
		}
		return filterStore;
	}
	
	/**
	 * Tests that a filter store can be correctly annotated from a persisted version
	 */
	public void testFilterStoreValidity() {
		try {
			FilterStore store = getFilterStore();
			IResource[] resources = store.getResources();
			assertNull("FilterStore should not support resources", resources);
			
			//C4
			IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java");
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
			assertTrue("the usage problem for src/x/y/z/C4.java should be filtered", store.isFiltered(problem));
			
			//C1
			resource = new Path(XML_LOC + "/src/x/C1.java");
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.REMOVED, IDelta.FIELD);
			assertTrue("the removed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem));
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY);
			assertTrue("the changed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem));
			
			//C3
			resource = new Path(XML_LOC + "/src/x/y/C3.java");
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
			assertTrue("the major version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem));
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
			assertTrue("the minor version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem));
			
			//MANIFEST.MF
			resource = new Path(XML_LOC + "/META-INF/MANIFEST.MF");
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MISSING, IApiProblem.NO_FLAGS);
			assertTrue("the missing since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem));
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.NO_FLAGS);
			assertTrue("the malformed since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem));
			problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_INVALID, IApiProblem.NO_FLAGS);
			assertTrue("the invalid since tag problem should be filterd for META-INF/MANIFEST.MF", store.isFiltered(problem));
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	
	/**
	 * Tests that asking the store if it filters an invalid problem will return 'false'
	 */
	public void testNonExistantProblem() {
		try {
			FilterStore store = getFilterStore();
			IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java");
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE, IDelta.ADDED);
			assertFalse("the bogus problem should not be filtered", store.isFiltered(problem));
		} 
		catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * tests removing an api problem filter 
	 */
	public void testRemoveFilter() {
		try {
			FilterStore store = getFilterStore();
			IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java");
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE, IDelta.ADDED);
			store.removeFilters(new IApiProblemFilter[] {ApiProblemFactory.newProblemFilter(COMPONENT_NAME, problem, null)});
			assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem));
		} 
		catch (CoreException e) {
			fail(e.getMessage());
		}
	}
	
	
	/**
	 * tests adding a filter using the method that accepts a filter
	 */
	public void testAddFilterFromFilter() {
		try {
			FilterStore store = getFilterStore();
			IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java");
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
			store.addFilters(new IApiProblemFilter[] {ApiProblemFactory.newProblemFilter(COMPONENT_NAME, problem, null)});
			assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem));
			store.removeFilters(new IApiProblemFilter[] {ApiProblemFactory.newProblemFilter(COMPONENT_NAME, problem, null)});
			assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem));
		}
		catch(CoreException ce) {
			fail(ce.getMessage());
		}
	}
	
	/**
	 * tests adding a filter using the method that accepts an api problem
	 */
	public void testAddFilterFromProblem() {
		try {
			FilterStore store = getFilterStore();
			IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java");
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
			
			store.addFiltersFor(new IApiProblem[] {problem});
			assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem));
			store.removeFilters(new IApiProblemFilter[] {ApiProblemFactory.newProblemFilter(COMPONENT_NAME, problem, null)});
			assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem));
		}
		catch(CoreException ce) {
			fail(ce.getMessage());
		}
	}
	
}
