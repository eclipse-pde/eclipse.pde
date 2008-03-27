/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.ArchiveClassFileContainer;
import org.eclipse.pde.api.tools.internal.DirectoryClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;

/**
 * Tests the class file containers
 * 
 * @since 1.0.0
 */
public class ClassFileContainerTests extends TestCase {

	public static Test suite() {
		return new TestSuite(ClassFileContainerTests.class);
	}	
	
	public ClassFileContainerTests() {
		super();
	}
	
	public ClassFileContainerTests(String name) {
		super(name);
	}
	
	/**
	 * Builds a sample archive on sample.jar
	 * 
	 * @return sample archive
	 */
	protected IClassFileContainer buildArchiveContainer() {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-jars").append("sample.jar");
		File file = path.toFile();
		assertTrue("Missing jar file", file.exists());
		return new ArchiveClassFileContainer(path.toOSString(), null);
	}
	
	/**
	 * Builds a sample container on directory
	 * 
	 * @return sample directory container
	 */
	protected IClassFileContainer buildDirectoryContainer() {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-bin-dir");
		File file = path.toFile();
		assertTrue("Missing bin directory", file.exists());
		return new DirectoryClassFileContainer(path.toOSString(), null);
	}	
	
	/**
	 * Tests retrieving package names from an archive.
	 * 
	 * @throws CoreException
	 */
	public void testArchivePackageNames() throws CoreException {
		doTestPackageNames(buildArchiveContainer());
	}	
	
	/**
	 * Tests retrieving package names from an directory.
	 * 
	 * @throws CoreException
	 */
	public void testDirectoryPackageNames() throws CoreException {
		doTestPackageNames(buildDirectoryContainer());
	}	
	
	/**
	 * Tests retrieving package names.
	 * 
	 * @param container class file container
	 * @throws CoreException
	 */
	protected void doTestPackageNames(IClassFileContainer container) throws CoreException {
		String[] packageNames = container.getPackageNames();
		Set<String> knownNames = new HashSet<String>();
		knownNames.add("");
		knownNames.add("a");
		knownNames.add("a.b.c");
		assertEquals("Wrong number of packages", 3, packageNames.length);
		for (int i = 0; i < packageNames.length; i++) {
			assertTrue("Missing package " + packageNames[i], knownNames.remove(packageNames[i]));
		}
		assertTrue("Should be no left over packages", knownNames.isEmpty());
	}
	
	/**
	 * Tests visiting packages in an archive.
	 * 
	 * @throws CoreException
	 */
	public void testArchiveVistPackages() throws CoreException {
		doTestVisitPackages(buildArchiveContainer());
	}
	
	/**
	 * Tests visiting packages in an directory.
	 * 
	 * @throws CoreException
	 */
	public void testDirectoryVistPackages() throws CoreException {
		doTestVisitPackages(buildDirectoryContainer());
	}
	
	/**
	 * Test visiting packages
	 * 
	 * @param container class file container
	 * @throws CoreException 
	 */
	protected void doTestVisitPackages(IClassFileContainer container) throws CoreException {
		final List<String> expectedPkgOrder = new ArrayList<String>();
		expectedPkgOrder.add("");
		expectedPkgOrder.add("a");
		expectedPkgOrder.add("a.b.c");
		final List<String> visit = new ArrayList<String>();
		ClassFileContainerVisitor visitor = new ClassFileContainerVisitor() {
			public boolean visitPackage(String packageName) {
				visit.add(packageName);
				return false;
			}
			public void visit(String packageName, IClassFile classFile) {
				assertTrue("Should not visit types", false);
			}
			public void endVisitPackage(String packageName) {
				assertTrue("Wrong end visit order", visit.get(visit.size() - 1).equals(packageName));
			}
			public void end(String packageName, IClassFile classFile) {
				assertTrue("Should not visit types", false);
			}
		};
		container.accept(visitor);
		assertEquals("Visited wrong number of packages", expectedPkgOrder.size(), visit.size());
		assertEquals("Visit order incorrect", expectedPkgOrder, visit);
	}
	
	/**
	 * Tests visiting class files in an archive.
	 * 
	 * @throws CoreException
	 */
	public void testArchiveVisitClassFiles() throws CoreException {
		doTestVisitClassFiles(buildArchiveContainer());
	}
	
	/**
	 * Tests visiting class files in a directory.
	 * 
	 * @throws CoreException
	 */
	public void testDirectoryVisitClassFiles() throws CoreException {
		doTestVisitClassFiles(buildDirectoryContainer());
	}	
	
	/**
	 * Test visiting class files
	 * 
	 * @param container class file container
	 * @throws CoreException
	 */
	protected void doTestVisitClassFiles(IClassFileContainer container) throws CoreException {
		final Map<String, List<String>> expectedTypes = new HashMap<String, List<String>>();
		final List<String> expectedPkgOrder = new ArrayList<String>();
		expectedPkgOrder.add("");
			List<String> cf = new ArrayList<String>();
			cf.add("DefA");
			cf.add("DefB");
			expectedTypes.put("", cf);
		expectedPkgOrder.add("a");
			cf = new ArrayList<String>();
			cf.add("a.ClassA");
			cf.add("a.ClassB");
			cf.add("a.ClassB$InsideB");
			expectedTypes.put("a", cf);
		expectedPkgOrder.add("a.b.c");
			cf = new ArrayList<String>();
			cf.add("a.b.c.ClassC");
			cf.add("a.b.c.ClassD");
			cf.add("a.b.c.InterfaceC");
			expectedTypes.put("a.b.c", cf);
		final List<String> visit = new ArrayList<String>();
		final Map<String, List<String>> visitTypes = new HashMap<String, List<String>>();
		ClassFileContainerVisitor visitor = new ClassFileContainerVisitor() {
			public boolean visitPackage(String packageName) {
				visit.add(packageName);
				return true;
			}
			public void visit(String packageName, IClassFile classFile) {
				assertTrue("Should not visit types", visit.get(visit.size() - 1).equals(packageName));
				List<String> types = visitTypes.get(packageName);
				if (types == null) {
					types = new ArrayList<String>();
					visitTypes.put(packageName, types);
				}
				types.add(classFile.getTypeName());
			}
			
			public void endVisitPackage(String packageName) {
				assertTrue("Wrong end visit order", visit.get(visit.size() - 1).equals(packageName));
				assertEquals("Visited wrong types", expectedTypes.get(packageName), visitTypes.get(packageName));
			}
			public void end(String packageName, IClassFile classFile) {
				List<String> types = visitTypes.get(packageName);
				assertTrue("Should not visit types", types.get(types.size() - 1).equals(classFile.getTypeName()));
			}
		};
		container.accept(visitor);
		assertEquals("Visited wrong number of packages", expectedPkgOrder.size(), visit.size());
		assertEquals("Visit order incorrect", expectedPkgOrder, visit);		
	}
}
