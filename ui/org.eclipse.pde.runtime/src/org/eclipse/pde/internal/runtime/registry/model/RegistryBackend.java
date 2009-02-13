/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	public void setEnabled(long id, boolean enabled);

	public void start(long id) throws BundleException;

	public void stop(long id) throws BundleException;

	public MultiStatus diagnose(long id);

	public void initializeBundles(IProgressMonitor monitor);

	public void initializeExtensionPoints(IProgressMonitor monitor);

	public void setRegistryListener(BackendChangeListener listener);

	public void initializeServices(IProgressMonitor monitor);

}