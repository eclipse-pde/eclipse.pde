/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

/**
 * Classes that implement this interface will have the ability
 * to contriubte to an editors validator.
 *
 */
public interface IEditorValidationProvider {
	/**
	 * Set a validator assiciated with this object
	 * @param validator
	 */
	public void setValidator(IEditorValidator validator);
	/**
	 * Return the validator currently associated with this object
	 * @return the IEditorValidator associated with this object
	 */
	public IEditorValidator getValidator() ;
	/**
	 * Validate this object.  This method will have to be called
	 * by the object itself, preferrably during the modification of
	 * that object's field.
	 *
	 */
	public void validate();
	/**
	 * Returns the input value of the object
	 * @return value
	 */
	public String getProviderValue();
	/**
	 * Returns the details on the contents of this provider.
	 * eg. label
	 * @return details/information
	 */
	public String getProviderDescription();
}
