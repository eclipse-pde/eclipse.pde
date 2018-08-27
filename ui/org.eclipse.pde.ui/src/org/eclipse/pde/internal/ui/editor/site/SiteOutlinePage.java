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

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.editor.*;

public class SiteOutlinePage extends FormOutlinePage {
	private LabelProvider fLabelProvider;

	/**
	 * @param editor
	 */
	public SiteOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage) {
			PDEFormPage page = (PDEFormPage) parent;
			ISiteModel model = (ISiteModel) page.getModel();
			if (model != null && model.isValid()) {
				ISite site = model.getSite();
				if (page.getId().equals(FeaturesPage.PAGE_ID)) {
					ArrayList<IWritable> result = new ArrayList<>();
					ISiteCategoryDefinition[] catDefs = site.getCategoryDefinitions();
					for (ISiteCategoryDefinition catDef : catDefs) {
						result.add(catDef);
					}
					ISiteFeature[] features = site.getFeatures();
					for (ISiteFeature feature : features) {
						if (feature.getCategories().length == 0)
							result.add(new SiteFeatureAdapter(null, feature));
					}
					return result.toArray();
				}
				if (page.getId().equals(ArchivePage.PAGE_ID))
					return site.getArchives();
			}
		}
		if (parent instanceof ISiteCategoryDefinition) {
			ISiteCategoryDefinition catDef = (ISiteCategoryDefinition) parent;
			ISiteModel model = catDef.getModel();
			if (model.isValid()) {
				ISite site = model.getSite();
				ISiteFeature[] features = site.getFeatures();
				HashSet<SiteFeatureAdapter> result = new HashSet<>();
				for (ISiteFeature feature : features) {
					ISiteCategory[] cats = feature.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(cat.getName(), feature));
						}
					}
				}
				return result.toArray();
			}
		}
		return new Object[0];
	}

	@Override
	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof ISiteCategoryDefinition || item instanceof SiteFeatureAdapter)
			pageId = FeaturesPage.PAGE_ID;
		else if (item instanceof ISiteArchive)
			pageId = ArchivePage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		fLabelProvider = new SiteLabelProvider();
		return fLabelProvider;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fLabelProvider != null)
			fLabelProvider.dispose();
	}
}
