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
package org.eclipse.pde.ui;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.core.resources.*;

/**
 * An interface for extension wizards. Clients should
 * implement this interface if they are plugging into
 * PDE using <samp>org.eclipse.pde.ui.newExtension</samp> extension point.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IExtensionWizard extends IBasePluginWizard {
	/**
	 * Initializes the wizard with the project of the plug-in and
	 * the model object for the plug-in manifest file. Java code and
	 * other resorces should be created in the source folder under the
	 * provided project. Changes in the plug-in manifest
	 * should be made using the APIs of the provided
	 * model. Changing the model will make the model dirty. This will
	 * show up in the UI indicating that the currently opened manifest
	 * file is modified and needs to be saved.
	 * <p>Although the wizard is launched to create an extension,
	 * there is no reason a wizard cannot create several at once.
	 *
	 * @param project the plug-in project resource where the new
	 * code and resources should go
	 * @param pluginModel the model instance that should
	 * be used to modify the plug-in manifest
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void init(IProject project, IPluginModelBase pluginModel);
}
