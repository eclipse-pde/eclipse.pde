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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An API component class file container, filtered by a scope.
 * 
 * @since 1.0.0
 */
public class ScopedClassFileContainer implements IClassFileContainer {
	
	/**
	 * Underlying component
	 */
	private IApiComponent fComponent;
	
	/**
	 * Map of packages names to consider to fully qualified type names in each package to consider,
	 * or an empty collection for all types in the package.
	 */
	private Map fTypesPerPackage = new HashMap();
	
	/**
	 * Cache of package names in the scope
	 */
	private Set fPackageNames;
	
	/**
	 * Proxy to underlying API component
	 */
	class ProxyVisitor extends ClassFileContainerVisitor {
		
		/**
		 * The visitor we are providing a proxy for.
		 */
		private ClassFileContainerVisitor fVisitor;
		
		/**
		 * Constructs a visitor that will visit the underlying API component
		 * filtering the class files based on a scope.
		 * 
		 * @param visitor outer visitor
		 */
		ProxyVisitor(ClassFileContainerVisitor visitor) {
			fVisitor = visitor;
		}

		public void end(IApiComponent component) {
			if (fComponent.equals(component)) {
				fVisitor.end(component);
			}
		}

		public boolean visit(IApiComponent component) {
			if (fComponent.equals(component)) {
				return fVisitor.visit(component);
			}
			return false;
		}

		public void end(String packageName, IClassFile classFile) {
			Set types = (Set)fTypesPerPackage.get(packageName);
			if (types != null && types.contains(classFile.getTypeName())) {
				fVisitor.end(packageName, classFile);
			}
		}

		public void endVisitPackage(String packageName) {
			if (fPackageNames.contains(packageName)) {
				fVisitor.endVisitPackage(packageName);
			}
		}

		public void visit(String packageName, IClassFile classFile) {
			Set types = (Set)fTypesPerPackage.get(packageName);
			if (types != null) {
				String typeName = classFile.getTypeName();
				int index = typeName.indexOf('$');
				if (index >= 0) {
					typeName = typeName.substring(0, index);
				}
				if (types.isEmpty() || types.contains(typeName)) {
					fVisitor.visit(packageName, classFile);
				}
			}
		}

		public boolean visitPackage(String packageName) {
			if (fPackageNames.contains(packageName)) {
				return fVisitor.visitPackage(packageName);
			}
			return false;
		}

	}

	/**
	 * Constructs a class file container on the given component filtered by the specified
	 * elements.
	 * 
	 * @param component API component
	 * @param elements leaf elements in the container
	 */
	public ScopedClassFileContainer(IApiComponent component, IElementDescriptor[] elements) {
		fComponent = component;
		init(elements);
	}
	
	/**
	 * Initializes this containers packages and types to consider based on the given
	 * leaf elements.
	 * 
	 * @param elements leaf elements
	 */
	private void init(IElementDescriptor[] elements) {
		for (int i = 0; i < elements.length; i++) {
			IElementDescriptor element = elements[i];
			IPackageDescriptor pkg = null;
			IReferenceTypeDescriptor type = null;
			if (element instanceof IPackageDescriptor) {
				pkg = (IPackageDescriptor) element;
			} else if (element instanceof IMemberDescriptor) {
				IMemberDescriptor member = (IMemberDescriptor)element;
				pkg = member.getPackage();
				if (element instanceof IReferenceTypeDescriptor) {
					type = (IReferenceTypeDescriptor) element;
				} else {
					type = member.getEnclosingType();
				}
			}
			String pkgname = pkg.getName();
			Set types = (Set) fTypesPerPackage.get(pkgname);
			if (types == null) {
				types = new HashSet();
				fTypesPerPackage.put(pkgname, types);
			}
			if (type != null) {
				types.add(type.getQualifiedName());
			}
		}
		fPackageNames = fTypesPerPackage.keySet();
	}
	
	/* (non-Javadoc)
	 * @see IClassFileContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ClassFileContainerVisitor visitor) throws CoreException {
		ProxyVisitor proxyVisitor = new ProxyVisitor(visitor);
		fComponent.accept(proxyVisitor);
	}

	/* (non-Javadoc)
	 * @see IClassFileContainer#close()
	 */
	public void close() throws CoreException {
		fComponent.close();
	}

	/* (non-Javadoc)
	 * @see IClassFileContainer#findClassFile(java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName) throws CoreException {
		String packageName = Util.getPackageName(qualifiedName);
		Set types = (Set) fTypesPerPackage.get(packageName);
		if (types != null) {
			if (types.isEmpty()) {
				// all types in the package
				return fComponent.findClassFile(qualifiedName);
			} else {
				if (types.contains(qualifiedName)) {
					return fComponent.findClassFile(qualifiedName);
				}
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see IClassFileContainer#findClassFile(java.lang.String,java.lang.String)
	 */
	public IClassFile findClassFile(String qualifiedName, String id) throws CoreException {
		String packageName = Util.getPackageName(qualifiedName);
		Set types = (Set) fTypesPerPackage.get(packageName);
		if (types != null) {
			if (types.isEmpty()) {
				// all types in the package
				return fComponent.findClassFile(qualifiedName, id);
			} else {
				if (types.contains(qualifiedName)) {
					return fComponent.findClassFile(qualifiedName, id);
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see IClassFileContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		return (String[]) fPackageNames.toArray(new String[fPackageNames.size()]);
	}

	/* (non-Javadoc)
	 * @see IClassFileContainer#getOrigin()
	 */
	public String getOrigin() {
		return this.fComponent.getId();
	}
}
