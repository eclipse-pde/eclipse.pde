package org.eclipse.pde.internal.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;

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
