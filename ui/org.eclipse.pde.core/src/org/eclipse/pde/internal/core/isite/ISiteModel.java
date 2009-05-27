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

import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.IModelChangeProvider;

/**
 * This model type is designed to hold data loaded from
 * "site.xml" file of an Eclipse update site.
 */
public interface ISiteModel extends IModel, IModelChangeProvider {
	/**
	 * Returns the top-level model object.
	 *
	 * @return top-level model object of the site model
	 */
	public ISite getSite();

	/**
	 * Returns the factory that should be used
	 * to create new instances of model objects.
	 *
	 * @return feature model factory
	 */
	ISiteModelFactory getFactory();

	/**
	 * Returns install location of the site.xml file in case of external files.
	 *
	 * @return install location for external files,
	 * or <samp>null</samp> for models based on
	 * workspace resources.
	 */
	public String getInstallLocation();

	/**
	 * Tests whether this model is enabled.
	 *
	 * @return <samp>true</samp> if the model is enabled
	 */
	public boolean isEnabled();

	/**
	 * Enables or disables this model.
	 *
	 * @param enabled the new enable state
	 */
	public void setEnabled(boolean enabled);
}
