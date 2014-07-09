/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import org.eclipse.pde.internal.core.isite.*;

public class SiteModelFactory implements ISiteModelFactory {
	private ISiteModel model;

	public SiteModelFactory(ISiteModel model) {
		this.model = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createSite()
	 */
	public ISite createSite() {
		Site site = new Site();
		site.model = model;
		site.parent = null;
		return site;
	}

	public ISiteFeature createFeature() {
		SiteFeature feature = new SiteFeature();
		feature.model = model;
		feature.parent = model.getSite();
		return feature;
	}

	public ISiteBundle createBundle() {
		SiteBundle bundle = new SiteBundle();
		bundle.model = model;
		bundle.parent = model.getSite();
		return bundle;
	}

	public ISiteCategory createCategory(ISiteFeature feature) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = feature;
		return category;
	}

	public ISiteCategory createCategory(ISiteBundle bundle) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = bundle;
		return category;
	}

	public ISiteCategoryDefinition createCategoryDefinition() {
		SiteCategoryDefinition def = new SiteCategoryDefinition();
		def.model = model;
		def.parent = model.getSite();
		return def;
	}

	public IRepositoryReference createRepositoryReference() {
		RepositoryReference repo = new RepositoryReference();
		repo.model = model;
		repo.parent = model.getSite();
		return repo;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createDescription(org.eclipse.pde.internal.core.isite.ISiteObject)
	 */
	public ISiteDescription createDescription(ISiteObject parent) {
		SiteDescription desc = new SiteDescription();
		desc.model = model;
		desc.parent = parent;
		return desc;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createArchive()
	 */
	public ISiteArchive createArchive() {
		SiteArchive archive = new SiteArchive();
		archive.model = model;
		archive.parent = model.getSite();
		return archive;
	}

}
