/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;

/**
 * A search scope containing only types from one component. More efficient than a general purpose
 * scope.
 * 
 * @since 1.0
 */
public class TypeScope implements IApiSearchScope {

	/**
	 * Associated component
	 */
	private IApiComponent fComponent;
	
	/**
	 * Map of package names to associated type descriptors
	 */
	private Map fPackageToTypes;
			
	/**
	 * Constructs a new class file container/search scope on the given types.
	 * 
	 * @param component API component
	 * @param types types within the component
	 */
	public TypeScope(IApiComponent component, IReferenceTypeDescriptor[] types) {
		fComponent = component;
		fPackageToTypes = new HashMap();
		for (int i = 0; i < types.length; i++) {
			IReferenceTypeDescriptor type = types[i];
			String name = type.getPackage().getName();
			Set set = (Set) fPackageToTypes.get(name);
			if (set == null) {
				set = new HashSet();
				fPackageToTypes.put(name, set);
			}
			set.add(type);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope#encloses(java.lang.String, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	public boolean encloses(String componentId, IElementDescriptor element) {
		if (getOrigin().equals(componentId)) {
			if (element.getElementType() == IElementDescriptor.T_FIELD || element.getElementType() == IElementDescriptor.T_METHOD) {
				element = ((IMemberDescriptor)element).getEnclosingType();
			}
			if (element.getElementType() == IElementDescriptor.T_REFERENCE_TYPE) {
				IReferenceTypeDescriptor type = (IReferenceTypeDescriptor) element;
				String pkg = type.getPackage().getName();
				Set types = (Set) fPackageToTypes.get(pkg);
				if (types != null) {
					return types.contains(type);
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#getOrigin()
	 */
	public String getOrigin() {
		return fComponent.getId();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		Set pkgs = fPackageToTypes.keySet();
		return (String[]) pkgs.toArray(new String[pkgs.size()]);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#accept(org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		if (visitor.visit(fComponent)) {
			Set entrySet = fPackageToTypes.entrySet();
			Iterator iterator = entrySet.iterator();
			while (iterator.hasNext()) {
				Entry entry = (Entry) iterator.next();
				String pkg = (String)entry.getKey();
				if (visitor.visitPackage(pkg)) {
					Set types = (Set) entry.getValue();
					Iterator typeIter = types.iterator();
					while (typeIter.hasNext()) {
						IReferenceTypeDescriptor type = (IReferenceTypeDescriptor) typeIter.next();
						IClassFile classFile = fComponent.findClassFile(type.getQualifiedName());
						if (classFile != null) {
							visitor.visit(pkg, classFile);
							visitor.end(pkg, classFile);
						}
					}
				}
				visitor.endVisitPackage(pkg);
			}
		}
		visitor.end(fComponent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#close()
	 */
	public void close() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		IReferenceTypeDescriptor descriptor = Factory.typeDescriptor(qualifiedName);
		if (encloses(getOrigin(), descriptor)) {
			return fComponent.findClassFile(qualifiedName);
		}
		return null;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer#findClassFile(java.lang.String, java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
		if (getOrigin().equals(id)) {
			return findClassFile(qualifiedName);
		}
		return null;
	}

	
}