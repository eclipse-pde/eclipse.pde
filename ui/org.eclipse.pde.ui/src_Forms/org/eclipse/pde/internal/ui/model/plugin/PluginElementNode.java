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
package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.ui.model.*;

public class PluginElementNode extends PluginParentNode
		implements
			IPluginElement {

	private static final long serialVersionUID = 1L;

	private transient ISchemaElement elementInfo;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#createCopy()
	 */
	public IPluginElement createCopy() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttribute(java.lang.String)
	 */
	public IPluginAttribute getAttribute(String name) {
		return (IPluginAttribute)fAttributes.get(name);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributes()
	 */
	public IPluginAttribute[] getAttributes() {
		return (IPluginAttribute[])fAttributes.values().toArray(new IPluginAttribute[fAttributes.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributeCount()
	 */
	public int getAttributeCount() {
		return fAttributes.size();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getText()
	 */
	public String getText() {
		IDocumentTextNode node = getTextNode();
		return node == null ? "" : node.getText(); //$NON-NLS-1$
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String name, String value) throws CoreException {
		setXMLAttribute(name, value);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setText(java.lang.String)
	 */
	public void setText(String text) throws CoreException {
		IDocumentTextNode node = getTextNode();
		if (node == null) {
			node = new DocumentTextNode();
			node.setEnclosingElement(this);
			addTextNode(node);
		}		
		node.setText(text);
		firePropertyChanged(this, P_TEXT, node, node);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		
		IDocumentNode[] children = getChildNodes();
		String text = getText();
		if (children.length > 0 || text.length() > 0) {
			buffer.append(writeShallow(false) + sep);
			if (text.length() > 0)
				buffer.append(getIndent() + "   " + text + sep); //$NON-NLS-1$
			for (int i = 0; i < children.length; i++) {
				children[i].setLineIndent(getLineIndent() + 3);
				buffer.append(children[i].write(true) + sep);
			}
			buffer.append(getIndent() + "</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			buffer.append(writeShallow(true));
		}
	
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		String sep = System.getProperty("line.separator"); //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName()); //$NON-NLS-1$

		IDocumentAttribute[] attrs = getNodeAttributes();
		if (attrs.length == 1) {
			if (attrs[0].getAttributeValue().length() > 0)
				buffer.append(" " + attrs[0].write()); //$NON-NLS-1$
		} else {
			for (int i = 0; i < attrs.length; i++) {
				if (attrs[i].getAttributeValue().length() > 0)
					buffer.append(sep + getIndent() + "      " + attrs[i].write()); //$NON-NLS-1$
			}
		}
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLTagName();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLTagName(name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getElementInfo()
	 */
	public Object getElementInfo() {
		if (elementInfo == null) {
			IDocumentNode node = getParentNode();
			for (;;) {
				if (node == null || node instanceof IPluginExtension)
					break;
				node = node.getParentNode();
			}
			if (node != null) {
				IPluginExtension extension = (IPluginExtension) node;
				ISchema schema = (ISchema)extension.getSchema();
				if (schema != null) {
					elementInfo = schema.findElement(getName());
				}
			}
		}
		return elementInfo;
	}
	
}
