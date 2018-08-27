/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.ui.*;
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
		fMissingSiteFeatureImage = PDEPluginImages.DESC_NOREF_FEATURE_OBJ.createImage();
		fCatDefImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
		fPageImage = PDEPluginImages.DESC_PAGE_OBJ.createImage();
		fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
		fSharedProvider.connect(this);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return fCatDefImage;
		if (element instanceof SiteFeatureAdapter) {
			if (PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(((SiteFeatureAdapter) element).feature.getId(), ((SiteFeatureAdapter) element).feature.getVersion()) == null)
				return fMissingSiteFeatureImage;
			return fSiteFeatureImage;
		}
		if (element instanceof IFormPage)
			return fPageImage;
		return fSharedProvider.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return ((ISiteCategoryDefinition) element).getName();
		if (element instanceof SiteFeatureAdapter) {
			ISiteFeature feature = ((SiteFeatureAdapter) element).feature;
			return fSharedProvider.getObjectText(feature);
		}
		if (element instanceof IFormPage)
			return ((IFormPage) element).getTitle();
		return fSharedProvider.getText(element);
	}

	@Override
	public void dispose() {
		fSharedProvider.disconnect(this);
		// Dispose of images
		if ((fCatDefImage != null) && (fCatDefImage.isDisposed() == false)) {
			fCatDefImage.dispose();
			fCatDefImage = null;
		}
		if ((fSiteFeatureImage != null) && (fSiteFeatureImage.isDisposed() == false)) {
			fSiteFeatureImage.dispose();
			fSiteFeatureImage = null;
		}
		if ((fMissingSiteFeatureImage != null) && (fMissingSiteFeatureImage.isDisposed() == false)) {
			fMissingSiteFeatureImage.dispose();
			fMissingSiteFeatureImage = null;
		}
		if ((fPageImage != null) && (fPageImage.isDisposed() == false)) {
			fPageImage.dispose();
			fPageImage = null;
		}
		super.dispose();
	}
}