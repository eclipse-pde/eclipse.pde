/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

import org.eclipse.core.runtime.*;


public interface IManifestHeader {
	
	/**
	 * Returns the header key
	 */
	String getKey();	

	/**
	 * Returns the header value
	 */	
	String getValue();
	
	/**
	 * Sets the name of the header
	 * This method will throw a CoreException if the model
	 * is not editable.
	 *
	 * @param key the header key
	 */
	void setKey(String key) throws CoreException;
	
	/**
	 * Sets the value of the header
	 * This method will throw a CoreException if the model
	 * is not editable.
	 *
	 * @param value the header value
	 */
	void setValue(String value);
}
