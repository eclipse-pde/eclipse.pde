/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.osgi.bundle.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class BundlePlugin extends BundlePluginBase implements IBundlePlugin {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	public String getClassName() {
		IBundle bundle = getBundle();
		if (bundle != null) {
			return bundle.getHeader(IBundle.KEY_ACTIVATOR);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			if (className!=null) {
				bundle.setHeader(IBundle.KEY_ACTIVATOR, className);
			}
			else {
				bundle.setHeader(
						IBundle.KEY_ACTIVATOR,
						null);
			}
		}
	}
}
