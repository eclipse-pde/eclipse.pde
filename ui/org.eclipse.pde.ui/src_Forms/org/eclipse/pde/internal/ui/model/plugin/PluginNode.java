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
		return getXMLAttributeValue("class");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPlugin#setClassName(java.lang.String)
	 */
	public void setClassName(String className) throws CoreException {
	}
}
