/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ibundle.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePluginModel
	extends BundlePluginModelBase
	implements IBundlePluginModel {
	
	public IPluginBase createPluginBase() {
		BundlePlugin bplugin = new BundlePlugin();
		bplugin.setModel(this);
		return bplugin;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModel#getPlugin()
	 */
	public IPlugin getPlugin() {
		return (IPlugin)getPluginBase();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginModelBase#isFragmentModel()
	 */
	public boolean isFragmentModel() {
		return false;
	}
}
