/*
 * Created on Feb 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class GeneralInfoSection extends PDESection {
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.EXPANDED|Section.TWISTIE);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("ManifestEditor.PluginSpecSection.title"));
		toolkit.createCompositeSeparator(section);
		section.setDescription(PDEPlugin.getResourceString("ManifestEditor.PluginSpecSection.desc"));
	}
}
