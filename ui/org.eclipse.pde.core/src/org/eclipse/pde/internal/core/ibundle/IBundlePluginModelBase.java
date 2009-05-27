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

import org.eclipse.pde.core.IEditable;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;

/**
 * An adapter of the pre-3.0 plug-in model base interface that is capable of
 * maintaining the predictable facade when dealing with plug-in with OSGi
 * manifest files. The goal is to use the same adapter interface with the
 * manifest coming and going and transparently switch between
 * META-INF/MANIFEST.MF and plugin.xml/fragment.xml.
 * 
 * @since 3.0
 */
public interface IBundlePluginModelBase extends IPluginModelBase, IEditable {
	/**
	 * Returns the underlying OSGi bundle model object if bundle manifest is
	 * present.
	 * 
	 * @return OSGi bundle model or <code>null</code> if bundle manifest is
	 *         not present.
	 */
	IBundleModel getBundleModel();

	/**
	 * Returns the model that is responsible for tracking extensions and
	 * extension points. Typically this content is stored in plugin.xml file.
	 * 
	 * @return extensions model or <code>null</code> if not present.
	 */
	ISharedExtensionsModel getExtensionsModel();

	/**
	 * Sets the bundle manifest model for this adapter. All calls related to
	 * data that is normally stored in this model (e.g. plug-in ID, plug-in
	 * name, provider name etc.) will be delegated to it if not
	 * <code>null</code>.
	 * 
	 * @param bundleModel
	 *            the bundle model to use in this adapter or <code>null</code>
	 *            if there is no bundle model.
	 */
	void setBundleModel(IBundleModel bundleModel);

	/**
	 * Sets the extensions model for this adapter. All the calls related to
	 * extensions and extension points will be delegated to this model if not
	 * <code>null</code>.
	 * 
	 * @param extensionsModel
	 *            the model that stores extensions and extension points
	 */
	void setExtensionsModel(ISharedExtensionsModel extensionsModel);

	/**
	 * Factory method for creating a new import object. This is important for
	 * maintaining the adapter because <code>IPluginBase</code> returns an
	 * array of <code>IPluginImport</code> objects for dependency information.
	 * 
	 * @return a newly created import object
	 */
	IPluginImport createImport();

	/**
	 * Factory method for creating a new runtime object. This is important for
	 * maintaining the adapter because <code>IPluginBase</code> returns an
	 * array of <code>IPluginLibrary</code> objects for runtime information.
	 * 
	 * @return a newly created plug-in library object
	 */
	IPluginLibrary createLibrary();

	/**
	 * Saves the adapter by delegating the operation to the underlying models
	 * that need saving.
	 */
	void save();

	/**
	 * Returns the bundle localization
	 * @return the bundle localization
	 */
	String getBundleLocalization();
}
