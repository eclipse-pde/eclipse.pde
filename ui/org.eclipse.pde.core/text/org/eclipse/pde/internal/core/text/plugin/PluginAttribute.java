/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.ISharedPluginModel;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;

public class PluginAttribute extends PluginObjectNode implements
		IPluginAttribute, IDocumentAttribute {
	
	private static final long serialVersionUID = 1L;

	private transient IDocumentNode fEnclosingElement;
	private transient int fNameOffset = -1;
	private transient int fNameLength = -1;
	private transient int fValueOffset = -1;
	private transient int fValueLength = -1;

	private String fValue;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#getValue()
	 */
	public String getValue() {
		return fValue;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginAttribute#setValue(java.lang.String)
	 */
	public void setValue(String value) throws CoreException {
		fValue = value;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setEnclosingElement(org.eclipse.pde.internal.ui.model.IDocumentNode)
	 */
	public void setEnclosingElement(IDocumentNode node) {
		fEnclosingElement = node;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getEnclosingElement()
	 */
	public IDocumentNode getEnclosingElement() {
		return fEnclosingElement;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameOffset(int)
	 */
	public void setNameOffset(int offset) {
		fNameOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameOffset()
	 */
	public int getNameOffset() {
		return fNameOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setNameLength(int)
	 */
	public void setNameLength(int length) {
		fNameLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getNameLength()
	 */
	public int getNameLength() {
		return fNameLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueOffset(int)
	 */
	public void setValueOffset(int offset) {
		fValueOffset = offset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueOffset()
	 */
	public int getValueOffset() {
		return fValueOffset;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#setValueLength(int)
	 */
	public void setValueLength(int length) {
		fValueLength = length;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getValueLength()
	 */
	public int getValueLength() {
		return fValueLength;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeName()
	 */
	public String getAttributeName() {
		return getName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#getAttributeValue()
	 */
	public String getAttributeValue() {
		return getValue();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.IDocumentAttribute#write()
	 */
	public String write() {
		return getName() + "=\"" + getWritableString(getValue()) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getWritableString(String source) {
		return super
				.getWritableString(source)
				.replaceAll("\\r", "&#x0D;") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\\n", "&#x0A;"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public void setAttributeName(String name) throws CoreException {
		setName(name);
	}
	
	public void setAttributeValue(String value) throws CoreException {
		setValue(value);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentAttribute#reconnectAttribute()
	 */
	public void reconnect(ISharedPluginModel model, ISchema schema, IDocumentNode parent) {
		super.reconnect(model, schema, parent);
		// Transient field:  Enclosing element
		// Essentially is the parent (an element)
		// Note: Parent field from plugin document node parent seems to be 
		// null; but, we will set it any ways
		fEnclosingElement = parent;
		// Transient field:  Name Length
		fNameLength = -1;
		// Transient field:  Name Offset
		fNameOffset = -1;
		// Transient field:  Value Length
		fValueLength = -1;
		// Transient field:  Value Offset
		fValueOffset = -1;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.plugin.PluginObjectNode#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		// Although attributes cannot be copied directly
		writer.write(write());
	}
	
}
