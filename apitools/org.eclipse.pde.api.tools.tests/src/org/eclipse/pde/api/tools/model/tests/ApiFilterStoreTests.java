/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.model.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ApiFilterStore} and {@link ApiProblemFilter}s
 *
 * @since 1.0.0
 */
public class ApiFilterStoreTests extends AbstractApiTest {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source"); //$NON-NLS-1$
	private static final IPath XML_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-xml"); //$NON-NLS-1$
	private static final IPath PLUGIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-plugins"); //$NON-NLS-1$

	@Before
	public void setUp() throws Exception {
		createProject(TESTING_PLUGIN_PROJECT_NAME, null);
		File projectSrc = SRC_LOC.toFile();
		assertTrue("the filter source dir must exist", projectSrc.exists()); //$NON-NLS-1$
		assertTrue("the filter source dir must be a directory", projectSrc.isDirectory()); //$NON-NLS-1$
		IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
		IPackageFragmentRoot srcroot = project.findPackageFragmentRoot(project.getProject().getFullPath().append("src")); //$NON-NLS-1$
		assertNotNull("the default src root must exist", srcroot); //$NON-NLS-1$
		FileUtils.importFileFromDirectory(projectSrc, srcroot.getPath(), new NullProgressMonitor());

		// Import the test .api_filters file
		File xmlsrc = XML_LOC.append(".api_filters").toFile(); //$NON-NLS-1$
		assertTrue("the filter xml dir must exist", xmlsrc.exists()); //$NON-NLS-1$
		assertFalse("the filter xml dir must be a file", xmlsrc.isDirectory()); //$NON-NLS-1$
		assertNotNull("no project", project); //$NON-NLS-1$
		IProject project2 = project.getProject();
		IPath settings = project2.getFullPath().append(".settings"); //$NON-NLS-1$
		FileUtils.importFileFromDirectory(xmlsrc, settings, new NullProgressMonitor());
		IResource filters = project2.findMember("/.settings/.api_filters", true); //$NON-NLS-1$
		assertNotNull("the .api_filters file must exist in the testing project", filters); //$NON-NLS-1$
	}

	@After
	public void tearDown() throws Exception {
		deleteProject(TESTING_PLUGIN_PROJECT_NAME);
	}

	/**
	 * Runs a series of assertions against the loaded {@link IApiFilterStore}
	 * @param store the store to check
	 */
	private void assertFilterStore(IApiFilterStore store, int count) {
		assertNotNull("the filter store for the testing project cannot be null", store); //$NON-NLS-1$
		IResource[] resources = store.getResources();
		assertEquals("there should be "+count+" resources with filters", count, resources.length); //$NON-NLS-1$ //$NON-NLS-2$
		IJavaProject jproject = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
		IProject project = jproject.getProject();

		//C4
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource); //$NON-NLS-1$
		IApiProblemFilter[] filters = store.getFilters(resource);
		assertEquals("There should be 1 filter for src/x/y/z/C4.java", 1, filters.length); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertTrue("the usage problem for src/x/y/z/C4.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		//C1
		resource = project.findMember(new Path("src/x/C1.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/C1.java must exist", resource); //$NON-NLS-1$
		filters = store.getFilters(resource);
		assertEquals("there should be 2 filters for src/x/C1.java", 2, filters.length); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.REMOVED, IDelta.FIELD);
		assertTrue("the removed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY);
		assertTrue("the changed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		//C3
		resource = project.findMember(new Path("src/x/y/C3.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/C3.java must exist"); //$NON-NLS-1$
		filters = store.getFilters(resource);
		assertEquals("there should be 2 filters for src/x/y/C3.java", 2, filters.length); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the major version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the minor version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		//MANIFEST.MF
		resource = project.findMember(new Path("META-INF/MANIFEST.MF")); //$NON-NLS-1$
		assertNotNull("the resource META-INF/MANIFEST.MF must exist", resource); //$NON-NLS-1$
		filters = store.getFilters(resource);
		assertEquals("there should be 3 filters for META-INF/MANIFEST.MF", 3, filters.length); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MISSING, IApiProblem.NO_FLAGS);
		assertTrue("the missing since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.NO_FLAGS);
		assertTrue("the malformed since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_INVALID, IApiProblem.NO_FLAGS);
		assertTrue("the invalid since tag problem should be filterd for META-INF/MANIFEST.MF", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * Tests that a filter store can be correctly annotated from a persisted
	 * version
	 *
	 * @throws CoreException
	 */
	@Test
	public void testAnnotateStoreFromLocalFile() throws CoreException {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component); //$NON-NLS-1$
		assertFilterStore(component.getFilterStore(), 4);
	}

	/**
	 * Tests that asking the store if it filters an invalid problem will return
	 * 'false'
	 *
	 * @throws CoreException
	 */
	@Test
	public void testNonExistantProblem() throws CoreException {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component); //$NON-NLS-1$
		IApiFilterStore store = component.getFilterStore();
		IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
				null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE,
				IDelta.ADDED);
		assertFalse("the bogus problem should not be filtered", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * tests removing an api problem filter
	 *
	 * @throws CoreException
	 */
	@Test
	public void testRemoveFilter() throws CoreException {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component); //$NON-NLS-1$
		IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
				null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT,
				IApiProblem.NO_FLAGS);
		IApiFilterStore store;
		store = component.getFilterStore();
		store.removeFilters(new IApiProblemFilter[] {
				ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, null) });
		assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * tests adding a filter using the method that accepts a filter
	 *
	 * @throws CoreException
	 */
	@Test
	public void testAddFilterFromFilter() throws CoreException {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component); //$NON-NLS-1$
		IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
				null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT,
				IApiProblem.NO_FLAGS);
		IApiFilterStore store;
		store = component.getFilterStore();
		store.addFilters(new IApiProblemFilter[] {
				ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, null) });
		assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem)); //$NON-NLS-1$
		store.removeFilters(new IApiProblemFilter[] {
				ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, null) });
		assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * tests adding a filter using the method that accepts an api problem
	 *
	 * @throws CoreException
	 */
	@Test
	public void testAddFilterFromProblem() throws CoreException {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component); //$NON-NLS-1$
		IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java")); //$NON-NLS-1$
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(),
				null, null, null, null, -1, -1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT,
				IApiProblem.NO_FLAGS);
		IApiFilterStore store;
		store = component.getFilterStore();
		store.addFiltersFor(new IApiProblem[] { problem });
		assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem)); //$NON-NLS-1$
		store.removeFilters(new IApiProblemFilter[] {
				ApiProblemFactory.newProblemFilter(component.getSymbolicName(), problem, null) });
		assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * Tests that a filter store will not be annotated from a bundle
	 *
	 * @throws IOException
	 * @throws InvocationTargetException
	 * @throws CoreException
	 */
	@Test
	public void testAnnotateStoreFromBundle() throws InvocationTargetException, IOException, CoreException {
		IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
		FileUtils.importFileFromDirectory(PLUGIN_LOC.append("component_c_1.0.0.jar").toFile(), project.getFullPath(), //$NON-NLS-1$
				new NullProgressMonitor());
		IResource res = project.findMember("component_c_1.0.0.jar"); //$NON-NLS-1$
		assertNotNull("the jar should exist in the project dir", res); //$NON-NLS-1$
		IResource jar = project.findMember("component_c_1.0.0.jar"); //$NON-NLS-1$
		assertNotNull("the component_c jar cannot be null", jar); //$NON-NLS-1$
		IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
		IApiComponent component = ApiModelFactory.newApiComponent(profile, jar.getLocation().toOSString());
		profile.addApiComponents(new IApiComponent[] { component });
		assertNotNull("the new component cannot be null", component); //$NON-NLS-1$
		IApiFilterStore store = component.getFilterStore();
		assertFalse("the new filter store must not be an instance of ApiFilterStore", store instanceof ApiFilterStore); //$NON-NLS-1$
		assertTrue("the new filter store must be an instance of FilterStore", store instanceof FilterStore); //$NON-NLS-1$
	}
}
