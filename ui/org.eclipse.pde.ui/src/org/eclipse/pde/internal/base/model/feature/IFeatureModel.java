package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.pde.internal.base.model.*;
import org.eclipse.pde.model.*;
import org.eclipse.core.resources.IResource;
/**
 * This model type is designed to hold data loaded from
 * "feature.xml" file of an Eclipse feature.
 */
public interface IFeatureModel extends IModel, IModelChangeProvider {
/**
 * Returns the top-level model object.
 *
 * @return top-level model object of the feature model
 */
public IFeature getFeature();
/**
 * Returns the factory that should be used
 * to create new instances of model objects.
 *
 * @return feature model factory
 */
IFeatureModelFactory getFactory();
/**
 * Returns install location of the feature.xml file
 * in case of external files.
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
