/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.build;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IWritable;

/**
 * The top-level model object of the model that is created from
 * <code>build.properties</code> file.
 *  
 *  @noimplement This interface is not intended to be implemented by clients.
 *  @noextend This interface is not intended to be extended by clients.
 */
public interface IBuild extends IWritable {
	/**
	 * Adds a new build entry. This method can throw a CoreException if the
	 * model is not editable.
	 * 
	 * @param entry
	 *            an entry to be added
	 * @throws CoreException if the model is not editable
	 */
	void add(IBuildEntry entry) throws CoreException;

	/**
	 * Returns all the build entries in this object.
	 * 
	 * @return an array of build entries
	 */
	IBuildEntry[] getBuildEntries();

	/**
	 * Returns the build entry with the specified name.
	 * 
	 * @param name
	 *            name of the desired entry
	 * @return the entry object with the specified name, or <samp>null</samp>
	 *         if not found.
	 */
	IBuildEntry getEntry(String name);

	/**
	 * Removes a build entry. This method can throw a CoreException if the model
	 * is not editable.
	 * 
	 * @param entry
	 *            an entry to be removed
	 * @throws CoreException if the model is not editable
	 */
	void remove(IBuildEntry entry) throws CoreException;
}
