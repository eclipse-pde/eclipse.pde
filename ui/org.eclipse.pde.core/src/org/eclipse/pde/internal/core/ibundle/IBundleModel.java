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
import java.util.Dictionary;
import org.eclipse.pde.core.*;
/**
 * This model is created from the "META-INF/MANIFEST.MF" file that represents
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
	 * @return a dictionary containing the manifest headers
	 */
	Dictionary getManifest();
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
	
	/**
	 * Returns a factory object that should be used
	 * to create new instances of the model objects.
	 */
	public IBundleModelFactory getFactory();
}