package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.*;

public class ExtensionPointPropertySource extends RegistryPropertySource {
	private IExtensionPoint extensionPoint;
	public static final String P_NAME = "name";
	public static final String KEY_ID = "RegistryView.extensionPointPR.id";
	public static final String KEY_NAME = "RegistryView.extensionPointPR.name";
	public static final String P_ID = "id";

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
