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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class PluginExtensionNode extends PluginParentNode implements IPluginExtension {

	private static final long serialVersionUID = 1L;
	private transient ISchema fSchema;

	@Override
	public String getPoint() {
		return getXMLAttributeValue(P_POINT);
	}

	@Override
	public void setPoint(String point) throws CoreException {
		setXMLAttribute(P_POINT, point);
	}

	@Override
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}

	@Override
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}

	@Override
	public String getTranslatedName() {
		String name = getName();
		if (name != null && name.trim().length() > 0) {
			return getResourceString(name);
		}
		String point = getPoint();
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(point);
		return schema == null ? "" : schema.getName(); //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}

	@Override
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}

	@Override
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuilder buffer = new StringBuilder();
		if (indent) {
			buffer.append(getIndent());
		}
		buffer.append(writeShallow(false));
		IDocumentElementNode[] children = getChildNodes();
		for (IDocumentElementNode childNode : children) {
			childNode.setLineIndent(getLineIndent() + 3);
			buffer.append(sep + childNode.write(true));
		}
		buffer.append(sep + getIndent() + "</extension>"); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write(true));
	}

	@Override
	public String writeShallow(boolean terminate) {
		String sep = getLineDelimiter();
		String attrIndent = "      "; //$NON-NLS-1$
		StringBuilder buffer = new StringBuilder("<extension"); //$NON-NLS-1$
		IDocumentAttributeNode attr = getDocumentAttribute(P_ID);
		if (attr != null && attr.getAttributeValue().trim().length() > 0) {
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		}
		attr = getDocumentAttribute(P_NAME);
		if (attr != null && attr.getAttributeValue().trim().length() > 0) {
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		}
		attr = getDocumentAttribute(P_POINT);
		if (attr != null && attr.getAttributeValue().trim().length() > 0) {
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		}
		if (terminate) {
			buffer.append("/"); //$NON-NLS-1$
		}
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public Object getSchema() {
		if (fSchema == null) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			fSchema = registry.getSchema(getPoint());
		} else if (fSchema.isDisposed()) {
			fSchema = null;
		}
		return fSchema;
	}

	@Override
	public void reconnect(IDocumentElementNode parent, IModel model) {
		super.reconnect(parent, model);
		// Transient Field:  Schema
		// Not necessary to reconnect schema.
		// getSchema will retrieve the schema on demand if it is null
		fSchema = null;
	}

}
