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
import org.eclipse.core.runtime.IExtension;

public class ExtensionAdapter extends ParentAdapter {

	public ExtensionAdapter(Object object) {
		super(object);
	}

	protected Object[] createChildren() {
		IExtension extension = (IExtension) getObject();

		IConfigurationElement[] elements = extension.getConfigurationElements();
		Object[] result = new ConfigurationElementAdapter[elements.length];
		for (int i = 0; i < elements.length; i++) {
			IConfigurationElement config = elements[i];
			result[i] = new ConfigurationElementAdapter(config);
		}
		return result;
	}
}
