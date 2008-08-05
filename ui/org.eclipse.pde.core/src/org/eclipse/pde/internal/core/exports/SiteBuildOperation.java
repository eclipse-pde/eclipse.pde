/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.exports;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Map;
import org.eclipse.pde.internal.build.IBuildPropertiesConstants;
import org.eclipse.pde.internal.build.IXMLConstants;
import org.eclipse.pde.internal.core.P2Utils;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.eclipse.pde.internal.core.isite.ISiteModel;

/**
 * Performs a site build operation that will build any features needed by the site and generate
 * p2 metadata for those features.
 * 
 * @see FeatureBasedExportOperation
 * @see FeatureExportOperation
 */
public class SiteBuildOperation extends FeatureBasedExportOperation {

	private ISiteModel fSiteModel;

	public SiteBuildOperation(FeatureExportInfo info, ISiteModel siteModel) {
		super(info);
		fSiteModel = siteModel;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureBasedExportOperation#createPostProcessingFiles()
	 */
	protected void createPostProcessingFiles() {
		createPostProcessingFile(new File(fFeatureLocation, FEATURE_POST_PROCESSING));
		createPostProcessingFile(new File(fFeatureLocation, PLUGIN_POST_PROCESSING));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.exports.FeatureExportOperation#setP2MetaDataProperties(java.util.Map)
	 */
	protected void setP2MetaDataProperties(Map map) {
		if (fInfo.toDirectory) {
			map.put(IXMLConstants.TARGET_P2_METADATA, IBuildPropertiesConstants.TRUE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FLAVOR, P2Utils.P2_FLAVOR_DEFAULT);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_PUBLISH_ARTIFACTS, IBuildPropertiesConstants.FALSE);
			map.put(IBuildPropertiesConstants.PROPERTY_P2_FINAL_MODE_OVERRIDE, IBuildPropertiesConstants.TRUE);
			if (fSiteModel != null) {
				ISiteDescription description = fSiteModel.getSite().getDescription();
				if (description != null && description.getName() != null && description.getName().length() > 0) {
					map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO_NAME, description.getName());
					map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO_NAME, description.getName());
				}
			}
			try {
				map.put(IBuildPropertiesConstants.PROPERTY_P2_METADATA_REPO, new File(fInfo.destinationDirectory).toURL().toString());
				map.put(IBuildPropertiesConstants.PROPERTY_P2_ARTIFACT_REPO, new File(fInfo.destinationDirectory).toURL().toString());
			} catch (MalformedURLException e) {
				PDECore.log(e);
			}
		}
	}

}
