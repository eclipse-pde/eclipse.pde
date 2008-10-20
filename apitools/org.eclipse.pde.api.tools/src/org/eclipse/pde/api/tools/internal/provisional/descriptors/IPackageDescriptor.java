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



/**
 * Description of a package.
 * <p>
 * A package has no parent.
 * </p>
 * @since 1.0.0
 */
public interface IPackageDescriptor extends IElementDescriptor {
	
	/**
	 * Returns this package's name. Package names are dot qualified.
	 * 
	 * @return package name
	 */
	public String getName();
	
	/**
	 * Returns a descriptor for a type in this package with the given name. The given
	 * name is not package qualified. Inner types are '$'-separated.
	 * 
	 * @param typeQualifiedName type qualified name
	 * @return type descriptor
	 */
	public IReferenceTypeDescriptor getType(String typeQualifiedName);
	
	/**
	 * Returns a descriptor for a type in this package with the given name. The given
	 * name is not package qualified. Inner types are '$'-separated.
	 * <p>
	 * Extra type signature information may be provided for generic types.
	 * </p>
	 * 
	 * @param typeQualifiedName type qualified name
	 * @param signature type signature information or <code>null</code>
	 * @return type descriptor
	 */
	public IReferenceTypeDescriptor getType(String typeQualifiedName, String signature);	
	
}
