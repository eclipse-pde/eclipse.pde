package org.eclipse.pde.ui;

import org.eclipse.pde.ui.templates.*;

/**
 * @author melhem
 *
 */
public interface IFragmentFieldData extends IFieldData {
	
	String getPluginId();
	
	String getPluginVersion();
	
	int getMatch();
}
