/*******************************************************************************
 * Copyright (c) Mar 26, 2014 IBM Corporation and others.
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

import junit.framework.Test;
import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.test.OrderedTestSuite;

/**
 * Tests reading JJava 8 classfiles and extracting specific references
 * 
 * @since 1.0.400
 */
public class Java8ClassfileScannerTests extends TestCase {

	private static IPath WORKSPACE_ROOT = null;
	private static String WORKSPACE_NAME = "test_classes_workspace_java8"; //$NON-NLS-1$
	private static IPath ROOT_PATH = null;
	private static DirectoryApiTypeContainer container = null;

	static {
		// setup workspace root
		WORKSPACE_ROOT = TestSuiteHelper.getPluginDirectoryPath().append(WORKSPACE_NAME);
		ROOT_PATH = TestSuiteHelper.getPluginDirectoryPath().append("test-source").append("invokedynamic"); //$NON-NLS-1$ //$NON-NLS-2$
		new File(WORKSPACE_ROOT.toOSString()).mkdirs();
	}

	public static Test suite() {
		return new OrderedTestSuite(Java8ClassfileScannerTests.class, new String[] {
				"testStaticMethodRef", //$NON-NLS-1$
				"testCleanup" //$NON-NLS-1$
		});
	}

	@Override
	protected void setUp() throws Exception {
		if (container == null) {
			String[] sourceFilePaths = new String[] { ROOT_PATH.toOSString() };
			assertTrue("working directory should compile", TestSuiteHelper.compile(sourceFilePaths, WORKSPACE_ROOT.toOSString(), TestSuiteHelper.getCompilerOptions())); //$NON-NLS-1$
			container = new DirectoryApiTypeContainer(null, WORKSPACE_ROOT.append("invokedynamic").toOSString()); //$NON-NLS-1$
		}
	}

	/**
	 * Returns the set of references collected from the given class file
	 * 
	 * @param qualifiedname
	 * @return the set of references from the specified class file name or
	 *         <code>null</code>
	 */
	protected List<IReference> getRefSet(String qualifiedname) {
		try {
			IApiTypeRoot cfile = container.findTypeRoot(qualifiedname);
			IApiType type = cfile.getStructure();
			List<IReference> references = type.extractReferences(IReference.MASK_REF_ALL, null);
			return references;
		} catch (CoreException ce) {
			fail(ce.getMessage());
		}
		return null;
	}

	/**
	 * Returns the fully qualified type name associated with the given member.
	 * 
	 * @param member
	 * @return fully qualified type name
	 */
	private String getTypeName(IApiMember member) throws CoreException {
		switch (member.getType()) {
			case IApiElement.TYPE:
				return member.getName();
			default:
				return member.getEnclosingType().getName();
		}
	}

	/**
	 * Finds a reference to a given target from a given source to a given target
	 * member of a specified kind from the given listing
	 * 
	 * @param sourcename the qualified name of the location the reference is
	 *            from
	 * @param sourceMember the name of the source member making the reference or
	 *            <code>null</code> if none
	 * @param targetname the qualified type name being referenced
	 * @param targetMember name of target member referenced or <code>null</code>
	 * @param kind the kind of reference. see {@link IReference} for kinds
	 * @param refs the current listing of references to search within
	 * @return an {@link IReference} matching the specified criteria or
	 *         <code>null</code> if none found
	 */
	protected IReference findMemberReference(String sourcename, String sourceMember, String targetname, String targetMember, int kind, List<IReference> refs) throws CoreException {
		IReference ref = null;
		for (Iterator<IReference> iter = refs.iterator(); iter.hasNext();) {
			ref = iter.next();
			if (ref.getReferenceKind() == kind) {
				if (getTypeName(ref.getMember()).equals(sourcename)) {
					if (ref.getReferencedTypeName().equals(targetname)) {
						if (sourceMember != null) {
							if (!ref.getMember().getName().equals(sourceMember)) {
								continue;
							}
						}
						if (targetMember != null) {
							if (!ref.getReferencedMemberName().equals(targetMember)) {
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
	 * Tests getting an invoke dynamic ref for a static method ref
	 * 
	 * @throws Exception
	 */
	public void testStaticMethodRef() throws Exception {
		List<IReference> refs = getRefSet("test1"); //$NON-NLS-1$
		IReference ref = findMemberReference("invokedynamic.test1", "m1", "invokedynamic.test1$MR", "mrCompare", IReference.REF_VIRTUALMETHOD, refs); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("There should be a ref for invokedynamic.test1$MR#mrCompare", ref != null); //$NON-NLS-1$
	}

	/**
	 * Cleans up after the tests are done. This must be the last test run
	 */
	public void testCleanup() {
		// remove workspace root
		assertTrue(TestSuiteHelper.delete(new File(WORKSPACE_ROOT.toOSString())));
	}
}
