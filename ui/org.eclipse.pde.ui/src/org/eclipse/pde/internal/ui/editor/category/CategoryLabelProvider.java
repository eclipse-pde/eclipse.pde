/*******************************************************************************
.
. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License 2.0 which accompanies this distribution, and is
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*   EclipseSource - initial API and implementation
*   Mickael Istria (Red Hat Inc.) - 383795: <bundle...> support and nested categories
*   Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
*   IBM Corporation - ongoing enhancements
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.plugin.IMatchRules;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.forms.editor.IFormPage;

class CategoryLabelProvider extends LabelProvider {

	private PDELabelProvider fSharedProvider;

	private Image fSiteFeatureImage;
	private Image fMissingSiteFeatureImage;
	private Image fSiteBundleImage;
	private Image fMissingSiteBundleImage;
	private Image fPageImage;
	private Image fCatDefImage;

	public CategoryLabelProvider() {
		fSiteFeatureImage = PDEPluginImages.DESC_FEATURE_OBJ.createImage();
		fMissingSiteFeatureImage = PDEPluginImages.DESC_NOREF_FEATURE_OBJ.createImage();
		fSiteBundleImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		fMissingSiteBundleImage = PDEPluginImages.DESC_PLUGIN_DIS_OBJ.createImage();
		fCatDefImage = PDEPluginImages.DESC_CATEGORY_OBJ.createImage();
		fPageImage = PDEPluginImages.DESC_PAGE_OBJ.createImage();
		fSharedProvider = PDEPlugin.getDefault().getLabelProvider();
		fSharedProvider.connect(this);
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return fCatDefImage;
		if (element instanceof SiteCategoryDefinitionAdapter) {
			return getImage(((SiteCategoryDefinitionAdapter) element).category);
		}
		if (element instanceof SiteFeatureAdapter) {
			if (PDECore.getDefault().getFeatureModelManager().findFeatureModelRelaxed(((SiteFeatureAdapter) element).feature.getId(), ((SiteFeatureAdapter) element).feature.getVersion()) == null)
				return fMissingSiteFeatureImage;
			return fSiteFeatureImage;
		}
		if (element instanceof SiteBundleAdapter) {
			ISiteBundle bundle = ((SiteBundleAdapter) element).bundle;
			if (PluginRegistry.findModel(bundle.getId(), bundle.getVersion(), IMatchRules.COMPATIBLE, null) == null) {
				return this.fMissingSiteBundleImage;
			}
			return this.fSiteBundleImage;
		}
		if (element instanceof IFormPage)
			return fPageImage;
		return fSharedProvider.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof ISiteCategoryDefinition)
			return ((ISiteCategoryDefinition) element).getName();
		if (element instanceof SiteCategoryDefinitionAdapter) {
			return getText(((SiteCategoryDefinitionAdapter) element).category);
		}
		if (element instanceof SiteFeatureAdapter) {
			ISiteFeature feature = ((SiteFeatureAdapter) element).feature;
			return fSharedProvider.getObjectText(feature);
		}
		if (element instanceof SiteBundleAdapter) {
			ISiteBundle bundle = ((SiteBundleAdapter) element).bundle;
			return fSharedProvider.getObjectText(bundle);
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