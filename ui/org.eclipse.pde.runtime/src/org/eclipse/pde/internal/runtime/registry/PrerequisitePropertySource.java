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

import org.eclipse.core.runtime.IPluginPrerequisite;
import org.eclipse.pde.internal.runtime.PDERuntimeMessages;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class PrerequisitePropertySource extends RegistryPropertySource {
	private IPluginPrerequisite prereq;
	public static final String P_ID = "id"; //$NON-NLS-1$
	public static final String P_VERSION = "version"; //$NON-NLS-1$
	public static final String P_EXPORTED = "exported"; //$NON-NLS-1$
	public static final String P_MATCH = "match"; //$NON-NLS-1$
	public static final String P_OPTIONAL = "optional"; //$NON-NLS-1$
	public PrerequisitePropertySource(IPluginPrerequisite prereq) {
		this.prereq = prereq;
	}
	public IPropertyDescriptor[] getPropertyDescriptors() {
		Vector result = new Vector();

		result.addElement(
			new PropertyDescriptor(
				P_EXPORTED,
				PDERuntimeMessages.RegistryView_prerequisitePR_exported));
		result.addElement(
			new PropertyDescriptor(
				P_ID,
				PDERuntimeMessages.RegistryView_prerequisitePR_id));
		result.addElement(
			new PropertyDescriptor(
				P_VERSION,
				PDERuntimeMessages.RegistryView_prerequisitePR_version));
		result.addElement(
			new PropertyDescriptor(
				P_MATCH,
				PDERuntimeMessages.RegistryView_prerequisitePR_match));
		result.addElement(
			new PropertyDescriptor(
				P_OPTIONAL,
				PDERuntimeMessages.RegistryView_prerequisitePR_optional));
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
				return PDERuntimeMessages.RegistryView_prerequisitePR_matchedCompatible;
			if (prereq.isMatchedAsEquivalent())
				return PDERuntimeMessages.RegistryView_prerequisitePR_matchedEquivalent;
			if (prereq.isMatchedAsExact())
				return PDERuntimeMessages.RegistryView_prerequisitePR_matchedExact;
			if (prereq.isMatchedAsGreaterOrEqual())
				return PDERuntimeMessages.RegistryView_prerequisitePR_matchedGreaterOrEqual;
			if (prereq.isMatchedAsPerfect())
				return PDERuntimeMessages.RegistryView_prerequisitePR_matchedPerfect;
		}
		return ""; //$NON-NLS-1$
	}
}
