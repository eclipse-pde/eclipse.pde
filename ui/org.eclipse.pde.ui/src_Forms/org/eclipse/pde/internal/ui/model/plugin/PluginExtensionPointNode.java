package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

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
		setXMLAttribute(P_SCHEMA, schema);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#getId()
	 */
	public String getId() {
		return getXMLAttributeValue(P_ID);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IIdentifiable#setId(java.lang.String)
	 */
	public void setId(String id) throws CoreException {
		setXMLAttribute(P_ID, id);
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
		String sep = System.getProperty("line.separator");
		StringBuffer buffer = new StringBuffer("<extension-point");
		IDocumentAttribute attr = getDocumentAttribute(P_ID);
		if (attr != null)
			buffer.append(" " + attr.write());
		attr = getDocumentAttribute(P_NAME);
		if (attr != null)
			buffer.append(" " + attr.write());
		attr = getDocumentAttribute(P_SCHEMA);
		if (attr != null)
			buffer.append(" " + attr.write());
		
		if (terminate)
			buffer.append("/");
		buffer.append(">" + sep);
		return buffer.toString();
	}
	
	
}
