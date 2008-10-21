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
package org.eclipse.pde.api.tools.internal.provisional;


import org.eclipse.core.resources.IResource;
import org.eclipse.pde.api.tools.internal.descriptors.PackageDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.ResourceDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.search.SearchScope;
import org.eclipse.pde.api.tools.internal.search.TypeScope;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Factory to create API model objects.
 * 
 * @since 1.0
 */
public class Factory {

	/**
	 * Returns a package descriptor for the package with the given name.
	 * An empty string indicates the default package. Package names are
	 * dot qualified.
	 *  
	 * @param packageName package name
	 * @return an {@link IPackageDescriptor} for the package
	 */
	public static IPackageDescriptor packageDescriptor(String packageName) {
		return new PackageDescriptorImpl(packageName);
	}
	
	/**
	 * Returns a resource descriptor for the given path.
	 * 
	 * @param resource the handle to the backing {@link IResource}
	 * @return an {@link IResourceDescriptor} for the given resource
	 */
	public static IResourceDescriptor resourceDescriptor(IResource resource) {
		return new ResourceDescriptorImpl(resource);
	}
	
	/**
	 * Utility method to create a type descriptor for a type with the
	 * given fully qualified name. Package names are dot qualified and
	 * type names are '$'-qualified.
	 * 
	 * @param fullyQualifiedName
	 * @return an {@link ITypeDescriptor} for the type
	 */
	public static IReferenceTypeDescriptor typeDescriptor(String fullyQualifiedName) {
		String packageName = Util.getPackageName(fullyQualifiedName);
		String typeName = Util.getTypeName(fullyQualifiedName);
		return packageDescriptor(packageName).getType(typeName);
	}
	
	/**
	 * Utility method to create a type descriptor for a method contained within the given 
	 * type
	 * 
	 * @param typename the name of the enclosing type for the method
	 * @param name the name of the method
	 * @param signaturethe signature of the method
	 * @return an {@link IMethodDescriptor} for the method
	 */
	public static IMethodDescriptor methodDescriptor(String typename, String name, String signature) {
		IReferenceTypeDescriptor type = typeDescriptor(typename);
		return type.getMethod(name, signature);
	}
	
	/**
	 * Utility method to create a type descriptor for a field contained within the given type
	 * 
	 * @param typename the name of the enclosing type for the field
	 * @param name the name of the field
	 * @return an {@link IFieldDescriptor} for the field
	 */
	public static IFieldDescriptor fieldDescriptor(String typename , String name) {
		IReferenceTypeDescriptor type = typeDescriptor(typename);
		return type.getField(name);
	}
	
	/**
	 * Returns a scope containing all elements in the given components.
	 * 
	 * @param components API components
	 * @return search scope
	 */
	public static IApiSearchScope newScope(IApiComponent[] components) {
		SearchScope scope = new SearchScope();
		for (int i = 0; i < components.length; i++) {
			scope.addComponent(components[i]);
		}
		return scope;
	}
	
	/**
	 * Returns a scope containing the specified elements in the given component.
	 * 
	 * @param component API component
	 * @param elements elements in the component
	 * @return search scope
	 */
	public static IApiSearchScope newScope(IApiComponent component, IElementDescriptor[] elements) {
		SearchScope scope = new SearchScope();
		for (int i = 0; i < elements.length; i++) {
			scope.addElement(component, elements[i]);
		}
		return scope;
	}
	
	/**
	 * Returns a new scope containing the specified types in the given component.
	 * 
	 * @param component API component
	 * @param types reference types
	 * @return search scope
	 */
	public static IApiSearchScope newTypeScope(IApiComponent component, IReferenceTypeDescriptor[] types) {
		return new TypeScope(component, types);
	}
}

