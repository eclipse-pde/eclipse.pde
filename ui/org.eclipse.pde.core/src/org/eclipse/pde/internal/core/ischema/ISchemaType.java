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
 * A base type interface. Schema type is associated
 * with schema elements and attributes to define
 * their grammar and/or valid value space.
 * For simple types, 'getName()' method
 * returns name of the type that defines
 * initial value space (for example, "string", "boolean" etc.).
 */
public interface ISchemaType {
	/**
	 * Returns the logical name of this type.
	 * @return name of the type
	 */
	public String getName();

	/**
	 * Returns the schema object in which this type is defined.
	 * @return the top-level schema object
	 */
	public ISchema getSchema();

	public void setSchema(ISchema schema);
}
