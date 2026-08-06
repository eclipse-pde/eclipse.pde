/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
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
		// Only an explicitly assigned plug-in is cached. Looking the id up on
		// every call keeps isResolved() in sync with the current target
		// platform, which the error decorations in the editor rely on.
		if (fPlugin != null || fId == null) {
			return fPlugin;
		}
		IPluginModelBase model = findModel();
		return model instanceof IPluginModel i ? i.getPlugin() : null;
	}

	protected IPluginModelBase findModel() {
		return PluginRegistry.findModel(fId);
	}

	@Override
	public String toString() {
		IPlugin plugin = getPlugin();
		if (plugin != null) {
			return plugin.getTranslatedName();
		}
		return fId != null ? fId : "?"; //$NON-NLS-1$
	}

	public boolean isResolved() {
		return getPlugin() != null;
	}

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
