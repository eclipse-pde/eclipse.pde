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
package org.eclipse.pde.api.tools.internal.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.ClassVisitorAdapter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.MethodVisitor;

/**
 * Extracts method declarations from a class file.
 * 
 * @since 1.0.0
 */
public class MethodExtractor extends ClassVisitorAdapter {

	/**
	 * List of method descriptors. Methods are added to the list
	 * as they are visited.
	 */
	private List fMethods = new ArrayList();
	
	/**
	 * Cache of descriptors in case of multiple access
	 */
	private IMethodDescriptor[] fDescriptors = null;
	
	/**
	 * Current type being visited.
	 */
	private IReferenceTypeDescriptor fType;

	/**
	 * Superclass name, or <code>null</code> if none
	 */
	private String fSuperName;
	
	/**
	 * Extended interfaces
	 */
	private String[] fInterfaces;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.util.ClassVisitorAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		fType = Util.getType(name.replace('/', '.'));
		if (superName != null) {
			fSuperName = superName.replace('/', '.');
		}
		if (interfaces != null) {
			fInterfaces = new String[interfaces.length];
			for (int i = 0; i < interfaces.length; i++) {
				fInterfaces[i] = interfaces[i].replace('/', '.');
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.util.ClassVisitorAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		fMethods.add(fType.getMethod(name, desc, access));
		return null;
	}
	
	/**
	 * Returns methods collected so far.
	 * 
	 * @return method descriptors
	 */
	public IMethodDescriptor[] getMethods() {
		if (fDescriptors == null) {
			fDescriptors = (IMethodDescriptor[]) fMethods.toArray(new IMethodDescriptor[fMethods.size()]);
		} 
		return fDescriptors;
	}
	
	/**
	 * Returns super class name or <code>null</code> if none.
	 * 
	 * @return super class name or <code>null</code> if none
	 */
	public String getSuperclassName() {
		return fSuperName;
	}
	
	/**
	 * Returns extended interfaces or <code>null</code> if none.
	 * 
	 * @return extended interfaces or <code>null</code> if none
	 */
	public String[] getInteraces() {
		return fInterfaces;
	}
}
