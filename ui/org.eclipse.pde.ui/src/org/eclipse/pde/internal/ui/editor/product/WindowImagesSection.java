package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;


public class WindowImagesSection extends PDESection {

	private FormEntry fImage16;
	private FormEntry fImage32;

	public WindowImagesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Window Images");
		section.setDescription("Specify the images that will be associated with the application window.  These images must be located in the product's defining plug-in.");

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImage16 = new FormEntry(client, toolkit, "16x16 Image:", "Browse...", isEditable());
		fImage16.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fImage16.setEditable(isEditable());
		
		fImage32 = new FormEntry(client, toolkit, "32x32 Image:", "Browse...", isEditable());
		fImage32.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fImage32.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}

}
