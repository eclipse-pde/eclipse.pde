package org.eclipse.pde.internal.base.model.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The top-level model object of the model that is created
 * from "plugin.jars" or "fragment.jars" file.
 */
public interface IBuild extends IWritable {
/**
 * Adds a new build entry. This method
 * can throw a CoreException if the
 * model is not editable.
 *
 * @param entry an entry to be added
 */
void add(IBuildEntry entry) throws CoreException;
/**
 * Returns all the build entries in this object.
 *
 * @return an array of build entries
 */
IBuildEntry[] getBuildEntries();
/**
 * Returns the build entry with the specified
 * name.
 *
 * @return the entry object with the specified name, or
 * <samp>null</samp> if not found.
 */
IBuildEntry getEntry(String name);
/**
 * Removes a build entry. This method
 * can throw a CoreException if the
 * model is not editable.
 *
 * @param entry an entry to be removed
 */
void remove(IBuildEntry entry) throws CoreException;
}
