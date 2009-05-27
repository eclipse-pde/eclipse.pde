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
package org.eclipse.pde.internal.core.ifeature;

/**
 * This model factory should be used to create
 * model objects of the feature model.
 */
public interface IFeatureModelFactory {
	/**
	 * Creates a new plug-in model object.
	 *
	 * @return new instance of the feature plug-in object
	 */
	IFeaturePlugin createPlugin();

	/**
	 * Creates a new data model object.
	 *
	 * @return new instance of the feature data object
	 */
	IFeatureData createData();

	/**
	 * Creates a new feature child object.
	 *
	 * @return new instance of the feature child object
	 */
	IFeatureChild createChild();

	/**
	 * Creates a new import model object.
	 *
	 * @return new instance of the feature import object
	 */
	IFeatureImport createImport();

	/**
	 * Creates a new feature URL instance.
	 *
	 * @return a new feature URL instance
	 */
	IFeatureURL createURL();

	/**
	 * Creates a new feature install handler.
	 *
	 * @return a new feature install handler.
	 */
	IFeatureInstallHandler createInstallHandler();

	/**
	 * 
	 */
	public IFeatureInfo createInfo(int info);

	/**
	 * Creates a new instance of a feature URL element for
	 * the provided URL parent and the type.
	 *
	 * @return a new URL element instance
	 */
	IFeatureURLElement createURLElement(IFeatureURL parent, int elementType);
}
