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
package org.eclipse.pde.internal.core.feature;

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.w3c.dom.*;

public abstract class FeatureObject
	extends PlatformObject
	implements IFeatureObject, ISourceObject {
	transient IFeatureModel model;
	transient IFeatureObject parent;
	protected String label;
	boolean inTheModel;
	protected int[] range;

	void setInTheModel(boolean value) {
		inTheModel = value;
	}

	public boolean isInTheModel() {
		return inTheModel;
	}

	protected void ensureModelEditable() throws CoreException {
		if (!model.isEditable()) {
			throwCoreException(PDECore.getResourceString("FeatureObject.readOnlyChange")); //$NON-NLS-1$
		}
	}
	protected void firePropertyChanged(
		String property,
		Object oldValue,
		Object newValue) {
		firePropertyChanged(this, property, oldValue, newValue);
	}
	protected void firePropertyChanged(
		IFeatureObject object,
		String property,
		Object oldValue,
		Object newValue) {
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelObjectChanged(object, property, oldValue, newValue);
		}
	}
	protected void fireStructureChanged(IFeatureObject child, int changeType) {
		fireStructureChanged(new IFeatureObject[] { child }, changeType);
	}
	protected void fireStructureChanged(
		IFeatureObject[] children,
		int changeType) {
		IFeatureModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(new ModelChangedEvent(provider, changeType, children, null));
		}
	}
	public IFeature getFeature() {
		return model.getFeature();
	}
	public String getLabel() {
		return label;
	}

	public String getTranslatableLabel() {
		if (label == null)
			return ""; //$NON-NLS-1$
		return model.getResourceString(label);
	}
	public IFeatureModel getModel() {
		return model;
	}
	String getNodeAttribute(Node node, String name) {
		Node attribute = node.getAttributes().getNamedItem(name);
		if (attribute != null)
			return attribute.getNodeValue();
		return null;
	}

	int getIntegerAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
			}
		}
		return 0;
	}
	
	boolean getBooleanAttribute(Node node, String name) {
		String value = getNodeAttribute(node, name);
		if (value != null) {
			return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
		return false;
	}
	
	protected String getNormalizedText(String source) {
		String result = source.replace('\t', ' ');
		result = result.trim();

		return result;
		/*
		 boolean skip = false;
		
		StringBuffer buff = new StringBuffer();
		for (int i=0; i<result.length(); i++) {
			char c = result.charAt(i);
			if (c=='\n') {
				skip = true;
			}
			else if (c==' ') {
				if (skip) continue;
			}
			else skip = false;
			
			buff.append(c);
		}
		return buff.toString();
		*/
	}

	public IFeatureObject getParent() {
		return parent;
	}

	protected void parse(Node node, Hashtable lineTable) {
		label = getNodeAttribute(node, "label"); //$NON-NLS-1$
	}

	protected void reset() {
		label = null;
	}

	public void setLabel(String newLabel) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.label;
		label = newLabel;
		firePropertyChanged(P_LABEL, oldValue, newLabel);
	}
	protected void throwCoreException(String message) throws CoreException {
		Status status =
			new Status(IStatus.ERROR, PDECore.getPluginId(), IStatus.OK, message, null);
		CoreException ce = new CoreException(status);
		ce.fillInStackTrace();
		throw ce;
	}

	public static String getWritableString(String source) {
		if (source == null)
			return ""; //$NON-NLS-1$
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);
			switch (c) {
				case '&' :
					buf.append("&amp;"); //$NON-NLS-1$
					break;
				case '<' :
					buf.append("&lt;"); //$NON-NLS-1$
					break;
				case '>' :
					buf.append("&gt;"); //$NON-NLS-1$
					break;
				case '\'' :
					buf.append("&apos;"); //$NON-NLS-1$
					break;
				case '\"' :
					buf.append("&quot;"); //$NON-NLS-1$
					break;
				default :
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_LABEL)) {
			setLabel(newValue != null ? newValue.toString() : null);
		}
	}

	public void write(String indent, PrintWriter writer) {
	}
	public void setModel(IFeatureModel model) {
		this.model = model;
	}
	
	public void setParent(IFeatureObject parent) {
		this.parent = parent;
	}
	
	void bindSourceLocation(Node node, Map lineTable) {
		Integer[] lines = (Integer[]) lineTable.get(node);
		if (lines != null) {
			range = new int[2];
			range[0] = lines[0].intValue();
			range[1] = lines[1].intValue();
		}
	}

	public int getStartLine() {
		if (range == null)
			return -1;
		return range[0];
	}
	public int getStopLine() {
		if (range == null)
			return -1;
		return range[1];
	}
}
