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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginExtensionPointNode extends PluginObjectNode
		implements
			IPluginExtensionPoint {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getFullId()
	 */
	public String getFullId() {
		return getPluginBase().getId() + "." + getId();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getSchema()
	 */
	public String getSchema() {
		return getXMLAttributeValue("schema");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#setSchema(java.lang.String)
	 */
	public void setSchema(String schema) throws CoreException {
		setXMLAttribute(P_SCHEMA, schema);
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
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		return indent ? getIndent() + writeShallow(true) : writeShallow(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		StringBuffer buffer = new StringBuffer("<extension-point");
		appendAttribute(buffer, P_ID);
		appendAttribute(buffer, P_NAME);
		appendAttribute(buffer, P_SCHEMA);

		if (terminate)
			buffer.append("/");
		buffer.append(">");
		return buffer.toString();
	}
	
	
}
