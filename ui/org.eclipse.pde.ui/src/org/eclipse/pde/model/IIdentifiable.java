package org.eclipse.pde.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
/**
 * Classes implement this interface if
 * their instances need to be uniquely identified
 * using an id.
 */
public interface IIdentifiable {
	/**
	 * A property that will be carried by the change event
	 * if 'id' field of this object is changed.
	 */
	public static final String P_ID = "id";
	/**
	 * Returns a unique id of this object.
	 * @return the id of this object
	 */
	public String getId();
	/**
	 * Sets the id of this IIdentifiable to the provided value.
	 * This method will throw CoreException if
	 * object is not editable.
	 *
	 *@param id a new id of this object
	 */
	void setId(String id) throws CoreException;
}