/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

import java.util.Map;
import org.eclipse.core.runtime.MultiStatus;
import org.osgi.framework.BundleException;

public interface RegistryBackend {

	public void connect();

	public void disconnect();

	public void setEnabled(Bundle bundle, boolean enabled);

	public void start(Bundle bundle) throws BundleException;

	public void stop(Bundle bundle) throws BundleException;

	public MultiStatus diagnose(Bundle bundle);

	public Map initializeBundles();

	public Map initializeExtensionPoints();

	public void setRegistryListener(BackendChangeListener listener);

	public void setRegistryModel(RegistryModel listener);

	public Map initializeServices();

}