/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ibundle;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * This model is created from the <code>META-INF/MANIFEST.MF</code> file that represents
 * the plug-in manifest in Eclipse 3.0 OSGi format.
 * <p>
 * If this model is editable, isEditable() will return true and the model
 * instance will implement IEditable interface. The model is capable of
 * providing change notification for the registered listeners.
 * 
 * @since 3.0
 */
public interface IBundleModel extends IModel, IModelChangeProvider {
	/**
	 * Returns the top-level model object of this model.
	 * 
	 * @return an object containing the manifest headers
	 */
	IBundle getBundle();

	/**
	 * Returns the location of the file used to create the model.
	 * 
	 * @return the location of the manifest file or <samp>null </samp> if the
	 *         file is in a workspace.
	 */
	public String getInstallLocation();

	/**
	 * Tests whether this is a model of a fragment bundle.
	 * 
	 * @return <code>true</code> if this is a fragment model,
	 *         <code>false</code> otherwise.
	 */
	public boolean isFragmentModel();

	public IBundleModelFactory getFactory();

}
