/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738 
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

import org.eclipse.core.resources.IResource;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * Represents the DS Text Model
 * 
 * @since 3.4
 * @see IDSComponent
 * @see IDSFactory
 */
public interface IDSModel extends IModelChangeProvider, IModel {

	/**
	 * Return the factory object of this model
	 * 
	 * @return existing IDSDocumentfactory object, or create a new object for
	 *         the first time it is called
	 */
	public abstract IDSDocumentFactory getFactory();

	/**
	 * Return the root component element of this model
	 * 
	 * @return existing IDSComponent object, or create a new object for the
	 *         first time it is called
	 */
	public abstract IDSComponent getDSComponent();

	/**
	 * Sets a workspace resource that this model is created from. Load/reload
	 * operations are not directly connected with the resource (although they
	 * can be). In some cases, models will load from a buffer (an editor
	 * document) rather than a resource. However, the buffer will eventually be
	 * synced up with this resource.
	 * <p>
	 * With the caveat of stepped loading, all other properties of the
	 * underlying resource could be used directly (path, project etc.).
	 * 
	 * @param resource
	 *            a workspace resource (file) that this model is associated
	 *            with.
	 */
	public abstract void setUnderlyingResource(IResource resource);

	/**
	 * Saves the model into the underlying resource
	 * 
	 */
	public abstract void save();

}
