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

import java.util.Vector;

import org.eclipse.pde.core.plugin.*;

/**
 */
public interface IExternalModelManager extends IModelManager {
	void addModelProviderListener(IModelProviderListener listener);
	void removeModelProviderListener(IModelProviderListener listener);
	void fireModelProviderEvent(IModelProviderEvent e);

	IPluginExtensionPoint findExtensionPoint(String fullID);
	IPlugin findPlugin(String id);
	IFragmentModel[] getFragmentModels();
	IPluginModel[] getPluginModels();
	IPluginModelBase[] getAllEnabledModels();
	
	void enableAll();
	boolean hasEnabledModels();

	void resetModels(Vector models, Vector fmodels);
}
