package org.eclipse.pde.internal.base.model.component;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
/**
 * This model factory should be used to create
 * model objects of the component model.
 */
public interface IComponentModelFactory {
/**
 * Creates a new fragment model object.
 *
 * @return new instance of the component fragment object
 */
IComponentFragment createFragment();
/**
 * Creates a new plug-in model object.
 *
 * @return new instance of the component plug-in object
 */
IComponentPlugin createPlugin();
/**
 * Creates a new component URL instance.
 *
 * @return a new component URL instance
 */
IComponentURL createURL();
/**
 * Creates a new instance of a component URL element for
 * the provided URL parent and the type.
 *
 * @return a new URL element instance
 */
IComponentURLElement createURLElement(IComponentURL parent, int elementType);
}
