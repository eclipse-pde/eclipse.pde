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

import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.ArchiveApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.junit.Test;

import com.ibm.icu.text.MessageFormat;

/**
 * Class tests that the tag scanner for the API tools correctly scans source for
 * API tags
 *
 * @since 1.0.0
 */
public class TagScannerTests {

	private static final IPath SRC_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-source"); //$NON-NLS-1$
	private static final IPath BIN_LOC = TestSuiteHelper.getPluginDirectoryPath().append("test-classes"); //$NON-NLS-1$

	/**
	 * Creates a new empty API component description, not owned by any
	 * component.
	 *
	 * @return
	 */
	protected IApiDescription newDescription() {
		return new ApiDescription(null);
	}

	/**
	 * Creates a new {@link ArchiveApiTypeContainer} on the given path
	 *
	 * @param path
	 * @return
	 */
	protected IApiTypeContainer newArchiveClassFileContainer(IPath path) {
		return new ArchiveApiTypeContainer(null, path.toOSString());
	}

	/**
	 * Returns a new compilation unit on the standard test source path with the
	 * specified name appended
	 *
	 * @param name
	 * @return a new compilation unit
	 */
	private CompilationUnit getCompilationUnit(String name) {
		Path path = (Path) SRC_LOC.append(name);
		return new CompilationUnit(path.toOSString(), IApiCoreConstants.UTF_8);
	}

	/**
	 * Performs the scan to populate the manifest and traps exceptions thrown
	 * from the scanner
	 *
	 * @param name
	 * @param manifest
	 * @param cfc
	 */
	protected void doScan(String name, IApiDescription manifest, IApiTypeContainer cfc) {
		try {
			TagScanner.newScanner().scan(getCompilationUnit(name), manifest, cfc, null, null);
		} catch (CoreException e) {
			fail(MessageFormat.format("Error scanning: {0}", new Object[] { name })); //$NON-NLS-1$
		}
	}

	/**
	 * Performs the scan to populate the manifest and traps exceptions thrown
	 * from the scanner
	 *
	 * @param name
	 * @param manifest
	 */
	protected void doScan(String name, IApiDescription manifest) {
		try {
			TagScanner.newScanner().scan(getCompilationUnit(name), manifest, null, null, null);
		} catch (CoreException e) {
			fail(MessageFormat.format("Error scanning: {0}", new Object[] { name })); //$NON-NLS-1$
		}
	}

	/**
	 * Performs the scan to populate the manifest and traps exceptions thrown
	 * from the scanner
	 *
	 * @param name
	 * @param manifest
	 * @param cfc
	 */
	protected void doScan(String name, IApiDescription manifest, Map<String, String> options) {
		try {
			TagScanner.newScanner().scan(getCompilationUnit(name), manifest, null, options, null);
		} catch (CoreException e) {
			fail(MessageFormat.format("Error scanning: {0}", new Object[] { name })); //$NON-NLS-1$
		}
	}

	/**
	 * Tests that methods with non-simple type parameters have their signatures
	 * resolved if a backing class file is provided (via an
	 * {@link IApiTypeContainer})
	 */
	@Test
	public void testBug212276() {
		DirectoryApiTypeContainer container = new DirectoryApiTypeContainer(null, BIN_LOC.toOSString());
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod10.java", manifest, container); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(Ljava/lang/String;Ljava/lang/Integer;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(String, Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(String, Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(String, Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "two", "(Ljava/util/List;Ljava/lang/Runnable;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(List, Runnable)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(List, Runnable)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(List, Runnable)'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(Ljava/lang/Object;Ljava/lang/Integer;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(Object, Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(Object, Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction and no override restriction on method 'void one(Object, Integer)'", description.getRestrictions() == (RestrictionModifiers.NO_OVERRIDE | RestrictionModifiers.NO_REFERENCE)); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "([[Ljava/lang/String;Ljava/lang/Integer;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(String[][], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(String[][], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void one(String[][], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that when given a class file container and the class file is not
	 * found and is required to resolve a method signature, an exception is
	 * thrown.
	 */
	@Test
	public void testMissingClassfile() {
		IApiTypeContainer container = new IApiTypeContainer() {
			@Override
			public String[] getPackageNames() throws CoreException {
				return new String[] { "there.are.none" }; //$NON-NLS-1$
			}

			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
				return null;
			}

			@Override
			public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
				return null;
			}

			@Override
			public void close() throws CoreException {
			}

			@Override
			public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
			}

			@Override
			public IApiElement getAncestor(int ancestorType) {
				return null;
			}

			@Override
			public String getName() {
				return "test container"; //$NON-NLS-1$
			}

			@Override
			public IApiElement getParent() {
				return null;
			}

			@Override
			public int getType() {
				return IApiElement.API_TYPE_CONTAINER;
			}

			@Override
			public IApiComponent getApiComponent() {
				return null;
			}

			@Override
			public int getContainerType() {
				return 0;
			}
		};
		IApiDescription manifest = newDescription();
		try {
			TagScanner.newScanner().scan(getCompilationUnit("a/b/c/TestMethod10.java"), manifest, container, null, null); //$NON-NLS-1$
		} catch (CoreException e) {
			fail("Should not be a core exception for missing class file"); //$NON-NLS-1$
		}
		return;
	}

	/**
	 * Tests that a source file with one type which has javadoc tags is scanned
	 * correctly. Scans the file <code>TestClass1</code>
	 */
	@Test
	public void testSingleTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass1")); //$NON-NLS-1$
		assertNotNull("the description for TestClass1 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no instantiate on TestClass1", description.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		assertTrue("TestClass1 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one type which has javadoc tags and
	 * contains one inner static type with tags is scanned correctly. Scans the
	 * file <code>TestClass2</code>
	 */
	@Test
	public void testSingleInnerStaticTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2")); //$NON-NLS-1$
		assertNotNull("the description for TestClass2 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no subclass on TestClass2", description.getRestrictions() == RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		assertTrue("TestClass2 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass2$InnerTestClass2")); //$NON-NLS-1$
		assertNotNull("the description for TestClass2$InnerTestClass2 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no subclass or instantiate on TestClass2$InnerTestClass2", description.getRestrictions() == (RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE)); //$NON-NLS-1$
		assertTrue("TestClass2$InnerTestClass2 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one type which has javadoc tags and
	 * contains one inner static type with tags is scanned correctly. Scans the
	 * file <code>TestClass3</code>
	 */
	@Test
	public void testSingleInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3")); //$NON-NLS-1$
		assertNull("the description for TestClass3 should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass3$InnerTestClass3")); //$NON-NLS-1$
		assertNull("the description for TestClass3$InnerTestClass3 should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one type which has javadoc tags and
	 * contains more than one nested inner static type with tags is scanned
	 * correctly. Scans the file <code>TestClass4</code>
	 */
	@Test
	public void testMultiNestedInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass4.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4")); //$NON-NLS-1$
		assertNull("the description for TestClass4 should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4")); //$NON-NLS-1$
		assertNull("the description for TestClass4$InnerTestClass4 should not exist", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4$Inner2TestClass4")); //$NON-NLS-1$
		assertNull("the description for TestClass4$InnerTestClass4$Inner2TestClass4 should not exist", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4")); //$NON-NLS-1$
		assertNull("the description for TestClass4$InnerTestClass4$Inner2TestClass4$Inner3TestClass4 should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with more than one type which has javadoc tags
	 * Scans the file <code>TestClass5</code>
	 */
	@Test
	public void testMultiTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass5.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5")); //$NON-NLS-1$
		assertNull("the description for TestClass5 should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5a")); //$NON-NLS-1$
		assertNull("the description for TestClass5a should not exist", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass5b")); //$NON-NLS-1$
		assertNull("the description for TestClass5b should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one type which has javadoc tags and
	 * contains more than one inner type with tags is scanned correctly. Scans
	 * the file <code>TestClass6</code>
	 */
	@Test
	public void testMultiInnerTypeDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass6.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6")); //$NON-NLS-1$
		assertNotNull("the description for TestClass6 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass6", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass6 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6a")); //$NON-NLS-1$
		assertNotNull("the description for TestClass6$InnerTestClass6a should exist", description); //$NON-NLS-1$
		assertTrue("There should be no subclass on TestClass6$InnerTestClass6a", description.getRestrictions() == RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		assertTrue("TestClass6$InnerTestClass6a should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6b")); //$NON-NLS-1$
		assertNotNull("the description for TestClass6$InnerTestClass6b should exist", description); //$NON-NLS-1$
		assertTrue("There should be no instantiate on TestClass6$InnerTestClass6b", description.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		assertTrue("TestClass6$InnerTestClass6b should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass6$InnerTestClass6c")); //$NON-NLS-1$
		assertNotNull("the description for TestClass6$InnerTestClass6c should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass6$InnerTestClass6c", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass6$InnerTestClass6c should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one type which has annotations and contains
	 * more than one inner type with annotations is scanned correctly. Scans the
	 * file <code>TestClass8</code>
	 *
	 * @since 1.0.400
	 */
	@Test
	public void testMultiInnerTypeAnnotations() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass8.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass8")); //$NON-NLS-1$
		assertNotNull("the description for TestClass8 should exist", description); //$NON-NLS-1$
		assertTrue("There should be noreference restrictions on TestClass8", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		assertTrue("TestClass8 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass8$InnerTestClass8a")); //$NON-NLS-1$
		assertNotNull("the description for TestClass8$InnerTestClass8a should exist", description); //$NON-NLS-1$
		assertTrue("There should be no subclass on TestClass8$InnerTestClass8a", description.getRestrictions() == RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		assertTrue("TestClass8$InnerTestClass6a should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass8$InnerTestClass8b")); //$NON-NLS-1$
		assertNotNull("the description for TestClass8$InnerTestClass8b should exist", description); //$NON-NLS-1$
		assertTrue("There should be no instantiate on TestClass8$InnerTestClass8b", description.getRestrictions() == RestrictionModifiers.NO_INSTANTIATE); //$NON-NLS-1$
		assertTrue("TestClass6$InnerTestClass8b should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass8$InnerTestClass8c")); //$NON-NLS-1$
		assertNotNull("the description for TestClass8$InnerTestClass8c should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass8$InnerTestClass8c", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass8$InnerTestClass8c should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with more than one type which has javadoc tags
	 * and contains more than one inner type with tags is scanned correctly.
	 * Scans the file <code>TestClass7</code>
	 */
	@Test
	public void testMultiTypeMultiInnerTypeDefatulPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestClass7.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7$InnerTestClass7")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7$InnerTestClass7 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7$InnerTestClass7", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7$InnerTestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7$InnerTestClass7$Inner2TestClass7")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7$InnerTestClass7$Inner2TestClass7 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no subclass on TestClass7$InnerTestClass7$Inner2TestClass7", description.getRestrictions() == RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		assertTrue("TestClass7$InnerTestClass7$Inner2TestClass7 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7a")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7a should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7a", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7a should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7a$InnerTestClass7a")); //$NON-NLS-1$
		// Bug 402393 - The description returned is for the parent element
		// and is expected because the parent has a restricted sub-type
		assertNotNull("the description for TestClass7a$InnerTestClass7a should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7a$InnerTestClass7a", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7a$InnerTestClass7a should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7b")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7b should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7b", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7b should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestClass7b$InnerTestClass7b")); //$NON-NLS-1$
		assertNotNull("the description for TestClass7b$InnerTestClass7b should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestClass7b$InnerTestClass7b", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestClass7b$InnerTestClass7b should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one interface declaration is scanned
	 * correctly. Scans the file <code>TestInterface1</code>
	 */
	@Test
	public void testInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface1")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface1 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no implement on TestInterface1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		assertTrue("TestInterface1 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with more than one interface declaration is
	 * scanned correctly. Scans the file <code>TestInterface2</code>
	 */
	@Test
	public void testMultiInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2")); //$NON-NLS-1$
		assertNull("the description for TestInterface2 should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2a")); //$NON-NLS-1$
		assertNull("the description for TestInterface2a should not exist", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface2b")); //$NON-NLS-1$
		assertNull("the description for TestInterface2b should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one interface declaration and a single
	 * nested interface is scanned correctly. Scans the file
	 * <code>TestInterface3</code>
	 */
	@Test
	public void testSingleInnerInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface3")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface3 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestInterface3", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestInterface3 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface3$Inner1")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface3$Inner1 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no implement on TestInterface3$Inner1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		assertTrue("TestInterface3$Inner1 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one interface declaration and multi nested
	 * interfaces are scanned correctly. Scans the file
	 * <code>TestInterface4</code>
	 */
	@Test
	public void testMultiInnerInterfaceDefaultPackage() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface4.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface4 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no restrictions on TestInterface4", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		assertTrue("TestInterface3 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4$Inner1")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface4$Inner1 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no implement on TestInterface4$Inner1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		assertTrue("TestInterface3$Inner1 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface4$Inner2")); //$NON-NLS-1$

		// Bug 402393 - The description returned is for the parent element
		// and is expected because the root type has a restricted sub-type
		assertNotNull("the description for TestInterface4$Inner2 should exist", description); //$NON-NLS-1$
		assertTrue("The root type should be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that a source file with one interface declaration and multi nested
	 * interfaces are scanned correctly. Scans the file
	 * <code>TestInterface5</code>
	 *
	 * @since 1.0.400
	 */
	@Test
	public void testMultiInnerInterfaceAnnotations() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestInterface5.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface5")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface5 should exist", description); //$NON-NLS-1$
		assertTrue("There should be noextend restrictions on TestInterface5", description.getRestrictions() == RestrictionModifiers.NO_EXTEND); //$NON-NLS-1$
		assertTrue("TestInterface5 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface5$Inner1")); //$NON-NLS-1$
		assertNotNull("the description for TestInterface5$Inner1 should exist", description); //$NON-NLS-1$
		assertTrue("There should be no implement on TestInterface5$Inner1", description.getRestrictions() == RestrictionModifiers.NO_IMPLEMENT); //$NON-NLS-1$
		assertTrue("TestInterface5$Inner1 should have API visibility", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.TestInterface5$Inner2")); //$NON-NLS-1$

		// Bug 402393 - The description returned is for the parent element
		// and is expected because the root type has a restricted sub-type
		assertNotNull("the description for TestInterface5$Inner2 should exist", description); //$NON-NLS-1$
		assertTrue("The root type should be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in a base
	 * public class. Scans the file <code>TestField1</code>
	 */
	@Test
	public void testFieldBaseClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField1", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field' in TestField1 should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'field'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'field'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that the source tags are added/collected properly for fields that
	 * have no restriction tags, but the parent class does. Scans the file
	 * <code>TestField7</code>
	 *
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=434444
	 */
	@Test
	public void testFieldBaseClassInheritedNotSupported() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField7.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField7", "field1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field1' in TestField7 should exist", description); //$NON-NLS-1$
		assertEquals("there shouldbe API visibility on field 'field1'", VisibilityModifiers.API, description.getVisibility()); //$NON-NLS-1$
		assertTrue("there should be noreference restrictions on field 'field1'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that the annotations are added/collected properly for fields. Scans
	 * the file <code>TestField10</code>
	 *
	 * @since 1.0.400
	 */
	@Test
	public void testFieldNoReference() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField10.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField10", "field1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field1' in TestField10 should exist", description); //$NON-NLS-1$
		assertEquals("there shouldbe API visibility on field 'field1'", VisibilityModifiers.API, description.getVisibility()); //$NON-NLS-1$
		assertEquals("There should be noreference restrictions on field 'field1'", RestrictionModifiers.NO_REFERENCE, description.getRestrictions()); //$NON-NLS-1$
	}

	/**
	 * Tests that the source tags are added/collected properly for fields that
	 * have no restriction tags, but the parent class does. Scans the file
	 * <code>TestField8</code>
	 *
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=434444
	 */
	@Test
	public void testFieldBaseClassInheritedNotSupported2() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField8.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField8", "field1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field1' in TestField8 should exist", description); //$NON-NLS-1$
		assertTrue("there should be API visibility on field 'field1'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("there should be noreference restrictions on field 'field1'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that the annotations are added/collected properly for fields that
	 * have restrictions, but that should not assume the parent restrictions<br>
	 * <br>
	 * Scans the file <code>TestField11</code>
	 *
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=434444
	 */
	@Test
	public void testFieldBaseClassInheritedNotSupported3() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField11.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField11", "field1")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field1' in TestField11 should exist", description); //$NON-NLS-1$
		assertTrue("there should be API visibility on field 'field1'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("there should be noreference restrictions on field 'field1'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in an
	 * inner class. Scans the file <code>TestField2</code>
	 */
	@Test
	public void testFieldInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField2$Inner", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("the description for field 'field' in TestField2$Inner should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField2$Inner", "number")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("the description for field 'number' in TestField2$Inner should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in a
	 * static inner class. Scans the file <code>TestField3</code>
	 */
	@Test
	public void testFieldStaticInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField3$Inner", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field' in TestField3$Inner should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'field'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'field'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField3$Inner", "number")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'number' in TestField3$Inner should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'number'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'number'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in
	 * multiple inner classes. Scans the file <code>TestField4</code>
	 */
	@Test
	public void testFieldMultiInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField4.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field' in TestField4$Inner1 should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'field' in TestField4$Inner1", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'field' in TestField4$Inner1", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1$Inner3", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'field' in TestField4$Inner1$Inner3 should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'field' in TestField4$Inner1$Inner3", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'field' in TestField4$Inner1$Inner3", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner1$Inner3$Inner", "number")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'number' in TestField4$Inner1$Inner3$Inner should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'number' in TestField4$Inner1$Inner3$Inner", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'number' in TestField4$Inner1$Inner3$Inner", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField4$Inner2", "field")); //$NON-NLS-1$//$NON-NLS-2$
		assertNotNull("the description for field 'field' in TestField4$Inner2 should exist", description); //$NON-NLS-1$
		assertTrue("there should be API visibility on field 'field' in TestField4$Inner2", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in an
	 * outer class. Scans the file <code>TestField5</code>
	 */
	@Test
	public void testFieldOuterClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField5.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField5Outer", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("the description for field 'field' in a.b.c.TestField5 should not exist", description); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for fields in an
	 * anonymous class. Scans the file <code>TestField6</code>
	 */
	@Test
	public void testFieldAnonymousClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestField6.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.TestField6", "number")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("the description for field 'number' in a.b.c.TestField6 should exist", description); //$NON-NLS-1$
		assertTrue("there shouldbe API visibility on field 'number' in a.b.c.TestField6", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no reference on field 'number' in a.b.c.TestField6", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class. Scans the file <code>TestMethod1</code>
	 */
	@Test
	public void testMethodBaseClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod1", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void three()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that annotations are added/collected properly for methods in a base
	 * public class. Scans the file <code>TestMethod22</code>
	 */
	@Test
	public void testMethodBaseClassAnnotations() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod22.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod22", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod22", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod22", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void three()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with a single Object parameter. Scans the file
	 * <code>TestMethod7</code>
	 */
	@Test
	public void testMethodSingleParam() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod7.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod7", "one", "(QString;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(String)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(String)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(String)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with a single primitive parameter. Scans the file
	 * <code>TestMethod8</code>
	 */
	@Test
	public void testMethodSinglePrimitiveParam() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod8.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod8", "one", "(I)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(int)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(int)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void one(int)'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with primitive parameters. Scans the file
	 * <code>TestMethod9</code>
	 */
	@Test
	public void testMethodPrimitiveParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod9.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod9", "one", "(IDF)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(int, double , float)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(int, double , float)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(int, double , float)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod9", "two", "(DF)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(double, float)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(double, float)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(double, float)'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$

	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with Object parameters. Scans the file
	 * <code>TestMethod10</code>
	 */
	@Test
	public void testMethodObjectParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod10.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "one", "(QString;QInteger;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(String, Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(String, Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(String, Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod10", "two", "(QList;QRunnable;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(List, Runnable)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(List, Runnable)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(List, Runnable)'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with primitive array parameters. Scans the file
	 * <code>TestMethod11</code>
	 */
	@Test
	public void testMethodPrimitiveArrayParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod11.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod11", "one", "([I[[C)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(int[], char[][])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(int[], char[][])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(int[], char[][])'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod11", "two", "([[F[D)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(float[][], double[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(float[][], double[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(float[][], double[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with Object array parameters. Scans the file
	 * <code>TestMethod12</code>
	 */
	@Test
	public void testMethodObjectArrayParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod12.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod12", "one", "([QString;[[QDouble;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(String[], Double[][])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(String[], Double[][])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(String[], Double[][])'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod12", "two", "([[QList;[QRunnable;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(List[][], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(List[][], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(List[][], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with a mix of parameters. Scans the file
	 * <code>TestMethod13</code>
	 */
	@Test
	public void testMethodMixedParams() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod13.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod13", "one", "(I[[QDouble;[CQInteger;)V")); //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one(int, Double[][], char[], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod13", "two", "([[QList;DC[I[QRunnable;)V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two(List[][], double, char, int[], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with an Object return type. Scans the file
	 * <code>TestMethod14</code>
	 */
	@Test
	public void testMethodObjectReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod14.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod14", "one", "(I[[QDouble;[CQInteger;)QString;")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'String one(int, Double[][], char[], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'String one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'String one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod14", "two", "([[QList;DC[I[QRunnable;)QDouble;")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'Double two(List[][], double, char, int[], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'Double two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'Double two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with a primitive return type. Scans the file
	 * <code>TestMethod15</code>
	 */
	@Test
	public void testMethodPrimitiveReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod15.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod15", "one", "(I[[QDouble;[CQInteger;)C")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'char one(int, Double[][], char[], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'char one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'char one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod15", "two", "([[QList;DC[I[QRunnable;)D")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'double two(List[][], double, char, int[], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'double two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'double two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with a primitive array return type. Scans the file
	 * <code>TestMethod17</code>
	 */
	@Test
	public void testMethodPrimitiveArrayReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod17.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod17", "one", "(I[[QDouble;[CQInteger;)[[C")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'char[][] one(int, Double[][], char[], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'char[][] one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'char[][] one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod17", "two", "([[QList;DC[I[QRunnable;)[D")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'double[] two(List[][], double, char, int[], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'double[] two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'double[] two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a base
	 * public class with an Object array return type. Scans the file
	 * <code>TestMethod16</code>
	 */
	@Test
	public void testMethodObjectArrayReturn() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod16.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod16", "one", "(I[[QDouble;[CQInteger;)[[QString;")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'String[][] one(int, Double[][], char[], Integer)'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'String[][] one(int, Double[][], char[], Integer)'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'String[][] one(int, Double[][], char[], Integer)'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod16", "two", "([[QList;DC[I[QRunnable;)[QDouble;")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'Double[] two(List[][], double, char, int[], Runnable[])'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'Double[] two(List[][], double, char, int[], Runnable[])'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'Double[] two(List[][], double, char, int[], Runnable[])'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in an
	 * inner class. Scans the file <code>TestMethod2</code>
	 */
	@Test
	public void testMethodInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod2$Inner", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("There should be no description for method 'void one()", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod2$Inner", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("There should not exist a description for method 'void two()'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in a
	 * static inner class. Scans the file <code>TestMethod3</code>
	 */
	@Test
	public void testMethodStaticInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod3$Inner", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void three()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in
	 * multiple inner classes. Scans the file <code>TestMethod4</code>
	 */
	@Test
	public void testMethodMultiInnerClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod4.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void three()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no override restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_OVERRIDE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner1$Inner3", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void three()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void three()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no extend restriction on method 'void three()'", description.getRestrictions() == RestrictionModifiers.NO_REFERENCE); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		// Bug 402393 - The description returned is for the parent element
		// and is expected because the parent element has a restricted
		// sub-element
		assertNotNull("There should not exist a description for method one() in class Inner2", description); //$NON-NLS-1$
		assertTrue("Inner2#one() must be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("There should exist a description for method 'void two()'", description); //$NON-NLS-1$
		assertTrue("Inner2#two() must be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2$Inner4", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("There should exist a description for method 'void one()'", description); //$NON-NLS-1$
		assertTrue("Inner2$Inner4#one() must be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod4$Inner2$Inner4", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("There should exist a description for method 'void two()'", description); //$NON-NLS-1$
		assertTrue("Inner2$Inner4#two() must be unrestricted", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in an
	 * outer class. Scans the file <code>TestMethod5</code>
	 */
	@Test
	public void testMethodOuterClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod5.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("The description for method 'void one()' should not exist", description); //$NON-NLS-1$

		// Prior to bug 402393 annotations were supported on package default
		// restricted classes
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("There should not exist a description for method 'void two()'", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod5Outer", "three", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("There should not exist a description for method 'void three()'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that source tags are added/collected properly for methods in an
	 * anonymous class. Scans the file <code>TestMethod6</code>
	 */
	@Test
	public void testMethodAnonymousClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod6.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod6", "run", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void run()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void run()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference and no override restriction on method 'void run()'", description.getRestrictions() == (RestrictionModifiers.NO_REFERENCE | RestrictionModifiers.NO_OVERRIDE)); //$NON-NLS-1$
	}

	/**
	 * Tests that a method properly inherits restrictions. Restrictions are not
	 * inherited. Scans the file <code>TestMethod18</code>
	 */
	@Test
	public void testMethodInheritValidRestriction() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod18.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod18", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod18Outer", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be no restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that a method properly inherits restrictions from source tags are
	 * added/collected properly for the enclosing type of the methods. In this
	 * case the parent tags cannot be inherited, expected result is 'no
	 * restriction' Scans the file <code>TestMethod19</code>
	 */
	@Test
	public void testMethodInheritInvalidRestrictionClass() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod19.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod19", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod19Outer", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that a method properly inherits restrictions from source tags are
	 * added/collected properly for the enclosing type of the methods. In this
	 * case the parent tags cannot be inherited, expected result is 'no
	 * restriction' Scans the file <code>TestField20</code>
	 */
	@Test
	public void testMethodInheritInvalidRestrictionInterface() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestMethod20.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod20", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void one()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void one()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no reference restriction on method 'void one()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestMethod20Outer", "two", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("There should exist a description for method 'void two()'", description != null); //$NON-NLS-1$
		assertTrue("There should be API visibility for method 'void two()'", description.getVisibility() == VisibilityModifiers.API); //$NON-NLS-1$
		assertTrue("There should be a no extend restriction on method 'void two()'", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests that a restriction on a @noreference constructor inside an enum
	 * class. https://bugs.eclipse.org/bugs/show_bug.cgi?id=253055
	 */
	@Test
	public void testEnumMethodWithNoReference() {
		IApiDescription manifest = newDescription();
		Map<String, String> options = JavaCore.getDefaultOptions();
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_5);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_5);
		doScan("a/b/c/TestMethod21.java", manifest, options); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testInterfaceWithBadTags() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanInterface.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.InvalidTagScanInterface")); //$NON-NLS-1$
		assertNotNull("there should be noreference restrictions for interface InvalidTagScanInterface", description); //$NON-NLS-1$
		assertTrue("The restrictions for InvalidTagScanInterface should be noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testClassWithBadTags1() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanClass1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.InvalidTagScanClass1")); //$NON-NLS-1$
		assertNotNull("there should be noreference annotations for class InvalidTagScanClass1", description); //$NON-NLS-1$
		assertTrue("The restrictions for InvalidTagScanClass1 should be noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testClassWithBadTags2() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanClass2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.InvalidTagScanClass2")); //$NON-NLS-1$
		assertNotNull("there should be noreference annotations for class InvalidTagScanClass2", description); //$NON-NLS-1$
		assertTrue("The restrictions for InvalidTagScanClass2 should be noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testClassWithBadTags3() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanClass3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.typeDescriptor("a.b.c.InvalidTagScanClass3")); //$NON-NLS-1$
		assertNotNull("there should be noreference annotations for class InvalidTagScanClass3", description); //$NON-NLS-1$
		assertTrue("The restrictions for InvalidTagScanClass3 should be noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testMethodWithBadTags1() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanMethod1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.InvalidTagScanMethod1", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("there should be no annotations for method 'public void one()'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testMethodWithBadTags2() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanMethod2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.InvalidTagScanMethod2", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("there should be no annotations for method 'public void one()'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testMethodWithBadTags3() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanMethod3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.InvalidTagScanMethod3", "one", "()V")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("there should be no annotations for method 'public void one()'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testFieldWithBadTags1() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanField1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.InvalidTagScanField1", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("there should be no annotations for field 'field'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testFieldWithBadTags2() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanField2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.InvalidTagScanField2", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("there should be no annotations for field 'field'", description); //$NON-NLS-1$
	}

	/**
	 * Tests that invalid Javadoc tags do not get leaked into the API
	 * description https://bugs.eclipse.org/bugs/show_bug.cgi?id=255222
	 */
	@Test
	public void testFieldWithBadTags3() {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/InvalidTagScanField3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.fieldDescriptor("a.b.c.InvalidTagScanField3", "field")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("there should be no annotations for field 'field'", description); //$NON-NLS-1$
	}

	/**
	 * Tests only default methods annotate API descriptions with @nooverride
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=427495
	 */
	@Test
	public void testJava8InterfaceMethod1() throws Exception {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestJava8DefaultMethod1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod1", "m2", "()I")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("there should be annotations for method 'm2'", description); //$NON-NLS-1$
		assertTrue("The annotations should include nooverride", (description.getRestrictions() & RestrictionModifiers.NO_OVERRIDE) > 0); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod1", "m1", "()I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("There should be API annotations for the non-default method", description); //$NON-NLS-1$
		assertTrue("The annotations for the non-default method should be API", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests default methods annotate API descriptions with @noreference
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=427495
	 */
	@Test
	public void testJava8InterfaceMethod2() throws Exception {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestJava8DefaultMethod2.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod2", "m2", "()I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("there should be annotations for method 'm2'", description); //$NON-NLS-1$
		assertTrue("The annotations should include noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod2", "m1", "()I")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("There should be API annotations for the non-default method", description); //$NON-NLS-1$
		assertTrue("The annotations for the non-default method should be API", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests default methods annotate API descriptions with @noreference
	 * and @nooverride
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=427495
	 */
	@Test
	public void testJava8InterfaceMethod3() throws Exception {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestJava8DefaultMethod3.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod3", "m2", "()I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("there should be annotations for method 'm2'", description); //$NON-NLS-1$
		assertTrue("The annotations should include noreference", (description.getRestrictions() & RestrictionModifiers.NO_REFERENCE) > 0); //$NON-NLS-1$
		assertTrue("The annotations should include noreference", (description.getRestrictions() & RestrictionModifiers.NO_OVERRIDE) > 0); //$NON-NLS-1$
		description = manifest.resolveAnnotations(Factory.methodDescriptor("a.b.c.TestJava8DefaultMethod3", "m1", "()I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("There should be API annotations for the non-default method", description); //$NON-NLS-1$
		assertTrue("The annotations for the non-default method should be API", description.getRestrictions() == RestrictionModifiers.NO_RESTRICTIONS); //$NON-NLS-1$
	}

	/**
	 * Tests resolving methods with generic type parameters. The resolution
	 * process calls into {@link Signatures#getMethodSignatureFromNode}
	 *
	 * @throws Exception
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=484268
	 */
	@Test
	public void testGenericMethodWithBounds() throws Exception {
		IApiDescription manifest = newDescription();
		doScan("a/b/c/TestGenericMethod1.java", manifest); //$NON-NLS-1$
		IApiAnnotations description = manifest.resolveAnnotations(
				Factory.methodDescriptor("a.b.c.TestGenericMethod1", "m1", "(QObject;)I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("there should be annotations for method 'm1'", description); //$NON-NLS-1$
		description = manifest.resolveAnnotations(
				Factory.methodDescriptor("a.b.c.TestGenericMethod1", "m2", "(QCollection;)I")); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		assertNotNull("There should be API annotations for the non-default method", description); //$NON-NLS-1$
	}

}
