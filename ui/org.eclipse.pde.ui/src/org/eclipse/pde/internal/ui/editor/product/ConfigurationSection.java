package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;


public class ConfigurationSection extends PDESection {

	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public ConfigurationSection(PDEFormPage page, Composite parent, int style) {
		super(page, parent, style);
	}

	/**
	 * @param page
	 * @param parent
	 * @param style
	 * @param titleBar
	 */
	public ConfigurationSection(PDEFormPage page, Composite parent, int style,
			boolean titleBar) {
		super(page, parent, style, titleBar);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
	}

}
