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
package org.eclipse.pde.core;

import org.eclipse.core.resources.*;


/**
 * Classes that implement this interface are
 * responsible for holding a table of
 * models associated with the underlying
 * objects. They have several responsibilities:
 * <ul>
 * <li>To hold model objects in one place
 * <li>To allow requesters to connect to the
 * models or to disconnect from them.
 * <li>To maintain reference count of
 * model users.
 * <li>To ensure "one writer/many readers"
 * access to the model so that only
 * one requester can get an editable
 * copy of the model at any time.
 * <li>To notify interested parties when
 * models are added and removed.
 * </ul>
 * Model providers are responsible for
 * listening to the workspace, updating
 * models whose underlying resources
 * have been updated, and
 * removing them from the table
 * when those resources have been
 * deleted.
 */
public interface IModelProvider {
/**
 * Registers a listener that will be notified about changes
 * in the managed models.
 *
 * @param listener the listener that will be registered
 */
void addModelProviderListener(IModelProviderListener listener);
/**
 * Connects the consumer with the model provider.
 * The connection will search for the model
 * whose underlying resource matches the
 * provided element. If the model exists,
 * reference count will be incremented.
 * If possible, the consumer will
 * be connected with the editable
 * copy of the model.
 *
 * @param element the underlying element that the model is associated with
 * @param consumer the consumer of the model
 */
//public void connect(Object element, Object consumer);
/**
 * Connects the consumer with the model associated with
 * the element. The additional flag allows consumer
 * to specify whether an editable or read-only copy
 * of the model is needed.
 * Editable copy will be created if not in use.
 * Otherwise, read-only version will be
 * provided regardless of the flag value
 *
 * @param element an underlying element of the requested model
 * @param consumer the model requester
 * @param editableCopy if true, an editable copy will be
 * provided, but inly if not in use already
 */
//public void connect(Object element, Object consumer, boolean editableCopy);
/**
 * Disassociates the consumer and the model for the
 * provided element.
 * If there are more than one consumer, reference
 * count will be decremented. If the model copy
 * was editable, it will be freed up for another
 * consumer.
 *
 * @param element the underlying element of the model
 * @param consumer the model requester
 */
//public void disconnect(Object element, Object consumer);
/**
 * Returns the model copy that matches the underlying
 * element. If the consumer is the one that is connected
 * to the editable copy, an editable copy will be
 * returned. All other consumers will receive
 * a read-only copy of the model.
 *
 * @param element the underlying element of the model
 * @param consumer the model requester
 * @return model instance that matches the underlying element
 */
//public IModel getModel(Object element, Object consumer);
public IModel getModel(IFile file);
/**
 * Deregisters a listener from notification.
 *
 * @param listener the listener to be deregistered
 */
void removeModelProviderListener(IModelProviderListener listener);
}
