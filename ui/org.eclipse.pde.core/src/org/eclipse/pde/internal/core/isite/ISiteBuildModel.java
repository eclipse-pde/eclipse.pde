package org.eclipse.pde.internal.core.isite;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
