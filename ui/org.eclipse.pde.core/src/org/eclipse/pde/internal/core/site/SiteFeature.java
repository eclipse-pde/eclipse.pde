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
import java.util.Vector;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteFeature extends VersionableObject implements ISiteFeature {
	private Vector categories = new Vector();
	private String type;
	private String url;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#addCategories(org.eclipse.pde.internal.core.isite.ISiteCategory)
	 */
	public void addCategories(ISiteCategory[] newCategories)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newCategories.length; i++) {
			ISiteCategory category = newCategories[i];
			((SiteCategory) category).setInTheModel(true);
			categories.add(newCategories[i]);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.INSERT);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#removeCategories(org.eclipse.pde.internal.core.isite.ISiteCategory)
	 */
	public void removeCategories(ISiteCategory[] newCategories)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newCategories.length; i++) {
			ISiteCategory category = newCategories[i];
			((SiteCategory) category).setInTheModel(false);
			categories.remove(newCategories[i]);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getCategories()
	 */
	public ISiteCategory[] getCategories() {
		return (ISiteCategory[]) categories.toArray(
			new ISiteCategory[categories.size()]);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getType()
	 */
	public String getType() {
		return type;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getURL()
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.type;
		this.type = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setURL(java.net.URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_TYPE, oldValue, url);
	}

	protected void parse(Node node) {
		super.parse(node);
		type = getNodeAttribute(node, "type");
		url = getNodeAttribute(node, "url");
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& child.getNodeName().equalsIgnoreCase("category")) {
				SiteCategory category =
					(SiteCategory) getModel().getFactory().createCategory(this);
				((SiteCategory) category).parse(child);
				((SiteCategory) category).setInTheModel(true);
				categories.add(category);
			}
		}
	}

	protected void reset() {
		super.reset();
		type = null;
		url = null;
		categories.clear();
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<feature");
		if (type != null)
			writer.print(" type=\"" + type + "\"");
		if (url != null)
			writer.print(" url=\"" + url + "\"");
		if (id != null)
			writer.print(" id=\"" + getId() + "\"");
		if (version != null)
			writer.print(" version=\"" + version + "\"");
		if (categories.size() > 0) {
			writer.println(">");
			String indent2 = indent + "   ";
			for (int i = 0; i < categories.size(); i++) {
				ISiteCategory category = (ISiteCategory) categories.get(i);
				category.write(indent2, writer);
			}
			writer.println(indent + "</feature>");
		} else
			writer.println("/>");
	}
	
	public IFile getArchiveFile() {
		if (url==null) return null;
		IResource resource = getModel().getUnderlyingResource();
		if (resource==null) return null;
		IProject project = resource.getProject();
		IFile file = project.getFile(new Path(url));
		if (file.exists()) return file;
		return null;
	}
}
