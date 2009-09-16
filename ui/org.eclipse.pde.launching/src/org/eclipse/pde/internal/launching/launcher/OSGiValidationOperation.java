/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	protected IPluginModelBase[] getModels() throws CoreException {
		return BundleLauncherHelper.getMergedBundles(fLaunchConfiguration, true);
	}

}
