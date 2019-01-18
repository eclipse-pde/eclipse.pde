/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.internal.core.text.DocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class PluginAttribute extends PluginObjectNode implements IPluginAttribute, IDocumentAttributeNode {

	private static final long serialVersionUID = 1L;

	// The plugin attribute interface requires this class to extend PluginObjectNode
	// However, by doing that this class also extends the document
	// element node class - which is wrong when implementing
	// the document attribute node interface
	// To work around this issue, we use an adaptor.
	private final DocumentAttributeNode fAttribute;

	private String fValue;

	/**
	 *
	 */
	public PluginAttribute() {
		super();
		fAttribute = new DocumentAttributeNode();
		fValue = null;
	}

	@Override
	public String getValue() {
		return fValue;
	}

	@Override
	public void setValue(String value) throws CoreException {
		fValue = value;
	}

	@Override
	public void setEnclosingElement(IDocumentElementNode node) {
		fAttribute.setEnclosingElement(node);
	}

	@Override
	public IDocumentElementNode getEnclosingElement() {
		return fAttribute.getEnclosingElement();
	}

	@Override
	public void setNameOffset(int offset) {
		fAttribute.setNameOffset(offset);
	}

	@Override
	public int getNameOffset() {
		return fAttribute.getNameOffset();
	}

	@Override
	public void setNameLength(int length) {
		fAttribute.setNameLength(length);
	}

	@Override
	public int getNameLength() {
		return fAttribute.getNameLength();
	}

	@Override
	public void setValueOffset(int offset) {
		fAttribute.setValueOffset(offset);
	}

	@Override
	public int getValueOffset() {
		return fAttribute.getValueOffset();
	}

	@Override
	public void setValueLength(int length) {
		fAttribute.setValueLength(length);
	}

	@Override
	public int getValueLength() {
		return fAttribute.getValueLength();
	}

	@Override
	public String getAttributeName() {
		return getName();
	}

	@Override
	public String getAttributeValue() {
		return getValue();
	}

	@Override
	public String write() {
		return getName() + "=\"" + getWritableString(getValue()) + "\""; //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public String getWritableString(String source) {
		return super.getWritableString(source).replaceAll("\\r", "&#x0D;") //$NON-NLS-1$ //$NON-NLS-2$
				.replaceAll("\\n", "&#x0A;"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public void setAttributeName(String name) throws CoreException {
		setName(name);
	}

	@Override
	public void setAttributeValue(String value) throws CoreException {
		setValue(value);
	}

	@Override
	public void reconnect(IDocumentElementNode parent) {
		// Inconsistency in model
		// A document attribute node should not extend plugin object because plugin object extends
		// document element node
		super.reconnect(parent, getModel());
		fAttribute.reconnect(parent);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		// Although attributes cannot be copied directly
		writer.write(write());
	}

}
