package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface ISiteArchive extends ISiteObject {
	String P_URL = "url";
	String P_PATH = "path";
	String getURL();
	void setURL(String url) throws CoreException;
	String getPath();
	void setPath(String path) throws CoreException;
}
