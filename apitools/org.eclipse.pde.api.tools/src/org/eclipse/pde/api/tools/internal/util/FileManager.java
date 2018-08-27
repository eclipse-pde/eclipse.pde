/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.io.File;
import java.util.HashSet;

/**
 * Manager to handle temporary files that have been created. Used as a fall-back
 * to ensure we clean up after ourselves
 *
 * @since 1.0.1
 */
public final class FileManager {

	private static FileManager fInstance = null;

	/**
	 * The set of recorded file paths
	 */
	private static HashSet<String> fFilePaths = null;

	/**
	 * Constructor private - no instantiation
	 */
	private FileManager() {
	}

	/**
	 * Returns the singleton instance of the manager
	 *
	 * @return the manager instance
	 */
	public synchronized static FileManager getManager() {
		if (fInstance == null) {
			fInstance = new FileManager();
		}
		return fInstance;
	}

	/**
	 * Records a file root path to be deleted on the next call to
	 * {@link #deleteFiles()}.
	 *
	 * @param absolutepath the absolute path in the local file system of the
	 *            file to delete
	 */
	public void recordTempFileRoot(String absolutepath) {
		if (absolutepath != null) {
			if (fFilePaths == null) {
				fFilePaths = new HashSet<>(10);
			}
			synchronized (fFilePaths) {
				fFilePaths.add(absolutepath);
			}
		}
	}

	/**
	 * Deletes all of the recorded file roots from the local filesystem (if
	 * still existing) and returns the success of the entire delete operation.
	 *
	 * @return true if all recorded files were deleted, false otherwise
	 */
	public boolean deleteFiles() {
		boolean success = true;
		if (fFilePaths != null) {
			synchronized (fFilePaths) {
				try {
					File file = null;
					for (String filename : fFilePaths) {
						file = new File(filename);
						success &= Util.delete(file);
					}
				} finally {
					fFilePaths.clear();
				}
			}
		}
		return success;
	}
}
