/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

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
