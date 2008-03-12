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
package org.eclipse.pde.api.tools.model.tests;

import java.io.File;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests the {@link ApiFilterStore} and {@link ApiProblemFilter}s
 * 
 * @since 1.0.0
 */
public class ApiFilterStoreTests extends AbstractApiTest {
	
	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source");
	private static final IPath XML_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-xml");
	private static final IPath PLUGIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-plugins");
	
	/**
	 * Tests that the .api_settings file is copied over to the testing project properly
	 */
	public void testCopyFilterFile() {
		try {
    		File dest = XML_LOC.append(".api_filters").toFile();
    		assertTrue("the filter xml dir must exist", dest.exists());
    		assertTrue("the filter xml dir must be a file", !dest.isDirectory());
    		IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
    		assertNotNull("no project", project);
			IProject project2 = project.getProject();
    		IPath settings = project2.getFullPath().append(".settings");
    		importFileFromDirectory(dest, settings, new NullProgressMonitor());
    		IResource filters = project2.findMember("/.settings/.api_filters", true);
    		assertNotNull("the .api_filters file must exist in the testing project", filters);
    	}
    	catch (Exception e) {
    		fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that the testing source is copied over correctly to the test project
	 */
	public void testCopySourceFiles() {
		try {
    		File dest = SRC_LOC.append("x").toFile();
    		assertTrue("the filter source dir must exist", dest.exists());
    		assertTrue("the filter source dir must be a directory", dest.isDirectory());
    		IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
    		IPackageFragmentRoot srcroot = project.findPackageFragmentRoot(project.getProject().getFullPath().append("src"));
    		assertNotNull("the default src root must exist", srcroot);
    		importFileFromDirectory(dest, srcroot.getPath(), new NullProgressMonitor());
    		IType type = project.findType("x.y.z.C4");
    		assertNotNull("the type C4 must exist in the testing project", type);
    	}
    	catch (Exception e) {
    		fail(e.getMessage());
		}
	}
	
	/**
	 * Runs a series of assertions against the loaded {@link IApiFilterStore} 
	 * @param store the store to check
	 */
	private void assertFilterStore(IApiFilterStore store, int count) {
		assertNotNull("the filter store for the testing project cannot be null");
		IResource[] resources = store.getResources();
		assertEquals("there should be "+count+" resources with filters", count, resources.length);
		IJavaProject jproject = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
		IProject project = jproject.getProject();
		
		//C4
		IResource resource = project.findMember(new Path("src/x/y/z/C4.java"));
		assertNotNull("the resource src/x/y/z/C4.java must exist", resource);
		IApiProblemFilter[] filters = store.getFilters(resource);
		assertTrue("There should be 1 filter for src/x/y/z/C4.java", filters.length == 1);
		IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_USAGE, IElementDescriptor.T_REFERENCE_TYPE, IApiProblem.ILLEGAL_IMPLEMENT, IApiProblem.NO_FLAGS);
		assertTrue("the usage problem for src/x/y/z/C4.java should be filtered", store.isFiltered(problem));
		
		//C1
		resource = project.findMember(new Path("src/x/C1.java"));
		assertNotNull("the resource src/x/C1.java must exist", resource);
		filters = store.getFilters(resource);
		assertTrue("there should be 2 filters for src/x/C1.java", filters.length == 2);
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_WARNING, IApiProblem.CATEGORY_BINARY, 4, IDelta.REMOVED, IDelta.FIELD);
		assertTrue("the removed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_INFO, IApiProblem.CATEGORY_BINARY, 4, IDelta.CHANGED, IDelta.VARARGS_TO_ARRAY);
		assertTrue("the changed binary problem for src/x/C1.java should be filtered", store.isFiltered(problem));
		
		//C3
		resource = project.findMember(new Path("src/x/y/C3.java"));
		assertNotNull("the resource src/x/y/C3.java must exist");
		filters = store.getFilters(resource);
		assertTrue("there should be 2 filters for src/x/y/C3.java", filters.length == 2);
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MAJOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the major version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_VERSION, 7, IApiProblem.MINOR_VERSION_CHANGE, IApiProblem.NO_FLAGS);
		assertTrue("the minor version problem for src/x/y/C3.java should be filtered", store.isFiltered(problem));
		
		//MANIFEST.MF
		resource = project.findMember(new Path("META-INF/MANIFEST.MF"));
		assertNotNull("the resource META-INF/MANIFEST.MF must exist", resource);
		filters = store.getFilters(resource);
		assertTrue("there should be 3 filters for META-INF/MANIFEST.MF", filters.length == 3);
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_WARNING, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MISSING, IApiProblem.NO_FLAGS);
		assertTrue("the missing since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_WARNING, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_MALFORMED, IApiProblem.NO_FLAGS);
		assertTrue("the malformed since tag problem should be filtered for META-INF/MANIFEST.MF", store.isFiltered(problem));
		problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_WARNING, IApiProblem.CATEGORY_SINCETAGS, 7, IApiProblem.SINCE_TAG_INVALID, IApiProblem.NO_FLAGS);
		assertTrue("the invalid since tag problem should be filterd for META-INF/MANIFEST.MF", store.isFiltered(problem));
	}
	
	/**
	 * Tests that a filter store can be correctly annotated from a persisted version
	 * disabled for now. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=221877
	 */
	public void _testAnnotateStoreFromLocalFile() {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component);
		try {
			assertFilterStore(component.getFilterStore(), 4);
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that asking the store if it filters an invalid problem will return 'false'
	 */
	public void testNonExistantProblem() {
		IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
		assertNotNull("the testing project api component must exist", component);
		try {
			IApiFilterStore store = component.getFilterStore();
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			IResource resource = project.findMember(new Path("src/x/y/z/C4.java"));
			assertNotNull("the resource src/x/y/z/C4.java must exist", resource);
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_WARNING, IApiProblem.CATEGORY_USAGE, 0, IApiProblem.MINOR_VERSION_CHANGE, IDelta.ADDED);
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
			IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the testing project api component must exist", component);
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			IResource resource = project.findMember(new Path("src/x/y/z/C4.java"));
			assertNotNull("the resource src/x/y/z/C4.java must exist", resource);
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
			IApiFilterStore store;
			store = component.getFilterStore();
			store.removeFilters(new IApiProblemFilter[] {component.newProblemFilter(problem)});
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
			IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the testing project api component must exist", component);
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			IResource resource = project.findMember(new Path("src/x/y/z/C4.java"));
			assertNotNull("the resource src/x/y/z/C4.java must exist", resource);
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
			IApiFilterStore store;
			store = component.getFilterStore();
			store.addFilters(new IApiProblemFilter[] {component.newProblemFilter(problem)});
			assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem));
			store.removeFilters(new IApiProblemFilter[] {component.newProblemFilter(problem)});
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
			IApiComponent component = getProjectApiComponent(TESTING_PLUGIN_PROJECT_NAME);
			assertNotNull("the testing project api component must exist", component);
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			IResource resource = project.findMember(new Path("src/x/y/z/C4.java"));
			assertNotNull("the resource src/x/y/z/C4.java must exist", resource);
			IApiProblem problem = ApiProblemFactory.newApiProblem(resource.getProjectRelativePath().toPortableString(), null, null, null, -1, -1, -1, IMarker.SEVERITY_ERROR, IApiProblem.CATEGORY_USAGE, 0, RestrictionModifiers.NO_IMPLEMENT, IApiProblem.NO_FLAGS);
			IApiFilterStore store;
			store = component.getFilterStore();
			store.addFilters(new IApiProblem[] {problem});
			assertTrue("src/x/y/z/C4.java should have a filter", store.isFiltered(problem));
			store.removeFilters(new IApiProblemFilter[] {component.newProblemFilter(problem)});
			assertFalse("src/x/y/z/C4.java should not have a filter", store.isFiltered(problem));
		}
		catch(CoreException ce) {
			fail(ce.getMessage());
		}
	}
	
	/**
	 * Tests that importing the test 'component_c' jar is successful
	 */
	public void testImportJar() {
		try {
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			importFileFromDirectory(PLUGIN_LOC.append("component_c_1.0.0.jar").toFile(), project.getFullPath(), new NullProgressMonitor());
			IResource res = project.findMember("component_c_1.0.0.jar");
			assertNotNull("the jar should exist in the project dir", res);
		}
		catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	/**
	 * Tests that a filter store will not be annotated from a bundle
	 */
	public void testAnnotateStoreFromBundle() {
		try {
			IProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME).getProject();
			IResource jar = project.findMember("component_c_1.0.0.jar");
			assertNotNull("the component_c jar cannot be null", jar);
			IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
			IApiComponent component = profile.newApiComponent(jar.getLocation().toOSString());
			profile.addApiComponents(new IApiComponent[] { component });
			assertNotNull("the new component cannot be null", component);
			IApiFilterStore store = component.getFilterStore();
			assertNull("the new filter store must be null", store);
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
}
