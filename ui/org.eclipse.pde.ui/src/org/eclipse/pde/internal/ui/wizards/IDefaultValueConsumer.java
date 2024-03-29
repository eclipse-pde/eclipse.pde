/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
	 */
	void init(Dictionary<String, String> values);

	/**
	 * Returns the default value for the given key
	 * @return the default value or <code>null</code> if not provided.
	 */
	String getDefaultValue(String key);
}
