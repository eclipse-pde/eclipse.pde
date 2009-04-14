/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.model.tests;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * Test creation of states and components.
 * 
 * @since 1.0.0
 */
public class ApiBaselineTests extends TestCase {
	
	/**
	 * Creates and validates simple state.
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testCreateBaseline() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		List<IRequiredComponentDescription> reqs = new ArrayList<IRequiredComponentDescription>();
		reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange("")));
		validateComponent(baseline, "component.a", "A Plug-in", "1.0.0", "J2SE-1.5", reqs);

		reqs = new ArrayList<IRequiredComponentDescription>();
		reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange("")));
		reqs.add(new RequiredComponentDescription("component.a", new BundleVersionRange("")));
		validateComponent(baseline, "component.b", "B Plug-in", "1.0.0", "J2SE-1.4", reqs);
		
	}
	
	/**
	 * Resolves a package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolvePackage() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		IApiComponent[] components = baseline.resolvePackage(baseline.getApiComponent("component.b"), "component.a");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", baseline.getApiComponent("component.a"), components[0]);
	}
	
	/**
	 * Resolves a package within a single component
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolvePackageWithinComponent() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		IApiComponent[] components = baseline.resolvePackage(baseline.getApiComponent("component.a"), "a.b.c");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", baseline.getApiComponent("component.a"), components[0]);
	}	
	
	/**
	 * Resolves a system package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolveJavaLangPackage() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		IApiComponent[] components = baseline.resolvePackage(baseline.getApiComponent("component.b"), "java.lang");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", baseline.getApiComponent(baseline.getExecutionEnvironment()), components[0]);
	}
	
	/**
	 * Resolves a system package
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testResolveSystemPackage() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		IApiComponent[] components = baseline.resolvePackage(baseline.getApiComponent("component.b"), "org.w3c.dom");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", baseline.getApiComponent(baseline.getExecutionEnvironment()), components[0]);
	}	
	
	/**
	 * Finds the class file for java.lang.Object
	 * 
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	public void testFindJavaLangObject() throws FileNotFoundException, CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNotNull("the testing baseline should exist", baseline);
		IApiComponent[] components = baseline.resolvePackage(baseline.getApiComponent("component.b"), "java.lang");
		assertNotNull("No component", components);
		assertEquals("Wrong size", 1, components.length);
		assertEquals("Wrong provider for package", baseline.getApiComponent(baseline.getExecutionEnvironment()), components[0]);
		IApiTypeRoot classFile = components[0].findTypeRoot("java.lang.Object");
		assertNotNull("Missing java.lang.Object", classFile);
		assertEquals("Wrong type name", "java.lang.Object", classFile.getTypeName());
	}	
	
	/**
	 * Validates basic component attributes.
	 * 
	 * @param baseline baseline to retrieve the component from
	 * @param id component id
	 * @param name component name
	 * @param version component version
	 * @param environment execution environment id
	 * @param requiredComponents list of {@link IRequiredComponentDescription}
	 * @throws CoreException 
	 */
	private void validateComponent(IApiBaseline baseline, String id, String name, String version, String environment, List<IRequiredComponentDescription> requiredComponents) throws CoreException {
		IApiComponent component = baseline.getApiComponent(id);
		
		assertEquals("Id: ", id , component.getId());
		assertEquals("Name: ", name , component.getName());
		assertEquals("Version: ", version , component.getVersion());
		String[] envs = component.getExecutionEnvironments();
		assertEquals("Wrong number of execution environments", 1, envs.length);
		assertEquals("Version: ", environment , envs[0]);
		
		IRequiredComponentDescription[] actual = component.getRequiredComponents();
		assertEquals("Wrong number of required components", requiredComponents.size(), actual.length);
				
		for (int i = 0; i < requiredComponents.size(); i++) {
			assertEquals("Wrong required component", requiredComponents.get(i), actual[i]);
		}		
	}
	
	/**
	 * Tests creating a baseline with a component that has a nested jar 
	 * of class files.
	 * 
	 * @throws CoreException
	 */
	public void testNestedJarComponent() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-nested-jars");
		IApiComponent component = baseline.getApiComponent("component.a");
		assertNotNull("missing component.a", component);
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertTrue("Missing containers:", containers.length > 0);
		IApiTypeRoot file = null;
		for (int i = 0; i < containers.length; i++) {
			IApiTypeContainer container = containers[i];
			String[] names = container.getPackageNames();
			for (int j = 0; j < names.length; j++) {
				if (names[j].equals("component.a")) {
					file = container.findTypeRoot("component.a.A");
					break;
				}
			}
			if (file != null) {
				break;
			}
		}
		assertNotNull("Missing class file", file);
		assertEquals("Wrong type name", "component.a.A", file.getTypeName());
	}
	
	/**
	 * Tests that an x-friends directive works. Component A exports package
	 * component.a.friend.of.b as a friend for b. Note - the package should
	 * still be private.
	 * 
	 * @throws CoreException 
	 */
	public void testXFriendsDirective() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.friend.of.b.FriendOfB"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be PRIVATE", VisibilityModifiers.isPrivate(visibility));
	}
	
	/**
	 * Tests that an x-internal directive works. Component A exports package
	 * component.a.internal as internal.
	 * 
	 * @throws CoreException 
	 */
	public void testXInternalDirective() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.internal.InternalClass"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility));
	}	
	
	/**
	 * Tests that a 'uses' directive works. Component A exports package
	 * component.a. with a 'uses' directive but should still be API.
	 * 
	 * @throws CoreException 
	 */
	public void testUsesDirective() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.A"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be API", VisibilityModifiers.isAPI(visibility));
	}	
	
	/**
	 * Tests that a non-exported package is private. Component A does not export package
	 * component.a.not.exported.
	 * 
	 * @throws CoreException 
	 */
	public void testNotExported() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		assertNotNull("Missing component.a", component);
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.not.exported.NotExported"));
		assertNotNull("Missing API description", result);
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility));
	}		
	
	/**
	 * Tests component prerequisites.
	 *  
	 * @throws CoreException
	 */
	public void testPrerequisites() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		IApiComponent[] prerequisiteComponents = baseline.getPrerequisiteComponents(new IApiComponent[]{component});
		for (int i = 0; i < prerequisiteComponents.length; i++) {
			IApiComponent apiComponent = prerequisiteComponents[i];
			if (apiComponent.getId().equals("org.eclipse.osgi")) {
				// done
				return;
			}
		}
		assertTrue("Missing prerequisite bundle", false);
	}
	
	/**
	 * Tests component dependents.
	 *  
	 * @throws CoreException
	 */
	public void testDependents() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		IApiComponent component = baseline.getApiComponent("component.a");
		IApiComponent[] dependents = baseline.getDependentComponents(new IApiComponent[]{component});
		assertEquals("Wrong number of dependents", 2, dependents.length);
		for (int i = 0; i < dependents.length; i++) {
			IApiComponent apiComponent = dependents[i];
			if (apiComponent.getId().equals("component.b")) {
				// done
				return;
			}
		}
		assertEquals("Missing dependent component.b", false);
	}	
	
	/**
	 * Tests getting the location from an 'old' baseline
	 */
	public void testGetLocation() throws Exception {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-plugins");
		assertNull("The location must be null", baseline.getLocation());
		baseline.setLocation("new_loc");
		assertNotNull("The location must not be null", baseline.getLocation());
	}
}
