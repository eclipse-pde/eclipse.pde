package org.eclipse.pde.internal.ui.model.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;

/**
 * @author melhem
 *
 */
public class PluginElementNode extends PluginParentNode
		implements
			IPluginElement {
	private String fText;
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#createCopy()
	 */
	public IPluginElement createCopy() {
		return null;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttribute(java.lang.String)
	 */
	public IPluginAttribute getAttribute(String name) {
		return (IPluginAttribute)fAttributes.get(name);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributes()
	 */
	public IPluginAttribute[] getAttributes() {
		return (IPluginAttribute[])fAttributes.values().toArray(new IPluginAttribute[fAttributes.size()]);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getAttributeCount()
	 */
	public int getAttributeCount() {
		return fAttributes.size();
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#getText()
	 */
	public String getText() {
		return fText;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setAttribute(java.lang.String, java.lang.String)
	 */
	public void setAttribute(String name, String value) throws CoreException {
		setXMLAttribute(name, value);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.plugin.IPluginElement#setText(java.lang.String)
	 */
	public void setText(String text) throws CoreException {
		fText = text;
	}
}
