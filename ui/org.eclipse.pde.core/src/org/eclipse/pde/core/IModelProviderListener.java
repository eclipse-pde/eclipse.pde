/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
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
