package org.eclipse.pde.internal.ui.search;

import org.eclipse.core.resources.IMarker;
import org.eclipse.search.ui.IGroupByKeyComputer;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginGroupByKeyComputer implements IGroupByKeyComputer {

	/**
	 * @see org.eclipse.search.ui.IGroupByKeyComputer#computeGroupByKey(org.eclipse.core.resources.IMarker)
	 */
	public Object computeGroupByKey(IMarker marker) {
		return PluginSearchResultCollector.getCurrentMatch();
	}

}
