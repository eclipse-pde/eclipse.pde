/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.bundle;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.ibundle.*;
import org.osgi.framework.*;

public class BundlePlugin extends BundlePluginBase implements IBundlePlugin {

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	public String getClassName() {
		return parseSingleValuedHeader(Constants.BUNDLE_ACTIVATOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
		IBundle bundle = getBundle();
		if (bundle != null) {
			String old = getClassName();
			bundle.setHeader(Constants.BUNDLE_ACTIVATOR, className);
			model.fireModelObjectChanged(this, P_CLASS_NAME, old, className);
		}
	}
}
