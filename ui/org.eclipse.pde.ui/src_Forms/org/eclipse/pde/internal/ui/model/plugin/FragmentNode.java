package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class FragmentNode extends PluginBaseNode implements IFragment {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginId()
	 */
	public String getPluginId() {
		return getXMLAttributeValue("plugin-id");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		return getXMLAttributeValue("plugin-version");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getRule()
	 */
	public int getRule() {
		String match = getXMLAttributeValue("match");
		if (match == null)
			return IMatchRules.NONE;
		if (match.equals("compatible"))			
			return IMatchRules.COMPATIBLE;		
		if (match.equals("perfect"))
			return IMatchRules.PERFECT;
		if (match.equals("equivalent"))
			return IMatchRules.EQUIVALENT;
		return IMatchRules.GREATER_OR_EQUAL;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginId(java.lang.String)
	 */
	public void setPluginId(String id) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
	}
}
