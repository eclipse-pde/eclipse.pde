/*
 * Created on Sep 30, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.core;

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public interface IModelManager {
	IPluginModel[] getPluginModels();
	IFragmentModel [] getFragmentModels();
	IFeatureModel[] getFeatureModels();
	IPluginModelBase[] getAllModels();
	IFragment[] getFragmentsFor(String pluginId, String version);
	void shutdown();
	boolean isInitialized();

}
