/*******************************************************************************
 * Copyright (c) 2007, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.net.URL;

import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class BuildApplication implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		//take down splash
		context.applicationRunning();

		IExtension extension = Platform.getExtensionRegistry().getExtension("org.eclipse.ant.core.antRunner"); //$NON-NLS-1$
		if (extension == null) {
			return null;
		}
		IConfigurationElement element = extension.getConfigurationElements()[0];
		Object ee = element.createExecutableExtension("run"); //$NON-NLS-1$
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		args = updateArgs(args);

		if (ee instanceof AntRunner) {
			return ((AntRunner) ee).run(args);
		}
		// else it is probably an old no longer supported IPlatformRunnable
		return null;
	}

	private String[] updateArgs(String[] args) throws IOException {
		for (String string : args) {
			if (string.equals("-f") || string.equals("-buildfile")) { //$NON-NLS-1$ //$NON-NLS-2$
				return args;
			}
		}
		int length = args.length;
		String[] result = new String[length + 2];
		System.arraycopy(args, 0, result, 0, length);
		result[length] = "-f"; //$NON-NLS-1$
		URL buildURL = BundleHelper.getDefault().find(IPath.fromOSString("/scripts/build.xml")); //$NON-NLS-1$
		result[length + 1] = FileLocator.toFileURL(buildURL).getFile();
		return result;
	}

	@Override
	public void stop() {
		// do nothing for now
	}
}
