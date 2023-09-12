/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.feature.FeatureImport;
import org.eclipse.pde.internal.core.feature.WorkspaceFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.swt.widgets.Shell;

public class CreateFeaturePatchOperation extends AbstractCreateFeatureOperation {

	private final IFeatureModel fFeatureModel;

	public CreateFeaturePatchOperation(IProject project, IPath location, FeatureData featureData, IFeatureModel featureModel, Shell shell) {
		super(project, location, featureData, shell);
		fFeatureModel = featureModel;
	}

	@Override
	protected void configureFeature(IFeature feature, WorkspaceFeatureModel model) throws CoreException {
		FeatureImport featureImport = (FeatureImport) model.getFactory().createImport();
		if (fFeatureModel != null) {
			featureImport.loadFrom(fFeatureModel.getFeature());
			featureImport.setPatch(true);
			featureImport.setVersion(fFeatureModel.getFeature().getVersion());
			featureImport.setId(fFeatureModel.getFeature().getId());
		} else if (fFeatureData.isPatch) {
			featureImport.setType(IFeatureImport.FEATURE);
			featureImport.setPatch(true);
			featureImport.setVersion(fFeatureData.featureToPatchVersion);
			featureImport.setId(fFeatureData.featureToPatchId);
		}
		feature.addImports(new IFeatureImport[] {featureImport});
	}

}
