/*
 * Created on Oct 19, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.core.osgi.bundle;

import java.util.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.osgi.bundle.*;
import org.osgi.framework.*;

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
		return parseSingleValuedHeader(Constants.BUNDLE_ACTIVATOR);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
		Dictionary manifest = getManifest();
		if (manifest != null) {
			manifest.put(Constants.BUNDLE_ACTIVATOR, className);
		}
	}
}
