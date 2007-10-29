/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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

	class ConfigurationAttribute implements IConfigurationAttribute {
		private String fLabel;
		public ConfigurationAttribute(String name, String value) {
			fLabel = name + " = " + value; //$NON-NLS-1$
		}
		public String getLabel() {
			return fLabel;
		}
	}
	
	public ConfigurationElementAdapter(Object object) {
		super(object);
	}

	protected Object[] createChildren() {
		IConfigurationElement config = (IConfigurationElement) getObject();
		String[] atts = config.getAttributeNames();
		IConfigurationAttribute[] catts = new IConfigurationAttribute[atts.length];
		for (int i = 0; i < atts.length; i++)
			catts[i] = new ConfigurationAttribute(atts[i], config.getAttribute(atts[i]));
		IConfigurationElement[] children = config.getChildren();
		Object[] result = new Object[children.length + catts.length];
		for (int i = 0; i < children.length; i++) {
			IConfigurationElement child = children[i];
			result[i] = new ConfigurationElementAdapter(child);
		}
		for (int i = 0; i < catts.length; i++) {
			IConfigurationAttribute child = catts[i];
			result[children.length + i] = new ConfigurationAttributeAdapter(child);
		}
		return result;
	}
}
