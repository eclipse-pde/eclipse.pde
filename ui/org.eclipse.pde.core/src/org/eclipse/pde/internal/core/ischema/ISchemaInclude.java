package org.eclipse.pde.internal.core.ischema;

import org.eclipse.core.runtime.CoreException;

/**
 * Classes that implement this interface represent
 * a schema that is included in another schema.
 */
public interface ISchemaInclude extends ISchemaObject {
	/**
	 * Model property of the schema location.
	 */
	String P_LOCATION = "location";
	
	String getLocation();
	void setLocation(String location) throws CoreException;
	ISchema getIncludedSchema();
	void dispose();
}