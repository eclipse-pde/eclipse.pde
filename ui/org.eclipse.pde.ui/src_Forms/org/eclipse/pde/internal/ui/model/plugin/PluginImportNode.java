package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

public class PluginImportNode extends PluginObjectNode implements IPluginImport {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isReexported()
	 */
	public boolean isReexported() {
		String value = getXMLAttributeValue(P_REEXPORTED);
		return value != null && value.equals("true");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#isOptional()
	 */
	public boolean isOptional() {
		String value = getXMLAttributeValue(P_OPTIONAL);
		return value != null && value.equals("true");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setReexported(boolean)
	 */
	public void setReexported(boolean value) throws CoreException {
		setXMLAttribute(P_REEXPORTED, value ? "true" : "false");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginImport#setOptional(boolean)
	 */
	public void setOptional(boolean value) throws CoreException {
		setXMLAttribute(P_OPTIONAL, value ? "true" : "false");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#getMatch()
	 */
	public int getMatch() {
		String match = getXMLAttributeValue(P_MATCH);
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
		return getXMLAttributeValue(P_VERSION);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setMatch(int)
	 */
	public void setMatch(int match) throws CoreException {
		switch(match) {
			case IMatchRules.GREATER_OR_EQUAL:
				setXMLAttribute(P_MATCH, "greaterOrEquals");
				break;
			case IMatchRules.EQUIVALENT:
				setXMLAttribute(P_MATCH, "equivalent");
				break;
			case IMatchRules.COMPATIBLE:
				setXMLAttribute(P_MATCH, "compatible");
				break;
			case IMatchRules.PERFECT:
				setXMLAttribute(P_MATCH, "perfect");
				break;
			default:
				setXMLAttribute(P_MATCH, null);
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginReference#setVersion(java.lang.String)
	 */
	public void setVersion(String version) throws CoreException {
		setXMLAttribute(P_VERSION, version);
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
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#write()
	 */
	public String write(boolean indent) {
		return indent ? getIndent() + writeShallow(true) : writeShallow(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		StringBuffer buffer = new StringBuffer("<import");
		appendAttribute(buffer, "plugin");
		appendAttribute(buffer, P_VERSION);
		appendAttribute(buffer, P_MATCH);
		appendAttribute(buffer, P_REEXPORTED, "false");
		appendAttribute(buffer, P_OPTIONAL, "false");

		if (terminate)
			buffer.append("/");
		buffer.append(">");
		return buffer.toString();		
	}
}
