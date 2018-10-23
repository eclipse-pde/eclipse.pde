/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductFeature;
import org.eclipse.pde.internal.core.iproduct.IProductPlugin;
import org.eclipse.pde.internal.launching.launcher.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;

public class ProductValidateAction extends Action {

	IProduct fProduct;

	public ProductValidateAction(IProduct product) {
		super(PDEUIMessages.ProductValidateAction_validate, IAction.AS_PUSH_BUTTON);
		setImageDescriptor(PDEPluginImages.DESC_VALIDATE_TOOL);
		fProduct = product;
	}

	@Override
	public void run() {
		Set<IPluginModelBase> launchPlugins = new HashSet<>();
		if (fProduct.useFeatures()) {
			IFeatureModel[] features = getUniqueFeatures();
			for (IFeatureModel feature : features) {
				addFeaturePlugins(feature.getFeature(), launchPlugins);
			}
		} else {
			IProductPlugin[] plugins = fProduct.getPlugins();
			for (IProductPlugin plugin : plugins) {
				String id = plugin.getId();
				if (id == null)
					continue;
				IPluginModelBase model = PluginRegistry.findModel(id);
				if (model != null && !launchPlugins.contains(model) && TargetPlatformHelper.matchesCurrentEnvironment(model))
					launchPlugins.add(model);
			}
		}
		try {
			IPluginModelBase[] models = launchPlugins.toArray(new IPluginModelBase[launchPlugins.size()]);
			LaunchValidationOperation operation = new ProductValidationOperation(models);
			LaunchPluginValidator.runValidationOperation(operation, new NullProgressMonitor());
			if (!operation.hasErrors()) {
				MessageDialog.open(SWT.ICON_INFORMATION, PDEPlugin.getActiveWorkbenchShell(),
						PDEUIMessages.PluginStatusDialog_pluginValidation,
						PDEUIMessages.AbstractLauncherToolbar_noProblems, 0, IDialogConstants.CLOSE_LABEL);
			}
		} catch (CoreException e) {
			if (e.getStatus().getSeverity() == IStatus.CANCEL) {
				return;
			}
			PDEPlugin.logException(e);
		}
	}

	private void addFeaturePlugins(IFeature feature, Set<IPluginModelBase> launchPlugins) {
		IFeaturePlugin[] plugins = feature.getPlugins();
		for (IFeaturePlugin plugin : plugins) {
			String id = plugin.getId();
			String version = plugin.getVersion();
			if (id == null || version == null)
				continue;
			IPluginModelBase model = PluginRegistry.findModel(id, version, IMatchRules.EQUIVALENT, null);
			if (model == null)
				model = PluginRegistry.findModel(id);
			if (model != null && !launchPlugins.contains(model) && TargetPlatformHelper.matchesCurrentEnvironment(model))
				launchPlugins.add(model);
		}
	}

	private IFeatureModel[] getUniqueFeatures() {
		ArrayList<IFeatureModel> list = new ArrayList<>();
		IProductFeature[] features = fProduct.getFeatures();
		for (IProductFeature feature : features) {
			String id = feature.getId();
			String version = feature.getVersion();
			addFeatureAndChildren(id, version, list);
		}
		return list.toArray(new IFeatureModel[list.size()]);
	}

	private void addFeatureAndChildren(String id, String version, List<IFeatureModel> list) {
		FeatureModelManager manager = PDECore.getDefault().getFeatureModelManager();
		IFeatureModel model = manager.findFeatureModel(id, version);
		if (model == null || list.contains(model))
			return;

		list.add(model);

		IFeatureChild[] children = model.getFeature().getIncludedFeatures();
		for (IFeatureChild child : children) {
			addFeatureAndChildren(child.getId(), child.getVersion(), list);
		}
	}
}
