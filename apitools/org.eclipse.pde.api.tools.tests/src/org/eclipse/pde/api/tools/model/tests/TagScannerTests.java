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

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ArchiveClassFileContainer;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.DirectoryClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;

import com.ibm.icu.text.MessageFormat;

/**
 * Class tests that the tag scanner for the API tools correctly scans source 
 * for API tags
 * 
 * @since 1.0.0
 */
public class TagScannerTests extends TestCase {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source");
	private static final IPath BIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-classes");
	
	/**
	 * Creates a new empty API component description, not owned by any component.
	 * 
	 * @return
	 */
	protected IApiDescription newDescription() {
		return new ApiDescription(null);
	}
	
	/**
	 * Creates a new {@link ArchiveClassFileContainer} on the given path
	 * @param path
	 * @return
	 */
	protected IClassFileContainer newArchiveClassFileContainer(IPath path) {
		return new ArchiveClassFileContainer(path.toOSString(), null);
	}
	
	/**
	 * Returns a new compilation unit on the standard test source path with the
	 * specified name appended
	 * @param name
	 * @return a new compilation unit
	 */
	private CompilationUnit getCompilationUnit(String name) {
		Path path = (Path)SRC_LOC.append(name);
		return new CompilationUnit(path.toOSString());
	}
	
	/**
	 * Performs the scan to populate the manifest and traps exceptions thrown from the scanner
	 * @param name
	 * @param manifest
	 * @param cfc
	 */
	protected void doScan(String name, IApiDescription manifest, IClassFileContainer cfc) {
		try {
			TagScanner.newScanner().scan(getCompilationUnit(name), manifest, cfc);
		}
		catch(CoreException e) {
			fail(MessageFormat.format("Error scanning: {0}", new String[] {name}));
		}
	}
	
	/**
	 * Performs the scan to populate the manifest and traps exceptions thrown from the scanner
	 * @param name
	 * @param manifest
	 */
	protected void doScan(String name, IApiDescription manifest) {
		try {
			TagScanner.newScanner().scan(getCompilationUnit(name), manifest, null);
		}
		catch(CoreException e) {
			fail(MessageFormat.format("Error scanning: {0}", new String[] {name}));
		}
	}
	
	/**
	 * Tests that methods with non-simple type parameters have their signatures resolved if a backing class file 
	 * is provided (via an {@link IClassFileContainer})
	 */
	public void testBug212276() {
		DirectoryClassFileContainer container = new DirectoryClassFileContainer(BIN_LOC.toOSString(), null);
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod10.java", manifest, container);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(Ljava/lang/String;Ljava/lang/Integer;)V"));
		assertTrue("There should exist a description for method 'void one(String, Integer)'", description != null);
		assertTrue("There should be API visibility for method 'void one(String, Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(String, Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "two", "(Ljava/util/List;Ljava/lang/Runnable;)V"));
		assertTrue("There should exist a description for method 'void two(List, Runnable)'", description != null);
		assertTrue("There should be API visibility for method 'void two(List, Runnable)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(List, Runnable)'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(Ljava/lang/Object;Ljava/lang/Integer;)V"));
		assertTrue("There should exist a description for method 'void one(Object, Integer)'", description != null);
		assertTrue("There should be API visibility for method 'void one(Object, Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction and no extend restriction on method 'void one(Object, Integer)'", description.getRestrictions() == (RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_REFERENCE));
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "([[Ljava/lang/String;Ljava/lang/Integer;)V"));
		assertTrue("There should exist a description for method 'void one(String[][], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'void one(String[][], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void one(String[][], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that when given a class file container and the class file is not found
	 * and is required to resolve a method signature, an exception is thrown.
	 */
	public void testMissingClassfile() {
		IClassFileContainer container = new IClassFileContainer() {
			public String[] getPackageNames() throws CoreException {
				return new String[]{"there.are.none"};
			}
			public String getOrigin() {
				return "none";
			}
			public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
				return null;
			}
			public IClassFile findClassFile(String qualifiedName) throws CoreException {
				return null;
			}
			public void close() throws CoreException {
			}
		
			public void accept(ClassFileContainerVisitor visitor) throws CoreException {
			}
		};
		IApiDescription manifest = newDescription();
		try { 
			TagScanner.newScanner().scan(getCompilationUnit("a/b/c/TestMethod10.java"), manifest, container);
		} catch (CoreException e) {
			return;
		}
		fail("Should have been a core exception for missing class file");
	}	
	
	/**
	 * Tests that a source file with one type which has javadoc tags
	 * is scanned correctly. Scans the file <code>TestClass1</code>
	 */
	public void testSingleTypeDefaultPackage() {	
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass1.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1"));
		assertNotNull("the description for TestClass1 should exist", description);
		assertTrue("There should be no instantiate on TestClass1", description.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE);
		assertTrue("TestClass1 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one type which has javadoc tags and contains 
	 * one inner static type with tags is scanned correctly. 
	 * Scans the file <code>TestClass2</code> 
	 */
	public void testSingleInnerStaticTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass2.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2"));
		assertNotNull("the description for TestClass2 should exist", description);
		assertTrue("There should be no subclass on TestClass2", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass2 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2$InnerTestClass2"));
		assertNotNull("the description for TestClass2$InnerTestClass2 should exist", description);
		assertTrue("There should be no subclass or instantiate on TestClass2$InnerTestClass2", description.getRestrictions() == (RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE));
		assertTrue("TestClass2$InnerTestClass2 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one type which has javadoc tags and contains 
	 * one inner static type with tags is scanned correctly. 
	 * Scans the file <code>TestClass3</code> 
	 */
	public void testSingleInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass3.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3"));
		assertNotNull("the description for TestClass3 should exist", description);
		assertTrue("There should be no restrictions on TestClass3", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass3 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3"));
		assertNotNull("the description for TestClass3$InnerTestClass3 should exist", description);
		assertTrue("There should be no subclass on TestClass3$InnerTestClass3", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass3$InnerTestClass3 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one type which has javadoc tags and contains 
	 * more than one nested inner static type with tags is scanned correctly. 
	 * Scans the file <code>TestClass4</code> 
	 */
	public void testMultiNestedInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass4.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4"));
		assertNotNull("the description for TestClass4 should exist", description);
		assertTrue("There should be no restrictions on TestClass4", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass4 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4"));
		assertNotNull("the description for TestClass4$InnerTestClass4 should exist", description);
		assertTrue("There should be no restrictions on TestClass4$InnerTestClass4", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass4$InnerTestClass4 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4$Inner2TestClass4"));
		assertNotNull("the description for TestClass4$InnerTestClass4$Inner2TestClass4 should exist", description);
		assertTrue("There should be no restrictions on TestClass4$InnerTestClass4$Inner2TestClass4", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass4$InnerTestClass4$Inner2TestClass4 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4"));
		assertNotNull("the description for TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4 should exist", description);
		assertTrue("There should be no subclass, no instantiate, no reference on TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4", description.getRestrictions() == (RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE));
		assertTrue("TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with more than one type which has javadoc tags
	 * Scans the file <code>TestClass5</code> 
	 */
	public void testMultiTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass5.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5"));
		assertNotNull("the description for TestClass5 should exist", description);
		assertTrue("There should be no restrictions on TestClass5", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass5 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5a"));
		assertNotNull("the description for TestClass5a should exist", description);
		assertTrue("There should be no subclass on TestClass5a", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass5a should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5b"));
		assertNotNull("the description for TestClass5b should exist", description);
		assertTrue("There should be no reference on TestClass5b", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass5b should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one type which has javadoc tags and contains 
	 * more than one inner type with tags is scanned correctly. 
	 * Scans the file <code>TestClass6</code> 
	 */
	public void testMultiInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass6.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6"));
		assertNotNull("the description for TestClass6 should exist", description);
		assertTrue("There should be no restrictions on TestClass6", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass6 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6a"));
		assertNotNull("the description for TestClass6$InnerTestClass6a should exist", description);
		assertTrue("There should be no subclass on TestClass6$InnerTestClass6a", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass6$InnerTestClass6a should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6b"));
		assertNotNull("the description for TestClass6$InnerTestClass6b should exist", description);
		assertTrue("There should be no instantiate on TestClass6$InnerTestClass6b", description.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE);
		assertTrue("TestClass6$InnerTestClass6b should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6c"));
		assertNotNull("the description for TestClass6$InnerTestClass6c should exist", description);
		assertTrue("There should be no restrictions on TestClass6$InnerTestClass6c", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass6$InnerTestClass6c should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with more than one type which has javadoc tags and contains 
	 * more than one inner type with tags is scanned correctly. 
	 * Scans the file <code>TestClass7</code> 
	 */
	public void testMultiTypeMultiInnerTypeDefatulPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass7.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7"));
		assertNotNull("the description for TestClass7 should exist", description);
		assertTrue("There should be no restrictions on TestClass7", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7$InnerTestClass7"));
		assertNotNull("the description for TestClass7$InnerTestClass7 should exist", description);
		assertTrue("There should be no restrictions on TestClass7$InnerTestClass7", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass7$InnerTestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7$InnerTestClass7$Inner2TestClass7"));
		assertNotNull("the description for TestClass7$InnerTestClass7$Inner2TestClass7 should exist", description);
		assertTrue("There should be no subclass on TestClass7$InnerTestClass7$Inner2TestClass7", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass7$InnerTestClass7$Inner2TestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7a"));
		assertNotNull("the description for TestClass7a should exist", description);
		assertTrue("There should be no restrictions on TestClass7a", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestClass7a should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7a$InnerTestClass7a"));
		assertNotNull("the description for TestClass7a$InnerTestClass7a should exist", description);
		assertTrue("There should be no reference on TestClass7a$InnerTestClass7a", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass7a$InnerTestClass7a should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7b"));
		assertNotNull("the description for TestClass7b should exist", description);
		assertTrue("There should be no reference on TestClass7b", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass7b should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7b$InnerTestClass7b"));
		assertNotNull("the description for TestClass7b$InnerTestClass7b should exist", description);
		assertTrue("There should be no extend on TestClass7b$InnerTestClass7b", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		assertTrue("TestClass7b$InnerTestClass7b should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one interface declaration is scanned correctly. 
	 * Scans the file <code>TestInterface1</code> 
	 */
	public void testInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface1.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface1"));
		assertNotNull("the description for TestInterface1 should exist", description);
		assertTrue("There should be no implement on TestInterface1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT);
		assertTrue("TestInterface1 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with more than one interface declaration is scanned correctly. 
	 * Scans the file <code>TestInterface2</code> 
	 */
	public void testMultiInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface2.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2"));
		assertNotNull("the description for TestInterface2 should exist", description);
		assertTrue("There should be no restrictions on TestInterface2", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestInterface2 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2a"));
		assertNotNull("the description for TestInterface2a should exist", description);
		assertTrue("There should be no implement on TestInterface2a", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT);
		assertTrue("TestInterface2a should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2b"));
		assertNotNull("the description for TestInterface2b should exist", description);
		assertTrue("There should be no restrictions on TestInterface2b", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestInterface2b should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one interface declaration and a single nested interface is scanned correctly. 
	 * Scans the file <code>TestInterface3</code> 
	 */
	public void testSingleInnerInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface3.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface3"));
		assertNotNull("the description for TestInterface3 should exist", description);
		assertTrue("There should be no restrictions on TestInterface3", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestInterface3 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface3$Inner1"));
		assertNotNull("the description for TestInterface3$Inner1 should exist", description);
		assertTrue("There should be no implement on TestInterface3$Inner1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT);
		assertTrue("TestInterface3$Inner1 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that a source file with one interface declaration and multi nested interfaces are scanned correctly. 
	 * Scans the file <code>TestInterface4</code> 
	 */
	public void testMultiInnerInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface4.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4"));
		assertNotNull("the description for TestInterface4 should exist", description);
		assertTrue("There should be no restrictions on TestInterface4", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestInterface3 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4$Inner1"));
		assertNotNull("the description for TestInterface4$Inner1 should exist", description);
		assertTrue("There should be no implement on TestInterface4$Inner1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT);
		assertTrue("TestInterface3$Inner1 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4$Inner2"));
		assertNotNull("the description for TestInterface4$Inner2 should exist", description);
		assertTrue("There should be no restrictions on TestInterface4$Inner2", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		assertTrue("TestInterface3$Inner2 should have API visibility", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in a base public class.
	 * Scans the file <code>TestField1</code>
	 */
	public void testFieldBaseClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField1.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField1", "field"));
		assertNotNull("the description for field 'field' in TestField1 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that the source tags are added/collected properly for fields that have no restriction tags, but the parent class does.
	 * Scans the file <code>TestField7</code>
	 */
	public void testFieldBaseClassInheritedNotSupported() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField7.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField7", "field1"));
		assertNotNull("the description for field 'field1' in TestField7 should exist", description);
		assertEquals("there shouldbe API visibility on field 'field1'", VisibilityModifiers.API, description.getVisibility());
		assertEquals("There should be no restrictions on field 'field1'", RestrictionModifiers.NO_RESTRICTIONS, description.getRestrictions());
	}
	
	/**
	 * Tests that the source tags are added/collected properly for fields that have no restriction tags, but the parent class does.
	 * Scans the file <code>TestField8</code>
	 */
	public void testFieldBaseClassInheritedSupported() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField8.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField8", "field1"));
		assertNotNull("the description for field 'field1' in TestField8 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field1'", description.getVisibility() == VisibilityModifiers.API);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in an inner class.
	 * Scans the file <code>TestField2</code>
	 */
	public void testFieldInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField2.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField2$Inner", "field"));
		assertNotNull("the description for field 'field' in TestField2$Inner should exist", description);
		assertTrue("there shouldbe API visibility on field 'field'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField2$Inner", "number"));
		assertNotNull("the description for field 'number' in TestField2$Inner should exist", description);
		assertTrue("there shouldbe API visibility on field 'number'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'number'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in a static inner class.
	 * Scans the file <code>TestField3</code>
	 */
	public void testFieldStaticInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField3.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField3$Inner", "field"));
		assertNotNull("the description for field 'field' in TestField3$Inner should exist", description);
		assertTrue("there shouldbe API visibility on field 'field'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField3$Inner", "number"));
		assertNotNull("the description for field 'number' in TestField3$Inner should exist", description);
		assertTrue("there shouldbe API visibility on field 'number'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'number'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in multiple inner classes.
	 * Scans the file <code>TestField4</code>
	 */
	public void testFieldMultiInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField4.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1", "field"));
		assertNotNull("the description for field 'field' in TestField4$Inner1 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field' in TestField4$Inner1", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field' in TestField4$Inner1", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1$Inner3", "field"));
		assertNotNull("the description for field 'field' in TestField4$Inner1$Inner3 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field' in TestField4$Inner1$Inner3", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field' in TestField4$Inner1$Inner3", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1$Inner3$Inner", "number"));
		assertNotNull("the description for field 'number' in TestField4$Inner1$Inner3$Inner should exist", description);
		assertTrue("there shouldbe API visibility on field 'number' in TestField4$Inner1$Inner3$Inner", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'number' in TestField4$Inner1$Inner3$Inner", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner2", "field"));
		assertNotNull("the description for field 'field' in TestField4$Inner2 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field' in TestField4$Inner2", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field' in TestField4$Inner2", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in an outer class.
	 * Scans the file <code>TestField5</code>
	 */
	public void testFieldOuterClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField5.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField5Outer", "field"));
		assertNotNull("the description for field 'field' in a.b.c.TestField5 should exist", description);
		assertTrue("there shouldbe API visibility on field 'field' in a.b.c.TestField5Outer", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'field' in a.b.c.TestField5Outer", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for fields in an anonymous class.
	 * Scans the file <code>TestField6</code>
	 */
	public void testFieldAnonymousClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField6.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField6", "number"));
		assertNotNull("the description for field 'number' in a.b.c.TestField6 should exist", description);
		assertTrue("there shouldbe API visibility on field 'number' in a.b.c.TestField6", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no reference on field 'number' in a.b.c.TestField6", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class.
	 * Scans the file <code>TestMethod1</code>
	 */
	public void testMethodBaseClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod1.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "three", "()V"));
		assertTrue("There should exist a description for method 'void three()'", description != null);
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with a single Object parameter.
	 * Scans the file <code>TestMethod7</code>
	 */
	public void testMethodSingleParam() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod7.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod7", "one", "(QString;)V"));
		assertTrue("There should exist a description for method 'void one(String)'", description != null);
		assertTrue("There should be API visibility for method 'void one(String)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(String)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with a single primitive parameter.
	 * Scans the file <code>TestMethod8</code>
	 */
	public void testMethodSinglePrimitiveParam() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod8.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod8", "one", "(I)V"));
		assertTrue("There should exist a description for method 'void one(int)'", description != null);
		assertTrue("There should be API visibility for method 'void one(int)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void one(int)'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with primitive parameters.
	 * Scans the file <code>TestMethod9</code>
	 */
	public void testMethodPrimitiveParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod9.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod9", "one", "(IDF)V"));
		assertTrue("There should exist a description for method 'void one(int, double , float)'", description != null);
		assertTrue("There should be API visibility for method 'void one(int, double , float)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(int, double , float)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod9", "two", "(DF)V"));
		assertTrue("There should exist a description for method 'void two(double, float)'", description != null);
		assertTrue("There should be API visibility for method 'void two(double, float)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(double, float)'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with Object parameters.
	 * Scans the file <code>TestMethod10</code>
	 */
	public void testMethodObjectParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod10.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(QString;QInteger;)V"));
		assertTrue("There should exist a description for method 'void one(String, Integer)'", description != null);
		assertTrue("There should be API visibility for method 'void one(String, Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(String, Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "two", "(QList;QRunnable;)V"));
		assertTrue("There should exist a description for method 'void two(List, Runnable)'", description != null);
		assertTrue("There should be API visibility for method 'void two(List, Runnable)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(List, Runnable)'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with primitive array parameters.
	 * Scans the file <code>TestMethod11</code>
	 */
	public void testMethodPrimitiveArrayParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod11.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod11", "one", "([I[[C)V"));
		assertTrue("There should exist a description for method 'void one(int[], char[][])'", description != null);
		assertTrue("There should be API visibility for method 'void one(int[], char[][])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(int[], char[][])'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod11", "two", "([[F[D)V"));
		assertTrue("There should exist a description for method 'void two(float[][], double[])'", description != null);
		assertTrue("There should be API visibility for method 'void two(float[][], double[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(float[][], double[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with Object array parameters.
	 * Scans the file <code>TestMethod12</code>
	 */
	public void testMethodObjectArrayParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod12.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod12", "one", "([QString;[[QDouble;)V"));
		assertTrue("There should exist a description for method 'void one(String[], Double[][])'", description != null);
		assertTrue("There should be API visibility for method 'void one(String[], Double[][])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(String[], Double[][])'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod12", "two", "([[QList;[QRunnable;)V"));
		assertTrue("There should exist a description for method 'void two(List[][], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'void two(List[][], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(List[][], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with a mix of  parameters.
	 * Scans the file <code>TestMethod13</code>
	 */
	public void testMethodMixedParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod13.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod13", "one", "(I[[QDouble;[CQInteger;)V"));
		assertTrue("There should exist a description for method 'void one(int, Double[][], char[], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'void one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod13", "two", "([[QList;DC[I[QRunnable;)V"));
		assertTrue("There should exist a description for method 'void two(List[][], double, char, int[], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'void two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with an Object return type.
	 * Scans the file <code>TestMethod14</code>
	 */
	public void testMethodObjectReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod14.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod14", "one", "(I[[QDouble;[CQInteger;)QString;"));
		assertTrue("There should exist a description for method 'String one(int, Double[][], char[], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'String one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'String one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod14", "two", "([[QList;DC[I[QRunnable;)QDouble;"));
		assertTrue("There should exist a description for method 'Double two(List[][], double, char, int[], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'Double two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'Double two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with a primitive return type.
	 * Scans the file <code>TestMethod15</code>
	 */
	public void testMethodPrimitiveReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod15.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod15", "one", "(I[[QDouble;[CQInteger;)C"));
		assertTrue("There should exist a description for method 'char one(int, Double[][], char[], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'char one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'char one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod15", "two", "([[QList;DC[I[QRunnable;)D"));
		assertTrue("There should exist a description for method 'double two(List[][], double, char, int[], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'double two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'double two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with a primitive array return type.
	 * Scans the file <code>TestMethod17</code>
	 */
	public void testMethodPrimitiveArrayReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod17.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod17", "one", "(I[[QDouble;[CQInteger;)[[C"));
		assertTrue("There should exist a description for method 'char[][] one(int, Double[][], char[], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'char[][] one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'char[][] one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod17", "two", "([[QList;DC[I[QRunnable;)[D"));
		assertTrue("There should exist a description for method 'double[] two(List[][], double, char, int[], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'double[] two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'double[] two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a base public class
	 * with an Object array return type.
	 * Scans the file <code>TestMethod16</code>
	 */
	public void testMethodObjectArrayReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod16.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod16", "one", "(I[[QDouble;[CQInteger;)[[QString;"));
		assertTrue("There should exist a description for method 'String[][] one(int, Double[][], char[], Integer)'", description != null);
		assertTrue("There should be API visibility for method 'String[][] one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'String[][] one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod16", "two", "([[QList;DC[I[QRunnable;)[QDouble;"));
		assertTrue("There should exist a description for method 'Double[] two(List[][], double, char, int[], Runnable[])'", description != null);
		assertTrue("There should be API visibility for method 'Double[] two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'Double[] two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in an inner class.
	 * Scans the file <code>TestMethod2</code>
	 */
	public void testMethodInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod2.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod2$Inner", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod2$Inner", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in a static inner class.
	 * Scans the file <code>TestMethod3</code>
	 */
	public void testMethodStaticInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod3.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "three", "()V"));
		assertTrue("There should exist a description for method 'void three()'", description != null);
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in multiple inner classes.
	 * Scans the file <code>TestMethod4</code>
	 */
	public void testMethodMultiInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod4.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "three", "()V"));
		assertTrue("There should exist a description for method 'void three()'", description != null);
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "three", "()V"));
		assertTrue("There should exist a description for method 'void three()'", description != null);
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2$Inner4", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2$Inner4", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in an outer class.
	 * Scans the file <code>TestMethod5</code>
	 */
	public void testMethodOuterClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod5.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_EXTEND);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "three", "()V"));
		assertTrue("There should exist a description for method 'void three()'", description != null);
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests that source tags are added/collected properly for methods in an anonymous class.
	 * Scans the file <code>TestMethod6</code>
	 */
	public void testMethodAnonymousClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod6.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod6", "run", "()V"));
		assertTrue("There should exist a description for method 'void run()'", description != null);
		assertTrue("There should be API visibility for method 'void run()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void run()'", description.getRestrictions() == (RestrictionModifiers.NO_REFERENCE | RestrictionModifiers.NO_EXTEND));
	}
	
	/**
	 * Tests that a method properly inherits restrictions. Restrictions are not inherited.
	 * Scans the file <code>TestMethod18</code>
	 */
	public void testMethodInheritValidRestriction() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod18.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod18", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod18Outer", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be no restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests that a method properly inherits restrictions from 
	 * source tags are added/collected properly for the enclosing type of the methods.
	 * In this case the parent tags cannot be inherited, expected result is 'no restriction'
	 * Scans the file <code>TestMethod19</code>
	 */
	public void testMethodInheritInvalidRestrictionClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod19.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod19", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod19Outer", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests that a method properly inherits restrictions from 
	 * source tags are added/collected properly for the enclosing type of the methods.
	 * In this case the parent tags cannot be inherited, expected result is 'no restriction'
	 * Scans the file <code>TestField20</code>
	 */
	public void testMethodInheritInvalidRestrictionInterface() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod20.java", manifest);
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod20", "one", "()V"));
		assertTrue("There should exist a description for method 'void one()'", description != null);
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod20Outer", "two", "()V"));
		assertTrue("There should exist a description for method 'void two()'", description != null);
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API);
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS);
	}
	
}
