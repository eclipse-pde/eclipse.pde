package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 */
public interface ISiteCategoryDefinition extends ISiteObject {
	String P_NAME = "name";
	String P_DESCRIPTION = "description";
	String getName();
	void setName(String name) throws CoreException;
	ISiteDescription getDescription();
	void setDescription(ISiteDescription description) throws CoreException;
}
