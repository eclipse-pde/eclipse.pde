/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.plugin;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;

public class PluginLibraryNode extends PluginObjectNode implements IPluginLibrary {

	private static final long serialVersionUID = 1L;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getContentFilters()
	 */
	public String[] getContentFilters() {
		IDocumentElementNode[] children = getChildNodes();
		ArrayList result = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode) children[i];
			if (node.getName().equals(P_EXPORTED)) {
				String name = children[i].getXMLAttributeValue(P_NAME);
				if (name != null && !name.equals("*")) { //$NON-NLS-1$
					int index = name.indexOf(".*"); //$NON-NLS-1$
					if (index != -1)
						name = name.substring(0, index);
					result.add(name);
				}
			}
		}
		return (String[]) result.toArray(new String[result.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getPackages()
	 */
	public String[] getPackages() {
		return new String[0];
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#isExported()
	 */
	public boolean isExported() {
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode) children[i];
			if (node.getName().equals(P_EXPORTED))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#isFullyExported()
	 */
	public boolean isFullyExported() {
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			PluginObjectNode node = (PluginObjectNode) children[i];
			if (node.getName().equals(P_EXPORTED)) {
				String name = children[i].getXMLAttributeValue(P_NAME);
				if (name != null && name.equals("*")) //$NON-NLS-1$
					return true;
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#getType()
	 */
	public String getType() {
		String type = getXMLAttributeValue(P_TYPE);
		return (type != null && type.equals("resource")) ? IPluginLibrary.RESOURCE : IPluginLibrary.CODE; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setContentFilters(java.lang.String[])
	 */
	public void setContentFilters(String[] filters) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#addContentFilter(java.lang.String)
	 */
	public void addContentFilter(String filter) throws CoreException {
		PluginElementNode node = new PluginElementNode();
		node.setXMLTagName(P_EXPORTED);
		node.setParentNode(this);
		node.setModel(getModel());
		node.setXMLAttribute(P_NAME, "*".equals(filter) || filter.endsWith(".*") ? filter : filter + ".*"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		addContentFilter(node);
	}

	public void addContentFilter(PluginElementNode node) {
		addChildNode(node);
		if (isInTheModel()) {
			node.setInTheModel(true);
			fireStructureChanged(node, IModelChangedEvent.INSERT);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#removeContentFilter(java.lang.String)
	 */
	public void removeContentFilter(String filter) throws CoreException {
		if (!filter.endsWith(".*")) //$NON-NLS-1$
			filter += ".*"; //$NON-NLS-1$
		IDocumentElementNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			if (children[i].getXMLTagName().equals(P_EXPORTED) && filter.equals(children[i].getXMLAttributeValue(P_NAME))) {
				removeContentFilter((PluginElementNode) children[i]);
			}
		}
	}

	public void removeContentFilter(PluginElementNode node) {
		removeChildNode(node);
		if (isInTheModel()) {
			node.setInTheModel(false);
			fireStructureChanged(node, IModelChangedEvent.REMOVE);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setPackages(java.lang.String[])
	 */
	public void setPackages(String[] packages) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setExported(boolean)
	 */
	public void setExported(boolean exported) throws CoreException {
		IDocumentElementNode[] children = getChildNodes();
		boolean alreadyExported = false;
		for (int i = 0; i < children.length; i++) {
			if (children[i].getXMLTagName().equals(P_EXPORTED)) {
				if (!"*".equals(children[i].getXMLAttributeValue(P_NAME))) { //$NON-NLS-1$
					removeContentFilter((PluginElementNode) children[i]);
				} else {
					alreadyExported = true;
					if (!exported) {
						removeContentFilter((PluginElementNode) children[i]);
					}
				}
			}
		}
		if (exported && !alreadyExported) {
			addContentFilter("*"); //$NON-NLS-1$
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginLibrary#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#getName()
	 */
	public String getName() {
		return getXMLAttributeValue(P_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginObject#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		setXMLAttribute(P_NAME, name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		String sep = getLineDelimiter();
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());

		IDocumentElementNode[] children = getChildNodes();
		if (children.length > 0) {
			buffer.append(writeShallow(false) + sep);
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
		StringBuffer buffer = new StringBuffer("<" + getXMLTagName()); //$NON-NLS-1$

		IDocumentAttributeNode[] attrs = getNodeAttributes();
		for (int i = 0; i < attrs.length; i++) {
			appendAttribute(buffer, attrs[i].getAttributeName());
		}
		if (terminate)
			buffer.append("/"); //$NON-NLS-1$
		buffer.append(">"); //$NON-NLS-1$
		return buffer.toString();
	}

	public String toString() {
		return getName();
	}

}
