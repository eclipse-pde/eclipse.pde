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
package org.eclipse.pde.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * An interface for extension wizards. Clients should implement this interface
 * if they are plugging into PDE using <samp>org.eclipse.pde.ui.newExtension
 * </samp> extension point.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IExtensionWizard extends IBasePluginWizard {
	/**
	 * Initializes the wizard with the project of the plug-in and the model
	 * object for the plug-in manifest file. Java code and other resorces should
	 * be created in the source folder under the provided project. Changes in
	 * the plug-in manifest should be made using the APIs of the provided model.
	 * Changing the model will make the model dirty. This will show up in the UI
	 * indicating that the currently opened manifest file is modified and needs
	 * to be saved.
	 * <p>
	 * Although the wizard is launched to create an extension, there is no
	 * reason a wizard cannot create several at once.
	 * 
	 * @param project
	 *            the plug-in project resource where the new code and resources
	 *            should go
	 * @param pluginModel
	 *            the model instance that should be used to modify the plug-in
	 *            manifest
	 */
	public void init(IProject project, IPluginModelBase pluginModel);
}
