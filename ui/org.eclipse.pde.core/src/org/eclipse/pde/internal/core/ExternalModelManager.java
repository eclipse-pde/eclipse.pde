/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * External models are models from the target platform.  This manager stores a list of plugin models 
 * created in the PDE state.  If the state or active target platform changes this manager must be updated by 
 * calling {@link #initializeModels(IPluginModelBase[])} with the new models from the state
 */
public class ExternalModelManager extends AbstractModelManager {

	private IPluginModelBase[] fModels = new IPluginModelBase[0];

	/**
	 * Returns all models this manager knows about.
	 * @return list of models, possibly empty
	 */
	protected IPluginModelBase[] getAllModels() {
		return fModels;
	}

	/**
	 * Updates the models to be stored in this manager
	 * @param models new set of models
	 */
	protected void modelsChanged(IPluginModelBase[] models) {
		// As of 3.6, the target models are always considered enabled
		if (models == null) {
			fModels = new IPluginModelBase[0];
		} else {
			fModels = models;
			for (int i = 0; i < models.length; i++) {
				models[i].setEnabled(true);
			}
		}
	}

}
