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

import org.eclipse.core.runtime.IExtension;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.ui.views.properties.*;

public class ExtensionPropertySource extends RegistryPropertySource {
	private IExtension extension;
	public static final String P_NAME = "name"; //$NON-NLS-1$
	public static final String P_ID = "id"; //$NON-NLS-1$
	public static final String P_POINT = "point"; //$NON-NLS-1$
	public ExtensionPropertySource(IExtension extension) {
	this.extension = extension;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_NAME, PDERuntimeMessages.RegistryView_extensionPR_name));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimeMessages.RegistryView_extensionPR_id));
	result.addElement(new PropertyDescriptor(P_POINT, PDERuntimeMessages.RegistryView_extensionPR_point));
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
