package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginImportNode extends PluginObjectNode implements IPluginImport {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isReexported()
	 */
	public boolean isReexported() {
		String value = getXMLAttributeValue("export");
		return value != null && value.equals("true");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isOptional()
	 */
	public boolean isOptional() {
		String value = getXMLAttributeValue("optional");
		return value != null && value.equals("true");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setReexported(boolean)
	 */
	public void setReexported(boolean value) throws CoreException {
		setXMLAttribute("export", value ? "true" : "false");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setOptional(boolean)
	 */
	public void setOptional(boolean value) throws CoreException {
		setXMLAttribute("optional", value ? "true" : "false");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#getMatch()
	 */
	public int getMatch() {
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
	 * @see org.eclipse.pde.core.plugin.IPluginReference#getVersion()
	 */
	public String getVersion() {
		return getXMLAttributeValue("version");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setMatch(int)
	 */
	public void setMatch(int match) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		setXMLAttribute("version", version);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue("plugin");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute("plugin", id);
	}
}
