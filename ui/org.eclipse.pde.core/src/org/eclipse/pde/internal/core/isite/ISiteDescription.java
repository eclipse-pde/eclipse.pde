package org.eclipse.pde.internal.core.isite;

import org.eclipse.core.runtime.CoreException;

/**
 * @author dejan
 *
 */
public interface ISiteDescription extends ISiteObject {
	String P_URL = "url";
	String P_TEXT = "text";
	
	String getURL();
	String getText();
	
	void setURL(String url) throws CoreException;
	void setText(String text) throws CoreException;

}
