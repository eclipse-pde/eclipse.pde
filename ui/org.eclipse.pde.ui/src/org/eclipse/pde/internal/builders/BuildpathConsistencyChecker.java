/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.builders;

import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import java.util.Vector;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.core.resources.IProject;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.core.runtime.CoreException;

public class BuildpathConsistencyChecker implements IModelProviderListener {
	Vector affected = new Vector();
	public void modelsChanged(IModelProviderEvent e) {
		WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
		affected.clear();
		IModel [] added = e.getAddedModels();
		IModel [] removed = e.getRemovedModels();
		IPluginModelBase [] plugins = manager.getWorkspacePluginModels();
		//IPluginModelBase [] fragments = manager.getWorkspaceFragmentModels();
		processModels(plugins, added, removed);
		//processModels(fragments, added, removed);
		processAffectedModels();
	}
	
	private void processModels(IPluginModelBase[] models, IModel [] added, IModel [] removed) {
		for (int i=0; i<models.length; i++) {
			if (isOnTheList(models[i], added)) continue;
			processModel(models[i], added, removed);
		}
	}

	private boolean isOnTheList(IPluginModelBase model, IModel[] list) {
		if (list==null) return false;
		for (int i=0; i<list.length; i++) {
			if (model.equals(list[i])) return true;
		}
		return false;
	}
	
	private void processModel(IPluginModelBase model, IModel [] added, IModel [] removed) {
		// check if this model's build path has been affected by the change
		IPlugin plugin = (IPlugin)model.getPluginBase();
		IPluginImport [] imports = plugin.getImports();

		for (int i=0; i<imports.length; i++) {
			IPluginImport iimport = imports[i];
			String id = iimport.getId();

			IPluginModelBase addedMatch = findMatch(id, added);
			IPluginModelBase removedMatch = findMatch(id, removed);
			if (addedMatch!=null || removedMatch!=null) {
				affected.add(model);
			}
		}
	}
	private IPluginModelBase findMatch(String id, IModel[] list) {
		if (list==null) return null;
		for (int i=0; i<list.length; i++) {
			IPluginModelBase model = (IPluginModelBase)list[i];
			if (model.getPluginBase().getId().equals(id)) return model;
		}
		return null;
	}
	private void processAffectedModels () {
		if (affected.size()==0) return;
		for (int i=0; i<affected.size(); i++) {
			IPluginModelBase model = (IPluginModelBase)affected.get(i);
			processAffectedModel(model);
		}
	}
	private void processAffectedModel(IPluginModelBase model) {
		IProject project = model.getUnderlyingResource().getProject();
		IJavaProject javaProject = JavaCore.create(project);
	}
}
