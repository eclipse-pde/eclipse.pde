/*******************************************************************************
 * Copyright (c) 2007, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.tests.util.ProjectUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Test creation of states and components.
 *
 * The API Baseline Tests should be run as JUnit tests, not JUnit Plug-in Tests.
 * This means that there is no OSGi environment available. The vm argument
 * requiredBundles must be set to a valid baseline. In addition, rather than use
 * the EE profiles provided by OSGi, the baseline will resolve using EEs found
 * in the org.eclipse.pde.api.tools.internal.util.profiles inside the
 * org.eclipse.pde.api.tools bundle.
 *
 * "-DrequiredBundles=${eclipse_home}/plugins"
 *
 * @since 1.0.0
 */
public class ApiBaselineTests {

	static final String _1_0_0 = "1.0.0"; //$NON-NLS-1$
	static final String COMPONENT_B = "component.b"; //$NON-NLS-1$
	static final String COMPONENT_A = "component.a"; //$NON-NLS-1$
	static final String TEST_PLUGINS = "test-plugins"; //$NON-NLS-1$

	IApiBaseline fBaseline = null;

	@Before
	public void setUp() throws Exception {
		if (fBaseline == null) {
			fBaseline = TestSuiteHelper.createTestingBaseline(TEST_PLUGINS);
			assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
			List<IRequiredComponentDescription> reqs = new ArrayList<>();
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange(Util.EMPTY_STRING))); //$NON-NLS-1$
			validateComponent(fBaseline, COMPONENT_A, "A Plug-in", _1_0_0, "J2SE-1.5", reqs); //$NON-NLS-1$ //$NON-NLS-2$

			reqs = new ArrayList<>();
			reqs.add(new RequiredComponentDescription("org.eclipse.core.runtime", new BundleVersionRange(Util.EMPTY_STRING))); //$NON-NLS-1$
			reqs.add(new RequiredComponentDescription(COMPONENT_A, new BundleVersionRange(Util.EMPTY_STRING)));
			validateComponent(fBaseline, COMPONENT_B, "B Plug-in", _1_0_0, "J2SE-1.4", reqs); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * Resolves a package
	 *
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	@Test
	public void testResolvePackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), COMPONENT_A);
		assertNotNull("No component", components); //$NON-NLS-1$
		assertEquals("Wrong size", 1, components.length); //$NON-NLS-1$
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(COMPONENT_A), components[0]); //$NON-NLS-1$
	}

	/**
	 * Resolves a package within a single component
	 *
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	@Test
	public void testResolvePackageWithinComponent() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_A), "a.b.c"); //$NON-NLS-1$
		assertNotNull("No component", components); //$NON-NLS-1$
		assertEquals("Wrong size", 1, components.length); //$NON-NLS-1$
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(COMPONENT_A), components[0]); //$NON-NLS-1$
	}

	/**
	 * Resolves a system package
	 *
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	@Test
	public void testResolveJavaLangPackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "java.lang"); //$NON-NLS-1$
		assertNotNull("No component", components); //$NON-NLS-1$
		assertEquals("Wrong size", 1, components.length); //$NON-NLS-1$
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]); //$NON-NLS-1$
	}

	/**
	 * Resolves a system package
	 *
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	@Test
	public void testResolveSystemPackage() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "org.w3c.dom"); //$NON-NLS-1$
		assertNotNull("No component", components); //$NON-NLS-1$
		assertEquals("Wrong size", 1, components.length); //$NON-NLS-1$
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]); //$NON-NLS-1$
	}

	/**
	 * Finds the class file for java.lang.Object
	 *
	 * @throws FileNotFoundException
	 * @throws CoreException
	 */
	@Test
	public void testFindJavaLangObject() throws FileNotFoundException, CoreException {
		assertNotNull("the testing baseline should exist", fBaseline); //$NON-NLS-1$
		IApiComponent[] components = fBaseline.resolvePackage(fBaseline.getApiComponent(COMPONENT_B), "java.lang"); //$NON-NLS-1$
		assertNotNull("No component", components); //$NON-NLS-1$
		assertEquals("Wrong size", 1, components.length); //$NON-NLS-1$
		assertEquals("Wrong provider for package", fBaseline.getApiComponent(fBaseline.getExecutionEnvironment()), components[0]); //$NON-NLS-1$
		IApiTypeRoot classFile = components[0].findTypeRoot("java.lang.Object"); //$NON-NLS-1$
		assertNotNull("Missing java.lang.Object", classFile); //$NON-NLS-1$
		String objectTypeName = "java.lang.Object"; //$NON-NLS-1$
		if (ProjectUtils.isJava9Compatible()) {
			objectTypeName = "classes.java.lang.Object"; //$NON-NLS-1$

		}
		assertEquals("Wrong type name", objectTypeName, classFile.getTypeName()); //$NON-NLS-1$
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

		assertEquals("Id: ", id, component.getSymbolicName()); //$NON-NLS-1$
		assertEquals("Name: ", name, component.getName()); //$NON-NLS-1$
		assertEquals("Version: ", version, component.getVersion()); //$NON-NLS-1$
		String[] envs = component.getExecutionEnvironments();
		assertEquals("Wrong number of execution environments", 1, envs.length); //$NON-NLS-1$
		assertEquals("Version: ", environment, envs[0]); //$NON-NLS-1$

		IRequiredComponentDescription[] actual = component.getRequiredComponents();
		assertEquals("Wrong number of required components", requiredComponents.size(), actual.length); //$NON-NLS-1$

		for (int i = 0; i < requiredComponents.size(); i++) {
			assertEquals("Wrong required component", requiredComponents.get(i), actual[i]); //$NON-NLS-1$
		}
	}

	/**
	 * Tests creating a baseline with a component that has a nested jar of class
	 * files.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testNestedJarComponent() throws CoreException {
		IApiBaseline baseline = TestSuiteHelper.createTestingBaseline("test-nested-jars"); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(COMPONENT_A);
		assertNotNull("missing component.a", component); //$NON-NLS-1$
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertTrue("Missing containers:", containers.length > 0); //$NON-NLS-1$
		IApiTypeRoot file = null;
		for (int i = 0; i < containers.length; i++) {
			IApiTypeContainer container = containers[i];
			String[] names = container.getPackageNames();
			for (int j = 0; j < names.length; j++) {
				if (names[j].equals(COMPONENT_A)) {
					file = container.findTypeRoot("component.a.A"); //$NON-NLS-1$
					break;
				}
			}
			if (file != null) {
				break;
			}
		}
		assertNotNull("Missing class file", file); //$NON-NLS-1$
		assertEquals("Wrong type name", "component.a.A", file.getTypeName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that an x-friends directive works. Component A exports package
	 * component.a.friend.of.b as a friend for b. Note - the package should
	 * still be private.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testXFriendsDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component); //$NON-NLS-1$
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.friend.of.b.FriendOfB")); //$NON-NLS-1$
		assertNotNull("Missing API description", result); //$NON-NLS-1$
		int visibility = result.getVisibility();
		assertTrue("Should be PRIVATE", VisibilityModifiers.isPrivate(visibility)); //$NON-NLS-1$
	}

	/**
	 * Tests that an x-internal directive works. Component A exports package
	 * component.a.internal as internal.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testXInternalDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component); //$NON-NLS-1$
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.internal.InternalClass")); //$NON-NLS-1$
		assertNotNull("Missing API description", result); //$NON-NLS-1$
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility)); //$NON-NLS-1$
	}

	/**
	 * Tests that a 'uses' directive works. Component A exports package
	 * component.a. with a 'uses' directive but should still be API.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testUsesDirective() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component); //$NON-NLS-1$
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.A")); //$NON-NLS-1$
		assertNotNull("Missing API description", result); //$NON-NLS-1$
		int visibility = result.getVisibility();
		assertTrue("Should be API", VisibilityModifiers.isAPI(visibility)); //$NON-NLS-1$
	}

	/**
	 * Tests that a non-exported package is private. Component A does not export
	 * package component.a.not.exported.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testNotExported() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		assertNotNull("Missing component.a", component); //$NON-NLS-1$
		IApiDescription description = component.getApiDescription();
		IApiAnnotations result = description.resolveAnnotations(Factory.typeDescriptor("component.a.not.exported.NotExported")); //$NON-NLS-1$
		assertNotNull("Missing API description", result); //$NON-NLS-1$
		int visibility = result.getVisibility();
		assertTrue("Should be private", VisibilityModifiers.isPrivate(visibility)); //$NON-NLS-1$
	}

	/**
	 * Tests component prerequisites.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testPrerequisites() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		IApiComponent[] prerequisiteComponents = fBaseline.getPrerequisiteComponents(new IApiComponent[] { component });
		for (int i = 0; i < prerequisiteComponents.length; i++) {
			IApiComponent apiComponent = prerequisiteComponents[i];
			if (apiComponent.getSymbolicName().equals("org.eclipse.osgi")) { //$NON-NLS-1$
				// done
				return;
			}
		}
		fail("Missing prerequisite bundle"); //$NON-NLS-1$
	}

	/**
	 * Tests component dependents.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testDependents() throws CoreException {
		IApiComponent component = fBaseline.getApiComponent(COMPONENT_A);
		IApiComponent[] dependents = fBaseline.getDependentComponents(new IApiComponent[] { component });
		assertEquals("Wrong number of dependents", 2, dependents.length); //$NON-NLS-1$
		for (IApiComponent apiComponent : dependents) {
			if (apiComponent.getSymbolicName().equals(COMPONENT_B)) {
				// done
				return;
			}
		}
		assertEquals("Missing dependent component.b", false); //$NON-NLS-1$
	}

	/**
	 * Tests getting the location from an 'old' baseline
	 */
	@Test
	public void testGetLocation() throws Exception {
		assertNull("The location must be null", fBaseline.getLocation()); //$NON-NLS-1$
		fBaseline.setLocation("new_loc"); //$NON-NLS-1$
		assertNotNull("The location must not be null", fBaseline.getLocation()); //$NON-NLS-1$
	}
}
