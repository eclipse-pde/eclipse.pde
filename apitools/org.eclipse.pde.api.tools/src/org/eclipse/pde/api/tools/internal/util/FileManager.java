/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.util;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Manager to handle temp files that have been created. Used as a fall-back to
 * ensure we clean up after ourselves
 * 
 * @since 1.0.1
 */
public final class FileManager {

	private static FileManager fInstance = null;
	
	/**
	 * The set of recorded file paths
	 */
	private static HashSet fFilePaths = null;
	
	/**
	 * Constructor
	 * private - no instantiation
	 */
	private FileManager() {}
	
	/**
	 * Returns the singleton instance of the manager
	 * @return the manager instance
	 */
	public synchronized static FileManager getManager() {
		if(fInstance == null) {
			fInstance = new FileManager();
		}
		return fInstance;
	}
	
	/**
	 * Records a file root path to be deleted on the next call to 
	 * {@link #deleteFiles()}.
	 * @param absolutepath the absolute path in the local file system of the file to delete
	 */
	public void recordTempFileRoot(String absolutepath) {
		if(absolutepath != null) {
			if(fFilePaths == null) {
				fFilePaths = new HashSet(10);
			}
			synchronized (fFilePaths) {
				fFilePaths.add(absolutepath);
			}
		}
	}
	
	/**
	 * Deletes all of the recorded file roots from the local filesystem (if still existing)
	 * and returns the success of the entire delete operation.
	 * @return true if all recorded files were deleted, false otherwise
	 */
	public boolean deleteFiles() {
		boolean success = true;
		if(fFilePaths != null) {
			synchronized (fFilePaths) {
				try {
					File file = null;
					for(Iterator iter = fFilePaths.iterator(); iter.hasNext();) {
						file = new File((String) iter.next());
						success &= Util.delete(file);
					}
				}
				finally {
					fFilePaths.clear();	
				}
			}
		}
		return success;
	}
}
