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
package org.eclipse.pde.core.osgi.bundle;

import org.eclipse.pde.core.*;
/**
 * This model is created from the "plugin.jars" file
 * that defines what source folders in the plug-in are
 * to be used to build require plug-in Jars.
 * <p>
 * If this model is editable, isEditable() will return
 * true and the model instance will implement IEditable
 * interface. The model is capable of providing
 * change notification for the registered listeners.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IBundleModel extends IModel, IModelChangeProvider {
	/**
	 * Returns the top-level model object of this model.
	 *
	 * @return a build.properties top-level model object
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IBundle getBundle();
	/**
	 * Returns the location of the file
	 * used to create the model.
	 *
	 * @return the location of the build.properties file
	 * or <samp>null</samp> if the file
	 * is in a workspace.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getInstallLocation();
	
	public boolean isFragmentModel();
}
