/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.project;

import org.eclipse.core.runtime.IPath;

/**
 * Specifies the origin of source, class files, and/or archive for an entry
 * on the Bundle-Classpath header. Instances of this class can be created
 * via {@link IBundleProjectService#newBundleClasspathEntry(IPath, IPath, IPath)}.
 * 
 * @since 3.6
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBundleClasspathEntry {

	/**
	 * Returns a project relative path for a folder containing source code targeted for
	 * the library this entry describes, or <code>null</code> if there is no source for
	 * the entry.
	 * <p>
	 * When a {@link #getSourcePath()} is specified, the binary path specifies the output
	 * folder for the source, and in this case <code>null</code> indicates the associated
	 * Java project's default build path output folder.
	 * <p>
	 * @return project relative path of folder containing source code or <code>null</code>
	 */
	public IPath getSourcePath();

	/**
	 * Returns a project relative path for a folder or archive containing class files and
	 * resource files targeted for the library this entry describes.
	 * <p>
	 * When a {@link #getSourcePath()} is specified, the binary path specifies the output
	 * folder for the source, and in this case <code>null</code> indicates the associated
	 * Java project's default build path output folder. When a {@link #getSourcePath()} is
	 * not specified, the binary path specifies a folder of class files.
	 * </p>
	 * @return project relative class file folder, archive or <code>null</code> to indicate
	 *  default build path output folder
	 */
	public IPath getBinaryPath();

	/**
	 * Returns the library on the Bundle-Classpath header the source and/binary files are targeted for
	 * or <code>null</code> to indicate the default entry <code>"."</code>.
	 * <p>
	 * When a {@link #getSourcePath()} or {@link #getBinaryPath()} is specified, this indicates that
	 * the library will be generated from source or binaries. When neither {@link #getSourcePath()}
	 * or {@link #getBinaryPath()} are specified, it indicates the library is contained in the
	 * project as an archive at the specified location.  
	 * </p>
	 * @return Bundle-Classpath library or <code>null</code>
	 */
	public IPath getLibrary();
}
