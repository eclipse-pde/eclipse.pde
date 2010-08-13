/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import java.net.URL;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.build.IBuildModel;

/**
 * This type of model is created by parsing the manifest file.
 * It serves as a base interface for both plug-in and
 * fragment models by holding data common to both.
 * If the file is a workspace resource, it will be
 * available as the underlying resource of the model.
 * The model may be read-only or editable.
 * It will also make a reference to the build.properties
 * model when created. The reference will be of the
 * same type as the model itself: if the model is
 * editable, it will attempt to obtain an exclusive
 * editable copy of build.properties model.
 * <p>
 * The plug-in model can be disabled. Disabling the
 * model will not change its data. Users of the
 * model will have to decide if the disabled state
 * if of any importance to them or not.
 * <p>
 * The model is capable of notifying listeners
 * about changes. An attempt to change a read-only
 * model will result in a CoreException.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IPluginModelBase extends ISharedExtensionsModel, IModelChangeProvider {
	/**
	 * Creates and return a top-level plugin model object
	 *  
	 * @return a top-level model object representing a plug-in or a fragment.
	 */
	IPluginBase createPluginBase();

	/**
	 * Returns an associated build.properties model
	 * that works in conjunction with this model.
	 * <p>
	 * This method always returns <code>null</code>
	 * </p>
	 * @return <code>null</code>
	 * @deprecated This method has always returned <code>null</code>. 
	 *   Since 3.7, use {@link PluginRegistry#createBuildModel(IPluginModelBase)} instead.
	 */
	IBuildModel getBuildModel();

	/**
	 * Returns a top-level model object. Equivalent to
	 * calling <pre>getPluginBase(true)</pre>.
	 * 
	 * @return a top-level model object representing a plug-in or a fragment.
	 */
	IPluginBase getPluginBase();

	/**
	 * Returns a top-level model object.
	 * 
	 * @param createIfMissing if true, root model object will
	 * be created if not defined.
	 * 
	 * @return a top-level model object
	 */
	IPluginBase getPluginBase(boolean createIfMissing);

	/**
	 * Returns </samp>true</samp> if this model is currently enabled.
	 *
	 *@return true if the model is enabled
	 */
	boolean isEnabled();

	/**
	 * Tests if this model is for the plug-in fragment.
	 * 
	 * @return <code>true</code> if the model is for the fragment,
	 * <code>false</code> otherwise.
	 */
	boolean isFragmentModel();

	/**
	 * Sets the enable state of the model.
	 *
	 * @param enabled the new enable state
	 */
	void setEnabled(boolean enabled);

	/**
	 * Returns the factory that can be used to
	 * create new objects for this model
	 * @return the plug-in model factory
	 */
	IPluginModelFactory getPluginFactory();

	/**
	 * Returns the location where property file containing
	 * translations of names in this model can be found.
	 * 
	 * @return the location of the property file with translations
	 */
	URL getNLLookupLocation();

	/**
	 * Returns the bundle description of the plug-in
	 * in case the plug-in uses the new OSGi bundle layout. 
	 * 
	 * @return bundle description if this is an OSGi plug-in,
	 * or <code>null</code> if the plug-in is in a classic
	 * format.
	 * 
	 * @since 3.0
	 */
	BundleDescription getBundleDescription();

	/**
	 * Associates the bundle description of the plug-in
	 * with this model in case the plug-in uses the new
	 * OSGi bundle layout.
	 * 
	 * @param description bundle description to associate
	 * with this model
	 * 
	 * @since 3.0
	 */
	void setBundleDescription(BundleDescription description);
}
