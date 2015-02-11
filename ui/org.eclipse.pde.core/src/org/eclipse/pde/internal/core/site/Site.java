/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Red Hat Inc. - Support for <bundle...> tag
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Site extends SiteObject implements ISite {
	private static final long serialVersionUID = 1L;
	final static String INDENT = "   "; //$NON-NLS-1$
	private Vector<ISiteObject> features = new Vector<ISiteObject>();
	private Vector<ISiteObject> bundles = new Vector<ISiteObject>();
	private Vector<ISiteObject> archives = new Vector<ISiteObject>();
	private Vector<ISiteObject> categoryDefs = new Vector<ISiteObject>();
	private Vector<ISiteObject> repositoryReferences = new Vector<ISiteObject>();

	private String type;
	private String url;
	private String mirrorsUrl;
	private String digestUrl;
	private String associateSitesUrl;
	private ISiteDescription description;
	private IStatsInfo statsInfo;

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setType(java.lang.String)
	 */
	public void setType(String type) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.type;
		this.type = type;
		firePropertyChanged(P_TYPE, oldValue, type);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getType()
	 */
	public String getType() {
		return type;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setURL(java.lang.String)
	 */
	public void setURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.url;
		this.url = url;
		firePropertyChanged(P_URL, oldValue, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getURL()
	 */
	public String getURL() {
		return url;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setDigestURL(java.lang.String)
	 */
	public void setDigestURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.digestUrl;
		this.digestUrl = url;
		firePropertyChanged(P_DIGEST_URL, oldValue, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getDigestURL()
	 */
	public String getDigestURL() {
		return digestUrl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setAssociateSitesURL(java.lang.String)
	 */
	public void setAssociateSitesURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.associateSitesUrl;
		this.associateSitesUrl = url;
		firePropertyChanged(P_ASSOCIATE_SITES_URL, oldValue, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getAssociateSitesURL()
	 */
	public String getAssociateSitesURL() {
		return associateSitesUrl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setMirrorsURL(java.lang.String)
	 */
	public void setMirrorsURL(String url) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.mirrorsUrl;
		this.mirrorsUrl = url;
		firePropertyChanged(P_MIRRORS_URL, oldValue, url);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getMirrorsURL()
	 */
	public String getMirrorsURL() {
		return mirrorsUrl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getDescription()
	 */
	public ISiteDescription getDescription() {
		return description;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setDescription(org.eclipse.pde.internal.core.isite.ISiteDescription)
	 */
	public void setDescription(ISiteDescription description) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.description;
		this.description = description;
		firePropertyChanged(P_DESCRIPTION, oldValue, description);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#addFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature[])
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#addBundles(org.eclipse.pde.internal.core.isite.ISiteBundle[])
	 */
	public void addBundles(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newBundles.length; i++) {
			ISiteBundle bundle = newBundles[i];
			((SiteBundle) bundle).setInTheModel(true);
			bundles.add(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#addArchives(org.eclipse.pde.internal.core.isite.ISiteArchive[])
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#addCategoryDefinitions(org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition[])
	 */
	public void addCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			((SiteCategoryDefinition) def).setInTheModel(true);
			categoryDefs.add(defs[i]);
		}
		fireStructureChanged(defs, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#addRepositoryReferences(org.eclipse.pde.internal.core.isite.IRepositoryReference[])
	 */
	public void addRepositoryReferences(IRepositoryReference[] repos) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < repos.length; i++) {
			IRepositoryReference repo = repos[i];
			((RepositoryReference) repo).setInTheModel(true);
			repositoryReferences.add(repos[i]);
		}
		fireStructureChanged(repos, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeFeatures(org.eclipse.pde.internal.core.isite.ISiteFeature[])
	 */
	public void removeFeatures(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteFeature feature = newFeatures[i];
			((SiteFeature) feature).setInTheModel(false);
			features.remove(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeBundles(org.eclipse.pde.internal.core.isite.ISiteBundle[])
	 */
	public void removeBundles(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newBundles.length; i++) {
			ISiteBundle bundle = newBundles[i];
			((SiteBundle) bundle).setInTheModel(false);
			bundles.remove(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeArchives(org.eclipse.pde.internal.core.isite.ISiteArchive[])
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeCategoryDefinitions(org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition[])
	 */
	public void removeCategoryDefinitions(ISiteCategoryDefinition[] defs) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			((SiteCategoryDefinition) def).setInTheModel(false);
			categoryDefs.remove(defs[i]);
		}
		fireStructureChanged(defs, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#removeRepositoryReferences(org.eclipse.pde.internal.core.isite.IRepositoryReference[])
	 */
	public void removeRepositoryReferences(IRepositoryReference[] repos) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < repos.length; i++) {
			IRepositoryReference repo = repos[i];
			((RepositoryReference) repo).setInTheModel(false);
			repositoryReferences.remove(repos[i]);
		}
		fireStructureChanged(repos, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getFeatures()
	 */
	public ISiteFeature[] getFeatures() {
		return features.toArray(new ISiteFeature[features.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getBundles()
	 */
	public ISiteBundle[] getBundles() {
		return bundles.toArray(new ISiteBundle[bundles.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getArchives()
	 */
	public ISiteArchive[] getArchives() {
		return archives.toArray(new ISiteArchive[archives.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getCategoryDefinitions()
	 */
	public ISiteCategoryDefinition[] getCategoryDefinitions() {
		return categoryDefs.toArray(new ISiteCategoryDefinition[categoryDefs.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getRepositoryReferences()
	 */
	public IRepositoryReference[] getRepositoryReferences() {
		return repositoryReferences.toArray(new IRepositoryReference[repositoryReferences.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#getStatsInfo()
	 */
	public IStatsInfo getStatsInfo() {
		return statsInfo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISite#setStatsInfo(org.eclipse.pde.internal.core.isite.IStatsInfo)
	 */
	public void setStatsInfo(IStatsInfo info) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.statsInfo;
		this.statsInfo = info;
		firePropertyChanged(P_STATS, oldValue, info);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.site.SiteObject#reset()
	 */
	@Override
	protected void reset() {
		archives.clear();
		categoryDefs.clear();
		repositoryReferences.clear();
		features.clear();
		bundles.clear();
		description = null;
		type = null;
		url = null;
		mirrorsUrl = null;
		digestUrl = null;
		associateSitesUrl = null;

		statsInfo = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.site.SiteObject#parse(org.w3c.dom.Node)
	 */
	@Override
	protected void parse(Node node) {
		type = getNodeAttribute(node, P_TYPE);
		url = getNodeAttribute(node, P_URL);
		mirrorsUrl = getNodeAttribute(node, P_MIRRORS_URL);
		digestUrl = getNodeAttribute(node, P_DIGEST_URL);
		associateSitesUrl = getNodeAttribute(node, P_ASSOCIATE_SITES_URL);
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				parseChild(child);
			}
		}
	}

	protected void parseChild(Node child) {
		String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
		if (tag.equals("feature")) { //$NON-NLS-1$
			ISiteFeature feature = getModel().getFactory().createFeature();
			((SiteFeature) feature).parse(child);
			((SiteFeature) feature).setInTheModel(true);
			features.add(feature);
		} else if (tag.equals("bundle")) { //$NON-NLS-1$
			ISiteBundle bundle = getModel().getFactory().createBundle();
			((SiteBundle) bundle).parse(child);
			((SiteBundle) bundle).setInTheModel(true);
			bundles.add(bundle);
		} else if (tag.equals("archive")) { //$NON-NLS-1$
			ISiteArchive archive = getModel().getFactory().createArchive();
			((SiteArchive) archive).parse(child);
			((SiteArchive) archive).setInTheModel(true);
			archives.add(archive);
		} else if (tag.equals("category-def")) { //$NON-NLS-1$
			ISiteCategoryDefinition def = getModel().getFactory().createCategoryDefinition();
			((SiteCategoryDefinition) def).parse(child);
			((SiteCategoryDefinition) def).setInTheModel(true);
			categoryDefs.add(def);
		} else if (tag.equals("repository-reference")) { //$NON-NLS-1$
			IRepositoryReference ref = getModel().getFactory().createRepositoryReference();
			((RepositoryReference) ref).parse(child);
			((RepositoryReference) ref).setInTheModel(true);
			repositoryReferences.add(ref);
		} else if (tag.equals("stats")) { //$NON-NLS-1$
			IStatsInfo info = getModel().getFactory().createStatsInfo();
			((StatsInfo) info).parse(child);
			((StatsInfo) info).setInTheModel(true);
			statsInfo = info;
		} else if (tag.equals(P_DESCRIPTION)) {
			if (description != null)
				return;
			description = getModel().getFactory().createDescription(this);
			((SiteDescription) description).parse(child);
			((SiteDescription) description).setInTheModel(true);
		}
	}

	@Override
	public void restoreProperty(String name, Object oldValue, Object newValue) throws CoreException {
		if (name.equals(P_TYPE)) {
			setType(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_URL)) {
			setURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_MIRRORS_URL)) {
			setMirrorsURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_DIGEST_URL)) {
			setDigestURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_ASSOCIATE_SITES_URL)) {
			setAssociateSitesURL(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_DESCRIPTION) && newValue instanceof ISiteDescription) {
			setDescription((ISiteDescription) newValue);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<site"); //$NON-NLS-1$
		String indent2 = indent + INDENT;
		String indenta = indent + INDENT + INDENT;
		writeIfDefined(indenta, writer, P_TYPE, getType());
		writeIfDefined(indenta, writer, P_URL, getURL());
		writeIfDefined(indenta, writer, P_MIRRORS_URL, getMirrorsURL());
		writeIfDefined(indenta, writer, P_DIGEST_URL, getDigestURL());
		writeIfDefined(indenta, writer, P_ASSOCIATE_SITES_URL, getAssociateSitesURL());
		writer.println(">"); //$NON-NLS-1$

		if (description != null) {
			description.write(indent2, writer);
		}

		if (statsInfo != null) {
			statsInfo.write(indent2, writer);
		}
		writeChildren(indent2, features, writer);
		writeChildren(indent2, bundles, writer);
		writeChildren(indent2, archives, writer);
		writeChildren(indent2, categoryDefs, writer);
		writeChildren(indent2, repositoryReferences, writer);
		writer.println(indent + "</site>"); //$NON-NLS-1$
	}

	public boolean isValid() {
		for (int i = 0; i < features.size(); i++) {
			ISiteFeature feature = (ISiteFeature) features.get(i);
			if (!feature.isValid())
				return false;
		}
		for (int i = 0; i < bundles.size(); i++) {
			ISiteBundle bundle = (ISiteBundle) bundles.get(i);
			if (!bundle.isValid())
				return false;
		}
		for (int i = 0; i < archives.size(); i++) {
			ISiteArchive archive = (ISiteArchive) archives.get(i);
			if (!archive.isValid())
				return false;
		}
		for (int i = 0; i < categoryDefs.size(); i++) {
			ISiteCategoryDefinition def = (ISiteCategoryDefinition) categoryDefs.get(i);
			if (!def.isValid())
				return false;
		}
		for (int i = 0; i < repositoryReferences.size(); i++) {
			IRepositoryReference repo = (IRepositoryReference) repositoryReferences.get(i);
			if (!repo.isValid())
				return false;
		}
		return true;
	}

	private void writeChildren(String indent, Vector<ISiteObject> children, PrintWriter writer) {
		for (int i = 0; i < children.size(); i++) {
			IWritable writable = children.get(i);
			writable.write(indent, writer);
		}
	}

	private void writeIfDefined(String indent, PrintWriter writer, String attName, String attValue) {
		if (attValue == null || attValue.length() <= 0)
			return;
		writer.println();
		writer.print(indent + attName + "=\"" + SiteObject.getWritableString(attValue) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
