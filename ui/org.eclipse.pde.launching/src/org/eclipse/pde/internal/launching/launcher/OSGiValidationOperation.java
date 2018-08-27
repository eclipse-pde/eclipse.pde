/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.launching.launcher;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class OSGiValidationOperation extends LaunchValidationOperation {

	public OSGiValidationOperation(ILaunchConfiguration configuration) {
		super(configuration);
	}

	@Override
	protected IPluginModelBase[] getModels() throws CoreException {
		return BundleLauncherHelper.getMergedBundles(fLaunchConfiguration, true);
	}

}
