package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
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
