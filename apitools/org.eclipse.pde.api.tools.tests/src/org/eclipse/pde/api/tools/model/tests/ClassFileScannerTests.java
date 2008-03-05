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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.api.tools.internal.DirectoryClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.search.ILocation;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers;
import org.eclipse.pde.api.tools.internal.search.ClassFileScanner;

/**
 * This class tests the class file scanner and the class file visitor
 * 
 * @since 1.0.0
 */
public class ClassFileScannerTests extends TestCase {
	
	private static IPath WORKSPACE_ROOT = null;
	private static String WORKSPACE_NAME = "test_classes_workspace";
	private static IPath ROOT_PATH = null;
	private static DirectoryClassFileContainer container = null;
	private static ClassFileScanner scanner = null;
	private static IApiComponent component = null;
	
	static {
		//setup workspace root
		WORKSPACE_ROOT = TestSuiteHelper.getPluginDirectoryPath().append(WORKSPACE_NAME);
		ROOT_PATH = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("classes");
		new File(WORKSPACE_ROOT.toOSString()).mkdirs();
	}
	
	/**
	 * Returns the set of references collected from the given class file
	 * @param qualifiedname
	 * @return the set of references from the specified class file name or <code>null</code>
	 */
	protected List getRefSet(String qualifiedname) {
		try {
			IClassFile cfile = container.findClassFile(qualifiedname);
			scanner.scan(component, cfile, false, ReferenceModifiers.MASK_REF_ALL);
			return scanner.getReferenceListing();
		}
		catch(CoreException ce) {
			fail(ce.getMessage());
		}
		return null;
	}
	
	/**
	 * Finds an {@link IReference} within the given set, where a matching ref has the same kind
	 * and the target of the reference matches the specified qualified name
	 * @param sourcename the qualified name of the source location
	 * @param targetname the qualified name of the target location
	 * @param kind the kind of the {@link IReference}
	 * @param refs the set of {@link IReference}s to search within
	 * @return a matching {@link IReference} or <code>null</code>
	 */
	protected IReference findReference(String sourcename, String targetname, int kind, List refs) {
		IReference ref = null;
		ILocation target = null;
		for(Iterator iter = refs.iterator(); iter.hasNext();) {
			ref = (IReference) iter.next();
			if(ref.getReferenceKind() == kind) {
				if(ref.getSourceLocation().getType().getQualifiedName().equals(sourcename)) {
					target = ref.getReferencedLocation();
					if(target.getType().getQualifiedName().equals(targetname)) {
						return ref;
					}
				}
			}
			ref = null;
		}
		return ref;
	}
	
	/**
	 * Finds a reference to a given target from a given source to a given target member of a specified kind from the given listing
	 * @param sourcename the qualified name of the location the reference is from
	 * @param sourceMember the name of the source member making the reference or <code>null</code> if none
	 * @param targetname the qualified type name being referenced
	 * @param targetMember name of target member referenced or <code>null</code>
	 * @param kind the kind of reference. see {@link IReference} for kinds
	 * @param refs the current listing of references to search within
	 * @return an {@link IReference} matching the specified criteria or <code>null</code> if none found
	 */
	protected IReference findMemberReference(String sourcename, String sourceMember, String targetname, String targetMember, int kind, List refs) {
		IReference ref = null;
		ILocation target = null;
		for(Iterator iter = refs.iterator(); iter.hasNext();) {
			ref = (IReference) iter.next();
			if(ref.getReferenceKind() == kind) {
				if(ref.getSourceLocation().getType().getQualifiedName().equals(sourcename)) {
					target = ref.getReferencedLocation();
					if(target.getType().getQualifiedName().equals(targetname)) {
						if (sourceMember != null) {
							IMemberDescriptor member = ref.getSourceLocation().getMember();
							if(!member.getName().equals(sourceMember)) {
								continue;
							}
						}
						if (targetMember != null) {
							IMemberDescriptor member = ref.getReferencedLocation().getMember();
							if(!member.getName().equals(targetMember)) {
								continue;
							}
						}
						return ref;
					}
				}
			}
			ref = null;
		}
		return ref;
	}
	
	/**
	 * Compiles the test-classes workspace
	 * @throws CoreException 
	 */
	public void testCompileClassWorkspace() throws CoreException {
		String[] sourceFilePaths = new String[] {ROOT_PATH.toOSString()};
		assertTrue("working directory should compile", TestSuiteHelper.compile(sourceFilePaths, WORKSPACE_ROOT.toOSString(), TestSuiteHelper.COMPILER_OPTIONS));
		assertTrue("Test12 should compile to 1.4", TestSuiteHelper.compile(ROOT_PATH.append("Test12.java").toOSString(),
				WORKSPACE_ROOT.toOSString(), 
				new String[] {"-1.4", "-preserveAllLocals", "-nowarn"}));
		container = new DirectoryClassFileContainer(WORKSPACE_ROOT.append("classes").toOSString(), null);
		scanner = ClassFileScanner.newScanner();
		component = new IApiComponent() {
			public String[] getPackageNames() throws CoreException {
				return null;
			}
			public IClassFile findClassFile(String qualifiedName) throws CoreException {
				return null;
			}
			public void close() throws CoreException {
			}
			public void accept(ClassFileContainerVisitor visitor) throws CoreException {
			}
			public String getVersion() {
				return null;
			}
			public IRequiredComponentDescription[] getRequiredComponents() {
				return null;
			}
			public String getName() {
				return "test";
			}
			public String getLocation() {
				return null;
			}
			public String getId() {
				return "test";
			}
			public String[] getExecutionEnvironments() {
				return null;
			}
			public IClassFileContainer[] getClassFileContainers() {
				return null;
			}
			public IApiDescription getApiDescription() {
				return null;
			}
			public boolean isSystemComponent() {
				return false;
			}
			public void dispose() {
			}
			public IApiProfile getProfile() {
				return null;
			}
			public void export(Map options, IProgressMonitor monitor) throws CoreException {
			}
			public IApiFilterStore getFilterStore() {
				return null;
			}
			public IApiProblemFilter newProblemFilter(IApiProblem problem) {
				return null;
			}
			public boolean isSourceComponent() {
				return false;
			}
			public boolean isFragment() {
				return false;
			}
			public boolean hasFragments() {
				return false;
			}
			public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
				return null;
			}
			public IClassFileContainer[] getClassFileContainers(String id) {
				return null;
			}
			public String getOrigin() {
				return this.getId();
			}
		};
	}
	
	/**
	 * Tests scanning a simple class file that extends nothing, implements nothing and has no members
	 */
	public void testScanEmptyClass() {
		List refs = getRefSet("Test1");
		IReference ref = findMemberReference("classes.Test1", null, "java.lang.Object", null, ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findMemberReference("classes.Test1", "<init>", "java.lang.Object", null, ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor", ref != null);
	}
	
	/**
	 * Test scanning a simple generic class file that extends nothing, implements nothing and has no members
	 */
	public void testScanEmptyGenericClass() {
		List refs = getRefSet("Test2");
		IReference ref = findMemberReference("classes.Test2", null, "java.lang.Object", null, ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("There should be an extends ref to java.lang.Object for an empty class", ref != null);
		ref = findMemberReference("classes.Test2", "<init>", "java.lang.Object", null, ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor", ref != null);
		ref = findMemberReference("classes.Test2", null, "java.lang.Object", null, ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("There should be a parameterized type ref to java.lang.Object", ref != null);
	}
	
	/**
	 * Tests scanning an empty inner class
	 */
	public void testScanInnerClass() {
		List refs = getRefSet("Test3$Inner");
		IReference ref = findMemberReference("classes.Test3$Inner", null, "java.lang.Object", null, ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findMemberReference("classes.Test3$Inner", "<init>", "java.lang.Object", null, ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning a empty static inner class
	 */
	public void testScanInnerStaticClass() {
		List refs = getRefSet("Test3$Inner2");
		IReference ref = findReference("classes.Test3$Inner2", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findReference("classes.Test3$Inner2", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning an empty inner class of an empty inner static class
	 */
	public void testScanInnerStaticInnerClass() {
		List refs = getRefSet("Test3$Inner2$Inner3");
		IReference ref = findReference("classes.Test3$Inner2$Inner3", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findReference("classes.Test3$Inner2$Inner3", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning an empty outer class
	 */
	public void testScanOuterClass() {
		List refs = getRefSet("Test3Outer");
		IReference ref = findReference("classes.Test3Outer", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findReference("classes.Test3Outer", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning an empty class of the inner class of an outer class
	 */
	public void testScanInnerOuterClass() {
		List refs = getRefSet("Test3Outer$Inner");
		IReference ref = findReference("classes.Test3Outer$Inner", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends ref to java.lang.Object", ref != null);
		ref = findReference("classes.Test3Outer$Inner", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning an inner static generic type
	 */
	public void testScanInnerGenericClass() {
		List refs = getRefSet("Test4$Inner");
		IReference ref = findReference("classes.Test4$Inner", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("There should be an extends ref to java.lang.Object for an inner empty class", ref != null);
		ref = findReference("classes.Test4$Inner", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
		ref = findReference("classes.Test4$Inner", "java.lang.Object", ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning an inner class of a static class of a generic type
	 */
	public void testScanInnerStaticInnerGenericClass() {
		List refs = getRefSet("Test4$Inner$Inner2");
		IReference ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("There should be an extends ref to java.lang.Object for an inner empty class", ref != null);
		ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
		ref = findReference("classes.Test4$Inner$Inner2", "java.lang.Object", ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning a non-generic class that extends something and implements interfaces
	 */
	public void testScanClassExtendsImplements() {
		List refs = getRefSet("Test5");
		IReference ref = findReference("classes.Test5", "java.util.ArrayList", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be an extends reference to java.util.ArrayList", ref != null);
		ref = findReference("classes.Test5", "java.lang.Iterable", ReferenceModifiers.REF_IMPLEMENTS, refs);
		assertTrue("there should be an implements reference to java.lang.Iterable", ref != null);
		ref = findReference("classes.Test5", "classes.ITest5", ReferenceModifiers.REF_IMPLEMENTS, refs);
		assertTrue("there should be an implements reference to classes.ITest5", ref != null);
		ref = findReference("classes.Test5", "java.util.ArrayList", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a ref to java.lang.Object in the default constructor of an inner class", ref != null);
	}
	
	/**
	 * Tests scanning a generic class that extends something and implements interfaces
	 */
	public void testScanGenericClassExtendsImplements() {
		List refs = getRefSet("Test6");
		IReference ref = findReference("classes.Test6", "classes.Test6Abstract", ReferenceModifiers.REF_CONSTRUCTORMETHOD, refs);
		assertTrue("there should be a REF_CONSTRUCTORMETHOD ref to classes.Test6Abstract", ref != null);
		ref = findReference("classes.Test6", "java.lang.Iterable", ReferenceModifiers.REF_IMPLEMENTS, refs);
		assertTrue("there should be a REF_IMPLEMENTS ref to java.lang.Iterable", ref != null);
		ref = findReference("classes.Test6", "java.util.ArrayList", ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.util.ArrayList", ref != null);
		ref = findReference("classes.Test6", "java.util.Map", ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref to java.util.Map", ref != null);
		ref = findReference("classes.Test6", "java.lang.String", ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref to java.lang.String", ref != null);
		ref = findReference("classes.Test6", "java.lang.String", ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.lang.String", ref != null);
		ref = findReference("classes.Test6", "java.util.Iterator", ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("there should be a REF_RETURNTYPE ref to java.util.Iterator", ref != null);
		ref = findReference("classes.Test6", "java.util.Map", ReferenceModifiers.REF_PARAMETERIZED_TYPEDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_TYPEDECL ref to java.util.Map", ref != null);
		ref = findReference("classes.Test6", "classes.Test6Abstract", ReferenceModifiers.REF_EXTENDS, refs);
		assertTrue("there should be a REF_EXTENDS ref to classes.Test6Abstract", ref != null);
	}
	
	/**
	 * Tests a variety of method declarations
	 */
	public void testScanMethodDecl() {
		List refs = getRefSet("Test7");
		IReference ref = findMemberReference("classes.Test7", "m1", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m1 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m1", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m1 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m2", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m2 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m2", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m2 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m3", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m3 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m3", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m3 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m7", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m7 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m7", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m7 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m8", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m8 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m8", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m8 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m9", "java.lang.String", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m9 should have a REF_RETURNTYPE ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test7", "m9", "java.lang.String", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m9 should have a REF_PARAMETER ref to java.lang.String", ref != null);
		
		
	}
	
	/**
	 * Tests a variety of method declarations with array types in them
	 */
	public void testScanMethodDeclArrayTypes() {
		List refs = getRefSet("Test7");
		IReference ref = findMemberReference("classes.Test7", "m4", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m4 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m4", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m4 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test7", "m5", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m5 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m5", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m5 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test7", "m6", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m6 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m6", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m6 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test7", "m10", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m10 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m10", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m10 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test7", "m11", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m11 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m11", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m11 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test7", "m12", "java.lang.Integer", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("m12 should have a REF_RETURNTYPE ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test7", "m12", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("m12 should have a REF_PARAMETER ref to java.lang.Double", ref != null);
	}
	
	/**
	 * Tests a variety of method declarations with generic types
	 */
	public void testScanMethodDeclGenerics() {
		List refs = getRefSet("Test8");
		IReference ref = findMemberReference("classes.Test8", "m1", "java.util.ArrayList", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("there should be a REF_RETURNTYPE ref for m1", ref != null);
		ref = findMemberReference("classes.Test8", "m1", "java.util.Map", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("there should be a REF_PARAMETER ref for m1", ref != null);
		ref = findMemberReference("classes.Test8", "m1", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("there should be a REF_PARAMETER ref for m1 for java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test8", "m1", "java.lang.Integer", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test8", "m1", "java.lang.String", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for java.lang.String", ref != null);
		ref = findMemberReference("classes.Test8", "m1", "classes.Test8Outer", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m1 for classes.Test8Outer", ref != null);
		
		ref = findMemberReference("classes.Test8", "m2", "java.util.ArrayList", null, ReferenceModifiers.REF_RETURNTYPE, refs);
		assertTrue("there should be a REF_RETURNTYPE ref for m2", ref != null);
		ref = findMemberReference("classes.Test8", "m2", "java.util.Map", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("there should be a REF_PARAMETER ref for m2", ref != null);
		ref = findMemberReference("classes.Test8", "m2", "java.lang.Double", null, ReferenceModifiers.REF_PARAMETER, refs);
		assertTrue("there should be a REF_PARAMETER ref for m2 for java.lang.Double", ref != null);
		ref = findMemberReference("classes.Test8", "m2", "java.lang.Integer", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test8", "m2", "java.lang.String", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for java.lang.String", ref != null);
		ref = findMemberReference("classes.Test8", "m2", "classes.Test8Outer", null, ReferenceModifiers.REF_PARAMETERIZED_METHODDECL, refs);
		assertTrue("there should be a REF_PARAMETERIZED_METHODDECL ref for m2 for classes.Test8Outer", ref != null);
	}
	
	/**
	 * Tests a variety of field declarations
	 */
	public void testScanFieldDecl() {
		List refs = getRefSet("Test9");
		IReference ref = findMemberReference("classes.Test9", "strs", "java.lang.String", null, ReferenceModifiers.REF_FIELDDECL, refs);
		assertTrue("there should be a REF_FIELDDECL ref for java.lang.String", ref != null);
		ref = findMemberReference("classes.Test9", "list", "java.util.ArrayList", null, ReferenceModifiers.REF_FIELDDECL, refs);
		assertTrue("there should be a REF_FIELDDECL ref for java.util.ArrayList", ref != null);
		ref = findMemberReference("classes.Test9", "object", "java.lang.Object", null, ReferenceModifiers.REF_FIELDDECL, refs);
		assertTrue("there should be a REF_FIELDDECL ref for java.lang.Object", ref != null);
		//TODO does not collect ref to Runnable in Test9 as there is no direct ref to Runnable in that classfile
		
	}
	
	/**
	 * Tests a variety of arrays that have been declared as local variables in methods
	 */
	public void testScanLocalVariableArrays() {
		List refs = getRefSet("Test10");
		IReference ref = findMemberReference("classes.Test10", null, "java.lang.String", null, ReferenceModifiers.REF_ARRAYALLOC, refs);
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.String", ref != null);
		ref = findMemberReference("classes.Test10", null, "java.lang.Object", null, ReferenceModifiers.REF_ARRAYALLOC, refs);
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Object", ref != null);
		ref = findMemberReference("classes.Test10", null, "java.lang.Integer", null, ReferenceModifiers.REF_ARRAYALLOC, refs);
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test10", null, "java.lang.Double", null, ReferenceModifiers.REF_ARRAYALLOC, refs);
		assertTrue("there should be a REF_ARRAYALLOC ref to java.lang.Double", ref != null);
	}
	
	/**
	 * Tests a variety of LDC ops that load things like Integer.class onto the stack
	 */
	public void testScanConstantPoolAccess() {
		List refs = getRefSet("Test11");
		IReference ref = findMemberReference("classes.Test11", null, "java.lang.Integer", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test11", null, "java.lang.Double", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test11", null, "java.lang.String", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);	
	}
	
	/**
	 * Tests a variety of LDC ops that load things like Integer.class onto the stack. This method uses a 
	 * 1.4 code level class, and checks that the LDC ref is actually processed via a Class.forName static method call
	 */
	public void testScanConstantPoolAccess1_4() {
		List refs = getRefSet("Test12");
		IReference ref = findMemberReference("classes.Test12", null, "java.lang.Integer", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test12", null, "java.lang.Double", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);
		ref = findMemberReference("classes.Test12", null, "java.lang.String", null, ReferenceModifiers.REF_CONSTANTPOOL, refs);
		assertTrue("there should be a REF_CONSTANTPOOL ref to java.lang.Integer", ref != null);	
	}
	
	/**
	 * Tests a variety of method calls 
	 */
	public void testScanMethodCalls() {
		List refs = getRefSet("Test13");
		IReference ref = findMemberReference("classes.Test13", "m1", "classes.Test13", "m2", ReferenceModifiers.REF_VIRTUALMETHOD, refs);
		assertTrue("the should be a REF_VIRTUALMETHOD ref to m2 from classes.Test13", ref != null);
		ref = findMemberReference("classes.Test13", "m1", "classes.Test13", "m3", ReferenceModifiers.REF_VIRTUALMETHOD, refs);
		assertTrue("the should be a REF_VIRTUALMETHOD ref to m3 from classes.Test13", ref != null);
		ref = findMemberReference("classes.Test13", "m4", "classes.Test13A", "getInteger", ReferenceModifiers.REF_VIRTUALMETHOD, refs);
		assertTrue("the should be a REF_VIRTUALMETHOD ref to getInteger from classes.Test13A", ref != null);
		ref = findMemberReference("classes.Test13", "m3", "classes.Test13A", "doSomething", ReferenceModifiers.REF_STATICMETHOD, refs);
		assertTrue("the should be a REF_STATICMETHOD ref to doSomething from classes.Test13A", ref != null);
	}
	
	/**
	 * Cleans up after the tests are done. This must be the last test run
	 */
	public void testCleanup() {
		// remove workspace root
		assertTrue(TestSuiteHelper.delete(new File(WORKSPACE_ROOT.toOSString())));
	}
}
