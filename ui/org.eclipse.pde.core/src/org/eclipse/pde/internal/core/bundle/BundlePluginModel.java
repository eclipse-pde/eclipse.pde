/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;

public class BundlePluginModel extends BundlePluginModelBase implements IBundlePluginModel {

	private static final long serialVersionUID = 1L;

	@Override
	public IPluginBase createPluginBase() {
		BundlePlugin bplugin = new BundlePlugin();
		bplugin.setModel(this);
		return bplugin;
	}

	@Override
	public IPlugin getPlugin() {
		return (IPlugin) getPluginBase();
	}

	@Override
	public boolean isFragmentModel() {
		return false;
	}
}
