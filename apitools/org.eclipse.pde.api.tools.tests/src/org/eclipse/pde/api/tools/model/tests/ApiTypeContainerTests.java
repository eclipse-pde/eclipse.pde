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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.model.ArchiveApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.junit.Test;

/**
 * Tests the class file containers
 *
 * @since 1.0.0
 */
public class ApiTypeContainerTests {

	/**
	 * Builds a sample archive on sample.jar
	 *
	 * @return sample archive
	 */
	protected IApiTypeContainer buildArchiveContainer() {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-jars").append("sample.jar"); //$NON-NLS-1$ //$NON-NLS-2$
		File file = path.toFile();
		assertTrue("Missing jar file", file.exists()); //$NON-NLS-1$
		return new ArchiveApiTypeContainer(null, path.toOSString());
	}

	/**
	 * Builds a sample container on directory
	 *
	 * @return sample directory container
	 */
	protected IApiTypeContainer buildDirectoryContainer() {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-bin-dir"); //$NON-NLS-1$
		File file = path.toFile();
		assertTrue("Missing bin directory", file.exists()); //$NON-NLS-1$
		return new DirectoryApiTypeContainer(null, path.toOSString());
	}

	/**
	 * Tests retrieving package names from an archive.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testArchivePackageNames() throws CoreException {
		doTestPackageNames(buildArchiveContainer());
	}

	/**
	 * Tests retrieving package names from an directory.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testDirectoryPackageNames() throws CoreException {
		doTestPackageNames(buildDirectoryContainer());
	}

	/**
	 * Tests retrieving package names.
	 *
	 * @param container class file container
	 * @throws CoreException
	 */
	protected void doTestPackageNames(IApiTypeContainer container) throws CoreException {
		String[] packageNames = container.getPackageNames();
		Set<String> knownNames = new HashSet<>();
		knownNames.add(""); //$NON-NLS-1$
		knownNames.add("a"); //$NON-NLS-1$
		knownNames.add("a.b.c"); //$NON-NLS-1$
		assertEquals("Wrong number of packages", 3, packageNames.length); //$NON-NLS-1$
		for (String packageName : packageNames) {
			assertTrue("Missing package " + packageName, knownNames.remove(packageName)); //$NON-NLS-1$
		}
		assertTrue("Should be no left over packages", knownNames.isEmpty()); //$NON-NLS-1$
	}

	/**
	 * Tests visiting packages in an archive.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testArchiveVistPackages() throws CoreException {
		doTestVisitPackages(buildArchiveContainer());
	}

	/**
	 * Tests visiting packages in an directory.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testDirectoryVistPackages() throws CoreException {
		doTestVisitPackages(buildDirectoryContainer());
	}

	/**
	 * Test visiting packages
	 *
	 * @param container class file container
	 * @throws CoreException
	 */
	protected void doTestVisitPackages(IApiTypeContainer container) throws CoreException {
		final List<String> expectedPkgOrder = new ArrayList<>();
		expectedPkgOrder.add(""); //$NON-NLS-1$
		expectedPkgOrder.add("a"); //$NON-NLS-1$
		expectedPkgOrder.add("a.b.c"); //$NON-NLS-1$
		final List<String> visit = new ArrayList<>();
		ApiTypeContainerVisitor visitor = new ApiTypeContainerVisitor() {
			@Override
			public boolean visitPackage(String packageName) {
				visit.add(packageName);
				return false;
			}
			@Override
			public void visit(String packageName, IApiTypeRoot classFile) {
				fail("Should not visit types"); //$NON-NLS-1$
			}
			@Override
			public void endVisitPackage(String packageName) {
				assertTrue("Wrong end visit order", visit.get(visit.size() - 1).equals(packageName)); //$NON-NLS-1$
			}
			@Override
			public void end(String packageName, IApiTypeRoot classFile) {
				fail("Should not visit types"); //$NON-NLS-1$
			}
		};
		container.accept(visitor);
		assertEquals("Visited wrong number of packages", expectedPkgOrder.size(), visit.size()); //$NON-NLS-1$
		assertEquals("Visit order incorrect", expectedPkgOrder, visit); //$NON-NLS-1$
	}

	/**
	 * Tests visiting class files in an archive.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testArchiveVisitClassFiles() throws CoreException {
		doTestVisitClassFiles(buildArchiveContainer());
	}

	/**
	 * Tests visiting class files in a directory.
	 *
	 * @throws CoreException
	 */
	@Test
	public void testDirectoryVisitClassFiles() throws CoreException {
		doTestVisitClassFiles(buildDirectoryContainer());
	}

	/**
	 * Test visiting class files
	 *
	 * @param container class file container
	 * @throws CoreException
	 */
	protected void doTestVisitClassFiles(IApiTypeContainer container) throws CoreException {
		final Map<String, List<String>> expectedTypes = new HashMap<>();
		final List<String> expectedPkgOrder = new ArrayList<>();
		expectedPkgOrder.add(""); //$NON-NLS-1$
			List<String> cf = new ArrayList<>();
			cf.add("DefA"); //$NON-NLS-1$
			cf.add("DefB"); //$NON-NLS-1$
			expectedTypes.put("", cf); //$NON-NLS-1$
		expectedPkgOrder.add("a"); //$NON-NLS-1$
			cf = new ArrayList<>();
			cf.add("a.ClassA"); //$NON-NLS-1$
			cf.add("a.ClassB"); //$NON-NLS-1$
			cf.add("a.ClassB$InsideB"); //$NON-NLS-1$
			expectedTypes.put("a", cf); //$NON-NLS-1$
		expectedPkgOrder.add("a.b.c"); //$NON-NLS-1$
			cf = new ArrayList<>();
			cf.add("a.b.c.ClassC"); //$NON-NLS-1$
			cf.add("a.b.c.ClassD"); //$NON-NLS-1$
			cf.add("a.b.c.InterfaceC"); //$NON-NLS-1$
			expectedTypes.put("a.b.c", cf); //$NON-NLS-1$
		final List<String> visit = new ArrayList<>();
		final Map<String, List<String>> visitTypes = new HashMap<>();
		ApiTypeContainerVisitor visitor = new ApiTypeContainerVisitor() {
			@Override
			public boolean visitPackage(String packageName) {
				visit.add(packageName);
				return true;
			}
			@Override
			public void visit(String packageName, IApiTypeRoot classFile) {
				assertTrue("Should not visit types", visit.get(visit.size() - 1).equals(packageName)); //$NON-NLS-1$
				List<String> types = visitTypes.get(packageName);
				if (types == null) {
					types = new ArrayList<>();
					visitTypes.put(packageName, types);
				}
				types.add(classFile.getTypeName());
			}

			@Override
			public void endVisitPackage(String packageName) {
				assertTrue("Wrong end visit order", visit.get(visit.size() - 1).equals(packageName)); //$NON-NLS-1$
				assertEquals("Visited wrong types", expectedTypes.get(packageName), visitTypes.get(packageName)); //$NON-NLS-1$
			}
			@Override
			public void end(String packageName, IApiTypeRoot classFile) {
				List<String> types = visitTypes.get(packageName);
				assertTrue("Should not visit types", types.get(types.size() - 1).equals(classFile.getTypeName())); //$NON-NLS-1$
			}
		};
		container.accept(visitor);
		assertEquals("Visited wrong number of packages", expectedPkgOrder.size(), visit.size()); //$NON-NLS-1$
		assertEquals("Visit order incorrect", expectedPkgOrder, visit);		 //$NON-NLS-1$
	}
}
