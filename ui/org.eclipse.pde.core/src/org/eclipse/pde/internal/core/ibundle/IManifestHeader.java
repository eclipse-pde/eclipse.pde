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
package org.eclipse.pde.internal.core.ibundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.text.IDocumentKey;

public interface IManifestHeader extends IDocumentKey {

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

	/**
	 * Forces the header to update its value based on the current components,
	 * attributes and directives it contains.
	 * 
	 */
	void update();

	/**
	 * Forces the header to update its value based on the current components,
	 * attributes and directives it contains.
	 * @param notify if true the model will be notified of the "changes"
	 */
	void update(boolean notify);
}
