/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import junit.framework.Test;

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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getTestingProjectName()
	 */
	@Override
	protected String getTestingProjectName() {
		// not used
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.builder.tests.ApiBuilderTest#getDefaultProblemId()
	 */
	@Override
	protected int getDefaultProblemId() {
		// not used
		return 0;
	}
	
	/**
	 * Returns the type container associated with the "bundle.a" project in the workspace.
	 * 
	 * @return
	 */
	protected IApiTypeContainer getTypeContainer() throws CoreException {
		IApiBaseline baseline = ApiBaselineManager.getManager().getWorkspaceBaseline();
		assertNotNull("Missing workspace baseline", baseline);
		IApiComponent component = baseline.getApiComponent(getEnv().getProject("bundle.a"));
		assertNotNull("Missing API component", component);
		IApiTypeContainer[] containers = component.getApiTypeContainers();
		assertEquals("Wrong number of API type containers", 1, containers.length);
		IApiTypeContainer container = containers[0];
		return container;
	}
	
	protected IPackageFragment[] getAllPackages() throws CoreException {
		IJavaProject project = JavaCore.create(getEnv().getProject("bundle.a"));
		IPackageFragmentRoot[] roots = project.getAllPackageFragmentRoots();
		List<IPackageFragment> pkgs = new ArrayList<IPackageFragment>();
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
		return (IPackageFragment[]) pkgs.toArray(new IPackageFragment[pkgs.size()]);
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
		Set<String> set = new HashSet<String>();
		set.add("a");
		set.add("a.annotations");
		set.add("a.bundles");
		set.add("a.bundles.internal");
		set.add("a.classes");
		set.add("a.classes.constructors");
		set.add("a.classes.fields");
		set.add("a.classes.hierarchy");
		set.add("a.classes.internal");
		set.add("a.classes.membertypes");
		set.add("a.classes.methods");
		set.add("a.classes.modifiers");
		set.add("a.classes.restrictions");
		set.add("a.classes.typeparameters");
		set.add("a.constructors");
		set.add("a.enums");
		set.add("a.fields");
		set.add("a.fields.modifiers");
		set.add("a.interfaces");
		set.add("a.interfaces.members");
		set.add("a.interfaces.restrictions");
		set.add("a.interfaces.typeparameters");
		set.add("a.methods");
		set.add("a.methods.modifiers");
		set.add("a.methods.typeparameters");
		set.add("a.since");
		set.add("a.version");
		set.add("a.version.internal");
		return set;
	}
	
	protected Set<String> collectAllTypeNames() throws CoreException {
		IPackageFragment[] allPackages = getAllPackages();
		Set<String> names = new HashSet<String>();
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
		assertEquals("Should be a project type container", IApiTypeContainer.FOLDER, container.getContainerType());
		
		// build expected list
		Set<String> set = getAllPackageNames();
		
		String[] names = container.getPackageNames();
		//assertEquals("Wrong number of package names", set.size(), names.length);
		for (int i = 0; i < names.length; i++) {
			set.remove(names[i]);
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVERS");
			Iterator<String> iterator = set.iterator();
			while (iterator.hasNext()) {
				String string = (String) iterator.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong packages in type container", set.isEmpty());
	}
	
	/**
	 * Test type lookup.
	 * 
	 * @throws CoreException
	 */
	public void testFindType() throws CoreException {
		IApiTypeContainer container = getTypeContainer();
		IApiTypeRoot root = container.findTypeRoot("a.classes.fields.AddPrivateField");
		assertNotNull("Unable to find type 'a.classes.fields.AddPrivateField'", root);
		IApiType structure = root.getStructure();
		assertEquals("Wrong type", "a.classes.fields.AddPrivateField", structure.getName());
	}
	
	/**
	 * Test that type lookup fails for a type that is not in the project.
	 * 
	 * @throws CoreException
	 */
	public void testMissingType() throws CoreException {
		IApiTypeContainer container = getTypeContainer();
		IApiTypeRoot root = container.findTypeRoot("some.bogus.Type");
		assertNull("Should not be able to find type 'some.bogus.Type'", root);
	}
	
	/**
	 * Visits the container - all packages and types.
	 * 
	 * @throws CoreException
	 */
	public void testVisitor() throws CoreException {
		final Set<String> pkgNames = new HashSet<String>();
		final Set<String> typeNames = new HashSet<String>();
		ApiTypeContainerVisitor visitor = new ApiTypeContainerVisitor() {
			public boolean visitPackage(String packageName) {
				pkgNames.add(packageName);
				return true;
			}
			public void visit(String packageName, IApiTypeRoot typeroot) {
				typeNames.add(typeroot.getTypeName());
			}
		};
		getTypeContainer().accept(visitor);
		
		// validate type names
		Set<String> set = collectAllTypeNames();
		Iterator iterator = typeNames.iterator();
		while (iterator.hasNext()) {
			set.remove(iterator.next());
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVER TYPES");
			Iterator<String> iterator2 = set.iterator();
			while (iterator2.hasNext()) {
				String string = (String) iterator2.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong types in type container", set.isEmpty());
		
		// validate package names
		// build expected list
		set = getAllPackageNames();
		iterator = pkgNames.iterator();
		while (iterator.hasNext()) {
			set.remove(iterator.next());
		}
		if (!set.isEmpty()) {
			System.out.println("LEFTOVER PACKAGES");
			Iterator<String> iterator2 = set.iterator();
			while (iterator2.hasNext()) {
				String string = (String) iterator2.next();
				System.out.println(string);
			}
		}
		assertTrue("Missing/wrong packages in type container", set.isEmpty());
		

	}

}
