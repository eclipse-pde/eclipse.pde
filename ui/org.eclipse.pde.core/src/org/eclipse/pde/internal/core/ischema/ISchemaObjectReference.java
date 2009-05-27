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
 * Classes that implement this interface store a reference
 * to a schema object defined elsewhere.
 */
public interface ISchemaObjectReference {
	/**
	 * Returns a name of this reference.
	 * @return reference object name
	 */
	public String getName();

	/**
	 * Returns a schema object that is referenced by this object.
	 * @return referenced schema object
	 */
	public ISchemaObject getReferencedObject();

	/**
	 * Returns a real Java class of the referenced object.
	 * @return Java class of the referenced object.
	 */
	public Class getReferencedObjectClass();

	/**
	 * Associates this reference with a schema object.
	 * @param referencedObject associates this reference with the object it references
	 */
	public void setReferencedObject(ISchemaObject referencedObject);
}
