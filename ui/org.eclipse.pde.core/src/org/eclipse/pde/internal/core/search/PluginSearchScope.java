package org.eclipse.pde.internal.core.search;

import org.eclipse.pde.internal.core.ModelEntry;

/**
 * @author W Melhem
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class PluginSearchScope {

	public static final int SCOPE_WORKSPACE = 0;
	public static final int SCOPE_SELECTION = 1;
	public static final int SCOPE_WORKING_SETS = 2;
	
	public static final int EXTERNAL_SCOPE_NONE = 0;
	public static final int EXTERNAL_SCOPE_ENABLED = 1;
	public static final int EXTERNAL_SCOPE_ALL = 2;
	
	/**
	 * Create a scope object with the provided arguments.
	 * @param workspaceScope  one of SCOPE_WORKSPACE, SCOPE_SELECTION,
	 * SCOPE_WORKING_SETS
	 * @param externalScope  one of EXTERNAL_SCOPE_NONE, EXTERNAL_SCOPE_ENABLED,
	 * EXTERNAL_SCOPE_ALL
	 * @param workingSets  goes with SCOPE_WORKING_SETS, otherwise null	 * @param description  an NL string that describes the entire scope	 */
	public PluginSearchScope (int workspaceScope, int externalScope, IWorkingSet[] workingSets, String description) {
	}
	
	
	/**
	 * Creates a default scope object that will return all the entries int eh
	 * Plugi
	 * @see java.lang.Object#Object()	 */
	public PluginSearchScope() {
	}
	
	public String getDescription() {
		return null;
	}
	
	public ModelEntry[] getMatchingEntries() {
		return new ModelEntry[0];
	}
	
}
