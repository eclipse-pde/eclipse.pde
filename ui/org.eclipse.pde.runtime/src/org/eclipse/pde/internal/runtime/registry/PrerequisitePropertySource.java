/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
	public static final String P_ID = "id"; //$NON-NLS-1$
	public static final String P_VERSION = "version"; //$NON-NLS-1$
	public static final String P_EXPORTED = "exported"; //$NON-NLS-1$
	public static final String P_MATCH = "match"; //$NON-NLS-1$
	public static final String P_OPTIONAL = "optional"; //$NON-NLS-1$
	public static final String KEY_EXPORTED =
		"RegistryView.prerequisitePR.exported"; //$NON-NLS-1$
	public static final String KEY_ID = "RegistryView.prerequisitePR.id"; //$NON-NLS-1$
	public static final String KEY_MATCH = "RegistryView.prerequisitePR.match"; //$NON-NLS-1$
	public static final String KEY_VERSION =
		"RegistryView.prerequisitePR.version"; //$NON-NLS-1$
	public static final String KEY_OPTIONAL =
		"RegistryView.prerequisitePR.optional"; //$NON-NLS-1$
	public static final String KEY_MATCHED_COMPATIBLE =
		"RegistryView.prerequisitePR.matchedCompatible"; //$NON-NLS-1$
	public static final String KEY_MATCHED_EXACT =
		"RegistryView.prerequisitePR.matchedExact"; //$NON-NLS-1$
	public static final String KEY_MATCHED_EQUIVALENT =
		"RegistryView.prerequisitePR.matchedEquivalent"; //$NON-NLS-1$
	public static final String KEY_MATCHED_GREATER_OR_EQUAL =
		"RegistryView.prerequisitePR.matchedGreaterOrEqual"; //$NON-NLS-1$
	public static final String KEY_MATCHED_PERFECT =
		"RegistryView.prerequisitePR.matchedPerfect"; //$NON-NLS-1$

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
			return prereq.isExported() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$

		if (name.equals(P_VERSION)) {
			Object version = prereq.getVersionIdentifier();
			return version != null ? version.toString() : ""; //$NON-NLS-1$
		}

		if (name.equals(P_OPTIONAL))
			return prereq.isOptional() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$

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
		return ""; //$NON-NLS-1$
	}
}
