package org.eclipse.pde.model.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import java.util.*;
/**
 * Classes that implement this interface are
 * capable of containing other plug-in objects.
 */
public interface IPluginParent extends IPluginObject {
	/**
	 * Adds a child object at the specified index.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param index the location of the child
	 * @param child the object to add
	 */
	void add(int index, IPluginObject child) throws CoreException;
	/**
	 * Adds a child object.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param child the object to add
	 */
	void add(IPluginObject child) throws CoreException;
	/**
	 * Returns the number of children
	 * currently owned by this parent.
	 *
	 * @return the number of children
	 */
	public int getChildCount();
	/**
	 * Returns the children owned by this parent.
	 *
	 * @return an array of children
	 */
	public IPluginObject[] getChildren();
	/**
	 * Removes a child object.
	 * This method may throw a CoreException if
	 * the model is not editable.
	 *
	 * @param child the object to remove
	 */
	void remove(IPluginObject child) throws CoreException;
}