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
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;

public class DependenciesLabelProvider extends LabelProvider {
	private PDELabelProvider fSharedProvider;

	private boolean fShowReexport;

	/**
	 * Constructor for PluginsLabelProvider.
	 */
	public DependenciesLabelProvider(boolean showRexport) {
		super();
		fShowReexport = showRexport;
		fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
		fSharedProvider.connect(this);
	}

	public void dispose() {
		fSharedProvider.disconnect(this);
		super.dispose();
	}

	public String getText(Object obj) {
		return fSharedProvider.getText(obj);
	}

	public Image getImage(Object obj) {
		if (obj instanceof IPluginImport) {
			IPluginImport iobj = (IPluginImport) obj;
			String id = iobj.getId();
			IPlugin plugin = PDECore.getDefault().findPlugin(id);
			int flags = 0;
			if (fShowReexport && iobj.isReexported())
				flags = SharedLabelProvider.F_EXPORT;
			if (plugin != null) {
				IPluginModelBase model = plugin.getPluginModel();
				if (model.getUnderlyingResource() == null)
					flags |= SharedLabelProvider.F_EXTERNAL;
			}
			if (plugin == null)
				flags = SharedLabelProvider.F_ERROR;

			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		return fSharedProvider.getImage(obj);
	}
}