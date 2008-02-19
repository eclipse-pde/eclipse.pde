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
import org.eclipse.pde.api.tools.internal.ApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.tests.AbstractApiTest;

/**
 * Tests the {@link ApiFilterStore} and {@link ApiProblemFilter}s
 * 
 * @since 1.0.0
 */
public class ApiFilterStoreTests extends AbstractApiTest {
	
	private static final Path SRC_LOC = new Path(System.getProperty("user.dir")+File.separator+"test-source");
	private static final Path XML_LOC = new Path(System.getProperty("user.dir")+File.separator+"test-xml");
	private static final Path PLUGIN_LOC = new Path(System.getProperty("user.dir")+File.separator+"test-plugins");
	
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
		IApiProblemFilter[] filters = store.getFilters();
		assertTrue("there should be "+count+" filters", filters.length == count);
		assertTrue("there should be no filter for package 'x'", !store.isFiltered(Factory.packageDescriptor("x"), new String[0]));
		assertTrue("there should be a filter for C1 for ADDED_CLASS_BOUND", store.isFiltered(Factory.typeDescriptor("x.C1"), new String[] {IApiProblemFilter.ADDED_CLASS_BOUND}));
		assertTrue("there should be a filter for C2 for ADDED_INTERFACE_BOUND and CHANGED_CONTRACTED_SUPERINTERFACES_SET", 
				store.isFiltered(Factory.typeDescriptor("x.C2"), new String[] {IApiProblemFilter.ADDED_INTERFACE_BOUND, IApiProblemFilter.CHANGED_CONTRACTED_SUPERINTERFACES_SET}));
		assertTrue("there should be a filter for C3 for CHANGED_SUPERCLASS", store.isFiltered(Factory.typeDescriptor("x.y.C3"), new String[] {IApiProblemFilter.CHANGED_SUPERCLASS}));
		assertTrue("There should be a filter for field 'field' for CHANGED_VALUE", store.isFiltered(Factory.fieldDescriptor("x.y.C3", "field"), new String[] {IApiProblemFilter.CHANGED_VALUE}));
		assertTrue("there should be a filter for unqualified method foo for ADDED_NO_EXTEND", store.isFiltered(Factory.methodDescriptor("x.y.C3", "foo", "(QInteger;)QObject;"), new String[] {IApiProblemFilter.ADDED_NO_EXTEND}));
		assertTrue("there should be a filter for qualified method foo for ADDED_NO_EXTEND", store.isFiltered(Factory.methodDescriptor("x.y.C3", "foo", "(Ljava/lang/Integer;)Ljava/lang/Object;"), new String[] {IApiProblemFilter.ADDED_NO_EXTEND}));
	}
	
	/**
	 * Tests that a filter store can be correctly annotated from a persisted version
	 */
	public void testAnnotateStoreFromLocalFile() {
		IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
		IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
		assertNotNull("the workspace profile must exist", profile);
		IApiComponent component = profile.getApiComponent(project.getElementName());
		assertNotNull("the testing project api component must exist", component);
		try {
			assertFilterStore(component.getFilterStore(), 8);
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Tests that if a parent type has matching filter kinds we can say 'yes' that a type is filtered even if the type
	 * itself does not appear in the store
	 */
	public void testParentFiltered() {
		try {
			IJavaProject project = getTestingJavaProject(TESTING_PLUGIN_PROJECT_NAME);
			IApiProfile profile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
			assertNotNull("the workspace profile must exist", profile);
			IApiComponent component = profile.getApiComponent(project.getElementName());
			assertNotNull("the testing project api component must exist", component);
			IApiFilterStore store = component.getFilterStore();
			IElementDescriptor type = Factory.typeDescriptor("x.y.z.C4");
			assertTrue("the type x.y.z.C4 should be filtered because its parent package x.y.z is", store.isFiltered(type, new String[] {IApiProblemFilter.REMOVED_FIELD}));
		}
		catch(CoreException e) {
			fail(e.getMessage());
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
	 * Tests that a filter store can be correctly annotated from a jar file (bundle)
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
			assertNotNull("the new filter store cannot be null", store);
			assertFilterStore(store, 8);
		}
		catch(CoreException e) {
			fail(e.getMessage());
		}
	}
}
