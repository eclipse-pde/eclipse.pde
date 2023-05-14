/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.views.dependencies;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.util.SharedLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.osgi.framework.Version;

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

	@Override
	public void dispose() {
		fSharedProvider.disconnect(this);
		super.dispose();
	}

	@Override
	public String getText(Object obj) {
		if (obj instanceof IPluginImport) {
			return ((IPluginImport) obj).getId();
		} else if (obj instanceof String) {
			return (String) obj;
		} else if (obj instanceof IPluginModelBase) {
			return ((IPluginModelBase) obj).getPluginBase(false).getId();
		} else if (obj instanceof IPluginBase) {
			return fSharedProvider.getObjectText((IPluginBase) obj);
		} else if (obj instanceof BundleDescription) {
			return getObjectText((BundleDescription) obj);
		} else if (obj instanceof VersionConstraint) {
			// ImportPackageSpecification, BundleSpecification
			BaseDescription desc = ((VersionConstraint) obj).getSupplier();
			if (desc instanceof BundleDescription)
				return getObjectText((BundleDescription) desc);
			else if (desc instanceof ExportPackageDescription)
				return getObjectText(((ExportPackageDescription) desc).getExporter());
			// if unresolved, just show name
			return ((VersionConstraint) obj).getName();
		}

		return fSharedProvider.getText(obj);
	}

	public String getObjectText(BundleDescription obj) {
		String name = fSharedProvider.getObjectText(obj);
		Version version = obj.getVersion();
		// Bug 183417 - Bidi3.3: Elements' labels in the extensions page in the fragment manifest characters order is incorrect
		// Use the PDELabelProvider.formatVersion function to properly format the version for all languages including bidi
		return name + ' ' + PDELabelProvider.formatVersion(version.toString());
	}

	@Override
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
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				if (model.getUnderlyingResource() == null)
					flags |= SharedLabelProvider.F_EXTERNAL;
			}

			if (model == null)
				flags = SharedLabelProvider.F_ERROR;

			if (model != null && model instanceof IFragmentModel)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		if (obj instanceof IPluginModelBase) {
			if (((IPluginModelBase) obj).getUnderlyingResource() == null)
				flags |= SharedLabelProvider.F_EXTERNAL;
			if (obj instanceof IFragmentModel)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		if (obj instanceof IPluginBase) {
			if (((IPluginBase) obj).getPluginModel().getUnderlyingResource() == null)
				flags |= SharedLabelProvider.F_EXTERNAL;
			if (obj instanceof IFragment)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		if (obj instanceof BundleDescription) {
			id = ((BundleDescription) obj).getSymbolicName();
		} else if (obj instanceof BundleSpecification) {
			id = ((VersionConstraint) obj).getName();
			if (fShowReexport) {
				if (((BundleSpecification) obj).isExported())
					flags |= SharedLabelProvider.F_EXPORT;
			}
		} else if (obj instanceof ImportPackageSpecification) {
			BaseDescription export = ((ImportPackageSpecification) obj).getSupplier();
			id = ((ExportPackageDescription) export).getExporter().getSymbolicName();
		}
		if (id != null) {
			IPluginModelBase model = PluginRegistry.findModel(id);
			if (model != null) {
				if (model.getUnderlyingResource() == null)
					flags |= SharedLabelProvider.F_EXTERNAL;
			}
			if (model == null)
				flags = SharedLabelProvider.F_ERROR;
			if (model != null && model instanceof IFragmentModel)
				return fSharedProvider.get(PDEPluginImages.DESC_FRAGMENT_OBJ, flags);
			return fSharedProvider.get(PDEPluginImages.DESC_PLUGIN_OBJ, flags);
		}
		return fSharedProvider.getImage(obj);
	}

}
