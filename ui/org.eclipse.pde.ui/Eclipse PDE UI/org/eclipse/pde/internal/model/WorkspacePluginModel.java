package org.eclipse.pde.internal.model;

import org.eclipse.pde.internal.base.model.plugin.*;

public class WorkspacePluginModel extends WorkspacePluginModelBase implements IPluginModel {

public WorkspacePluginModel() {
	super();
}
public WorkspacePluginModel(org.eclipse.core.resources.IFile file) {
	super(file);
}
public IPluginBase createPluginBase() {
	Plugin plugin = new Plugin();
	plugin.setModel(this);
	return plugin;
}
public IPlugin getPlugin() {
	return (IPlugin)getPluginBase();
}
}
