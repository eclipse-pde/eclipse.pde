/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.isite;

import org.eclipse.pde.core.*;
/**
 * This model type is designed to hold data loaded from
 * "site.xml" file of an Eclipse update site.
 */
public interface ISiteBuildModel extends IModel, IModelChangeProvider {
/**
 * Returns the top-level model object.
 *
 * @return top-level model object of the site model
 */
ISiteBuild getSiteBuild();

ISiteBuild createSiteBuild();

ISiteBuildFeature createFeature();
/**
 * Returns install location of the site.xml file in case of external files.
 *
 * @return install location for external files,
 * or <samp>null</samp> for models based on
 * workspace resources.
 */
public String getInstallLocation();
}
