package org.eclipse.pde.internal.core.site;

import org.eclipse.pde.internal.core.isite.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteModelFactory implements ISiteModelFactory {
	private ISiteModel model;
	public SiteModelFactory(ISiteModel model) {
		this.model = model;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createSite()
	 */
	public ISite createSite() {
		Site site = new Site();
		site.model = model;
		site.parent = null;
		return site;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createFeature()
	 */
	public ISiteFeature createFeature() {
		SiteFeature feature = new SiteFeature();
		feature.model = model;
		feature.parent = model.getSite();
		return feature;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createCategory()
	 */
	public ISiteCategory createCategory(ISiteFeature feature) {
		SiteCategory category = new SiteCategory();
		category.model = model;
		category.parent = feature;
		return category;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createCategoryDefinition()
	 */
	public ISiteCategoryDefinition createCategoryDefinition() {
		SiteCategoryDefinition def = new SiteCategoryDefinition();
		def.model = model;
		def.parent = model.getSite();
		return def;
	}

	/**
	 * @see org.eclipse.pde.internal.core.isite.ISiteModelFactory#createDescription()
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
