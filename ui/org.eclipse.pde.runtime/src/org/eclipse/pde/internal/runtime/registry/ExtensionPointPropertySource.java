/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import java.util.Vector;

import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.views.properties.*;

public class ExtensionPointPropertySource extends RegistryPropertySource {
	private IExtensionPoint extensionPoint;
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String KEY_ID = "RegistryView.extensionPointPR.id"; //$NON-NLS-1$
	public static final String KEY_NAME = "RegistryView.extensionPointPR.name"; //$NON-NLS-1$
	public static final String P_ID = "id"; //$NON-NLS-1$

public ExtensionPointPropertySource(IExtensionPoint extensionPoint) {
	this.extensionPoint = extensionPoint;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_NAME, PDERuntimePlugin.getResourceString(KEY_NAME)));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimePlugin.getResourceString(KEY_ID)));
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_NAME))
		return extensionPoint.getLabel();
	if (name.equals(P_ID))
		return extensionPoint.getUniqueIdentifier();
	return null;
}
}
