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
package org.eclipse.pde.api.tools.internal.provisional.descriptors;

import org.eclipse.jdt.core.Flags;



/**
 * Description of a class, interface, or enum.
 * <p>
 * A type has either a package or enclosing type for a parent.
 * </p>
 * <p>
 * Package segments of a type name are dot-separated 
 * </p>
 * @since 1.0.0
 */
public interface IReferenceTypeDescriptor extends IMemberDescriptor, ITypeDescriptor {
	
	/**
	 * Returns this type's fully qualified name. Package names are dot qualified
	 * and inner types are '$'-separated.
	 * 
	 * @return type name
	 */
	public String getQualifiedName();	
	
	/**
	 * Returns type signature information about this type or <code>null</code>
	 * if none. May contain extra information for parameterized types.
	 * 
	 * @return type signature information for this type or <code>null</code>
	 */
	public String getGenericSignature();
		
	/**
	 * Returns a descriptor for the package this type is contained in.
	 * 
	 * @return package descriptor
	 */
	public IPackageDescriptor getPackage();
	
	/**
	 * Returns a descriptor for a member type.
	 * 
	 * @param simpleName simple type name
	 * @return type descriptor
	 */
	public IReferenceTypeDescriptor getType(String simpleName);
	
	/**
	 * Returns a descriptor for a member type with the given modifiers.
	 * 
	 * @param simpleName simple type name
	 * @param modifiers modifiers define by {@link Flags}
	 * @return type descriptor
	 */
	public IReferenceTypeDescriptor getType(String simpleName, int modifiers);	
	
	/**
	 * Returns a descriptor for a field with the given name in this type.
	 * 
	 * @param name field name
	 * @return field descriptor
	 * 
	 */
	public IFieldDescriptor getField(String name);
	
	/**
	 * Returns a descriptor for a field with the given name in this type with the
	 * specified modifiers.
	 * 
	 * @param name field name
	 * @param modifiers modifiers
	 * @return field descriptor
	 * 
	 * 
	 */
	public IFieldDescriptor getField(String name, int modifiers);	
	
	/**
	 * Returns a descriptor for a non-synthetic method with the given
	 * name and signature in this type.
	 * 
	 * @param name method name
	 * @param signature method signature
	 * @return method descriptor
	 */
	public IMethodDescriptor getMethod(String name, String signature);
	
	/**
	 * Returns a descriptor for a method with the given name
	 * and signature in this type.
	 * 
	 * @param name method name
	 * @param signature method signature
	 * @param modifiers member modifiers
	 * @return method descriptor
	 */
	public IMethodDescriptor getMethod(String name, String signature, int modifiers);		
	
	/**
	 * Returns an array type descriptor of this type with the specified number
	 * of dimensions.
	 * 
	 * @param dimensions number of dimensions in the array
	 * @return array type descriptor
	 */
	public IArrayTypeDescriptor getArray(int dimensions);
	
	/**
	 * Returns whether this type describes an anonymous inner
	 * type.
	 * 
	 * @return whether this type describes an anonymous inner
	 * type
	 */
	public boolean isAnonymous();
	
}
