
package org.eclipse.pde.internal;

import org.eclipse.pde.internal.model.*;
import org.eclipse.pde.model.IModel;
import org.eclipse.pde.model.plugin.*;
import java.util.Hashtable;

public class ModelSynchronizer {
	IModelProviderListener workspaceListener, externalListener;
	Hashtable models;
	
	public ModelSynchronizer() {
		models = new Hashtable();
		workspaceListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				workspaceModelsChanged(e);
			}
		};
		externalListener = new IModelProviderListener() {
			public void modelsChanged(IModelProviderEvent e) {
				externalModelsChanged(e);
			}
		};
	}
	
	class ModelEntry {
		IPluginModelBase workspaceModel;
		IPluginModelBase externalModel;
	}
	
	public void register(ExternalModelManager manager) {
		manager.addModelProviderListener(externalListener);
	}
	public void register(WorkspaceModelManager manager) {
		manager.addModelProviderListener(workspaceListener);
	}
	public void unregister(ExternalModelManager manager) {
		manager.removeModelProviderListener(externalListener);
	}
	public void unregister(WorkspaceModelManager manager) {
		manager.removeModelProviderListener(workspaceListener);
	}
	private void workspaceModelsChanged(IModelProviderEvent e) {
	}
	private void externalModelsChanged(IModelProviderEvent e) {
	}
	private void showEvent(IModelProviderEvent e) {
		System.out.println("Added:");
		showArray(e.getAddedModels());
		System.out.println("Removed:");
		showArray(e.getRemovedModels());
		System.out.println("Changed:");
		showArray(e.getChangedModels());
	}
	private void showArray(IModel[] models) {
		if (models==null) {
			System.out.println("   [empty]");
			return;
		}
		for (int i=0; i<models.length; i++) {
			IPluginModelBase model = (IPluginModelBase)models[i];
			IPluginBase base = model.getPluginBase();
			System.out.println("    "+base.getId());
		}
	}
}