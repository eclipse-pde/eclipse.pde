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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * An interface for extension editor wizards. Clients should implement this
 * interface if they are plugging into PDE using
 * <samp>org.eclipse.pde.ui.newExtension </samp> extension point and want to
 * register wizards for custom editing of the selected extensions and extension
 * elements in the plug-in manifest wizard.
 * 
 * @noextend This interface is not intended to be extended by clients.
 * @since 3.0
 */
public interface IExtensionEditorWizard extends IBasePluginWizard {
	/**
	 * Initializes the wizard with the project of the plug-in and the model
	 * object for the plug-in manifest file. Java code and other resorces should
	 * be created in the source folder under the provided project. Changes in
	 * the plug-in manifest should be made using the APIs of the provided model.
	 * Changing the model will make the model dirty. This will show up in the UI
	 * indicating that the currently opened manifest file is modified and needs
	 * to be saved.
	 * <p>
	 * The wizard is opened on a current selection in the extension tree. It is
	 * supposed to modify the selected element and/or its children using plug-in
	 * model APIs. The setters on the APIs will cause the model to be dirty and
	 * make the editor dirty as well. Saving the editor will commit the changes
	 * made by the wizard to the edited file.
	 * 
	 * @param project
	 *            the plug-in project resource where the new code and resources
	 *            should go
	 * @param pluginModel
	 *            the model instance that should be used to modify the plug-in
	 *            manifest
	 * @param selection
	 *            the currently selected extension or extension element in the
	 *            manifest editor extension tree
	 *  
	 */
	public void init(IProject project, IPluginModelBase pluginModel, IStructuredSelection selection);
}
