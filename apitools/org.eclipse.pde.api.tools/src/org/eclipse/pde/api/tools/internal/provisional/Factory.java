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

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.ApiProfile;
import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.descriptors.PackageDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.PrimitiveDescriptorImpl;
import org.eclipse.pde.api.tools.internal.descriptors.ResourceDescriptorImpl;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPrimitiveTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IResourceDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchCriteria;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchEngine;
import org.eclipse.pde.api.tools.internal.provisional.search.IApiSearchScope;
import org.eclipse.pde.api.tools.internal.search.SearchCriteria;
import org.eclipse.pde.api.tools.internal.search.SearchEngine;
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
	 * Creates a new empty API profile with the given name. Its execution
	 * environment will be automatically resolved when components are added
	 * to it.
	 * <p>
	 * Note, a profile can only automatically resolve an execution environment
	 * when it is created within an Eclipse SDK. A profile created in a non-OSGi
	 * environment must have its execution environment specified at creation
	 * time.
	 * </p>
	 *
	 * @param name profile name
	 */
	public static IApiProfile newApiProfile(String name) {
		return new ApiProfile(name);
	}
	
	/**
	 * Creates a new empty API profile with the specified execution environment.
	 * <p>
	 * The execution environment description file describes how an execution 
	 * environment profile is provided by or mapped to a specific JRE. The format for
	 * this file is described here
	 * <code>http://wiki.eclipse.org/index.php/Execution_Environment_Descriptions</code>.
	 * </p>
	 * @param name profile name
	 * @param eeDescription execution environment description file
	 * @return API profile
	 * @throws CoreException if unable to create a new profile with the specified attributes 
	 */
	public static IApiProfile newApiProfile(String name, File eeDescription) throws CoreException {
		return new ApiProfile(name, eeDescription);
	}

	/**
	 * Returns a type descriptor for the primitive boolean type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a boolean type
	 */
	public static IPrimitiveTypeDescriptor booleanType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_BOOLEAN);
	}
	
	/**
	 * Returns a type descriptor for the primitive byte type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a byte type
	 */
	public static IPrimitiveTypeDescriptor byteType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_BYTE);
	}	
	
	/**
	 * Returns a type descriptor for the primitive char type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a char type
	 */
	public static IPrimitiveTypeDescriptor charType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_CHAR);
	}	
	
	/**
	 * Returns a type descriptor for the primitive double type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a double type
	 */
	public static IPrimitiveTypeDescriptor doubleType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_DOUBLE);
	}	
	
	/**
	 * Returns a type descriptor for the primitive float type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a float type
	 */
	public static IPrimitiveTypeDescriptor floatType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_FLOAT);
	}	
	
	/**
	 * Returns a type descriptor for the primitive int type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for an int type
	 */
	public static IPrimitiveTypeDescriptor intType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_INT);
	}
	
	/**
	 * Returns a type descriptor for the primitive long type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a long type
	 */
	public static IPrimitiveTypeDescriptor longType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_LONG);
	}	
	
	/**
	 * Returns a type descriptor for the primitive short type.
	 * 
	 * @return an {@link IPrimitiveTypeDescriptor} for a short type
	 */
	public static IPrimitiveTypeDescriptor shortType() {
		return new PrimitiveDescriptorImpl(Signature.SIG_SHORT);
	}
	
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
	
	/**
	 * Creates and returns a new search engine.
	 * 
	 * @return search engine
	 */
	public static IApiSearchEngine newSearchEngine() {
		return new SearchEngine();
	}
	
	/**
	 * Creates and returns an API profile from the given input stream. The
	 * contents of the stream must have been created with
	 * {@link IApiProfile#writeProfileDescription(java.io.OutputStream)}.
	 * 
	 * @param descriptionStream profile description
	 * @return API profile
	 * @exception CoreException if unable to create a profile from the stream
	 */
	public static IApiProfile createProfile(InputStream descriptionStream) throws CoreException {
		return ApiProfileManager.restoreProfile(descriptionStream);
	}
	
	/**
	 * Creates and returns a new search criteria.
	 * 
	 * @return new search criteria
	 */
	public static IApiSearchCriteria newSearchCriteria() {
		return new SearchCriteria();
	}
}

