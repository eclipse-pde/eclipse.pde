/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.view;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
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
		if (obj instanceof IPluginImport) {
			return ((IPluginImport) obj).getId();
		} else if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof IPluginModelBase) {
			return ((IPluginModelBase) obj).getPluginBase(false).getId();
		} else if (obj instanceof IPluginBase) {
			return ((IPluginBase) obj).getId();
		}

		return fSharedProvider.getText(obj);
	}

	public Image getImage(Object obj) {
		int flags = 0;
		String id = null;
		if (obj instanceof IPluginImport) {
			IPluginImport iobj = (IPluginImport) obj;
			id = iobj.getId();
			if (fShowReexport && iobj.isReexported())
				flags = SharedLabelProvider.F_EXPORT;
		} else if (obj instanceof String) {
			id = (String) obj;
		}
		if (id != null) {
			IPluginModelBase model = PDECore.getDefault().getModelManager().findModel(id);
			if (model != null) {
				if (model.getUnderlyingResource() == null)
					flags |= SharedLabelProvider.F_EXTERNAL;
			}
		
			if (model == null)
				flags = SharedLabelProvider.F_ERROR;

			if(model != null && model instanceof IFragmentModel)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		if (obj instanceof IPluginModelBase) {
			if (((IPluginModelBase) obj).getUnderlyingResource() == null)
				flags |= SharedLabelProvider.F_EXTERNAL;
			if(obj instanceof IFragmentModel)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		if (obj instanceof IPluginBase) {
			if (((IPluginBase) obj).getPluginModel().getUnderlyingResource() == null)
				flags |= SharedLabelProvider.F_EXTERNAL;
			if(obj instanceof IFragment)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		return fSharedProvider.getImage(obj);
	}

}
