/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.ui.PDELabelProvider;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

class SiteLabelProvider extends LabelProvider {

	private PDELabelProvider fSharedProvider;

	/**
	 * Comment for <code>fLabelProvider</code>
	 */
	private Image fSiteFeatureImage;

	private Image fMissingSiteFeatureImage;
	
	private Image fPageImage;

	private Image fCatDefImage;

	public SiteLabelProvider() {
		fSiteFeatureImage = PDEPluginImages.DESC_FEATURE_OBJ.createImage();
		fMissingSiteFeatureImage = PDEPluginImages.DESC_NOREF_FEATURE_OBJ
				.createImage();
		fCatDefImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
		fPageImage = PDEPluginImages.DESC_PAGE_OBJ.createImage();
		fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
		fSharedProvider.connect(this);
	}


	public Image getImage(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return fCatDefImage;
		if (element instanceof SiteFeatureAdapter) {
			if (PDECore.getDefault().getFeatureModelManager().findFeatureModel(
					((SiteFeatureAdapter) element).feature.getId(),
					((SiteFeatureAdapter) element).feature.getVersion()) == null)
				return fMissingSiteFeatureImage;
			return fSiteFeatureImage;
		}
		if (element instanceof IFormPage)
			return fPageImage;
		return fSharedProvider.getImage(element);
	}

	public String getText(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return ((ISiteCategoryDefinition) element).getName();
		if (element instanceof SiteFeatureAdapter) {
			ISiteFeature feature = ((SiteFeatureAdapter) element).feature;
			if (feature.getId() != null && feature.getVersion() != null)
				return feature.getId() + " (" + feature.getVersion() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
			return feature.getURL();
		}
		if (element instanceof IFormPage)
			return ((IFormPage)element).getTitle();
		return fSharedProvider.getText(element);
	}

	public void dispose() {
		fSharedProvider.disconnect(this);
		super.dispose();
		fCatDefImage.dispose();
		fSiteFeatureImage.dispose();
	}
}
