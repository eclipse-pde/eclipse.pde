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
package org.eclipse.pde.internal.ui.model.plugin;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

/**
 * @author melhem
 *  
 */
public class PluginObjectNode extends PluginDocumentNode
		implements
			IPluginObject {
	private String fName;
	private boolean fInTheModel;
	private transient ISharedPluginModel fModel;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getModel()
	 */
	public ISharedPluginModel getModel() {
		return fModel;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginModel()
	 */
	public IPluginModelBase getPluginModel() {
		return (IPluginModelBase) fModel;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return fName;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isInTheModel()
	 */
	public boolean isInTheModel() {
		return fInTheModel;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		return getResourceString(getName());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getParent()
	 */
	public IPluginObject getParent() {
		return (IPluginObject) getParentNode();
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getPluginBase()
	 */
	public IPluginBase getPluginBase() {
		return fModel != null
				? ((IPluginModelBase) fModel).getPluginBase()
				: null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getResourceString(java.lang.String)
	 */
	public String getResourceString(String key) {
		return fModel != null ? fModel.getResourceString(key) : key;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		fName = name;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#isValid()
	 */
	public boolean isValid() {
		return false;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String,
	 *      java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setInTheModel(boolean)
	 */
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}

	public void setModel(ISharedPluginModel model) {
		fModel = model;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#setXMLAttribute(java.lang.String,
	 *      java.lang.String)
	 */
	public void setXMLAttribute(String name, String value) {
		String oldValue = getXMLAttributeValue(name);
		PluginAttribute attr = (PluginAttribute) fAttributes.get(name);
		try {
			if (value == null)
				value = "";
				if (attr == null) {
					attr = new PluginAttribute();
					attr.setName(name);
					attr.setEnclosingElement(this);
					fAttributes.put(name, attr);
				}
				attr.setValue(value == null ? "" : value);
		} catch (CoreException e) {
		}
		if (fInTheModel)
			firePropertyChanged(attr.getEnclosingElement(), attr
					.getAttributeName(), oldValue, value);
	}

	protected void firePropertyChanged(IDocumentNode node, String property,
			Object oldValue, Object newValue) {
		if (fModel.isEditable() && fModel instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) fModel;
			provider.fireModelObjectChanged(node, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IPluginObject child, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType,
					new Object[]{child}, null);
			fireModelChanged(e);
		}
	}

	protected void fireModelChanged(IModelChangedEvent e) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(e);
		}
	}

	public String getWritableString(String source) {
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
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#writeShallow()
	 */
	public String writeShallow(boolean terminate) {
		return "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#write()
	 */
	public String write(boolean indent) {
		return "";
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof PluginObjectNode))
			return false;

		PluginObjectNode node = (PluginObjectNode) obj;
		if (!node.getXMLTagName().equals(getXMLTagName()))
			return false;

		if (node.getChildNodes().length != getChildNodes().length)
			return false;

		for (int i = 0; i < node.getChildNodes().length; i++) {
			if (!node.getChildAt(i).equals(getChildAt(i)))
				return false;
		}

		if (node.getNodeAttributes().length != getNodeAttributes().length)
			return false;
		for (int i = 0; i < node.getNodeAttributes().length; i++) {
			if (!node.getNodeAttributes()[i].equals(getNodeAttributes()[i]))
				return false;
		}

		return true;
	}
	protected void appendAttribute(StringBuffer buffer, String attrName) {
		appendAttribute(buffer, attrName, "");
	}
	
	protected void appendAttribute(StringBuffer buffer, String attrName, String defaultValue) {
		IDocumentAttribute attr = getDocumentAttribute(attrName);
		if (attr != null) {
			String value = attr.getAttributeValue();
			if (value != null && value.trim().length() > 0 && !value.equals(defaultValue))
				buffer.append(" " + attr.write());
		}
	}

}