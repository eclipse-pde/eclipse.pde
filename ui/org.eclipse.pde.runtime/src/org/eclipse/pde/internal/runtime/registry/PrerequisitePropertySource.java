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

import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.ui.views.properties.*;

public class PrerequisitePropertySource extends RegistryPropertySource {
	private IPluginPrerequisite prereq;
	public static final String P_ID = "id";
	public static final String P_VERSION = "version";
	public static final String P_EXPORTED = "exported";
	public static final String P_MATCH = "match";
	public static final String P_OPTIONAL = "optional";
	public static final String KEY_EXPORTED =
		"RegistryView.prerequisitePR.exported";
	public static final String KEY_ID = "RegistryView.prerequisitePR.id";
	public static final String KEY_MATCH = "RegistryView.prerequisitePR.match";
	public static final String KEY_VERSION =
		"RegistryView.prerequisitePR.version";
	public static final String KEY_OPTIONAL =
		"RegistryView.prerequisitePR.optional";
	public static final String KEY_MATCHED_COMPATIBLE =
		"RegistryView.prerequisitePR.matchedCompatible";
	public static final String KEY_MATCHED_EXACT =
		"RegistryView.prerequisitePR.matchedExact";
	public static final String KEY_MATCHED_EQUIVALENT =
		"RegistryView.prerequisitePR.matchedEquivalent";
	public static final String KEY_MATCHED_GREATER_OR_EQUAL =
		"RegistryView.prerequisitePR.matchedGreaterOrEqual";
	public static final String KEY_MATCHED_PERFECT =
		"RegistryView.prerequisitePR.matchedPerfect";

	public PrerequisitePropertySource(IPluginPrerequisite prereq) {
		this.prereq = prereq;
	}
	public IPropertyDescriptor[] getPropertyDescriptors() {
		Vector result = new Vector();

		result.addElement(
			new PropertyDescriptor(
				P_EXPORTED,
				PDERuntimePlugin.getResourceString(KEY_EXPORTED)));
		result.addElement(
			new PropertyDescriptor(
				P_ID,
				PDERuntimePlugin.getResourceString(KEY_ID)));
		result.addElement(
			new PropertyDescriptor(
				P_VERSION,
				PDERuntimePlugin.getResourceString(KEY_VERSION)));
		result.addElement(
			new PropertyDescriptor(
				P_MATCH,
				PDERuntimePlugin.getResourceString(KEY_MATCH)));
		result.addElement(
			new PropertyDescriptor(
				P_OPTIONAL,
				PDERuntimePlugin.getResourceString(KEY_OPTIONAL)));
		return toDescriptorArray(result);
		
	}

	public Object getPropertyValue(Object name) {
		if (name.equals(P_ID))
			return prereq.getUniqueIdentifier();

		if (name.equals(P_EXPORTED))
			return prereq.isExported() ? "true" : "false";

		if (name.equals(P_VERSION)) {
			Object version = prereq.getVersionIdentifier();
			return version != null ? version.toString() : "";
		}

		if (name.equals(P_OPTIONAL))
			return prereq.isOptional() ? "true" : "false";

		if (name.equals(P_MATCH)) {
			if (prereq.isMatchedAsCompatible())
				return PDERuntimePlugin.getResourceString(
					KEY_MATCHED_COMPATIBLE);
			if (prereq.isMatchedAsEquivalent())
				return PDERuntimePlugin.getResourceString(
					KEY_MATCHED_EQUIVALENT);
			if (prereq.isMatchedAsExact())
				return PDERuntimePlugin.getResourceString(KEY_MATCHED_EXACT);
			if (prereq.isMatchedAsGreaterOrEqual())
				return PDERuntimePlugin.getResourceString(
					KEY_MATCHED_GREATER_OR_EQUAL);
			if (prereq.isMatchedAsPerfect())
				return PDERuntimePlugin.getResourceString(KEY_MATCHED_PERFECT);
		}
		return "";
	}
}
