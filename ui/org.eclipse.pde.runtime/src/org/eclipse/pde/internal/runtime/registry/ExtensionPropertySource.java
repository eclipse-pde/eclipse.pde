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

import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.views.properties.*;

public class ExtensionPropertySource extends RegistryPropertySource {
	private IExtension extension;
	public static final String P_NAME = "name";
	public static final String P_ID = "id";
	public static final String P_POINT = "point";
	public static final String KEY_POINT = "RegistryView.extensionPR.point";
	public static final String KEY_ID = "RegistryView.extensionPR.id";
	public static final String KEY_NAME = "RegistryView.extensionPR.name";

public ExtensionPropertySource(IExtension extension) {
	this.extension = extension;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_NAME, PDERuntimePlugin.getResourceString(KEY_NAME)));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimePlugin.getResourceString(KEY_ID)));
	result.addElement(new PropertyDescriptor(P_POINT, PDERuntimePlugin.getResourceString(KEY_POINT)));
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_NAME))
		return extension.getLabel();
	if (name.equals(P_ID))
		return extension.getUniqueIdentifier();
	if (name.equals(P_POINT))
		return extension.getExtensionPointUniqueIdentifier();
	return null;
}
}
