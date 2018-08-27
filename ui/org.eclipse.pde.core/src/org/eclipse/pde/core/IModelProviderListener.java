/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

/**
 * Classes should implement this interface in order to be able to register as
 * model provider listeners. They will be notified about events such as models
 * being added or removed. These changes are typically caused by the changes in
 * the workspace when models are built on top of workspace resources.
 *
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IModelProviderListener {
	/**
	 * Notifies the listener that models have been changed in the model
	 * provider.
	 *
	 * @param event
	 *            the event that specifies the type of change
	 */
	public void modelsChanged(IModelProviderEvent event);
}
