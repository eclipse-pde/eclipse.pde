/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.views.properties.*;

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
