/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.IOException;
import java.net.URL;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class BuildApplication implements IApplication {

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#start(org.eclipse.equinox.app.IApplicationContext)
	 */
	public Object start(IApplicationContext context) throws Exception {
		//take down splash
		context.applicationRunning();

		IExtension extension = Platform.getExtensionRegistry().getExtension("org.eclipse.ant.core.antRunner"); //$NON-NLS-1$
		if (extension == null)
			return null;
		IConfigurationElement element = extension.getConfigurationElements()[0];
		Object ee = element.createExecutableExtension("run"); //$NON-NLS-1$
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		args = updateArgs(args);

		if (ee instanceof AntRunner) {
			return ((AntRunner) ee).run(args);
		}
		// else it is probably an old IPlatformRunnable
		return doPlatformRunnable(ee, args);
	}

	/*
	 * If the Executable Extension is an old IPlatformRunnable, use this method to run it to 
	 * avoid the warnings about deprecation.
	 * @deprecated
	 * @throws Exception
	 */
	private Object doPlatformRunnable(Object ee, Object args) throws Exception {
		if (ee instanceof IPlatformRunnable)
			return ((IPlatformRunnable) ee).run(args);
		return null;
	}

	private String[] updateArgs(String[] args) throws IOException {
		for (int i = 0; i < args.length; i++) {
			String string = args[i];
			if (string.equals("-f") || string.equals("-buildfile")) //$NON-NLS-1$ //$NON-NLS-2$
				return args;
		}
		int length = args.length;
		String[] result = new String[length + 2];
		System.arraycopy(args, 0, result, 0, length);
		result[length] = "-f"; //$NON-NLS-1$
		URL buildURL = BundleHelper.getDefault().find(new Path("/scripts/build.xml")); //$NON-NLS-1$
		result[length + 1] = FileLocator.toFileURL(buildURL).getFile();
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.equinox.app.IApplication#stop()
	 */
	public void stop() {
		// do nothing for now
	}
}
