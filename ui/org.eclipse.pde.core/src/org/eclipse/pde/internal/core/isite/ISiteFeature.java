package org.eclipse.pde.internal.core.isite;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ifeature.IVersionable;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface ISiteFeature extends IVersionable, ISiteObject {
	String P_TYPE = "type";
	String P_URL = "url";
	void addCategories(ISiteCategory [] categories) throws CoreException;
	void removeCategories(ISiteCategory [] categories) throws CoreException;
	ISiteCategory [] getCategories();
	String getType();
	URL getURL();
	void setType(String type) throws CoreException;
	void setURL(URL url) throws CoreException;
}
