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
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;

public class CalleesContentProvider extends DependenciesViewPageContentProvider {
	public CalleesContentProvider(DependenciesView view) {
		super(view);
	}

	/**
	 * @param plugin
	 * @return
	 */
	protected Object[] findCallees(IPluginBase plugin) {
		if (plugin instanceof IFragment) {
			String hostId = ((IFragment) plugin).getPluginId();
			IPluginModelBase hostPlugin = PluginRegistry.findModel(hostId);
			if (hostPlugin != null) {
				IPluginImport[] imports = plugin.getImports();
				Object[] result = new Object[imports.length + 1];
				System.arraycopy(imports, 0, result, 0, imports.length);
				result[imports.length] = hostPlugin.getPluginBase();
				return result;
			}
		}
		return plugin.getImports();
	}

}
