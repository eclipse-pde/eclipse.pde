/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import java.io.Serializable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.pde.core.IWritable;

/**
 * A base of all site model objects.
 */
public interface ISiteObject extends IWritable, IAdaptable, Serializable {
	/**
	 * A property name that will be used to notify about changes in the "label"
	 * field.
	 */
	String P_LABEL = "label"; //$NON-NLS-1$

	/**
	 * Returns the top-level site model object.
	 * 
	 * @return root feature object
	 */
	public ISite getSite();

	/**
	 * Returns the label of this feature model object'
	 * 
	 * @return feature object label
	 */
	String getLabel();

	/**
	 * Returns the site model that owns this model object.
	 * 
	 * @return the site model
	 */
	ISiteModel getModel();

	boolean isInTheModel();

	/**
	 * Returns the parent of this model object.
	 * 
	 * @return the model object parent
	 */
	public ISiteObject getParent();

	/**
	 * Sets the new label of this model object. This method may throw a
	 * CoreException if the model is not editable.
	 * 
	 * @param label
	 *            the new label
	 */
	void setLabel(String label) throws CoreException;

	/**
	 * Returns true if this object as all the required attributes set, false
	 * otherwise.
	 */
	boolean isValid();
}
