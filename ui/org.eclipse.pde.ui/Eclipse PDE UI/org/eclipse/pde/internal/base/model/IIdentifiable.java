package org.eclipse.pde.internal.base.model;

import org.eclipse.core.runtime.CoreException;
/**
 * Classes implement this interface if
 * their instances need to be uniquely identified
 * with an id.
 */
public interface IIdentifiable {
/**
 * A property that the change event will carry
 * if 'id' field of this object is changed.
 */
	public static final String P_ID = "id";
/**
 * Returns a unique ID of this object.
 * @return the id of this object
 */
public String getId();
/**
 * Sets the Id of this Identifiable to the provided value.
 * This method will throw CoreException if
 * object is not editable.
 *
 *@param id a new id of this object
 */
void setId(String id) throws CoreException;
}
