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
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.Choice;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.ifeature.IFeatureEntry;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

public class FeatureEntryPropertySource extends FeaturePropertySource {
	protected Vector descriptors;
	public final static String KEY_ID = "FeatureEditor.ReferenceProp.id";
	public final static String KEY_VERSION =
		"FeatureEditor.ReferenceProp.version";
	public final static String KEY_DOWNLOAD_SIZE =
		"FeatureEditor.ReferenceProp.download-size";
	public final static String KEY_INSTALL_SIZE =
		"FeatureEditor.ReferenceProp.install-size";

	private final static String P_ID = "id";
	private final static String P_OS = "os";
	private final static String P_WS = "ws";
	private final static String P_NL = "nl";
	private final static String P_ARCH = "arch";
	private final static String P_INSTALL_SIZE = "install-size";
	private final static String P_DOWNLOAD_SIZE = "download-size";

	public FeatureEntryPropertySource(IFeatureEntry entry) {
		super(entry);
	}

	protected void createPropertyDescriptors() {
		descriptors = new Vector();
		PropertyDescriptor desc =
			new PropertyDescriptor(P_ID, PDEPlugin.getResourceString(KEY_ID));
		descriptors.addElement(desc);
		desc =
			createTextPropertyDescriptor(
				P_INSTALL_SIZE,
				PDEPlugin.getResourceString(KEY_INSTALL_SIZE));
		descriptors.addElement(desc);
		desc =
			createTextPropertyDescriptor(
				P_DOWNLOAD_SIZE,
				PDEPlugin.getResourceString(KEY_DOWNLOAD_SIZE));
		descriptors.addElement(desc);

		desc = createChoicePropertyDescriptor(P_OS, P_OS, getOSChoices());
		descriptors.addElement(desc);
		desc = createChoicePropertyDescriptor(P_WS, P_WS, getWSChoices());
		descriptors.addElement(desc);
		desc = createChoicePropertyDescriptor(P_NL, P_NL, getNLChoices());
		descriptors.addElement(desc);
		desc = createChoicePropertyDescriptor(P_ARCH, P_ARCH, getArchChoices());
		descriptors.addElement(desc);
	}
	
	public IFeatureEntry getEntry() {
		return (IFeatureEntry)object;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors == null) {
			createPropertyDescriptors();
		}
		return toDescriptorArray(descriptors);
	}

	private PropertyDescriptor createChoicePropertyDescriptor(
		String name,
		String displayName,
		Choice[] choices) {
		return new PortabilityChoiceDescriptor(
			name,
			displayName,
			choices,
			!isEditable());
	}

	public Object getPropertyValue(Object name) {
		if (name.equals(P_ID)) {
			return getEntry().getId();
		}

		if (name.equals(P_INSTALL_SIZE)) {
			long installSize = getEntry().getInstallSize();
			if (installSize == -1)
				return "";
			else
				return "" + installSize;
		}
		if (name.equals(P_DOWNLOAD_SIZE)) {
			long downloadSize = getEntry().getDownloadSize();
			if (downloadSize == -1)
				return "";
			else
				return "" + downloadSize;
		}
		if (name.equals(P_OS)) {
			return getEntry().getOS();
		}
		if (name.equals(P_WS)) {
			return getEntry().getWS();
		}
		if (name.equals(P_NL)) {
			return getEntry().getNL();
		}
		if (name.equals(P_ARCH)) {
			return getEntry().getArch();
		}
		return null;
	}
	public void setElement(IFeatureEntry entry) {
		object = entry;
	}
	public void setPropertyValue(Object name, Object value) {
		String svalue = value.toString();
		String realValue =
			svalue == null | svalue.length() == 0 ? null : svalue;
		try {
			if (name.equals(P_OS)) {
				getEntry().setOS(realValue);
			} else if (name.equals(P_WS)) {
				getEntry().setWS(realValue);
			} else if (name.equals(P_NL)) {
				getEntry().setNL(realValue);
			} else if (name.equals(P_ARCH)) {
				getEntry().setArch(realValue);
			} else if (name.equals(P_DOWNLOAD_SIZE)) {
				long lvalue = getLong(realValue);
				getEntry().setDownloadSize(lvalue);
			} else if (name.equals(P_INSTALL_SIZE)) {
				long lvalue = getLong(realValue);
				getEntry().setInstallSize(lvalue);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private long getLong(String svalue) {
		if (svalue == null)
			return -1;
		try {
			return Long.parseLong(svalue);
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	public static Choice[] getOSChoices() {
		return TargetPlatform.getOSChoices();
	}

	public static Choice[] getWSChoices() {
		return TargetPlatform.getWSChoices();
	}

	public static Choice[] getArchChoices() {
		return TargetPlatform.getArchChoices();
	}

	public static Choice[] getNLChoices() {
		return TargetPlatform.getNLChoices();
	}
}
