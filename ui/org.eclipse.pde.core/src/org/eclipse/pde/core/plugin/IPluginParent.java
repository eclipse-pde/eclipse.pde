package org.eclipse.pde.core.plugin;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
/**
 * Classes that implement this interface are
 * capable of containing other plug-in objects.
 */
public interface IPluginParent extends IPluginObject {
	/**
	 * A property that will be used when firing notification
	 * of the sibling swap.
	 */
	public static final String P_SIBLING_ORDER = "sibling_order";
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
	 * Returns the position of the child in this parent.
	 * @param child a child of this parent
	 * @return a 0-based index of the child
	 */
	int getIndexOf(IPluginObject child);
	/**
	 * Swaps the position of of the provided siblings 
	 * in the parent.
	 * @param child1 the first child
	 * @param child2 the second child
	 * @throws CoreException thrown if the model is not editable.
	 */	
	void swap(IPluginObject child1, IPluginObject child2) throws CoreException;
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