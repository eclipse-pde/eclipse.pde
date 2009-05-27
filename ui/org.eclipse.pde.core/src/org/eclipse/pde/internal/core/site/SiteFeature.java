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
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SiteFeature extends VersionableObject implements ISiteFeature {
	private static final long serialVersionUID = 1L;
	private Vector fCategories = new Vector();
	private String fType;
	private String fUrl;
	private String fOS;
	private String fWS;
	private String fArch;
	private String fNL;
	private boolean fIsPatch;

	public boolean isValid() {
		if (fUrl == null)
			return false;
		for (int i = 0; i < fCategories.size(); i++) {
			ISiteCategory category = (ISiteCategory) fCategories.get(i);
			if (!category.isValid())
				return false;
		}
		return true;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#addCategories(org.eclipse.pde.internal.core.isite.ISiteCategory)
	 */
	public void addCategories(ISiteCategory[] newCategories) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newCategories.length; i++) {
			ISiteCategory category = newCategories[i];
			((SiteCategory) category).setInTheModel(true);
			fCategories.add(newCategories[i]);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.INSERT);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#removeCategories(org.eclipse.pde.internal.core.isite.ISiteCategory)
	 */
	public void removeCategories(ISiteCategory[] newCategories) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newCategories.length; i++) {
			ISiteCategory category = newCategories[i];
			((SiteCategory) category).setInTheModel(false);
			fCategories.remove(newCategories[i]);
		}
		fireStructureChanged(newCategories, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getCategories()
	 */
	public ISiteCategory[] getCategories() {
		return (ISiteCategory[]) fCategories.toArray(new ISiteCategory[fCategories.size()]);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getType()
	 */
	public String getType() {
		return fType;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getURL()
	 */
	public String getURL() {
		return fUrl;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fType;
		this.fType = type;
		firePropertyChanged(P_TYPE, oldValue, fType);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setURL(java.net.URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.fUrl;
		this.fUrl = url;
		firePropertyChanged(P_TYPE, oldValue, url);
	}

	protected void parse(Node node) {
		super.parse(node);
		fType = getNodeAttribute(node, "type"); //$NON-NLS-1$
		fUrl = getNodeAttribute(node, "url"); //$NON-NLS-1$
		fOS = getNodeAttribute(node, "os"); //$NON-NLS-1$
		fNL = getNodeAttribute(node, "nl"); //$NON-NLS-1$
		fWS = getNodeAttribute(node, "ws"); //$NON-NLS-1$
		fArch = getNodeAttribute(node, "arch"); //$NON-NLS-1$
		String value = getNodeAttribute(node, "patch"); //$NON-NLS-1$
		fIsPatch = value != null && value.equals("true"); //$NON-NLS-1$
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equalsIgnoreCase("category")) { //$NON-NLS-1$
				SiteCategory category = (SiteCategory) getModel().getFactory().createCategory(this);
				category.parse(child);
				category.setInTheModel(true);
				fCategories.add(category);
			}
		}
	}

	protected void reset() {
		super.reset();
		fType = null;
		fUrl = null;
		fOS = null;
		fWS = null;
		fArch = null;
		fNL = null;
		fIsPatch = false;
		fCategories.clear();
	}

	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_ARCH)) {
			setArch(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_NL)) {
			setNL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_OS)) {
			setOS(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_WS)) {
			setWS(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_PATCH)) {
			setIsPatch(((Boolean) newValue).booleanValue());
		} else {
			super.restoreProperty(name, oldValue, newValue);
		}
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<feature"); //$NON-NLS-1$
		if (fType != null)
			writer.print(" type=\"" + fType + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fUrl != null)
			writer.print(" url=\"" + fUrl + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (id != null)
			writer.print(" id=\"" + getId() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (version != null)
			writer.print(" version=\"" + getVersion() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (label != null)
			writer.print(" label=\"" + getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fOS != null)
			writer.print(" os=\"" + fOS + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fWS != null)
			writer.print(" ws=\"" + fWS + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fNL != null)
			writer.print(" nl=\"" + fNL + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fArch != null)
			writer.print(" arch=\"" + fArch + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (fIsPatch)
			writer.print(" patch=\"true\""); //$NON-NLS-1$
		if (fCategories.size() > 0) {
			writer.println(">"); //$NON-NLS-1$
			String indent2 = indent + "   "; //$NON-NLS-1$
			for (int i = 0; i < fCategories.size(); i++) {
				ISiteCategory category = (ISiteCategory) fCategories.get(i);
				category.write(indent2, writer);
			}
			writer.println(indent + "</feature>"); //$NON-NLS-1$
		} else
			writer.println("/>"); //$NON-NLS-1$
	}

	public IFile getArchiveFile() {
		if (fUrl == null)
			return null;
		IResource resource = getModel().getUnderlyingResource();
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		IFile file = project.getFile(new Path(fUrl));
		if (file.exists())
			return file;
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getOS()
	 */
	public String getOS() {
		return fOS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getNL()
	 */
	public String getNL() {
		return fNL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getArch()
	 */
	public String getArch() {
		return fArch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getWS()
	 */
	public String getWS() {
		return fWS;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setOS(java.lang.String)
	 */
	public void setOS(String os) throws CoreException {
		ensureModelEditable();
		Object oldValue = fOS;
		fOS = os;
		firePropertyChanged(P_OS, oldValue, fOS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setWS(java.lang.String)
	 */
	public void setWS(String ws) throws CoreException {
		ensureModelEditable();
		Object oldValue = fWS;
		fWS = ws;
		firePropertyChanged(P_WS, oldValue, fWS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setArch(java.lang.String)
	 */
	public void setArch(String arch) throws CoreException {
		ensureModelEditable();
		Object oldValue = fArch;
		fArch = arch;
		firePropertyChanged(P_ARCH, oldValue, fArch);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setNL(java.lang.String)
	 */
	public void setNL(String nl) throws CoreException {
		ensureModelEditable();
		Object oldValue = fNL;
		fNL = nl;
		firePropertyChanged(P_NL, oldValue, fNL);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#isPatch()
	 */
	public boolean isPatch() {
		return fIsPatch;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setIsPatch(boolean)
	 */
	public void setIsPatch(boolean patch) throws CoreException {
		ensureModelEditable();
		Object oldValue = new Boolean(fIsPatch);
		fIsPatch = patch;
		firePropertyChanged(P_PATCH, oldValue, new Boolean(fIsPatch));
	}
}
