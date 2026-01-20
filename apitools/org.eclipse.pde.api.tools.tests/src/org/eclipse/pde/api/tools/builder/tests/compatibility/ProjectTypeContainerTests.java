/*******************************************************************************
 * Copyright (c) 2009, 2024 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

import junit.framework.Test;

/**
 * Tests project type container
 */
public class ProjectTypeContainerTests extends CompatibilityTest {

	public ProjectTypeContainerTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(ProjectTypeContainerTests.class);
	}

	@Override
	protected String getTestingProjectName() {
		// not used
		return null;
	}

	@Override
	protected int getDefaultProblemId() {
		// not used
		return 0;
	}

	/**
	 * Returns the component associated with the given project in the workspace.
	 */
	private IApiComponent getComponent(String projectName) {
		IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		assertNotNull("Missing workspace baseline", baseline); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(getEnv().getProject(projectName));
		assertNotNull("Missing API component", component); //$NON-NLS-1$
		return component;
	}

	/**
	 * Returns the type container associated with the given project in the
	 * workspace.
	 */
	private IApiTypeContainer getTypeContainer(String projectName) throws CoreException {
		IApiComponent component = getComponent(projectName);
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertEquals("Wrong number of API type containers", 1, containers.length); //$NON-NLS-1$
		return containers[0];
	}

	protected IPackageFragment[] getAllPackages() throws CoreException {
		IJavaProject project = JavaCore.create(getEnv().getProject("bundle.a")); //$NON-NLS-1$
		List<IPackageFragment> pkgs = new ArrayList<>();
		for (IPackageFragmentRoot root : project.getAllPackageFragmentRoots()) {
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				for (IJavaElement child : root.getChildren()) {
					IPackageFragment frag = (IPackageFragment) child;
					pkgs.add(frag);
					collectAllPackages(frag, pkgs);
				}
			}
		}
		return pkgs.toArray(new IPackageFragment[pkgs.size()]);
	}

	protected void collectAllPackages(IPackageFragment pkg, List<IPackageFragment> collect) throws CoreException {
		for (IJavaElement element : pkg.getChildren()) {
			if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				IPackageFragment frag = (IPackageFragment) element;
				collect.add(frag);
				collectAllPackages(frag, collect);
			}
		}
	}

	/**
	 * Returns all expected packages in the test project.
	 */
	protected Set<String> getAllPackageNames() {
		// build expected list
		Set<String> set = new HashSet<>();
		set.add("a"); //$NON-NLS-1$
		set.add("a.annotations"); //$NON-NLS-1$
		set.add("a.bundles"); //$NON-NLS-1$
		set.add("a.bundles.internal"); //$NON-NLS-1$
		set.add("a.classes"); //$NON-NLS-1$
		set.add("a.classes.constructors"); //$NON-NLS-1$
		set.add("a.classes.fields"); //$NON-NLS-1$
		set.add("a.classes.hierarchy"); //$NON-NLS-1$
		set.add("a.classes.internal"); //$NON-NLS-1$
		set.add("a.classes.membertypes"); //$NON-NLS-1$
		set.add("a.classes.methods"); //$NON-NLS-1$
		set.add("a.classes.modifiers"); //$NON-NLS-1$
		set.add("a.classes.restrictions"); //$NON-NLS-1$
		set.add("a.classes.typeparameters"); //$NON-NLS-1$
		set.add("a.constructors"); //$NON-NLS-1$
		set.add("a.enums"); //$NON-NLS-1$
		set.add("a.fields"); //$NON-NLS-1$
		set.add("a.fields.modifiers"); //$NON-NLS-1$
		set.add("a.interfaces"); //$NON-NLS-1$
		set.add("a.interfaces.members"); //$NON-NLS-1$
		set.add("a.interfaces.restrictions"); //$NON-NLS-1$
		set.add("a.interfaces.typeparameters"); //$NON-NLS-1$
		set.add("a.methods"); //$NON-NLS-1$
		set.add("a.methods.modifiers"); //$NON-NLS-1$
		set.add("a.methods.typeparameters"); //$NON-NLS-1$
		set.add("a.since"); //$NON-NLS-1$
		set.add("a.version"); //$NON-NLS-1$
		set.add("a.version.internal"); //$NON-NLS-1$
		return set;
	}

	protected Set<String> collectAllTypeNames() throws CoreException {
		Set<String> names = new HashSet<>();
		for (IPackageFragment pkg : getAllPackages()) {
			for (ICompilationUnit unit : pkg.getCompilationUnits()) {
				for (IType iType : unit.getTypes()) {
					names.add(iType.getFullyQualifiedName('$'));
				}
			}
		}
		return names;
	}

	/**
	 * Tests whether the execution environment can be extracted from both the
	 * {@code Bundle-RequiredExecutionEnvironment} and the
	 * {@code Require-Capability} header.
	 */
	public void testExecutionEnvironment() throws CoreException {
		IApiComponent bundleA = getComponent("bundle.a"); //$NON-NLS-1$
		assertEquals("Unable to find BREE for bundle using 'Bundle-RequiredExecutionEvironment'", //$NON-NLS-1$
				List.of("JavaSE-1.8"), bundleA.getExecutionEnvironments()); //$NON-NLS-1$

		IApiComponent bundleB = getComponent("bundle.b"); //$NON-NLS-1$
		assertEquals("Unable to find BREE for bundle using 'Require-Capability'", //$NON-NLS-1$
				List.of("JavaSE-17"), bundleB.getExecutionEnvironments()); //$NON-NLS-1$
	}

	/**
	 * Tests whether missing execution environments in the manifest are detected
	 * correctly.
	 */
	public void testNoExecutionEnvironment() throws CoreException {
		// Verify that the test-project is an existing java project in order to
		// ensure it gets the EE of the bound JDK injected (because it does not
		// declare an EE in its Manifest).
		assertTrue(JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getProject("bundle.c")).exists()); //$NON-NLS-1$
		IApiComponent bundleC = getComponent("bundle.c"); //$NON-NLS-1$
		assertEquals("Expected no EE because none is specified in the Manifest", //$NON-NLS-1$
				List.of(), bundleC.getExecutionEnvironments());
	}

	/**
	 * Tests all packages are returned.
	 */
	public void testPackageNames() throws CoreException {
		IApiTypeContainer container = getTypeContainer("bundle.a"); //$NON-NLS-1$
		assertEquals("Should be a project type container", IApiTypeContainer.FOLDER, container.getContainerType()); //$NON-NLS-1$

		assertThat(container.getPackageNames()).withFailMessage("Missing/wrong packages in type container") //$NON-NLS-1$
				.containsAll(getAllPackageNames());
	}

	/**
	 * Test type lookup.
	 */
	public void testFindType() throws CoreException {
		IApiTypeContainer container = getTypeContainer("bundle.a"); //$NON-NLS-1$
		IApiTypeRoot root = container.findTypeRoot("a.classes.fields.AddPrivateField"); //$NON-NLS-1$
		assertNotNull("Unable to find type 'a.classes.fields.AddPrivateField'", root); //$NON-NLS-1$
		IApiType structure = root.getStructure();
		assertEquals("Wrong type", "a.classes.fields.AddPrivateField", structure.getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Test that type lookup fails for a type that is not in the project.
	 */
	public void testMissingType() throws CoreException {
		IApiTypeContainer container = getTypeContainer("bundle.a"); //$NON-NLS-1$
		IApiTypeRoot root = container.findTypeRoot("some.bogus.Type"); //$NON-NLS-1$
		assertNull("Should not be able to find type 'some.bogus.Type'", root); //$NON-NLS-1$
	}

	/**
	 * Visits the container - all packages and types.
	 */
	public void testVisitor() throws CoreException {
		final Set<String> pkgNames = new HashSet<>();
		final Set<String> typeNames = new HashSet<>();
		ApiTypeContainerVisitor visitor = new ApiTypeContainerVisitor() {
			@Override
			public boolean visitPackage(String packageName) {
				pkgNames.add(packageName);
				return true;
			}

			@Override
			public void visit(String packageName, IApiTypeRoot typeroot) {
				typeNames.add(typeroot.getTypeName());
			}
		};
		getTypeContainer("bundle.a").accept(visitor); //$NON-NLS-1$

		// validate type names
		assertThat(typeNames).containsAll(collectAllTypeNames());

		// validate package names
		assertThat(pkgNames).containsAll(getAllPackageNames());
	}

	/**
	 * Tests that packages are correctly discovered in projects with multiple
	 * source folders that share the same output folder. This is a regression
	 * test for issue #2096 where packages were missing when multiple source
	 * folders output to the same location.
	 */
	public void testMultipleSourceFoldersWithSharedOutput() throws CoreException {
		IApiComponent component = getComponent("bundle.multisource"); //$NON-NLS-1$
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertEquals("Wrong number of API type containers", 1, containers.length); //$NON-NLS-1$
		IApiTypeContainer container = containers[0];
		// A single ProjectTypeContainer handles multiple source roots sharing
		// the same output location by tracking all package fragment roots
		assertEquals("Should be a folder type container", IApiTypeContainer.FOLDER, //$NON-NLS-1$
				container.getContainerType());

		String[] packageNames = container.getPackageNames();
		Set<String> pkgSet = new HashSet<>();
		for (String pkg : packageNames) {
			pkgSet.add(pkg);
		}

		// Both packages from different source folders should be found
		assertTrue("Missing package test.pkg1 from src1 folder", pkgSet.contains("test.pkg1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Missing package test.pkg2 from src2 folder", pkgSet.contains("test.pkg2")); //$NON-NLS-1$ //$NON-NLS-2$

		// Verify we can find types from both source folders
		IApiTypeRoot type1 = container.findTypeRoot("test.pkg1.TestClass1"); //$NON-NLS-1$
		assertNotNull("Should find TestClass1 from src1", type1); //$NON-NLS-1$

		IApiTypeRoot type2 = container.findTypeRoot("test.pkg2.TestClass2"); //$NON-NLS-1$
		assertNotNull("Should find TestClass2 from src2", type2); //$NON-NLS-1$
	}

}
