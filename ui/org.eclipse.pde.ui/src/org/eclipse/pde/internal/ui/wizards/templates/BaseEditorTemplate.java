/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.wizards.templates;


/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class BaseEditorTemplate extends PDETemplateSection {
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.ui.templates.ITemplateSection#getFoldersToInclude()
	 */
	public String[] getFoldersToInclude() {
		return new String[] {"icons/"};
	}
}
