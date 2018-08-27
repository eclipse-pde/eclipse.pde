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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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

	@Before
	public void setUp() throws Exception {
		createProject(TESTING_PLUGIN_PROJECT_NAME, null);
		File projectSrc = SRC_LOC.toFile();
		assertTrue("the filter source dir must exist", projectSrc.exists()); //$NON-NLS-1$
		assertTrue(";the filter source dir must be a directory", projectSrc.isDirectory()); //$NON-NLS-1$
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

	private BundleComponent getComponent() throws CoreException {
		if (fComponent == null){
			IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
			IApiBaseline profile = ApiPlugin.getDefault().getApiBaselineManager().getWorkspaceBaseline();
			assertNotNull("the workspace profile must exist", profile); //$NON-NLS-1$
			BundleComponent component = new BundleComponent(profile, project.getProject().getLocation().toOSString(), 0);
			assertNotNull("the component must exist", component); //$NON-NLS-1$
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
	 *
	 * @throws CoreException
	 */
	@Test
	public void testFilterStoreValidity() throws CoreException {
		FilterStore store = getFilterStore();
		IResource[] resources = store.getResources();
		assertNull("FilterStore should not support resources", resources); //$NON-NLS-1$

		// C4
		IPath resource = new Path("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, IElementDescriptor.TYPE, IApiProblem.ILLEGAL_IMPLEMENT,
				IApiProblem.NO_FLAGS);
		assertTrue("the usage problem for src/x/y/z/C4.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		// C1
		resource = new Path("src/x/C1.java"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.REMOVED, IDelta.FIELD);
		assertTrue("the removed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_COMPATIBILITY, 4, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY);
		assertTrue("the changed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		// C3
		resource = new Path("src/x/y/C3.java"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the major version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the minor version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem)); //$NON-NLS-1$

		// MANIFEST.MF
		resource = new Path("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MISSING, IApiProblem.NO_FLAGS);
		assertTrue("the missing since tag problem should be filtered for META-INF/MANIFEST.MF", //$NON-NLS-1$
				store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.NO_FLAGS);
		assertTrue("the malformed since tag problem should be filtered for META-INF/MANIFEST.MF", //$NON-NLS-1$
				store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1, -1, -1,
				IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_INVALID, IApiProblem.NO_FLAGS);
		assertTrue("the invalid since tag problem should be filterd for META-INF/MANIFEST.MF", //$NON-NLS-1$
				store.isFiltered(problem));
	}


	/**
	 * Tests that asking the store if it filters an invalid problem will return
	 * 'false'
	 *
	 * @throws CoreException
	 */
	@Test
	public void testNonExistantProblem() throws CoreException {
		FilterStore store = getFilterStore();
		IPath resource = new Path(XML_LOC + "/src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE, IDelta.ADDED);
		assertFalse("the bogus problem should not be filtered", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * tests adding then removing an api problem filter
	 *
	 * @throws CoreException
	 */
	@Test
	public void testAddRemoveFromFilter() throws CoreException {
		BundleComponent component = getComponent();
		FilterStore store = getFilterStore();

		IPath resource = new Path("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
		store.addFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem)); //$NON-NLS-1$
		boolean removed = store.removeFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue("A filter should have been removed", removed); //$NON-NLS-1$
		assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem)); //$NON-NLS-1$
	}

	/**
	 * tests adding then rmeoving a filter using the method that accepts an api
	 * problem
	 *
	 * @throws CoreException
	 */
	@Test
	public void testAddRemoveFromProblem() throws CoreException {
		BundleComponent component = getComponent();
		FilterStore store = getFilterStore();
		IPath resource = new Path("src/x/y/z/C4.java"); //$NON-NLS-1$
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.toPortableString(), null, null, null, null, -1,
				-1, -1, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);

		store.addFiltersFor(new IApiProblem[] { problem });
		assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem)); //$NON-NLS-1$
		boolean removed = store.removeFilters(
				new IApiProblemFilter[] { ApiProblemFactory.newProblemFilter(component.getName(), problem, null) });
		assertTrue("A filter should have been removed", removed); //$NON-NLS-1$
		assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem)); //$NON-NLS-1$
	}

}
