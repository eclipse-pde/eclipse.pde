package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.model.*;

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
		return getXMLAttributeValue(P_POINT);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginExtension#setPoint(java.lang.String)
	 */
	public void setPoint(String point) throws CoreException {
		setXMLAttribute(P_POINT, point);
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
		String sep = System.getProperty("line.separator");
		StringBuffer buffer = new StringBuffer();
		if (indent)
			buffer.append(getIndent());
		buffer.append(writeShallow(false));		
		IDocumentNode[] children = getChildNodes();
		for (int i = 0; i < children.length; i++) {
			children[i].setLineIndent(getLineIndent() + 3);
			children[i].write(true);
		}
		buffer.append(getIndent() + "</extension>" + sep);
		return buffer.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.model.plugin.PluginObjectNode#writeShallow(boolean)
	 */
	public String writeShallow(boolean terminate) {
		String sep = System.getProperty("line.separator");
		String attrIndent = "      ";
		StringBuffer buffer = new StringBuffer("<extension");
		IDocumentAttribute attr = getDocumentAttribute(P_ID);
		if (attr != null)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		attr = getDocumentAttribute(P_NAME);
		if (attr != null)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		attr = getDocumentAttribute(P_POINT);
		if (attr != null)
			buffer.append(sep + getIndent() + attrIndent + attr.write());
		if (terminate)
			buffer.append("/");
		buffer.append(">" + sep);
		return buffer.toString();
	}
}
