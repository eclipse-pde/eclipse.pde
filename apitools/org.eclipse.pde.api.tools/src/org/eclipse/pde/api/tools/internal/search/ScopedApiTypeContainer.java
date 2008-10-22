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
import org.eclipse.pde.api.tools.internal.model.ApiElement;
import org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * An API component {@link IApiTypeContainer}, filtered by a scope.
 * 
 * @since 1.0.0
 */
public class ScopedApiTypeContainer extends ApiElement implements IApiTypeContainer {
	
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
	class ProxyVisitor extends ApiTypeContainerVisitor {
		
		/**
		 * The visitor we are providing a proxy for.
		 */
		private ApiTypeContainerVisitor fVisitor;
		
		/**
		 * Constructs a visitor that will visit the underlying API component
		 * filtering the class files based on a scope.
		 * 
		 * @param visitor outer visitor
		 */
		ProxyVisitor(ApiTypeContainerVisitor visitor) {
			fVisitor = visitor;
		}

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#end(org.eclipse.pde.api.tools.internal.provisional.IApiComponent)
		 */
		public void end(IApiComponent component) {
			if (((IApiComponent) getParent()).equals(component)) {
				fVisitor.end(component);
			}
		}

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#visit(org.eclipse.pde.api.tools.internal.provisional.IApiComponent)
		 */
		public boolean visit(IApiComponent component) {
			if (((IApiComponent) getParent()).equals(component)) {
				return fVisitor.visit(component);
			}
			return false;
		}

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#end(java.lang.String, org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot)
		 */
		public void end(String packageName, IApiTypeRoot classFile) {
			Set types = (Set)fTypesPerPackage.get(packageName);
			if (types != null && types.contains(classFile.getTypeName())) {
				fVisitor.end(packageName, classFile);
			}
		}

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#endVisitPackage(java.lang.String)
		 */
		public void endVisitPackage(String packageName) {
			if (fPackageNames.contains(packageName)) {
				fVisitor.endVisitPackage(packageName);
			}
		}

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#visit(java.lang.String, org.eclipse.pde.api.tools.internal.provisional.IApiTypeRoot)
		 */
		public void visit(String packageName, IApiTypeRoot classFile) {
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

		/**
		 * @see org.eclipse.pde.api.tools.internal.provisional.ApiTypeContainerVisitor#visitPackage(java.lang.String)
		 */
		public boolean visitPackage(String packageName) {
			if (fPackageNames.contains(packageName)) {
				return fVisitor.visitPackage(packageName);
			}
			return false;
		}
	}

	/**
	 * Constructs an {@link IApiTypeContainer} on the given component filtered by the specified
	 * elements.
	 * 
	 * @param component API component
	 * @param elements leaf elements in the container
	 */
	public ScopedApiTypeContainer(IApiComponent component, IElementDescriptor[] elements) {
		super(component, IApiElement.API_TYPE_CONTAINER, null);
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
	 * @see IApiTypeContainer#accept(org.eclipse.pde.api.tools.model.component.ClassFileContainerVisitor)
	 */
	public void accept(ApiTypeContainerVisitor visitor) throws CoreException {
		ProxyVisitor proxyVisitor = new ProxyVisitor(visitor);
		((IApiComponent) getParent()).accept(proxyVisitor);
	}

	/* (non-Javadoc)
	 * @see IApiTypeContainer#close()
	 */
	public synchronized void close() throws CoreException {
		((IApiComponent) getParent()).close();
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName) throws CoreException {
		String packageName = Util.getPackageName(qualifiedName);
		Set types = (Set) fTypesPerPackage.get(packageName);
		if (types != null) {
			if (types.isEmpty()) {
				// all types in the package
				return ((IApiComponent) getParent()).findTypeRoot(qualifiedName);
			} else {
				if (types.contains(qualifiedName)) {
					return ((IApiComponent) getParent()).findTypeRoot(qualifiedName);
				}
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiTypeContainer#findTypeRoot(java.lang.String, java.lang.String)
	 */
	public IApiTypeRoot findTypeRoot(String qualifiedName, String id) throws CoreException {
		String packageName = Util.getPackageName(qualifiedName);
		Set types = (Set) fTypesPerPackage.get(packageName);
		if (types != null) {
			if (types.isEmpty()) {
				// all types in the package
				return ((IApiComponent) getParent()).findTypeRoot(qualifiedName, id);
			} else {
				if (types.contains(qualifiedName)) {
					return ((IApiComponent) getParent()).findTypeRoot(qualifiedName, id);
				}
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see IApiTypeContainer#getPackageNames()
	 */
	public String[] getPackageNames() throws CoreException {
		return (String[]) fPackageNames.toArray(new String[fPackageNames.size()]);
	}
}
