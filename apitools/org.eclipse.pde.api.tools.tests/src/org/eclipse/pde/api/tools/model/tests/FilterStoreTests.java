/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;
import org.eclipse.pde.api.tools.tests.util.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the {@link org.eclipse.pde.api.tools.internal.FilterStore} which does not
 * require workspace resources but does not save changes.
 *
 * @since 1.0.300
 */
public class FilterStoreTests extends AbstractApiTest {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source"); //$NON-NLS-1$
	private static final IPath XML_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-xml"); //$NON-NLS-1$

	private BundleComponent fComponent = null;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		createProject(TESTING_PLUGIN_PROJECT_NAME, null);
		File projectSrc = SRC_LOC.toFile();
		assertTrue(projectSrc.exists(), "the filter source dir must exist"); //$NON-NLS-1$
		assertTrue(projectSrc.isDirectory(), ";the filter source dir must be a directory"); //$NON-NLS-1$
		IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
		IPackageFragmentRoot srcroot = project.findPackageFragmentRoot(project.getProject().getFullPath().append("src")); //$NON-NLS-1$
		assertNotNull(srcroot, "the default src root must exist"); //$NON-NLS-1$
		FileUtils.importFileFromDirectory(projectSrc, srcroot.getPath());

		// Import the test .api_filters file
		File xmlsrc = XML_LOC.append(".api_filters").toFile(); //$NON-NLS-1$
		assertTrue(xmlsrc.exists(), "the filter xml dir must exist"); //$NON-NLS-1$
		assertFalse(xmlsrc.isDirectory(), "the filter xml dir must be a file"); //$NON-NLS-1$
		assertNotNull(project, "no project"); //$NON-NLS-1$
		IProject project2 = project.getProject();
		IPath settings = project2.getFullPath().append(".settings"); //$NON-NLS-1$
		FileUtils.importFileFromDirectory(xmlsrc, settings);
		IResource filters = project2.findMember("/.settings/.api_filters", true); //$NON-NLS-1$
		assertNotNull(filters, "the .api_filters file must exist in the testing project"); //$NON-NLS-1$
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		deleteProject(TESTING_PLUGIN_PROJECT_NAME);
		super.tearDown();
	}

	private BundleComponent getComponent() throws CoreException {
		if (fComponent == null){
			IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
			IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
			assertNotNull(profile, "the workspace profile must exist"); //$NON-NLS-1$
			BundleComponent component = new BundleComponent(profile, project.getProject().getLocation().toOSString(), 0);
			assertNotNull(component, "the component must exist"); //$NON-NLS-1$
			fComponent = component;
		}
		return fComponent;
	}

	private FilterStore getFilterStore() throws CoreException {
		return (FilterStore)getComponent().getFilterStore();
	}

	@Test
	public void testBogus(){
		assertNull(null);
	}

	/**
	 * Tests that a filter store can be correctly annotated from a persisted
	 * version
	 */
	@Test
	public void testFilterStoreValidity() throws CoreException {
		FilterStore store = getFilterStore();
		IResource[] resources = store.getResources();
		assertNull(resources, "FilterStore should not support resources"); //$NON-NLS-1$

		// C4
		IPath resource = IPath.fromOSString("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT,
				IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the usage problem for src/x/y/z/C4.java should be filtered"); //$NON-NLS-1$

		// C1
		resource = IPath.fromOSString("src/x/C1.java"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.REMOVED, IDelta.FIELD);
		assertTrue(store.isFiltered(problem), "the removed binary problem for src/x/C1.java should be filtered"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY);
		assertTrue(store.isFiltered(problem), "the changed binary problem for src/x/C1.java should be filtered"); //$NON-NLS-1$

		// C3
		resource = IPath.fromOSString("src/x/y/C3.java"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the major version problem for src/x/y/C3.java should be filtered"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the minor version problem for src/x/y/C3.java should be filtered"); //$NON-NLS-1$

		// MANIFEST.MF
		resource = IPath.fromOSString("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MISSING, IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the missing since tag problem should be filtered for META-INF/MANIFEST.MF"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the malformed since tag problem should be filtered for META-INF/MANIFEST.MF"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_INVALID, IApiProblem.NO_FLAGS);
		assertTrue(store.isFiltered(problem), "the invalid since tag problem should be filterd for META-INF/MANIFEST.MF"); //$NON-NLS-1$
	}


	/**
	 * Tests that asking the store if it filters an invalid problem will return
	 * 'false'
	 */
	@Test
	public void testNonExistantProblem() throws CoreException {
		FilterStore store = getFilterStore();
		IPath resource = IPath.fromOSString(XML_LOC + "/src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE, IDelta.ADDED);
		assertFalse(store.isFiltered(problem), "the bogus problem should not be filtered"); //$NON-NLS-1$
	}

	/**
	 * tests adding then removing an api problem filter
	 */
	@Test
	public void testAddRemoveFromFilter() throws CoreException {
		BundleComponent component = getComponent();
		FilterStore store = getFilterStore();

		IPath resource = IPath.fromOSString("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
		store.addFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue(store.isFiltered(problem), "src/x/y/z/C4.java should have a filter"); //$NON-NLS-1$
		boolean removed = store.removeFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue(removed, "A filter should have been removed"); //$NON-NLS-1$
		assertFalse(store.isFiltered(problem), "src/x/y/z/C4.java should not have a filter"); //$NON-NLS-1$
	}

	/**
	 * tests adding then rmeoving a filter using the method that accepts an api
	 * problem
	 */
	@Test
	public void testAddRemoveFromProblem() throws CoreException {
		BundleComponent component = getComponent();
		FilterStore store = getFilterStore();
		IPath resource = IPath.fromOSString("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);

		store.addFiltersFor(new IApiProblem[] { problem });
		assertTrue(store.isFiltered(problem), "src/x/y/z/C4.java should have a filter"); //$NON-NLS-1$
		boolean removed = store.removeFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue(removed, "A filter should have been removed"); //$NON-NLS-1$
		assertFalse(store.isFiltered(problem), "src/x/y/z/C4.java should not have a filter"); //$NON-NLS-1$
	}

}
