package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IConfigurationElement;

public class ConfigurationElementAdapter extends ParentAdapter {


public ConfigurationElementAdapter(Object object) {
	super(object);
}
protected Object[] createChildren() {
	IConfigurationElement config = (IConfigurationElement)getObject();
	IConfigurationElement [] children = config.getChildren();
	if (children.length==0) return null;
	Object[] result = new Object[children.length];
	for (int i=0; i<children.length; i++) {
		IConfigurationElement child = children[i];
		result[i] = new ConfigurationElementAdapter(child);
	}
	return result;
}
}
