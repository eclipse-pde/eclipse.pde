package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.pde.ui.*;

/**
 * @author melhem
 *
 */
public class FragmentFieldData extends AbstractFieldData implements IFragmentFieldData {

	private String fPluginId;
	private String fPluginVersion;
	private int fMatch;
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFragmentFieldData#getPluginId()
	 */
	public String getPluginId() {
		return fPluginId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFragmentFieldData#getPluginVersion()
	 */
	public String getPluginVersion() {
		return fPluginVersion;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.IFragmentFieldData#getMatch()
	 */
	public int getMatch() {
		return fMatch;
	}
	
	public void setPluginId(String id) {
		fPluginId = id;
	}
	
	public void setPluginVersion(String version) {
		fPluginVersion = version;
	}
	
	public void setMatch(int match) {
		fMatch = match;
	}
}
