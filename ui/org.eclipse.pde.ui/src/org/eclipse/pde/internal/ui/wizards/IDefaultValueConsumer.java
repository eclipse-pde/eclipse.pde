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
package org.eclipse.pde.internal.ui.wizards;

import java.util.Dictionary;

/**
 * Wizards that implement this interface
 * can be initialized with the default values so that
 * their pages come preset.
 */
public interface IDefaultValueConsumer {
	/**
	 * Initializes the consumer with the values.
	 * @param values
	 */
	void init(Dictionary values);

	/**
	 * Returns the default value for the given key
	 * @param key
	 * @return the default value or <code>null</code> if not provided.
	 */
	String getDefaultValue(String key);
}
