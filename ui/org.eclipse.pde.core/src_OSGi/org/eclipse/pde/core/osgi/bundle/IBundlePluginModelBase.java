/*
 * Created on Oct 1, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.core.osgi.bundle;

import org.eclipse.pde.core.plugin.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IBundlePluginModelBase extends IPluginModelBase {
	IBundleModel getBundleModel();
	IExtensionsModel getExtensionsModel();
	void setBundleModel(IBundleModel bundleModel);
	void setExtensionsModel(IExtensionsModel extensionsModel);
}