/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

/**
 * Event provides a list of objects that have changed.
 * Possible objects on the list are IBundle, IService, IExtension, IExtensionPoint.
 *
 * This is temporary solution and is subject to future changes.
 */
public interface ModelChangeListener {

	void modelChanged(ModelChangeDelta[] deltas);
}
