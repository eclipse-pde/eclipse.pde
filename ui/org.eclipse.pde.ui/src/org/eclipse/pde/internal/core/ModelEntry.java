package org.eclipse.pde.internal.core;

import org.eclipse.pde.core.plugin.IPluginModelBase;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.core.resources.IResource;
import java.io.*;

public class ModelEntry extends PlatformObject {
	public static final int AUTOMATIC = 0;
	public static final int WORKSPACE = 1;
	public static final int EXTERNAL = 2;
	private String id;
	private IPluginModelBase workspaceModel;
	private IPluginModelBase externalModel;
	private int mode = AUTOMATIC;

	public ModelEntry(String id) {
		this.id = id;
	}

	public IPluginModelBase getActiveModel() {
		if (mode == AUTOMATIC) {
			if (workspaceModel != null)
				return workspaceModel;
			return externalModel;
		} else
			return (mode == WORKSPACE) ? workspaceModel : externalModel;
	}

	public String getId() {
		return id;
	}

	public Object[] getChildren() {
		if (workspaceModel == null && externalModel != null) {
			String location = externalModel.getInstallLocation();
			File file = new File(location);
			FileAdapter adapter = new EntryFileAdapter(this, file);
			return adapter.getChildren();
		}
		return new Object[0];
	}

	public void setWorkspaceModel(IPluginModelBase model) {
		this.workspaceModel = model;
	}

	public void setExternalModel(IPluginModelBase model) {
		this.externalModel = model;
	}
	public IPluginModelBase getWorkspaceModel() {
		return workspaceModel;
	}

	public IPluginModelBase getExternalModel() {
		return externalModel;
	}
	public boolean isEmpty() {
		return workspaceModel == null && externalModel == null;
	}
}