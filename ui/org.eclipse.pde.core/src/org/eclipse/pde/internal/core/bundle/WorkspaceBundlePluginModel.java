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
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;

public class WorkspaceBundlePluginModel extends WorkspaceBundlePluginModelBase implements IPluginModel {

	private static final long serialVersionUID = 1L;

	public WorkspaceBundlePluginModel(IFile manifestFile, IFile pluginFile) {
		super(manifestFile, pluginFile);
	}

	public IPluginBase createPluginBase() {
		BundlePlugin base = new BundlePlugin();
		base.setModel(this);
		return base;
	}

	public IPlugin getPlugin() {
		return (IPlugin) getPluginBase();
	}

}
