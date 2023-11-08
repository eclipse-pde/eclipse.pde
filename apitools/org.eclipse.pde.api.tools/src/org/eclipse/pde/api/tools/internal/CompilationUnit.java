/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * A compilation unit in the API context acts as a proxy to the stream of file
 * contents. It holds meta-data about the underlying file, but does not hold on
 * to the actual contents of the file.
 *
 * @since 1.0.0
 */
public class CompilationUnit {

	private String name = null;
	private String filepath = null;
	private ICompilationUnit unit = null;
	/**
	 * The encoding to use when reading the compilation units' contents
	 *
	 * @since 1.0.600
	 */
	private String encoding = null;

	/**
	 * The full path to the file Constructor
	 *
	 * @param filepath the absolute path to the file. If the path points to a
	 *            file that does not exist an {@link IllegalArgumentException}
	 *            is thrown
	 * @param encoding the file encoding for the backing source
	 */
	public CompilationUnit(String filepath, String encoding) {
		File file = new File(filepath);
		if (!file.exists()) {
			throw new IllegalArgumentException("The specified path is not an existing file"); //$NON-NLS-1$
		}
		this.filepath = filepath;
		name = file.getName();
		this.encoding = resolveEncoding(encoding);
	}

	/**
	 * Constructor for use within the workspace
	 *
	 * @param compilationUnit the {@link ICompilationUnit} backing this
	 *            {@link CompilationUnit}
	 */
	public CompilationUnit(ICompilationUnit compilationUnit) {
		unit = compilationUnit;
		name = compilationUnit.getElementName();
		this.encoding = resolveEncoding(null);
	}

	/**
	 * Resolves the file encoding to use given the base. <br>
	 * <br>
	 * If there is no base encoding we check if there is a backing
	 * {@link ICompilationUnit}, and if so we try to use its direct resource
	 * encoding. Failing that we try to get it's projects' encoding. Failing
	 * that we try to use the workspace encoding. <br>
	 * <br>
	 * If there is no backing {@link ICompilationUnit}, for example when created
	 * from one of the Ant tasks, we simply use the system encoding if the
	 * encoding has not already been set.
	 *
	 * @param base
	 * @return the encoding to use for this {@link CompilationUnit}
	 * @since 1.0.600
	 */
	String resolveEncoding(String base) {
		if (base != null) {
			return base;
		}
		if (unit != null) {
			IResource resource = unit.getResource();
			if (resource != null && resource.getType() == IResource.FILE) {
				try {
					return ((IFile) resource).getCharset();
				} catch (CoreException e) {
					try {
						IProject p = resource.getProject();
						if (p != null) {
							return p.getDefaultCharset();
						}
					} catch (CoreException ce) {
						// fall through, either the project was null or we had
						// an exception
					}
				}
			}
			return ResourcesPlugin.getEncoding();
		}
		return System.getProperty("file.encoding"); //$NON-NLS-1$
	}

	/**
	 * @return the name of the file
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the input stream of the file
	 *
	 * @return the input stream of the files' contents
	 * @throws FileNotFoundException if the input stream could not connect to
	 *             the actual file
	 */
	public InputStream getInputStream() throws FileNotFoundException {
		if (unit != null) {
			try {
				return ((IFile) (unit.getCorrespondingResource())).getContents();
			} catch (CoreException e) {
				throw new FileNotFoundException(e.getStatus().getMessage());
			}
		}
		return new FileInputStream(new File(filepath));
	}

	/**
	 * Returns the encoding for this compilation unit.
	 *
	 * @return the encoding to use
	 * @throws CoreException
	 * @since 1.0.600
	 */
	public String getEncoding() throws CoreException {
		return this.encoding;
	}

	@Override
	public String toString() {
		return getName();
	}
}
