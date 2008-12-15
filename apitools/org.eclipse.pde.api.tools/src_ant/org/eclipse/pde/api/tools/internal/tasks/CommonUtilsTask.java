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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.util.TarException;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Common code for api tooling ant task.
 *
 */
abstract class CommonUtilsTask extends Task {
	private static final String CVS_FOLDER_NAME = "CVS"; //$NON-NLS-1$
	private static final String ECLIPSE_FOLDER_NAME = "eclipse"; //$NON-NLS-1$
	protected static final String ISO_8859_1 = "ISO-8859-1"; //$NON-NLS-1$
	private static final String PLUGINS_FOLDER_NAME = "plugins"; //$NON-NLS-1$

	protected static final String CURRENT = "currentBaseline"; //$NON-NLS-1$
	protected static final String CURRENT_PROFILE_NAME = "current_baseline"; //$NON-NLS-1$
	protected static final String REFERENCE = "referenceBaseline"; //$NON-NLS-1$
	protected static final String REFERENCE_PROFILE_NAME = "reference_baseline"; //$NON-NLS-1$


	protected static IApiBaseline createBaseline(String baselineName, File dir, String eeFileLocation) {
		try {
			IApiBaseline baseline = null;
			if (ApiPlugin.isRunningInFramework()) {
				baseline = ApiModelFactory.newApiBaseline(baselineName);
			} else if (eeFileLocation != null) {
				baseline = ApiModelFactory.newApiBaseline(baselineName, new File(eeFileLocation));
			} else {
				baseline = ApiModelFactory.newApiBaseline(baselineName, Util.getEEDescriptionFile());
			}
			// create a component for each jar/directory in the folder
			File[] files = dir.listFiles();
			if(files == null) {
				throw new BuildException(
						Messages.bind(Messages.directoryIsEmpty,
						dir.getAbsolutePath()));
			}
			List components = new ArrayList();
			for (int i = 0; i < files.length; i++) {
				File bundle = files[i];
				if (!bundle.getName().equals(CVS_FOLDER_NAME)) {
					// ignore CVS folder
					IApiComponent component = ApiModelFactory.newApiComponent(baseline, bundle.getAbsolutePath());
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
	protected static void deleteBaseline(String referenceLocation, File folder) {
		if (isArchive(referenceLocation)) {
			Util.delete(folder.getParentFile());
		}
	}
	protected static File extractSDK(String installDirName, String location) {
		File file = new File(location);
		File locationFile = file;
		if (!locationFile.exists()) {
			throw new BuildException(Messages.bind(Messages.fileDoesnotExist, location));
		}
		if (isArchive(location)) {
			File tempDir = new File(System.getProperty("java.io.tmpdir")); //$NON-NLS-1$
			File installDir = new File(tempDir, installDirName);
			if (installDir.exists()) {
				// delta existing folder
				if (!Util.delete(installDir)) {
					throw new BuildException(
						Messages.bind(
							Messages.couldNotDelete,
							installDir.getAbsolutePath()));
				}
			}
			if (!installDir.mkdirs()) {
				throw new BuildException(
						Messages.bind(
								Messages.couldNotCreate,
								installDir.getAbsolutePath()));
			}
			try {
				if (isZipJarFile(location)) {
					Util.unzip(location, installDir.getAbsolutePath());
				} else if (isTGZFile(location)) {
					Util.guntar(location, installDir.getAbsolutePath());
				}
			} catch (IOException e) {
				throw new BuildException(
					Messages.bind(
						Messages.couldNotUnzip,
						new String[] {
								location,
								installDir.getAbsolutePath()
						}));
			} catch (TarException e) {
				throw new BuildException(
						Messages.bind(
								Messages.couldNotUntar,
								new String[] {
										location,
										installDir.getAbsolutePath()
								}));
			}
			return new File(installDir, ECLIPSE_FOLDER_NAME);
		} else {
			return locationFile;
		}
	}
	protected static File getInstallDir(File dir) {
		return new File(dir, PLUGINS_FOLDER_NAME);
	}
	private static boolean isArchive(String fileName) {
		return isZipJarFile(fileName) || isTGZFile(fileName);
	}
	private static boolean isTGZFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".tar.gz") //$NON-NLS-1$
			|| normalizedFileName.endsWith(".tgz"); //$NON-NLS-1$
	}
	private static boolean isZipJarFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".zip") //$NON-NLS-1$
			|| normalizedFileName.endsWith(".jar"); //$NON-NLS-1$
	}
	protected static Set initializeExcludedElement(String excludeListLocation) {
		Set excludedElement = new HashSet();
		if (excludeListLocation == null){
			return excludedElement;
		}
		File file = new File(excludeListLocation);
		if (!file.exists()) {
			return excludedElement;
		}
		InputStream stream = null;
		char[] contents = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(file));
			contents = Util.getInputStreamAsCharArray(stream, -1, CommonUtilsTask.ISO_8859_1);
		} catch (FileNotFoundException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		if (contents == null) {
			return excludedElement;
		}
		LineNumberReader reader = new LineNumberReader(new StringReader(new String(contents)));
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) continue; //$NON-NLS-1$
				excludedElement.add(line);
			}
		} catch (IOException e) {
			// ignore
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// ignore
			}
		}
		return excludedElement;
	}
	protected boolean debug;
	protected String eeFileLocation;
	protected String currentBaselineLocation;
	protected String referenceBaselineLocation;

	protected String reportLocation;
}
