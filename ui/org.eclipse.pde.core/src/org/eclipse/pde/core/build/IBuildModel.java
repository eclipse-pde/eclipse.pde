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
package org.eclipse.pde.core.build;

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * This model is created from the <code>build.properties</code> file
 * that defines what source folders in the plug-in are
 * to be used to build require plug-in Jars.
 * <p>
 * If this model is editable, isEditable() will return
 * true and the model instance will implement IEditable
 * interface. The model is capable of providing
 * change notification for the registered listeners.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBuildModel extends IModel, IModelChangeProvider {
	/**
	 * Returns the top-level model object of this model.
	 *
	 * @return a build.properties top-level model object
	 */
	IBuild getBuild();

	/**
	 * Returns the factory that should be used
	 * to create new instance of model objects.
	 * @return the build.properties model factory
	 */
	IBuildModelFactory getFactory();

	/**
	 * Returns the location of the file
	 * used to create the model.
	 *
	 * @return the location of the build.properties file
	 * or <samp>null</samp> if the file
	 * is in a workspace.
	 */
	String getInstallLocation();
}
