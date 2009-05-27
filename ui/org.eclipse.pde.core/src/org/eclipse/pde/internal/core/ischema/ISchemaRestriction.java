/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ischema;

/**
 * Simple schema types can optionally have a restriction
 * objects that narrows the value space for the type.
 * The restrictions introduce additional requirements
 * for the value to be considered valid for the type.
 * For example, enumeration restriction defines
 * a closed set of values that are legal for the type.
 */
public interface ISchemaRestriction extends ISchemaObject {
	/**
	 * Returns the simple type to which this restriction applies.
	 * @return simple type to which this restriciton applies
	 */
	public ISchemaSimpleType getBaseType();

	/**
	 * Returns children of this restriction. Actual types
	 * of the children depend on the restriction itself.
	 * @return restriction children objects
	 */
	public Object[] getChildren();

	/**
	 * Tests if the provided value belongs to
	 * the value set defined by this restriction.
	 * @return true if the provided value
	 * is valid for this restriction
	 */
	boolean isValueValid(Object value);

	/**
	 * Associates this restriction with the simple type object.
	 *
	 * @param baseType type object that owns this restriction
	 */
	public void setBaseType(ISchemaSimpleType baseType);
}
