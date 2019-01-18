/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Red Hat Inc. - Support for bundles and nested categories
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 296392
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import org.eclipse.pde.internal.core.isite.IRepositoryReference;
import org.eclipse.pde.internal.core.isite.ISite;
import org.eclipse.pde.internal.core.isite.ISiteArchive;
import org.eclipse.pde.internal.core.isite.ISiteBundle;
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.core.isite.ISiteModelFactory;
import org.eclipse.pde.internal.core.isite.ISiteObject;
import org.eclipse.pde.internal.core.isite.IStatsInfo;

public class SiteModelFactory implements ISiteModelFactory {
	private final ISiteModel model;

	public SiteModelFactory(ISiteModel model) {
		this.model = model;
	}

	@Override
	public ISite createSite() {
		Site site = new Site();
		site.model = model;
		site.parent = null;
		return site;
	}

	@Override
	public ISiteFeature createFeature() {
		SiteFeature feature = new SiteFeature();
		feature.model = model;
		feature.parent = model.getSite();
		return feature;
	}

	@Override
	public ISiteBundle createBundle() {
		SiteBundle bundle = new SiteBundle();
		bundle.model = model;
		bundle.parent = model.getSite();
		return bundle;
	}

	@Override
	public ISiteCategory createCategory(ISiteFeature feature) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = feature;
		return category;
	}

	@Override
	public ISiteCategory createCategory(ISiteBundle bundle) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = bundle;
		return category;
	}

	@Override
	public ISiteCategory createCategory(ISiteCategoryDefinition categoryDef) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = categoryDef;
		return category;
	}

	@Override
	public ISiteCategoryDefinition createCategoryDefinition() {
		SiteCategoryDefinition def = new SiteCategoryDefinition();
		def.model = model;
		def.parent = model.getSite();
		return def;
	}

	@Override
	public IRepositoryReference createRepositoryReference() {
		RepositoryReference repo = new RepositoryReference();
		repo.model = model;
		repo.parent = model.getSite();
		return repo;
	}

	@Override
	public ISiteDescription createDescription(ISiteObject parent) {
		SiteDescription desc = new SiteDescription();
		desc.model = model;
		desc.parent = parent;
		return desc;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createArchive()
	 */
	@Override
	public ISiteArchive createArchive() {
		SiteArchive archive = new SiteArchive();
		archive.model = model;
		archive.parent = model.getSite();
		return archive;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createStatsInfo()
	 */
	@Override
	public IStatsInfo createStatsInfo() {
		StatsInfo info = new StatsInfo();
		info.model = model;
		info.parent = model.getSite();
		return info;
	}

}
