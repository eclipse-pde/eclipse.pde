package org.eclipse.pde.internal.base.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.net.*;
/**
 * The container for all URL definitions of this component.
 */
public interface IFeatureURL extends IFeatureObject {
/**
 * Add a URL element that should be used to
 * discover new Eclipse components. This
 * method may throw a CoreException if
 * the model is not editable.
 *
 * @param discovery a new discovery URL element
 */
public void addDiscovery(IFeatureURLElement discovery) throws CoreException;
/**
 * Add a URL element that should be used to
 * update Eclipse components. This
 * method may throw a CoreException if
 * the model is not editable.
 *
 * @param update a new update URL element
 */
public void addUpdate(IFeatureURLElement update) throws CoreException;
/**
 * Return all URL elements that can be used
 * to discover new Eclipse components.
 *
 * @return an array of URL components
 */
public IFeatureURLElement [] getDiscoveries();
/**
 * Return all URL elements that can be used
 * to update new Eclipse components.
 *
 * @return an array of URL components
 */
public IFeatureURLElement [] getUpdates();
/**
 * Remove a URL element that should be used to
 * discover new Eclipse components. This
 * method may throw a CoreException if
 * the model is not editable.
 *
 * @param discovery a discovery URL element to remove
 */
public void removeDiscovery(IFeatureURLElement discovery) throws CoreException;
/**
 * Remove a URL element that should be used to
 * update new Eclipse components. This
 * method may throw a CoreException if
 * the model is not editable.
 *
 * @param update an update URL element to remove
 */
public void removeUpdate(IFeatureURLElement update) throws CoreException;
}
