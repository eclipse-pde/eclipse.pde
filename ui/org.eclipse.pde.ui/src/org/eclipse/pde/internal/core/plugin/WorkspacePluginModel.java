package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;

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
