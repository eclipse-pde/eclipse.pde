/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests.resources;

import org.eclipse.core.internal.runtime.AdapterManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.pde.core.target.TargetFeature;

/**
 * Basic target location used to test usage of extensions of ITargetLocation,
 * currently only serializes to a basic xml and returns null for other request.
 * More capabilities should be added as more tests are required
 */
public class TestTargetLocation implements ITargetLocation {

	@Override
	public IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
		return Status.OK_STATUS;
	}

	@Override
	public boolean isResolved() {
		return true;
	}

	@Override
	public IStatus getStatus() {
		return Status.OK_STATUS;
	}

	@Override
	public String getType() {
		return "TestTargetLocation";
	}

	@Override
	public String getLocation(boolean resolve) {
		return null;
	}

	@Override
	public TargetBundle[] getBundles() {
		return null;
	}

	@Override
	public TargetFeature[] getFeatures() {
		return null;
	}

	@Override
	public String[] getVMArguments() {
		return null;
	}

	@Override
	public String serialize() {
		return "<location locationTestAttribute=\"test1\"><message messageTestAttribute=\"test2\">This is a custom target location</message></location>";
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return AdapterManager.getDefault().getAdapter(this, adapter);
	}

}
