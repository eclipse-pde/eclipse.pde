/*
 * Created on Nov 10, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.core.plugin.*;


/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public abstract class BaseEditorTemplate extends PDETemplateSection {
	private IPluginReference [] dep;
	
	public String getUsedExtensionPoint() {
		return "org.eclipse.ui.editors";
	}
	public IPluginReference[] getDependencies(String schemaVersion) {
		if (schemaVersion==null)
			return new IPluginReference[0];
		if (dep==null) {
			dep = new IPluginReference[4];
			dep[0] = new PluginReference("org.eclipse.ui.ide", null, 0);
			dep[1] = new PluginReference("org.eclipse.jface.text", null, 0);
			dep[2] = new PluginReference("org.eclipse.ui.workbench.texteditor", null, 0);
			dep[3] = new PluginReference("org.eclipse.ui.editors", null, 0);
		}
		return dep;
	}
}
