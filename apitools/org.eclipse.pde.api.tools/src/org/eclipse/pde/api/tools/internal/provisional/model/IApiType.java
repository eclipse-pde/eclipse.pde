/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.model;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


/**
 * Provides the structure of a type - for example, a class or interface along
 * with its methods and fields.
 * 
 * @see IApiMethod
 * @see IApiField
 * 
 * @since 1.1
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IApiType extends IApiMember {

	/**
	 * Returns the {@link IApiMethod} that encloses this type, iff this type is
	 * a local type (class defined in a method body). A call to this method will
	 * load the enclosing type to find the enclosing method.
	 * 
	 * @return the {@link IApiMethod} that encloses this type or <code>null</code>
	 */
	public IApiMethod getEnclosingMethod();
	
	/**
	 * Returns the field with the specified name
	 * in this type (for example, <code>"bar"</code>), or <code>null</code>
	 * if none.
	 *
	 * @param name the given name
	 * @return the field with the specified name in this type or <code>null</code>
	 */
	public IApiField getField(String name);
	
	/**
	 * Returns the fields declared by this type.
	 *
	 * @return the fields declared by this type
	 */
	public IApiField[] getFields();
	
	/**
	 * Returns the method with the specified name and signature
	 * in this type. For example, <code>"foo", "(Ljava.lang.String;I)V"</code>,
	 * retrieves the method <code>foo(String, int)</code>. To retrieve a constructor,
	 * <code>"<init>"</code> must specified must be as method name.
	 *
	 * @param name method name
	 * @param parameterTypeSignatures parameter types
	 * @return the method with the specified name and parameter types in this type
	 *  or <code>null</code>
	 */
	public IApiMethod getMethod(String name, String signature);

	/**
	 * Returns the methods and constructors declared by this type.
	 *
	 * @return the methods and constructors declared by this type
	 */
	public IApiMethod[] getMethods();
	
	/**
	 * Returns whether this type is an anonymous type.
	 *
	 * @return true if this type is an anonymous type, false otherwise
	 */
	public boolean isAnonymous();
	
	/**
	 * Returns whether this type is a local type.
	 * 
	 * @return whether this type is a local type
	 */
	public boolean isLocal();

	/**
	 * Returns whether this type represents a class.
	 * <p>
	 * Note that a class can neither be an interface, an
	 * enumeration class, nor an annotation type.
	 * </p>
	 *
	 * @return true if this type represents a class, false otherwise
	 */
	public boolean isClass();

	/**
	 * Returns whether this type represents an enumeration class.
	 * <p>
	 * Note that an enumeration class can neither be a class,
	 * an interface, nor an annotation type.
	 * </p>
	 * 
	 * @return true if this type represents an enumeration class,
	 * false otherwise
	 */
	public boolean isEnum();

	/**
	 * Returns whether this type represents an interface.
	 * <p>
	 * Note that an interface can also be an annotation type,
	 * but it can neither be a class nor an enumeration class.
	 * </p>
	 *
	 * @return true if this type represents an interface, false otherwise
	 */
	public boolean isInterface();

	/**
	 * Returns whether this type represents an annotation type.
	 * <p>
	 * Note that an annotation type is also an interface,
	 * but it can neither be a class nor an enumeration class.
	 * </p>
	 *
	 * @return true if this type represents an annotation type,
	 * false otherwise
	 */
	public boolean isAnnotation();	
	
	/**
	 * Returns whether this type is a member type.
	 * 
	 * @return whether this type is a member type
	 */
	public boolean isMemberType();
	
	/**
	 * Returns the fully qualified name of this type's superclass, or <code>null</code>
	 * if none.
	 * <p>
	 * For interfaces, the superclass name is always <code>"java.lang.Object"</code>.
	 * For anonymous types, the superclass name is the name appearing after the 'new' keyword'.
	 * If the superclass is a parameterized type, the string
	 * may include its type arguments enclosed in "&lt;&gt;".
	 * If the returned string is needed for anything other than display
	 * purposes, use {@link #getSuperclassTypeSignature()} which returns
	 * a structured type signature string containing more precise information.
	 * </p>
	 *
	 * @return the name of this type's superclass or <code>null</code>
	 */
	public String getSuperclassName();
	
	/**
	 * Returns the superclass of this class or <code>null</code> if none. For interfaces,
	 * java.lang.Object is returned.
	 * 
	 * @return superclass or <code>null</code>
	 * @throws CoreException if unable to retrieve the superclass
	 */
	public IApiType getSuperclass() throws CoreException;

	/**
	 * Returns the names of interfaces that this type implements or extends
	 * or <code>null</code> if none.
	 * <p>
	 * For classes, this gives the interfaces that this class implements.
	 * For interfaces, this gives the interfaces that this interface extends.
	 * Null is returned if this type does not implement or extend any interfaces.
	 * </p>
	 *
	 * @return  the names of interfaces that this type implements or extends,
	 * or <code>null</code> if none
	 */
	public String[] getSuperInterfaceNames();	
	
	/**
	 * Returns the resolved super interfaces that this type implements or extends,
	 * or an empty collection if none.
	 * 
	 * @return resolved super interfaces
	 * @throws CoreException if unable to retrieve super interfaces
	 */
	public IApiType[] getSuperInterfaces() throws CoreException;
	
	/**
	 * Returns member types contained in this type.
	 *  
	 * @return member types, possibly an empty array
	 * @exception CoreException if unable to retrieve member types
	 */
	public IApiType[] getMemberTypes() throws CoreException;
	
	/**
	 * Returns the member type with the given simple name or <code>null</code> if none.
	 * 
	 * @param simpleName simple name
	 * @return member type or <code>null</code>
	 * @exception CoreException if unable to retrieve the type
	 */
	public IApiType getMemberType(String simpleName) throws CoreException;
	
	/**
	 * Returns the simple (unqualified) name for this type.
	 * 
	 * @return unqualified name
	 */
	public String getSimpleName();
	
	/**
	 * Returns the {@link IApiTypeRoot} that this type is defined in
	 * 
	 * @return the {@link IApiTypeRoot} this type is defined in
	 */
	public IApiTypeRoot getTypeRoot();
	
	/**
	 * Extracts and returns all references made from this type of the specified kind.
	 * The list contains instances of {@link IReference}.
	 *  
	 * @param referenceMask kinds of references to extract/search for as described by
	 *  {@link org.eclipse.pde.api.tools.internal.provisional.search.ReferenceModifiers}
	 * @param monitor progress monitor or <code>null</code>
	 * @return extracted {@link IReference}s, possibly an empty collection
	 * @throws CoreException if this type does not exist or an exception occurs reading
	 * 	underlying storage
	 */
	public List extractReferences(int referenceMask, IProgressMonitor monitor) throws CoreException;
}
