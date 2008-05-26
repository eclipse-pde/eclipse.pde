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
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common code for ant task.
 *
 */
public abstract class CommonUtilsTask extends Task {
	private static final String PLUGINS_FOLDER_NAME = "plugins"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$

	public static void extractSDK(File installDir, String location) {
		if (installDir.exists()) {
			// delta existing folder
			if (!Util.delete(installDir)) {
				throw new BuildException("Could not delete : " + installDir.getAbsolutePath()); //$NON-NLS-1$
			}
		}
		if (!installDir.mkdirs()) {
			throw new BuildException("Could not create : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}

		try {
			Util.unzip(location, installDir.getAbsolutePath());
		} catch (IOException e) {
			throw new BuildException("Could not unzip SDK into : " + installDir.getAbsolutePath()); //$NON-NLS-1$
		}
	}
	
	public static String getInstallDir(File dir, String profileInstallName) {
		return new File(new File(new File(dir, profileInstallName), ECLIPSE_FOLDER_NAME), PLUGINS_FOLDER_NAME).getAbsolutePath();
	}

	public static IApiProfile createProfile(String profileName, String fileName, String eeFileLocation) {
		try {
			IApiProfile baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = Factory.newApiProfile(profileName);
			} else if (eeFileLocation != null) {
				baseline = Factory.newApiProfile(profileName, new File(eeFileLocation));
			} else {
				baseline = Factory.newApiProfile(profileName, Util.getEEDescriptionFile());
			}
			// create a component for each jar/directory in the folder
			File dir = new File(fileName);
			File[] files = dir.listFiles();
			List components = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals(CVS_FOLDER_NAME)) {
					// ignore CVS folder
					IApiComponent component = baseline.newApiComponent(bundle.getAbsolutePath());
					if(component != null) {
						components.add(component);
					}
				}
			}
			
			baseline.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			return baseline;
		} catch (CoreException e) {
			e.printStackTrace();
			return null;
		}
	}

}
