package org.eclipse.pde.internal.core.search;

import org.eclipse.pde.core.plugin.IPluginObject;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface IPluginSearchResultCollector {
	
	void setOperation(PluginSearchOperation operation);
	
	PluginSearchOperation getOperation();
	
	void accept(IPluginObject match);
	
	void searchStarted();

}
