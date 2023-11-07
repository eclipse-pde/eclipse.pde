/*******************************************************************************
 * Copyright (c) 2013, 2016 EclipseSource and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   EclipseSource - initial API and implementation
 *   IBM Corporation - ongoing enhancements
 ******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.editor.FormOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;

public class CategoryOutlinePage extends FormOutlinePage {
	private LabelProvider fLabelProvider;

	public CategoryOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	@Override
	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage page) {
			ISiteModel model = (ISiteModel) page.getModel();
			if (model.isValid()) {
				ISite site = model.getSite();
				if (page.getId().equals(IUsPage.PAGE_ID)) {
					ArrayList<IWritable> result = new ArrayList<>();
					ISiteCategoryDefinition[] catDefs = site.getCategoryDefinitions();
					Collections.addAll(result, catDefs);
					ISiteFeature[] features = site.getFeatures();
					for (ISiteFeature feature : features) {
						if (feature.getCategories().length == 0)
							result.add(new SiteFeatureAdapter(null, feature));
					}
					ISiteBundle[] bundles = site.getBundles();
					for (ISiteBundle bundle : bundles) {
						if (bundle.getCategories().length == 0) {
							result.add(new SiteBundleAdapter(null, bundle));
						}
					}
					return result.toArray();
				}
			}
		}
		if (parent instanceof ISiteCategoryDefinition catDef) {
			ISiteModel model = catDef.getModel();
			if (model.isValid()) {
				ISite site = model.getSite();
				ISiteFeature[] features = site.getFeatures();
				HashSet<IWritable> result = new HashSet<>();
				for (ISiteFeature feature : features) {
					ISiteCategory[] cats = feature.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(cat.getName(), feature));
						}
					}
				}
				ISiteBundle[] bundles = site.getBundles();
				for (ISiteBundle bundle : bundles) {
					ISiteCategory[] cats = bundle.getCategories();
					for (ISiteCategory cat : cats) {
						if (cat.getDefinition() != null && cat.getDefinition().equals(catDef)) {
							result.add(new SiteBundleAdapter(cat.getName(), bundle));
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
		if (item instanceof ISiteCategoryDefinition || item instanceof SiteFeatureAdapter || item instanceof SiteBundleAdapter)
			pageId = IUsPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}

	@Override
	public ILabelProvider createLabelProvider() {
		fLabelProvider = new CategoryLabelProvider();
		return fLabelProvider;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fLabelProvider != null)
			fLabelProvider.dispose();
	}
}
