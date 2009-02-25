/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import org.eclipse.core.resources.IFile;

/**
 * Classes that implement this interface are responsible for holding a table of
 * models associated with the underlying objects. They have several
 * responsibilities:
 * <ul>
 * <li>To hold model objects in one place
 * <li>To allow requesters to connect to the models or to disconnect from them.
 * <li>To notify interested parties when models are added and removed.
 * </ul>
 * Model providers are responsible for listening to the workspace, updating
 * models whose underlying resources have been updated, and removing them from
 * the table when those resources have been deleted.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IModelProvider {
	/**
	 * Registers a listener that will be notified about changes in the managed
	 * models.
	 * 
	 * @param listener
	 *            the listener that will be registered
	 */
	void addModelProviderListener(IModelProviderListener listener);

	/**
	 * Returns the model for the provided file resource.
	 * 
	 * @param file
	 *            the file resource we need the model for
	 * @return the object that represents a structured representation of the
	 *         file content
	 */
	public IModel getModel(IFile file);

	/**
	 * Deregisters a listener from notification.
	 * 
	 * @param listener
	 *            the listener to be deregistered
	 */
	void removeModelProviderListener(IModelProviderListener listener);
}
