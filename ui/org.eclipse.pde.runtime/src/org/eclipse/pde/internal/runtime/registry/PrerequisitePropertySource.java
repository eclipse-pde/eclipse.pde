package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.ui.views.properties.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.runtime.*;

public class PrerequisitePropertySource extends RegistryPropertySource {
	private IPluginPrerequisite prereq;
	public static final String P_ID = "id";
	public static final String P_VERSION = "version";
	public static final String P_EXPORTED = "exported";
	public static final String KEY_EXPORTED = "RegistryView.prerequisitePR.exported";
	public static final String KEY_ID = "RegistryView.prerequisitePR.id";
	public static final String KEY_VERSION = "RegistryView.prerequisitePR.version";
	public static final String KEY_MATCHED_COMPATIBLE = "RegistryView.prerequisitePR.matchedCompatible";
	public static final String KEY_MATCHED_EXACT = "RegistryView.prerequisitePR.matchedExact";
	public static final String P_MATCHED_AS_COMPATIBLE = "compatible";
	public static final String P_MATCHED_AS_EXACT = "exact";

public PrerequisitePropertySource(IPluginPrerequisite prereq) {
	this.prereq = prereq;
}
public IPropertyDescriptor[] getPropertyDescriptors() {
	Vector result = new Vector();

	result.addElement(new PropertyDescriptor(P_EXPORTED, PDERuntimePlugin.getResourceString(KEY_EXPORTED)));
	result.addElement(new PropertyDescriptor(P_ID, PDERuntimePlugin.getResourceString(KEY_ID)));
	result.addElement(new PropertyDescriptor(P_VERSION, PDERuntimePlugin.getResourceString(KEY_VERSION)));
	result.addElement(new PropertyDescriptor(P_MATCHED_AS_COMPATIBLE, PDERuntimePlugin.getResourceString(KEY_MATCHED_COMPATIBLE)));
	result.addElement(new PropertyDescriptor(P_MATCHED_AS_EXACT, PDERuntimePlugin.getResourceString(KEY_MATCHED_EXACT)));
	return toDescriptorArray(result);
}
public Object getPropertyValue(Object name) {
	if (name.equals(P_ID))
		return prereq.getUniqueIdentifier();
	if (name.equals(P_EXPORTED))
		return prereq.isExported()?"true":"false";
	if (name.equals(P_VERSION)) {
		Object version = prereq.getVersionIdentifier();
		return version!=null?version.toString():"";
	}
	if (name.equals(P_MATCHED_AS_COMPATIBLE))
		return prereq.isMatchedAsCompatible()?"true":"false";
	if (name.equals(P_MATCHED_AS_EXACT))
		return prereq.isMatchedAsExact()?"true":"false";
	return null;
}
}
