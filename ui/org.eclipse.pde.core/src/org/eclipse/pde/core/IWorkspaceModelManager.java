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
package org.eclipse.pde.core;

import org.eclipse.core.resources.IProject;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public interface IWorkspaceModelManager	extends IModelManager, IModelProvider {
	//public void fireModelsChanged(IModel[] models);
	boolean getAllEditableModelsUnused(Class modelClass);
	IModel getWorkspaceModel(IProject project);
	IPluginModelBase[] getAllModels();
	boolean isLocked();
	void removeModelProviderListener(IModelProviderListener listener);
	void reset();
}
