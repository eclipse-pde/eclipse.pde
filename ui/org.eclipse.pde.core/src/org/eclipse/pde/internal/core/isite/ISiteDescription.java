package org.eclipse.pde.internal.core.isite;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 */
public interface ISiteDescription extends ISiteObject {
	String P_URL = "url";
	String P_TEXT = "text";
	
	URL getURL();
	String getText();
	
	void setURL(URL url) throws CoreException;
	void setText(String text) throws CoreException;

}
