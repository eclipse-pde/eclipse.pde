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

import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;

public class ExtensionPointAdapter extends ParentAdapter {

	public ExtensionPointAdapter(Object object) {
		super(object);
	}

	protected Object[] createChildren() {
		IExtensionPoint extensionPoint = (IExtensionPoint) getObject();

		IExtension[] extensions = extensionPoint.getExtensions();
		Object[] result = new Object[extensions.length];
		for (int i = 0; i < extensions.length; i++) {
			IExtension extension = extensions[i];
			result[i] = new ExtensionAdapter(extension);
		}
		return result;
	}
}
