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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.text.DocumentTextNode;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;

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
		IDocumentTextNode old = getTextNode();
		IDocumentTextNode newNode = null;
		if (old == null) {
			newNode = new DocumentTextNode();
			newNode.setEnclosingElement(this);
			addTextNode(newNode);
			newNode.setText(text.trim());
		} else
			old.setText(text.trim());
		firePropertyChanged(this, P_TEXT, old, newNode);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		
		IDocumentNode[] children = getChildNodes();
		String text = getText();
		buffer.append(writeShallow(false));
		if (getAttributeCount() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(sep);
		if (children.length > 0 || text.length() > 0) {
			if (text.length() > 0) {
				buffer.append(getIndent());
				buffer.append("   "); //$NON-NLS-1$
				buffer.append(text);
				buffer.append(sep);
			}
			for (int i = 0; i < children.length; i++) {
				children[i].setLineIndent(getLineIndent() + 3);
				buffer.append(children[i].write(true));
				buffer.append(sep);
			}
		}
		if (getAttributeCount() > 0 || children.length > 0 || text.length() > 0)
			buffer.append(getIndent());

		buffer.append("</" + getXMLTagName() + ">"); //$NON-NLS-1$ //$NON-NLS-2$	
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName()); //$NON-NLS-1$

		IDocumentAttribute[] attrs = getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			if (attrs[i].getAttributeValue().length() > 0)
				buffer.append(sep + getIndent() + "      " + attrs[i].write()); //$NON-NLS-1$
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
