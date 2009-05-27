/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.IPluginImport;

public class PluginImportNode extends PluginObjectNode implements IPluginImport {

	private static final long serialVersionUID = 1L;

	public PluginImportNode(String id) {
		super();
		String name = "plugin"; //$NON-NLS-1$
		try {
			if (id == null)
				id = ""; //$NON-NLS-1$
			PluginAttribute attr = new PluginAttribute();
			attr.setName(name);
			attr.setEnclosingElement(this);
			getNodeAttributesMap().put(name, attr);
			attr.setValue(id);
		} catch (CoreException e) {
		}
	}

	public PluginImportNode() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isReexported()
	 */
	public boolean isReexported() {
		String value = getXMLAttributeValue(P_REEXPORTED);
		return value != null && value.equals("true"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isOptional()
	 */
	public boolean isOptional() {
		String value = getXMLAttributeValue(P_OPTIONAL);
		return value != null && value.equals("true"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setReexported(boolean)
	 */
	public void setReexported(boolean value) throws CoreException {
		setXMLAttribute(P_REEXPORTED, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setOptional(boolean)
	 */
	public void setOptional(boolean value) throws CoreException {
		setXMLAttribute(P_OPTIONAL, value ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#getMatch()
	 */
	public int getMatch() {
		String match = getXMLAttributeValue(P_MATCH);
		if (match == null || match.trim().length() == 0)
			return IMatchRules.NONE;
		if (match.equals("compatible")) //$NON-NLS-1$
			return IMatchRules.COMPATIBLE;
		if (match.equals("perfect")) //$NON-NLS-1$
			return IMatchRules.PERFECT;
		if (match.equals("equivalent")) //$NON-NLS-1$
			return IMatchRules.EQUIVALENT;
		return IMatchRules.GREATER_OR_EQUAL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#getVersion()
	 */
	public String getVersion() {
		return getXMLAttributeValue(P_VERSION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setMatch(int)
	 */
	public void setMatch(int match) throws CoreException {
		switch (match) {
			case IMatchRules.GREATER_OR_EQUAL :
				setXMLAttribute(P_MATCH, "greaterOrEqual"); //$NON-NLS-1$
				break;
			case IMatchRules.EQUIVALENT :
				setXMLAttribute(P_MATCH, "equivalent"); //$NON-NLS-1$
				break;
			case IMatchRules.COMPATIBLE :
				setXMLAttribute(P_MATCH, "compatible"); //$NON-NLS-1$
				break;
			case IMatchRules.PERFECT :
				setXMLAttribute(P_MATCH, "perfect"); //$NON-NLS-1$
				break;
			default :
				setXMLAttribute(P_MATCH, null);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		setXMLAttribute(P_VERSION, version);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue("plugin"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute("plugin", id); //$NON-NLS-1$
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
		StringBuffer buffer = new StringBuffer("<import"); //$NON-NLS-1$
		appendAttribute(buffer, "plugin"); //$NON-NLS-1$
		appendAttribute(buffer, P_VERSION);
		appendAttribute(buffer, P_MATCH);
		appendAttribute(buffer, P_REEXPORTED, "false"); //$NON-NLS-1$
		appendAttribute(buffer, P_OPTIONAL, "false"); //$NON-NLS-1$

		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

}
