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
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;
import org.w3c.dom.Node;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteCategoryDefinition
	extends SiteObject
	implements ISiteCategoryDefinition {
	private String name;
	private ISiteDescription description;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition#getName()
	 */
	public String getName() {
		return name;
	}
	
	public boolean isValid() {
		return name!=null && getLabel()!=null;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition#setName(java.lang.String)
	 */
	public void setName(String name) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.name;
		this.name = name;
		firePropertyChanged(P_NAME, oldValue, name);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition#getDescription()
	 */
	public ISiteDescription getDescription() {
		return description;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition#setDescription(org.eclipse.pde.internal.core.isite.ISiteDescription)
	 */
	public void setDescription(ISiteDescription description)
		throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESCRIPTION, oldValue, description);
	}

	protected void reset() {
		super.reset();
		name = null;
		description = null;
	}

	protected void parse(Node node) {
		super.parse(node);
		name = getNodeAttribute(node, "name");
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = (Node) children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().equalsIgnoreCase("description")) {
				description = getModel().getFactory().createDescription(this);
				((SiteDescription) description).parse(child);
				((SiteDescription)description).setInTheModel(true);
				break;
			}
		}
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_NAME)) {
			setName(newValue != null ? newValue.toString() : null);
		} else if (
			name.equals(P_DESCRIPTION)
				&& newValue instanceof ISiteDescription) {
			setDescription((ISiteDescription) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<category-def");
		if (name != null)
			writer.print(" name=\"" + name + "\"");
		if (label != null)
			writer.print(" label=\"" + label + "\"");
		if (description != null) {
			writer.println(">");
			description.write(indent + Site.INDENT, writer);
			writer.println(indent + "</category-def>");
		} else
			writer.println("/>");
	}
}
