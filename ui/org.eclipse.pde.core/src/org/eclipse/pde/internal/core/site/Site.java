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

import java.io.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
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
public class Site extends SiteObject implements ISite {
	final static String INDENT = "   ";
	private Vector features = new Vector();
	private Vector archives = new Vector();
	private Vector categoryDefs = new Vector();
	private String type;
	private String url;
	private ISiteDescription description;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.type;
		this.type = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getType()
	 */
	public String getType() {
		return type;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#setURL(java.net.URL)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.type;
		this.url = url;
		firePropertyChanged(P_TYPE, oldValue, url);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getURL()
	 */
	public String getURL() {
		return url;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getDescription()
	 */
	public ISiteDescription getDescription() {
		return description;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#setDescription(org.eclipse.pde.internal.core.isite.ISiteDescription)
	 */
	public void setDescription(ISiteDescription description)
		throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_TYPE, oldValue, description);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#addFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature)
	 */
	public void addFeatures(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteFeature feature = newFeatures[i];
			((SiteFeature) feature).setInTheModel(true);
			features.add(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.INSERT);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#addArchives(org.eclipse.pde.internal.core.isite.ISiteArchive)
	 */
	public void addArchives(ISiteArchive[] archs) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < archs.length; i++) {
			ISiteArchive archive = archs[i];
			((SiteArchive) archive).setInTheModel(true);
			archives.add(archs[i]);
		}
		fireStructureChanged(archs, IModelChangedEvent.INSERT);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#addCategoryDefinitions(org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition)
	 */
	public void addCategoryDefinitions(ISiteCategoryDefinition[] defs)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			((SiteCategoryDefinition) def).setInTheModel(true);
			categoryDefs.add(defs[i]);
		}
		fireStructureChanged(defs, IModelChangedEvent.INSERT);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature)
	 */
	public void removeFeatures(ISiteFeature[] newFeatures)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteFeature feature = newFeatures[i];
			((SiteFeature) feature).setInTheModel(false);
			features.remove(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeArchives(org.eclipse.pde.internal.core.isite.ISiteArchive)
	 */
	public void removeArchives(ISiteArchive[] archs) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < archs.length; i++) {
			ISiteArchive archive = archs[i];
			((SiteArchive) archive).setInTheModel(false);
			archives.remove(archs[i]);
		}
		fireStructureChanged(archs, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeCategoryDefinitions(org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition)
	 */
	public void removeCategoryDefinitions(ISiteCategoryDefinition[] defs)
		throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			((SiteCategoryDefinition) def).setInTheModel(false);
			categoryDefs.remove(defs[i]);
		}
		fireStructureChanged(defs, IModelChangedEvent.REMOVE);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getFeatures()
	 */
	public ISiteFeature[] getFeatures() {
		return (ISiteFeature[]) features.toArray(
			new ISiteFeature[features.size()]);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getArchives()
	 */
	public ISiteArchive[] getArchives() {
		return (ISiteArchive[]) archives.toArray(
			new ISiteArchive[archives.size()]);
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISite#getCategoryDefinitions()
	 */
	public ISiteCategoryDefinition[] getCategoryDefinitions() {
		return (ISiteCategoryDefinition[]) categoryDefs.toArray(
			new ISiteCategoryDefinition[categoryDefs.size()]);
	}
	protected void reset() {
		archives.clear();
		categoryDefs.clear();
		features.clear();
		description = null;
		type = null;
		url = null;
	}
	protected void parse(Node node, Hashtable lineTable) {
		type = getNodeAttribute(node, "type");
		url = getNodeAttribute(node, "url");
		bindSourceLocation(node, lineTable);
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				parseChild(child, lineTable);
			}
		}
	}

	protected void parseChild(Node child, Hashtable lineTable) {
		String tag = child.getNodeName().toLowerCase();
		if (tag.equals("feature")) {
			ISiteFeature feature = getModel().getFactory().createFeature();
			((SiteFeature) feature).parse(child, lineTable);
			((SiteFeature) feature).setInTheModel(true);
			features.add(feature);
		} else if (tag.equals("archive")) {
			ISiteArchive archive = getModel().getFactory().createArchive();
			((SiteArchive) archive).parse(child, lineTable);
			((SiteArchive) archive).setInTheModel(true);
			archives.add(archive);
		} else if (tag.equals("category-def")) {
			ISiteCategoryDefinition def =
				getModel().getFactory().createCategoryDefinition();
			((SiteCategoryDefinition) def).parse(child, lineTable);
			((SiteCategoryDefinition) def).setInTheModel(true);
			categoryDefs.add(def);
		} else if (tag.equals("description")) {
			if (description != null)
				return;
			description = getModel().getFactory().createDescription(this);
			((SiteDescription) description).parse(child, lineTable);
			((SiteDescription) description).setInTheModel(true);
		}
	}
	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (
			name.equals(P_DESCRIPTION)
				&& newValue instanceof ISiteDescription) {
			setDescription((ISiteDescription) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<site");
		String indent2 = indent + INDENT;
		String indenta = indent + INDENT + INDENT;
		writeIfDefined(indenta, writer, "type", getType());
		writeIfDefined(indenta, writer, "url", getURL());
		writer.println(">");

		if (description != null) {
			writer.println();
			description.write(indent2, writer);
		}
		writeChildren(indent2, features, writer);
		writeChildren(indent2, archives, writer);
		writeChildren(indent2, categoryDefs, writer);
		writer.println(indent + "</site>");
	}
	
	public boolean isValid() {
		for (int i=0; i<features.size(); i++) {
			ISiteFeature feature = (ISiteFeature)features.get(i);
			if (!feature.isValid()) return false;
		}
		for (int i=0; i<archives.size(); i++) {
			ISiteArchive archive = (ISiteArchive)archives.get(i);
			if (!archive.isValid()) return false;
		}
		for (int i=0; i<categoryDefs.size(); i++) {
			ISiteCategoryDefinition def = (ISiteCategoryDefinition)categoryDefs.get(i);
			if (!def.isValid()) return false;
		}
		return true;
	}
	
	
	
	private void writeChildren(
		String indent,
		Vector children,
		PrintWriter writer) {
		for (int i = 0; i < children.size(); i++) {
			IWritable writable = (IWritable) children.get(i);
			writable.write(indent, writer);
		}
	}
	private void writeIfDefined(
		String indent,
		PrintWriter writer,
		String attName,
		String attValue) {
		if (attValue == null)
			return;
		writer.println();
		writer.print(indent + attName + "=\"" + attValue + "\"");
	}
}
