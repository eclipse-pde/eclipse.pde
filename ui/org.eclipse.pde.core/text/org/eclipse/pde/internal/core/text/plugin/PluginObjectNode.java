/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.io.PrintWriter;
import java.nio.charset.Charset;
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
import org.eclipse.pde.internal.core.plugin.IWritableDelimiter;
import org.eclipse.pde.internal.core.text.DocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentRange;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.util.PDEXMLHelper;

public class PluginObjectNode extends DocumentElementNode implements IPluginObject, IWritableDelimiter {

	private transient boolean fInTheModel;
	private transient ISharedPluginModel fModel;

	private static final long serialVersionUID = 1L;
	private String fName;

	/**
	 *
	 */
	public PluginObjectNode() {
		super();
	}

	@Override
	public ISharedPluginModel getModel() {
		return fModel;
	}

	@Override
	public IPluginModelBase getPluginModel() {
		return (IPluginModelBase) fModel;
	}

	@Override
	public String getName() {
		return fName;
	}

	@Override
	public boolean isInTheModel() {
		return fInTheModel;
	}

	@Override
	public String getTranslatedName() {
		return getResourceString(getName());
	}

	@Override
	public IPluginObject getParent() {
		return (IPluginObject) getParentNode();
	}

	@Override
	public IPluginBase getPluginBase() {
		return fModel != null ? ((IPluginModelBase) fModel).getPluginBase() : null;
	}

	@Override
	public String getResourceString(String key) {
		return fModel != null ? fModel.getResourceString(key) : key;
	}

	@Override
	public void setName(String name) throws CoreException {
		fName = name;
	}

	@Override
	public boolean isValid() {
		return false;
	}

	@Override
	public void write(String indent, PrintWriter writer) {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public void setInTheModel(boolean inModel) {
		fInTheModel = inModel;
	}

	public void setModel(ISharedPluginModel model) {
		fModel = model;
	}

	@Override
	public boolean setXMLAttribute(String name, String value) {
		// Overrided by necessity - dealing with different objects
		String oldValue = getXMLAttributeValue(name);
		if (oldValue != null && oldValue.equals(value)) {
			return false;
		}
		PluginAttribute attr = (PluginAttribute) getNodeAttributesMap().get(name);
		try {
			if (value == null) {
				value = ""; //$NON-NLS-1$
			}
			if (attr == null) {
				attr = new PluginAttribute();
				attr.setName(name);
				attr.setEnclosingElement(this);
				attr.setModel(getModel());
				getNodeAttributesMap().put(name, attr);
			}
			attr.setValue(value);
		} catch (CoreException e) {
		}
		if (fInTheModel) {
			firePropertyChanged(attr.getEnclosingElement(), attr.getAttributeName(), oldValue, value);
		}
		return true;
	}

	protected void firePropertyChanged(IDocumentRange node, String property, Object oldValue, Object newValue) {
		if (fModel.isEditable()) {
			fModel.fireModelObjectChanged(node, property, oldValue, newValue);
		}
	}

	protected void fireStructureChanged(IPluginObject child, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType, new Object[] {child}, null);
			fireModelChanged(e);
		}
	}

	protected void fireStructureChanged(IPluginObject[] children, int changeType) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangedEvent e = new ModelChangedEvent(fModel, changeType, children, null);
			fireModelChanged(e);
		}
	}

	private void fireModelChanged(IModelChangedEvent e) {
		IModel model = getModel();
		if (model.isEditable() && model instanceof IModelChangeProvider) {
			IModelChangeProvider provider = (IModelChangeProvider) model;
			provider.fireModelChanged(e);
		}
	}

	public String getWritableString(String source) {
		return PDEXMLHelper.getWritableString(source);
	}

	protected void appendAttribute(StringBuilder buffer, String attrName) {
		appendAttribute(buffer, attrName, ""); //$NON-NLS-1$
	}

	protected void appendAttribute(StringBuilder buffer, String attrName, String defaultValue) {
		IDocumentAttributeNode attr = getDocumentAttribute(attrName);
		if (attr != null) {
			String value = attr.getAttributeValue();
			if (value != null && value.trim().length() > 0 && !value.equals(defaultValue)) {
				buffer.append(" " + attr.write()); //$NON-NLS-1$
			}
		}
	}

	@Override
	public String getLineDelimiter() {
		ISharedPluginModel model = getModel();
		IDocument document = ((IEditingModel) model).getDocument();
		return TextUtilities.getDefaultLineDelimiter(document);
	}

	@Override
	public void addChildNode(IDocumentElementNode child, int position) {
		super.addChildNode(child, position);
		((IPluginObject) child).setInTheModel(true);
	}

	@Override
	public String toString() {
		return write(false);
	}

	@Override
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient field:  In The Model
		// Value set to true when added to the parent; however, serialized
		// children's value remains unchanged.  Since, reconnect and add calls
		// are made so close together, set value to true for parent and all
		// children
		fInTheModel = true;
		// Transient field:  Model
		if (model instanceof ISharedPluginModel) {
			fModel = (ISharedPluginModel) model;
		}
	}

	@Override
	public void writeDelimeter(PrintWriter writer) {
		// NO-OP
		// Child classes to override
	}

	@Override
	public String getXMLAttributeValue(String name) {
		// Overrided by necessity - dealing with different objects
		PluginAttribute attr = (PluginAttribute) getNodeAttributesMap().get(name);
		return attr == null ? null : attr.getValue();
	}

	@Override
	public String write(boolean indent) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}

	@Override
	public String writeShallow(boolean terminate) {
		// Used by text edit operations
		// Subclasses to override
		return ""; //$NON-NLS-1$
	}

	@Override
	protected Charset getFileEncoding() {
		if ((fModel != null) && (fModel instanceof IEditingModel)) {
			return ((IEditingModel) fModel).getCharset();
		}
		return super.getFileEncoding();
	}

}
