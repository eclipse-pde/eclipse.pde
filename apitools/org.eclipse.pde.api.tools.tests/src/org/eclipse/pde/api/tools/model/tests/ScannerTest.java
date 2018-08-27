/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

import junit.framework.TestCase;

/**
 * Root class for testing scanning classfiles
 *
 * @since 1.0.400
 */
public abstract class ScannerTest extends TestCase {

	private static DirectoryApiTypeContainer container = null;

	/**
	 * Get the testing workspace to use
	 *
	 * @return workspace root path
	 */
	protected abstract IPath getWorkspaceRoot();

	/**
	 * Get the path to the source to be compiled
	 *
	 * @return source path
	 */
	protected abstract IPath getSourcePath();

	/**
	 * The name of the workspace folder
	 *
	 * @return the name of the folder to use in the workspace location
	 */
	protected abstract String getPackageName();

	/**
	 * Compile the source to scan
	 *
	 * @return if compilation succeeded
	 */
	protected abstract boolean doCompile();

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		IPath root = getWorkspaceRoot();
		File file = root.toFile();
		if (!file.exists()) {
			file.mkdirs();
		}

		if (container == null) {
			assertTrue("The test workspace failed to compile", doCompile()); //$NON-NLS-1$
			container = new DirectoryApiTypeContainer(null, root.append(getPackageName()).toOSString());
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

	}

	/**
	 * Returns the container
	 *
	 * @return the container
	 */
	protected DirectoryApiTypeContainer getContainer() {
		return container;
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
	 * Finds an {@link IReference} within the given set, where a matching ref
	 * has the same kind and the target of the reference matches the specified
	 * qualified name
	 *
	 * @param sourcename the qualified name of the source location
	 * @param targetname the qualified name of the target location
	 * @param kind the kind of the {@link IReference}
	 * @param refs the set of {@link IReference}s to search within
	 * @return a matching {@link IReference} or <code>null</code>
	 */
	protected IReference findReference(String sourcename, String targetname, int kind, List<IReference> refs) throws CoreException {
		IReference ref = null;
		for (Iterator<IReference> iter = refs.iterator(); iter.hasNext();) {
			ref = iter.next();
			if (ref.getReferenceKind() == kind) {
				if (getTypeName(ref.getMember()).equals(sourcename)) {
					if (ref.getReferencedTypeName().equals(targetname)) {
						return ref;
					}
				}
			}
			ref = null;
		}
		return ref;
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
	 * Close the container and null it out
	 *
	 * @throws Exception
	 */
	protected void cleanUp() throws Exception {
		if (container != null) {
			container.close();
			container = null;
		}
	}
}
