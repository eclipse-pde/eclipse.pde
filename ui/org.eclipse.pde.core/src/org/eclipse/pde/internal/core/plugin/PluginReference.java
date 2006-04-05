/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.plugin;

import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.internal.core.PDECore;

public class PluginReference extends PlatformObject {
	private String id;
	private transient IPlugin plugin;
	
	public PluginReference() {}
	
	public PluginReference(String id) {
		this.id = id;
		if (id!=null)
			plugin = PDECore.getDefault().findPlugin(id);
	}
	public PluginReference(IPlugin plugin) {
		this.id = plugin.getId();
		this.plugin = plugin;
	}
	public String getId() {
		return id;
	}
	public IPlugin getPlugin() {
		if (plugin == null && id !=null) {
			IPluginModel model = PDECore.getDefault().getModelManager().findPluginModel(id);
			plugin = model != null ? model.getPlugin() : null;
		}
		return plugin;
	}
	public String toString() {
		if (plugin!=null) {
			return plugin.getTranslatedName();
		}
		return id!=null?id:"?"; //$NON-NLS-1$
	}
	public boolean isResolved() {
		return plugin!=null;
	}
}
