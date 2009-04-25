/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

/**
 * Notifies on any changes coming from backend. Usually RegistryModel is only interested in receiving
 * news about that.
 *
 */
public interface BackendChangeListener {

	void addBundle(Bundle adapter);

	void removeBundle(Bundle adapter);

	void updateBundle(Bundle adapter, int updated);

	void addService(ServiceRegistration adapter);

	void removeService(ServiceRegistration adapter);

	void updateService(ServiceRegistration adapter);

	void addExtensions(Extension[] extensions);

	void removeExtensions(Extension[] extensions);

	void addExtensionPoints(ExtensionPoint[] extensionPoints);

	void removeExtensionPoints(ExtensionPoint[] extensionPoints);
}
