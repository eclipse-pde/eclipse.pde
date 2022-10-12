/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.MultiStatus;
import org.osgi.framework.BundleException;

public interface RegistryBackend {

	public void connect(IProgressMonitor monitor);

	public void disconnect();

	public void start(long id) throws BundleException;

	public void stop(long id) throws BundleException;

	public MultiStatus diagnose(long id);

	public void initializeBundles(IProgressMonitor monitor);

	public void initializeExtensionPoints(IProgressMonitor monitor);

	public void setRegistryListener(BackendChangeListener listener);

	public void initializeServices(IProgressMonitor monitor);

}