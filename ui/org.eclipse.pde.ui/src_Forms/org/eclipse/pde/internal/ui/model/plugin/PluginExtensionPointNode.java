package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginExtensionPointNode extends PluginObjectNode
		implements
			IPluginExtensionPoint {
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getFullId()
	 */
	public String getFullId() {
		return getPluginBase().getId() + "." + getId();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#getSchema()
	 */
	public String getSchema() {
		return getXMLAttributeValue("schema");
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtensionPoint#setSchema(java.lang.String)
	 */
	public void setSchema(String schema) throws CoreException {
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
