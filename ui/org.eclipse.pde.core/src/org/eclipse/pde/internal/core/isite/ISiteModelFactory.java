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
package org.eclipse.pde.internal.core.isite;


/**
 * This model factory should be used to create
 * model objects of the feature model.
 */
public interface ISiteModelFactory {
	/**
	 * Creates a new plug-in model object.
	 *
	 * @return new instance of the feature plug-in object
	 */
	ISite createSite();

	/**
	 * Creates a new data model object.
	 *
	 * @return new instance of the feature data object
	 */
	ISiteFeature createFeature();

	/**
	 * Creates a new bundle model object.
	 *
	 * @return new instance of the bundle data object
	 */
	ISiteBundle createBundle();

	/**
	 * Creates a new feature child object.
	 *
	 * @return new instance of the feature child object
	 */
	ISiteCategory createCategory(ISiteFeature feature);

	/**
	 * Creates a new feature child object.
	 *
	 * @return new instance of the feature child object
	 */
	ISiteCategory createCategory(ISiteBundle feature);

	/**
	 * Creates a new Category child object.
	 *
	 * @return new instance of the category child object
	 */
	ISiteCategory createCategory(ISiteCategoryDefinition aCategory);

	/**
	 * Creates a new import model object.
	 *
	 * @return new instance of the feature import object
	 */
	ISiteCategoryDefinition createCategoryDefinition();

	/**
	 * Creates a new feature URL instance.
	 *
	 * @return a new feature URL instance
	 */
	ISiteDescription createDescription(ISiteObject parent);

	/**
	 * Creates a new feature install handler.
	 *
	 * @return a new feature install handler.
	 */
	ISiteArchive createArchive();

	/**
	 *  Creates a new repository reference.
	 *
	 * @return a new repository reference.
	 */
	IRepositoryReference createRepositoryReference();

	/**
	 * Creates a new stats info.
	 *
	 * @return a new stats info
	 */
	IStatsInfo createStatsInfo();

}
