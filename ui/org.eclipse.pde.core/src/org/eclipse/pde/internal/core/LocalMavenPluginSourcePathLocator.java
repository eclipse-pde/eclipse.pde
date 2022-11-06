/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - extracted from ClasspathUtilCore
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;

/**
 * Check if there is already a local maven source bundle available
 *
 */
public class LocalMavenPluginSourcePathLocator implements IPluginSourcePathLocator {

	@Override
	public IPath locateSource(IPluginBase plugin) {
		String installLocation = plugin.getModel().getInstallLocation();
		if (installLocation != null) {
			File path = new File(installLocation);
			// The usual Maven covention foo-123.jar => foo-123-sources.jar
			String bundleFileName = path.getName();
			String sourceFileName = bundleFileName.substring(0, bundleFileName.indexOf(".jar")) + "-sources.jar"; //$NON-NLS-1$ //$NON-NLS-2$
			File sourceFile = new File(path.getParentFile(), sourceFileName);
			if (sourceFile.isFile()) {
				return new Path(sourceFile.getAbsolutePath());
			}
		}
		return null;
	}

}
