package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.base.model.*;
/**
 * The top-level model object of the model that is created
 * from "plugin.jars" or "fragment.jars" file.
 */
public interface IJars extends IWritable {
/**
 * Adds a new Jar entry. This method
 * can throw a CoreException if the
 * model is not editable.
 *
 * @param entry an entry to be added
 */
void add(IJarEntry entry) throws CoreException;
/**
 * Returns the JAR entry with the specified
 * name.
 *
 * @return the entry object with the specified name, or
 * <samp>null</samp> if not found.
 */
IJarEntry getEntry(String name);
/**
 * Returns all the Jar entries in this object.
 *
 * @return an array of Jar entries
 */
IJarEntry[] getJarEntries();
/**
 * Removes a Jar entry. This method
 * can throw a CoreException if the
 * model is not editable.
 *
 * @param entry an entry to be removed
 */
void remove(IJarEntry entry) throws CoreException;
}
