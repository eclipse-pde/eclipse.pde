/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;

/**
 * A compilation unit in the API context acts as a proxy to the stream of file contents. It holds 
 * meta-data about the underlying file, but does not hold on to the actual contents of the file.
 * 
 * @since 1.0.0
 */
public class CompilationUnit {

	private String name = null;
	private String filepath = null;
	private ICompilationUnit unit = null;
	
	/**
	 * The full path to the file
	 * Constructor
	 * @param filepath the absolute path to the file. If the path points to a file that does
	 * not exist an {@link IllegalArgumentException} is thrown
	 */
	public CompilationUnit(String filepath) {
		File file = new File(filepath);
		if(!file.exists()) {
			throw new IllegalArgumentException("The specified path is not an existing file"); //$NON-NLS-1$
		}
		this.filepath = filepath;
		name = file.getName();
	}
	
	public CompilationUnit(ICompilationUnit compilationUnit) {
		unit = compilationUnit;
		name = compilationUnit.getElementName();
	}
	
	/**
	 * @return the name of the file
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the input stream of the file
	 * @return the input stream of the files' contents
	 * @throws FileNotFoundException if the input stream could not connect
	 * to the actual file
	 */
	public InputStream getInputStream() throws FileNotFoundException {
		if (unit != null) {
			try {
				return ((IFile)(unit.getCorrespondingResource())).getContents();
			} catch (CoreException e) {
				// TODO: should throw CoreException
				throw new FileNotFoundException(e.getStatus().getMessage());
			}
		}
		return new FileInputStream(new File(filepath));
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return getName();
	}
}
