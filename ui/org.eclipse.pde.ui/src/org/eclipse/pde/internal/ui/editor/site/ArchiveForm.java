package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

/**
 * @author melhem
 *
 */
public class ArchiveForm extends ScrollableSectionForm {
	
	private PDEFormPage page;
	private DescriptionSection fDescSection;
	private ArchiveSection fArchiveSection;
	
	public ArchiveForm(PDEFormPage page) {
		this.page = page;
		setHeadingText("Description and Layout");
		setVerticalFit(true);
	}
	
	protected void createFormClient(Composite parent) {
		FormWidgetFactory factory = getFactory();
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 10;
		parent.setLayout(layout);
		
		fDescSection = new DescriptionSection(page);
		Control control = fDescSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		fArchiveSection = new ArchiveSection(page);
		control = fArchiveSection.createControl(parent, factory);
		control.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		registerSection(fDescSection);
		registerSection(fArchiveSection);
	}
	
	public void dispose() {
		unregisterSection(fDescSection);
		unregisterSection(fArchiveSection);
		super.dispose();
	}

}
