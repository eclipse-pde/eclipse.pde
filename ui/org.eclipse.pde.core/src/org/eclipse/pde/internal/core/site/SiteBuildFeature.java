/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

public class SiteBuildFeature
	extends SiteBuildObject
	implements ISiteBuildFeature {
	private static final long serialVersionUID = 1L;
	private String id;
	private String version;
	private IFeature feature;

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getType()
	 */
	public String getId() {
		return id;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#getURL()
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteFeature#setType(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.id;
		this.id = id;
		firePropertyChanged(P_ID, oldValue, id);
	}

	public void setVersion(String version) throws CoreException {
		ensureModelEditable();
		Object oldValue = this.version;
		this.version = version;
		firePropertyChanged(P_VERSION, oldValue, version);
	}

	protected void parse(Node node) {
		id = getNodeAttribute(node, "id"); //$NON-NLS-1$
		version = getNodeAttribute(node, "version"); //$NON-NLS-1$
	}

	public IFeature getReferencedFeature() {
		if (feature == null) {
			WorkspaceModelManager manager =
				PDECore.getDefault().getWorkspaceModelManager();
			IFeatureModel[] models = manager.getFeatureModels();
			for (int i = 0; i < models.length; i++) {
				IFeatureModel model = models[i];
				IFeature feature = model.getFeature();
				if (feature.getId().equals(id)
					&& feature.getVersion().equals(version)) {
					this.feature = feature;
					break;
				}
			}
			// look it up
		}
		return feature;
	}

	public void setReferencedFeature(IFeature feature) {
		this.feature = feature;
		if (feature != null) {
			id = feature.getId();
			version = feature.getVersion();
		}
	}

	protected void reset() {
		id = null;
		version = null;
		feature = null;
	}

	public void restoreProperty(String name, Object oldValue, Object newValue)
		throws CoreException {
		if (name.equals(P_ID)) {
			setId(newValue != null ? newValue.toString() : null);
		} else if (name.equals(P_VERSION)) {
			setVersion(newValue != null ? newValue.toString() : null);
		} else
			super.restoreProperty(name, oldValue, newValue);
	}

	/**
	 * @see org.eclipse.pde.core.IWritable#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.print(indent);
		writer.print("<feature"); //$NON-NLS-1$
		if (id != null)
			writer.print(" id=\"" + id + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		if (version != null)
			writer.print(" version=\"" + version + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		writer.println("/>"); //$NON-NLS-1$
	}
	
	public String getTargetURL() {
		ISiteBuild siteBuild = getSiteBuild();
		IPath featureLocation = siteBuild.getFeatureLocation();
		String jar = id + "_"+version+".jar"; //$NON-NLS-1$ //$NON-NLS-2$
		return featureLocation.append(jar).toString();
	}
}
