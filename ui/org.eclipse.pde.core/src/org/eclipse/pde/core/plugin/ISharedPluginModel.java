/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.plugin;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

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
public interface ISharedPluginModel extends IModel, IModelChangeProvider {
	/**
	 * Returns a factory object that should be used
	 * to create new instances of the model objects.
	 * 
	 * @return factory object to create new model objects 
	 */
	IExtensionsModelFactory getFactory();

	/**
	 * Returns a location of the file that was used
	 * to create this model.  The location can be that 
	 * of a directory or that of a JAR file.
	 *
	 * @return a location of the external model, or
	 * <samp>null</samp> if the model is not created 
	 * from a resource or a file in the file system.
	 */
	String getInstallLocation();

}
