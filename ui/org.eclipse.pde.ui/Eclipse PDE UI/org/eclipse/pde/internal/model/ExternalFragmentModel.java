package org.eclipse.pde.internal.model;

import org.eclipse.pde.internal.base.model.plugin.*;

public class ExternalFragmentModel extends ExternalPluginModelBase implements IFragmentModel {

public ExternalFragmentModel() {
	super();
}
public IPluginBase createPluginBase() {
	PluginBase base = new Fragment();
	base.setModel(this);
	return base;
}
public IFragment getFragment() {
	return (IFragment)getPluginBase();
}
public boolean isFragmentModel() {
	return true;
}
}
