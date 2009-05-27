/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class PluginReference extends PlatformObject {
	private String fId;

	private transient IPlugin fPlugin;

	public PluginReference() {
	}

	public PluginReference(String id) {
		fId = id;
	}

	public PluginReference(IPlugin plugin) {
		fId = plugin.getId();
		fPlugin = plugin;
	}

	public String getId() {
		return fId;
	}

	public IPlugin getPlugin() {
		if (fPlugin == null && fId != null) {
			IPluginModelBase model = PluginRegistry.findModel(fId);
			fPlugin = model instanceof IPluginModel ? ((IPluginModel) model).getPlugin() : null;
		}
		return fPlugin;
	}

	public String toString() {
		if (fPlugin != null) {
			return fPlugin.getTranslatedName();
		}
		return fId != null ? fId : "?"; //$NON-NLS-1$
	}

	public boolean isResolved() {
		return getPlugin() != null;
	}

	/**
	 * @param plugin
	 */
	public void reconnect(IPluginModelBase model) {
		// Transient Field:  Plugin
		IPlugin plugin = null;
		if (model instanceof IPluginModel) {
			plugin = ((IPluginModel) model).getPlugin();
		}
		// It could also be an IFragmentModel
		// Having IPlugin has an instance variable for both models does not
		// make sense
		// If we have a fragment model, leave the plugin as null
		fPlugin = plugin;
	}

}
