package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;


public class LauncherSection extends PDESection {

	public LauncherSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Program Executable");
		section.setDescription("Customize the executable that is used to launch the product:");

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.verticalSpan = 3;
		section.setLayoutData(gd);
	}

}
