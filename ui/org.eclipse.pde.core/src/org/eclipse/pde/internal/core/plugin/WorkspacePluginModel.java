/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;

public class WorkspacePluginModel extends WorkspacePluginModelBase implements IPluginModel {

	private static final long serialVersionUID = 1L;

	public WorkspacePluginModel(IFile file, boolean abbreviated) {
		super(file, abbreviated);
	}

	@Override
	public IPluginBase createPluginBase() {
		Plugin plugin = new Plugin(!isEditable());
		plugin.setModel(this);
		return plugin;
	}

	@Override
	public IPlugin getPlugin() {
		return (IPlugin) getPluginBase();
	}
}
