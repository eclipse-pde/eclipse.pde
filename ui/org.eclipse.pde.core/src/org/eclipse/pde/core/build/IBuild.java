package org.eclipse.pde.core.build;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IWritable;
/**
 * The top-level model object of the model that is created
 * from "plugin.jars" or "fragment.jars" file.
 * <p>
 * <b>Note:</b> This interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IBuild extends IWritable {
	/**
	 * Adds a new build entry. This method
	 * can throw a CoreException if the
	 * model is not editable.
	 *
	 * @param entry an entry to be added
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void add(IBuildEntry entry) throws CoreException;
	/**
	 * Returns all the build entries in this object.
	 *
	 * @return an array of build entries
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IBuildEntry[] getBuildEntries();
	/**
	 * Returns the build entry with the specified
	 * name.
	 *
	 * @param the name of the desired entry
	 * @return the entry object with the specified name, or
	 * <samp>null</samp> if not found.
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	IBuildEntry getEntry(String name);
	/**
	 * Removes a build entry. This method
	 * can throw a CoreException if the
	 * model is not editable.
	 *
	 * @param entry an entry to be removed
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void remove(IBuildEntry entry) throws CoreException;
}