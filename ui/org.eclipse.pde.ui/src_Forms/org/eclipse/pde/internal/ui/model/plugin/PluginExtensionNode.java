/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.*;
import org.eclipse.pde.internal.ui.model.*;

public class PluginExtensionNode extends PluginParentNode
		implements
			IPluginExtension {
	private static final long serialVersionUID = 1L;
	private transient ISchema fSchema;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#getPoint()
	 */
	public String getPoint() {
		return getXMLAttributeValue(P_POINT);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#setPoint(java.lang.String)
	 */
	public void setPoint(String point) throws CoreException {
		setXMLAttribute(P_POINT, point);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getTranslatedName()
	 */
	public String getTranslatedName() {
		String name = getName();
		if (name != null && name.trim().length() > 0)
			return getResourceString(name);
		String point = getPoint();
		ISchema schema = PDECore.getDefault().getSchemaRegistry().getSchema(point);
		return schema == null ? "" : schema.getName();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		buffer.append(writeShallow(false));		
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			children[i].setLineIndent(getLineIndent() + 3);
			buffer.append(sep + children[i].write(true));
		}
		buffer.append(sep + getIndent() + "</extension>"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		String sep = getLineDelimiter();
		String attrIndent = "      "; //$NON-NLS-1$
		StringBuffer buffer = new StringBuffer("<extension"); //$NON-NLS-1$
		IDocumentAttribute attr = getDocumentAttribute(P_ID);
		if (attr != null && attr.getAttributeValue().trim().length() > 0)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		attr = getDocumentAttribute(P_NAME);
		if (attr != null && attr.getAttributeValue().trim().length() > 0)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		attr = getDocumentAttribute(P_POINT);
		if (attr != null && attr.getAttributeValue().trim().length() > 0)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#getSchema()
	 */
	public Object getSchema() {
		if (fSchema == null) {
			SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
			fSchema = registry.getSchema(getPoint());
		} else if (fSchema.isDisposed()) {
			fSchema = null;
		}
		return fSchema;
	}
}
