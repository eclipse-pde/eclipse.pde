/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
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
	 * Creates a new feature child object.
	 *
	 * @return new instance of the feature child object
	 */
	ISiteCategory createCategory(ISiteFeature feature);

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
}
