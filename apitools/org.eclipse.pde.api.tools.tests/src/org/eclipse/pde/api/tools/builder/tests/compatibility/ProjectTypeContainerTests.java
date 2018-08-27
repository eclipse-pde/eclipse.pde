/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
	 * Returns the type container associated with the "bundle.a" project in the
	 * workspace.
	 *
	 * @return
	 */
	protected IApiTypeContainer getTypeContainer() throws CoreException {
		IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		assertNotNull("Missing workspace baseline", baseline); //$NON-NLS-1$
		IApiComponent component = baseline.getApiComponent(getEnv().getProject("bundle.a")); //$NON-NLS-1$
		assertNotNull("Missing API component", component); //$NON-NLS-1$
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertEquals("Wrong number of API type containers", 1, containers.length); //$NON-NLS-1$
		IApiTypeContainer container = containers[0];
		return container;
	}

	protected IPackageFragment[] getAllPackages() throws CoreException {
		IJavaProject project = JavaCore.create(getEnv().getProject("bundle.a")); //$NON-NLS-1$
		IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
		List<IPackageFragment> pkgs = new ArrayList<>();
		for (int i = 0; i < roots.length; i++) {
			IPackageFragmentRoot root = roots[i];
			if (root.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IJavaElement[] children = root.getChildren();
				for (int j = 0; j < children.length; j++) {
					IPackageFragment frag = (IPackageFragment) children[j];
					pkgs.add(frag);
					collectAllPackages(frag, pkgs);
				}
			}
		}
		return pkgs.toArray(new IPackageFragment[pkgs.size()]);
	}

	protected void collectAllPackages(IPackageFragment pkg, List<IPackageFragment> collect) throws CoreException {
		IJavaElement[] children = pkg.getChildren();
		for (int i = 0; i < children.length; i++) {
			IJavaElement element = children[i];
			if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
				IPackageFragment frag = (IPackageFragment) element;
				collect.add(frag);
				collectAllPackages(frag, collect);
			}
		}
	}

	/**
	 * Returns all expected packages in the test project.
	 *
	 * @return
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
		IPackageFragment[] allPackages = getAllPackages();
		Set<String> names = new HashSet<>();
		for (int i = 0; i < allPackages.length; i++) {
			IPackageFragment pkg = allPackages[i];
			ICompilationUnit[] units = pkg.getCompilationUnits();
			for (int j = 0; j < units.length; j++) {
				ICompilationUnit unit = units[j];
				IType[] types = unit.getTypes();
				for (int k = 0; k < types.length; k++) {
					IType iType = types[k];
					names.add(iType.getFullyQualifiedName('$'));
				}
			}
		}
		return names;
	}

	/**
	 * Tests all packages are returned.
	 *
	 * @throws CoreException
	 */
	public void testPackageNames() throws CoreException {
		IApiTypeContainer container = getTypeContainer();
		assertEquals("Should be a project type container", IApiTypeContainer.FOLDER, container.getContainerType()); //$NON-NLS-1$

		// build expected list
		Set<String> set = getAllPackageNames();

		String[] names = container.getPackageNames();
		// assertEquals("Wrong number of package names", set.size(),
		// names.length);
		for (int i = 0; i < names.length; i++) {
			set.remove(names[i]);
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVERS"); //$NON-NLS-1$
			Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				String string = iterator.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong packages in type container", set.isEmpty()); //$NON-NLS-1$
	}

	/**
	 * Test type lookup.
	 *
	 * @throws CoreException
	 */
	public void testFindType() throws CoreException {
		IApiTypeContainer container = getTypeContainer();
		IApiTypeRoot root = container.findTypeRoot("a.classes.fields.AddPrivateField"); //$NON-NLS-1$
		assertNotNull("Unable to find type 'a.classes.fields.AddPrivateField'", root); //$NON-NLS-1$
		IApiType structure = root.getStructure();
		assertEquals("Wrong type", "a.classes.fields.AddPrivateField", structure.getName()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Test that type lookup fails for a type that is not in the project.
	 *
	 * @throws CoreException
	 */
	public void testMissingType() throws CoreException {
		IApiTypeContainer container = getTypeContainer();
		IApiTypeRoot root = container.findTypeRoot("some.bogus.Type"); //$NON-NLS-1$
		assertNull("Should not be able to find type 'some.bogus.Type'", root); //$NON-NLS-1$
	}

	/**
	 * Visits the container - all packages and types.
	 *
	 * @throws CoreException
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
		getTypeContainer().accept(visitor);

		// validate type names
		Set<String> set = collectAllTypeNames();
		Iterator<String> iterator = typeNames.iterator();
		while (iterator.hasNext()) {
			set.remove(iterator.next());
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVER TYPES"); //$NON-NLS-1$
			Iterator<String> iterator2 = set.iterator();
			while (iterator2.hasNext()) {
				String string = iterator2.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong types in type container", set.isEmpty()); //$NON-NLS-1$

		// validate package names
		// build expected list
		set = getAllPackageNames();
		iterator = pkgNames.iterator();
		while (iterator.hasNext()) {
			set.remove(iterator.next());
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVER PACKAGES"); //$NON-NLS-1$
			Iterator<String> iterator2 = set.iterator();
			while (iterator2.hasNext()) {
				String string = iterator2.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong packages in type container", set.isEmpty()); //$NON-NLS-1$

	}

}
