package org.eclipse.pde.internal.ui.model.plugin;

import java.util.*;
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
		return getXMLAttributeValue(P_PLUGIN_ID);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#getPluginVersion()
	 */
	public String getPluginVersion() {
		return getXMLAttributeValue(P_PLUGIN_VERSION);
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
		setXMLAttribute(P_PLUGIN_ID, id);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setPluginVersion(java.lang.String)
	 */
	public void setPluginVersion(String version) throws CoreException {
		setXMLAttribute(P_PLUGIN_VERSION, version);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IFragment#setRule(int)
	 */
	public void setRule(int rule) throws CoreException {
		String match = "";
		switch (rule) {
			case IMatchRules.COMPATIBLE:
				match = "compatible";
				break;
			case IMatchRules.EQUIVALENT:
				match = "equivalent";
				break;
			case IMatchRules.PERFECT:
				match = "perfect";
				break;
			case IMatchRules.GREATER_OR_EQUAL:
				match = "greaterOrEqual";
		}
		setXMLAttribute(P_RULE, match);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginBaseNode#getSpecificAttributes()
	 */
	protected String[] getSpecificAttributes() {
		ArrayList result = new ArrayList();
		
		String pluginID = getPluginId();
		if (pluginID != null && pluginID.trim().length() > 0)
			result.add("   " + P_PLUGIN_ID + "=\"" + pluginID + "\"");
		
		String pluginVersion = getPluginVersion();
		if (pluginVersion != null && pluginVersion.trim().length() > 0) 
			result.add("   " + P_PLUGIN_VERSION + "=\"" + pluginVersion + "\"");
		
		String match = getXMLAttributeValue(P_RULE);
		if (match != null && match.trim().length() > 0)
			result.add("   " + P_RULE + "=\"" + match + "\"");
			
		return (String[]) result.toArray(new String[result.size()]);
	}
}
