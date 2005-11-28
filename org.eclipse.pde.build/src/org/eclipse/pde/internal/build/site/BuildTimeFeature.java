/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.site;

import org.eclipse.update.core.*;

public class BuildTimeFeature extends Feature {
	private boolean binary = false;
	private int contextQualifierLength = -1;

	public IIncludedFeatureReference[] getRawIncludedFeatureReferences() {
		return getFeatureIncluded();
	}

	public boolean isBinary() {
		return binary;
	}

	public void setBinary(boolean isCompiled) {
		this.binary = isCompiled;
	}

	private VersionedIdentifier versionId;
	
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getFeatureIdentifier();
		String ver = getFeatureVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				//UpdateCore.warn("Unable to create versioned identifier:" + id + ":" + ver); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		versionId = new VersionedIdentifier(getURL().toExternalForm(), null);
		return versionId;
	}
	
	public void setFeatureVersion(String featureVersion) {
		super.setFeatureVersion(featureVersion);
		versionId = null;
	}

	public void setContextQualifierLength(int l) {
		contextQualifierLength = l;
	}
	
	public int getContextQualifierLength(){
		return contextQualifierLength;
	}
}
