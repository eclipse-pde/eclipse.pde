/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

import java.io.InputStream;
import org.eclipse.core.runtime.CoreException;

/**
 * Common implementation of target handles.
 * 
 * @since 3.5
 */
public abstract class AbstractTargetHandle implements ITargetHandle {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.target.ITargetHandle#getTargetDefinition()
	 */
	public ITargetDefinition getTargetDefinition() throws CoreException {
		TargetDefinition definition = new TargetDefinition(this);
		if (exists()) {
			definition.setContents(getInputStream());
		}
		return definition;
	}

	/**
	 * Returns an input stream of the target definition's contents.
	 * 
	 * @return stream of content
	 * @throws CoreException if an error occurs
	 */
	protected abstract InputStream getInputStream() throws CoreException;

	/**
	 * Deletes the underlying target definition.
	 * 
	 * @throws CoreException if unable to delete
	 */
	abstract void delete() throws CoreException;

	/**
	 * Saves the definition to underlying storage.
	 * 
	 * @param definition target to save
	 * @throws CoreException on failure
	 */
	abstract void save(ITargetDefinition definition) throws CoreException;
}
