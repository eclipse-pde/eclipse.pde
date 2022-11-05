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
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.sourcelocators;

import java.io.File;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.pde.core.IPluginSourcePathLocator;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.ISharedPluginModel;

/**
 * A plugin source path locator that uses for a co-located source bundle in the
 * same directory. This could be e.g the case for bundle pools or products with
 * a plugin folder.
 */
public class EclipsePluginSourcePathLocator implements IPluginSourcePathLocator {

	@Override
	public IPath locateSource(IPluginBase plugin) {
		ISharedPluginModel model = plugin.getModel();
		String installLocation = model.getInstallLocation();
		if (installLocation != null) {
			File path = new File(installLocation);
			if (path.isFile()) {
				File sourceFile = new File(path.getParentFile(),
						plugin.getId() + ".source_" + plugin.getVersion() + ".jar"); //$NON-NLS-1$ //$NON-NLS-2$
				if (sourceFile.isFile()) {
					return new Path(sourceFile.getAbsolutePath());
				}
			}
		}
		return null;
	}

}
