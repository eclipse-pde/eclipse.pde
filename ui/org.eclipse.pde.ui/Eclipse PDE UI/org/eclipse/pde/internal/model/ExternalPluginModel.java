package org.eclipse.pde.internal.model;

import org.eclipse.pde.internal.base.model.plugin.*;

public class ExternalPluginModel extends ExternalPluginModelBase implements IPluginModel {

public ExternalPluginModel() {
	super();
}
public IPluginBase createPluginBase() {
	PluginBase base = new Plugin();
	base.setModel(this);
	return base;
}
public IPlugin getPlugin() {
	return (IPlugin)getPluginBase();
}
}
