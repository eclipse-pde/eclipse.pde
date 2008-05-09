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


/**
 * Describes an API component required by another component.
 * 
 * @since 1.0.0
 */
public interface IRequiredComponentDescription {
	
	/**
	 * Returns the symbolic name of the required component.
	 * 
	 * @return symbolic name of the required component
	 */
	public String getId();
	
	/**
	 * Returns a range of compatible versions of the required component.
	 * 
	 * @return compatible version range
	 */
	public IVersionRange getVersionRange();
	
	/**
	 * Returns whether the required component is optional.
	 * 
	 * @return whether the required component is optional.
	 */
	public boolean isOptional();
	
	/**
	 * Returns whether the required component is re-exported by the declaring component.
	 * 
	 * @return whether the required component is re-exported by the declaring component
	 */
	public boolean isExported();
}
