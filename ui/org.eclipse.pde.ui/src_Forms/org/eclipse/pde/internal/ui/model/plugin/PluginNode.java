package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginNode extends PluginBaseNode implements IPlugin {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#getClassName()
	 */
	public String getClassName() {
		return getXMLAttributeValue(P_CLASS_NAME);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
		setXMLAttribute(P_CLASS_NAME, className);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginBaseNode#getSpecificAttributes()
	 */
	protected String getSpecificAttributes() {
		String classname = getClassName();
		if (classname != null && classname.length() > 0)
			return "\tclass=\"" + classname + "\"";
		return "";
	}
	
}
