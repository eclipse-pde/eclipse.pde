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

import org.eclipse.core.runtime.*;
import java.net.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.core.ifeature.*;

public class URLElementPropertySource extends FeaturePropertySource {
	private Vector descriptors;
	private final static String P_URL = "url";
	public final static String KEY_TYPE = "FeatureEditor.URLProp.type";
	public final static String KEY_SITE_TYPE = "FeatureEditor.URLProp.siteType";
	public final static String KEY_LABEL = "FeatureEditor.URLProp.label";
	public final static String KEY_URL = "FeatureEditor.URLProp.URL";
	private final static String P_TYPE = "type";
	private final static String P_LABEL = "label";
	private final static String P_SITE_TYPE = "siteType";
	private final static String[] elementTypes =
		{
			null,
			PDEPlugin.getResourceString("FeatureEditor.URLProp.type.update"),
			PDEPlugin.getResourceString(
				"FeatureEditor.URLProp.type.discovery")};

	private final static String[] siteTypes = { "update", "web" };

	public URLElementPropertySource(IFeatureURLElement element) {
		super(element);
	}
	public org
		.eclipse
		.pde
		.internal
		.core
		.ifeature
		.IFeatureURLElement getElement() {
		return (IFeatureURLElement) object;
	}
	public IPropertyDescriptor[] getPropertyDescriptors() {
		if (descriptors == null) {
			descriptors = new Vector();
			PropertyDescriptor desc =
				new PropertyDescriptor(
					P_TYPE,
					PDEPlugin.getResourceString(KEY_TYPE));
			descriptors.addElement(desc);
			desc =
				createTextPropertyDescriptor(
					P_LABEL,
					PDEPlugin.getResourceString(KEY_LABEL));
			descriptors.addElement(desc);
			desc =
				createTextPropertyDescriptor(
					P_URL,
					PDEPlugin.getResourceString(KEY_URL));
			descriptors.addElement(desc);
			desc =
				createChoicePropertyDescriptor(
					P_SITE_TYPE,
					PDEPlugin.getResourceString(KEY_SITE_TYPE),
					siteTypes);
			descriptors.addElement(desc);
		}
		return toDescriptorArray(descriptors);
	}
	public Object getPropertyValue(Object name) {
		if (name.equals(P_TYPE)) {
			return elementTypes[getElement().getElementType()];
		}
		if (name.equals(P_LABEL)) {
			return getElement().getLabel();
		}
		if (name.equals(P_URL)) {
			return getElement().getURL().toString();
		}
		if (name.equals(P_SITE_TYPE)) {
			return new Integer(getElement().getSiteType());
		}
		return null;
	}
	public void setElement(IFeatureURLElement newElement) {
		object = newElement;
	}
	public void setPropertyValue(Object name, Object value) {
		String svalue = value.toString();
		String realValue =
			svalue == null | svalue.length() == 0 ? null : svalue;
		try {
			if (name.equals(P_URL)) {
				try {
					URL url = null;
					if (realValue != null)
						url = new URL(realValue);
					getElement().setURL(url);
				} catch (MalformedURLException e) {
				}
			} else if (name.equals(P_LABEL)) {
				getElement().setLabel(realValue);
			} else if (name.equals(P_SITE_TYPE)) {
				Integer ivalue = (Integer) value;
				getElement().setSiteType(ivalue.intValue());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
