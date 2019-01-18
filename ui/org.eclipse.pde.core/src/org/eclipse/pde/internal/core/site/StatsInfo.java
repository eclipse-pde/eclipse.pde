/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.core.isite.IStatsInfo;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class StatsInfo extends SiteObject implements IStatsInfo {

	private static final long serialVersionUID = 1L;
	final static String INDENT = "   "; //$NON-NLS-1$

	public static final String P_URL = "url"; //$NON-NLS-1$
	private String fURL;
	private final Vector<ISiteObject> featureArtifacts = new Vector<>();
	private final Vector<ISiteObject> bundleArtifacts = new Vector<>();

	public StatsInfo() {
		super();
	}

	@Override
	public void setURL(String url) throws CoreException {
		String old = fURL;
		fURL = url;
		ensureModelEditable();
		firePropertyChanged(P_URL, old, fURL);
	}

	@Override
	public String getURL() {
		return fURL;
	}

	@Override
	public void addFeatureArtifacts(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (ISiteFeature feature : newFeatures) {
			((SiteFeature) feature).setInTheModel(true);
			featureArtifacts.add(feature);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.INSERT);
	}

	@Override
	public void addBundleArtifacts(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (ISiteBundle bundle : newBundles) {
			((SiteBundle) bundle).setInTheModel(true);
			bundleArtifacts.add(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.INSERT);
	}

	@Override
	public void removeFeatureArtifacts(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (ISiteFeature feature : newFeatures) {
			((SiteFeature) feature).setInTheModel(false);
			featureArtifacts.remove(feature);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.REMOVE);
	}

	@Override
	public void removeBundleArtifacts(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (ISiteBundle bundle : newBundles) {
			((SiteBundle) bundle).setInTheModel(false);
			bundleArtifacts.remove(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.REMOVE);
	}

	@Override
	public ISiteFeature[] getFeatureArtifacts() {
		return featureArtifacts.toArray(new ISiteFeature[featureArtifacts.size()]);
	}

	@Override
	public ISiteBundle[] getBundleArtifacts() {
		return bundleArtifacts.toArray(new ISiteBundle[bundleArtifacts.size()]);
	}

	@Override
	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute("location"); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					parseChild(child);
				}
			}
		}
	}

	protected void parseChild(Node child) {
		String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
		if (tag.equals("feature")) { //$NON-NLS-1$
			ISiteFeature feature = getModel().getFactory().createFeature();
			((SiteFeature) feature).parse(child);
			((SiteFeature) feature).setInTheModel(true);
			featureArtifacts.add(feature);
		} else if (tag.equals("bundle")) { //$NON-NLS-1$
			ISiteBundle bundle = getModel().getFactory().createBundle();
			((SiteBundle) bundle).parse(child);
			((SiteBundle) bundle).setInTheModel(true);
			bundleArtifacts.add(bundle);
		}
	}

	@Override
	public void write(String indent, PrintWriter writer) {
		if (isURLDefined()) {
			writer.print(indent + "<stats location=\"" + fURL + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println(">"); //$NON-NLS-1$
			String indent2 = indent + INDENT;
			// features
			for (int i = 0; i < featureArtifacts.size(); i++) {
				IWritable writable = featureArtifacts.get(i);
				writable.write(indent2, writer);
			}
			// bundles
			for (int i = 0; i < bundleArtifacts.size(); i++) {
				IWritable writable = bundleArtifacts.get(i);
				writable.write(indent2, writer);
			}
			writer.println(indent + "</stats>"); //$NON-NLS-1$
		}
	}

	private boolean isURLDefined() {
		return fURL != null && fURL.length() > 0;
	}

	@Override
	public boolean isValid() {
		for (int i = 0; i < featureArtifacts.size(); i++) {
			ISiteFeature feature = (ISiteFeature) featureArtifacts.get(i);
			if (!feature.isValid()) {
				return false;
			}
		}
		for (int i = 0; i < bundleArtifacts.size(); i++) {
			ISiteBundle bundle = (ISiteBundle) bundleArtifacts.get(i);
			if (!bundle.isValid()) {
				return false;
			}
		}
		return isURLDefined();
	}

}
