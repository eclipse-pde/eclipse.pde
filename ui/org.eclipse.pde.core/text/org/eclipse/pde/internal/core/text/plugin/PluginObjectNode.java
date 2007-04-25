/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.plugin.IWritableDelimeter;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentObject;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;

public class PluginObjectNode extends PluginDocumentNode implements
		IPluginObject, IDocumentObject, IWritableDelimeter {

	// TODO: MP: CCP TOUCH
	private transient boolean fInTheModel;
	// TODO: MP: CCP TOUCH
	private transient ISharedPluginModel fModel;
	
	private static final long serialVersionUID = 1L;
	private String fName;

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
		if (oldValue != null && oldValue.equals(value))
			return;
		PluginAttribute attr = (PluginAttribute) fAttributes.get(name);
		try {
			if (value == null)
				value = ""; //$NON-NLS-1$
				if (attr == null) {
					attr = new PluginAttribute();
					attr.setName(name);
					attr.setEnclosingElement(this);
					fAttributes.put(name, attr);
				}
				attr.setValue(value == null ? "" : value); //$NON-NLS-1$
		} catch (CoreException e) {
		}
		if (fInTheModel)
			firePropertyChanged(attr.getEnclosingElement(), attr
					.getAttributeName(), oldValue, value);
	}

	protected void firePropertyChanged(IDocumentRange node, String property,
			Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			fModel.fireModelObjectChanged(node, property, oldValue, newValue);
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
	
	protected void fireStructureChanged(IPluginObject[] children, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType,
					children, null);
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
		return PDEXMLHelper.getWritableString(source);
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#writeShallow()
	 */
	public String writeShallow(boolean terminate) {
		return ""; //$NON-NLS-1$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.model.IDocumentNode#write()
	 */
	public String write(boolean indent) {
		return ""; //$NON-NLS-1$
	}

	protected void appendAttribute(StringBuffer buffer, String attrName) {
		appendAttribute(buffer, attrName, ""); //$NON-NLS-1$
	}
	
	protected void appendAttribute(StringBuffer buffer, String attrName, String defaultValue) {
		IDocumentAttribute attr = getDocumentAttribute(attrName);
		if (attr != null) {
			String value = attr.getAttributeValue();
			if (value != null && value.trim().length() > 0 && !value.equals(defaultValue))
				buffer.append(" " + attr.write()); //$NON-NLS-1$
		}
	}
	
	public String getLineDelimiter() {
		// TODO: MP: CCP TOUCH
		ISharedPluginModel model = getModel();
		IDocument document = ((IEditingModel)model).getDocument();
		return TextUtilities.getDefaultLineDelimiter(document);
	}
	
	public void addChildNode(IDocumentNode child, int position) {
		super.addChildNode(child, position);
		((IPluginObject)child).setInTheModel(true);
	}
	
	public String toString() {
		return write(false);
	}
	
	public boolean isRoot() {
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginDocumentNode#reconnect(org.eclipse.pde.core.plugin.ISharedPluginModel, org.eclipse.pde.internal.core.ischema.ISchema, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public void reconnect(ISharedPluginModel model, ISchema schema, IDocumentNode parent) {
		// TODO: MP: CCP TOUCH
		super.reconnect(model, schema, parent);
		// Transient field:  In The Model
		fInTheModel = false;
		// Transient field:  Model
		fModel = model;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.plugin.IWritableDelimeter#writeDelimeter(java.io.PrintWriter)
	 */
	public void writeDelimeter(PrintWriter writer) {
		// NO-OP
		// Child classes to override
	}
	
}
