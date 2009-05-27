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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.core.IWritable;

/**
 * Base interface for all objects that belong to the extension point
 * schema model.
 */
public interface ISchemaObject extends IAdaptable, IWritable {
	/**
	 * Property constant that will be used in the model change event
	 * when description field of this object changes.
	 */
	public static final String P_DESCRIPTION = "description"; //$NON-NLS-1$
	/**
	 * Property constant that will be used in the model change event
	 * when "name" field of this object changes.
	 */
	public static final String P_NAME = "name"; //$NON-NLS-1$

	/**
	 * Returns text associated with this schema object. Typically, it is
	 * annotation that will be used to compose the reference HTML documentation.
	 * The text may contain HTML tags.
	 */
	public String getDescription();

	/**
	 * Returns the presentation name of this schema object.
	 */
	public String getName();

	/**
	 * Returns the parent of this schema object.
	 */
	ISchemaObject getParent();

	void setParent(ISchemaObject parent);

	/**
	 * Returns the schema object to which this object belongs.
	 */
	public ISchema getSchema();
}
