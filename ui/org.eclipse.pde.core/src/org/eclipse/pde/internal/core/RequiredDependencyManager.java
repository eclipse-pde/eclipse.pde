/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;


public class RequiredDependencyManager {
	
	/*public static IPluginModelBase[] addRequiredPlugins(
			IPluginModelBase[] selected, IPluginModelBase[] allModels) {
		if (selected.length == 0 || selected.length == allModels.length)
			return new IPluginModelBase[0];

		HashSet existing = new HashSet();
		ArrayList result = new ArrayList();
		for (int i = 0; i < selected.length; i++) {
			String id = selected[i].getPluginBase().getId();
			if (id != null)
				existing.add(id);
		}
		
		Iterator iter = existing.iterator();
		while (iter.hasNext()) {
			addDependencies(iter.next().toString(), existing, result);
		}
		
		
		return (IPluginModelBase[])result.values().toArray(new IPluginModelBase[result.size()]);
	}
	
	/*private void handleAddRequired() {
		ArrayList result = new ArrayList();
		for (int i = 0; i < items.length; i++) {
			IPluginModelBase model = (IPluginModelBase)items[i].getData();
			if (fTablePart.getTableViewer().getChecked(model))
				addPluginAndDependencies((IPluginModelBase) items[i].getData(), result);
		}
		fTablePart.setSelection(result.toArray());
	}
	
	protected void addPluginAndDependencies(
			IPluginModelBase model,
			ArrayList selected) {
				
			if (!selected.contains(model)) {
				selected.add(model);
				if (!model.isEnabled())
					fChangedModels.add(model);
				addDependencies(getAllModels(), model, selected);
			}
		}
		
	protected void addDependencies(
	    IPluginModelBase[] models,
		IPluginModelBase model,
		ArrayList selected) {
		
		IPluginImport[] required = model.getPluginBase().getImports();
		if (required.length > 0) {
			for (int i = 0; i < required.length; i++) {
				IPluginModelBase found = findModel(models, required[i].getId());
				if (found != null) {
					addPluginAndDependencies(found, selected);
				}
			}
		}
		
		if (model instanceof IPluginModel) {
			IFragmentModel[] fragments = findFragments(models, ((IPluginModel)model).getPlugin());
			for (int i = 0; i < fragments.length; i++) {
				String id = fragments[i].getFragment().getId();
				if (!"org.eclipse.ui.workbench.compatibility".equals(id)) //$NON-NLS-1$
					addPluginAndDependencies(fragments[i], selected);
			}
		} else {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IPluginModelBase found = findModel(models, fragment.getPluginId());
			if (found != null) {
				addPluginAndDependencies(found, selected);
			}
		}
	}

	private IPluginModelBase findModel(IPluginModelBase[] models, String id) {
		for (int i = 0; i < models.length; i++) {
			String modelId = models[i].getPluginBase().getId();
			if (modelId != null && modelId.equals(id))
				return models[i];
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPluginModelBase[] models, IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (models[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) models[i]).getFragment();
				if (plugin.getId().equalsIgnoreCase(fragment.getPluginId())) {
					result.add(models[i]);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}
	*/


}
