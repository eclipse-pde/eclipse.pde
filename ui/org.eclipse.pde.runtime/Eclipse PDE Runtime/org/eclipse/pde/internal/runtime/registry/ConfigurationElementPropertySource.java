package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.core.runtime.*;

public class ConfigurationElementPropertySource extends RegistryPropertySource {
	private IConfigurationElement config;

public ConfigurationElementPropertySource(IConfigurationElement config) {
	this.config = config;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	String [] atts = config.getAttributeNames();
	for (int i=0; i<atts.length; i++) {
	   result.addElement(new PropertyDescriptor(atts[i], atts[i]));
	}
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	return config.getAttribute(name.toString());
}
}
