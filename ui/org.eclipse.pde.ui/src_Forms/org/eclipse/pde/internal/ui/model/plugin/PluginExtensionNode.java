package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginExtensionNode extends PluginParentNode
		implements
			IPluginExtension {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#getPoint()
	 */
	public String getPoint() {
		return getXMLAttributeValue("point");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#setPoint(java.lang.String)
	 */
	public void setPoint(String point) throws CoreException {
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue("id");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
	}
}
