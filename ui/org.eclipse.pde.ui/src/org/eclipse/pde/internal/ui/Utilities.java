/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui;

import java.io.*;
import java.util.Date;
import org.eclipse.core.runtime.Platform;

/**
 * This class is a collection of utility functions for creating and using temporary working directories. 
 */
public class Utilities {

	private static long tmpseed = (new Date()).getTime();
	private static String dirRoot = null;

	/**
	 * Returns a new working directory (in temporary space). Ensures
	 * the directory exists. Any directory levels that had to be created
	 * are marked for deletion on exit.
	 * 
	 * @return working directory
	 * @exception IOException
	 * @since 2.0
	 */
	public static synchronized File createWorkingDirectory() throws IOException {

		if (dirRoot == null) {
			dirRoot = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
			// in Linux, returns '/tmp', we must add '/'
			if (!dirRoot.endsWith(File.separator))
				dirRoot += File.separator;

			// on Unix/Linux, the temp dir is shared by many users, so we need to ensure 
			// that the top working directory is different for each user
			if (!Platform.getOS().equals("win32")) { //$NON-NLS-1$
				String home = System.getProperty("user.home"); //$NON-NLS-1$
				home = Integer.toString(home.hashCode());
				dirRoot += home + File.separator;
			}
			dirRoot += "eclipse" + File.separator + ".update" + File.separator + Long.toString(tmpseed) + File.separator; //$NON-NLS-1$ //$NON-NLS-2$
		}

		String tmpName = dirRoot + Long.toString(++tmpseed) + File.separator;

		File tmpDir = new File(tmpName);
		verifyPath(tmpDir, false);
		if (!tmpDir.exists())
			throw new FileNotFoundException(tmpName);
		return tmpDir;
	}

	/**
	 * Perform shutdown processing for temporary file handling.
	 * This method is called when platform is shutting down.
	 * It is not intended to be called at any other time under
	 * normal circumstances. A side-effect of calling this method
	 * is that the contents of the temporary directory managed 
	 * by this class are deleted. 
	 * 
	 * @since 2.0
	 */
	public static void shutdown() {
		if (dirRoot == null)
			return;
		File temp = new File(dirRoot); // temp directory root for this run
		cleanupTemp(temp);
		temp.delete();
	}

	private static void cleanupTemp(File root) {
		File[] files = root.listFiles();
		for (int i = 0; files != null && i < files.length; i++) {
			if (files[i].isDirectory())
				cleanupTemp(files[i]);
			files[i].delete();
		}
	}

	private static void verifyPath(File path, boolean isFile) {
		// if we are expecting a file back off 1 path element
		if (isFile) {
			if (path.getAbsolutePath().endsWith(File.separator)) {
				// make sure this is a file
				path = path.getParentFile();
				isFile = false;
			}
		}

		// already exists ... just return
		if (path.exists())
			return;

		// does not exist ... ensure parent exists
		File parent = path.getParentFile();
		verifyPath(parent, false);

		// ensure directories are made. Mark files or directories for deletion
		if (!isFile)
			path.mkdir();
		path.deleteOnExit();
	}
}
