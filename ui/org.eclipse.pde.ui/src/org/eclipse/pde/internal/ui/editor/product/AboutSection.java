package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;


public class AboutSection extends PDESection {

	private FormEntry fImageEntry;
	private FormEntry fTextEntry;

	public AboutSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("About Dialog");
		section.setDescription("Customize the image and text of the About dialog:");
		
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(3, false);
		layout.marginTop = 5;
		client.setLayout(layout);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImageEntry = new FormEntry(client, toolkit, "Image:", "Browse...", isEditable());
		fImageEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fImageEntry.setEditable(isEditable());
		
		fTextEntry = new FormEntry(client, toolkit, "Description:", SWT.MULTI|SWT.WRAP);
		fTextEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fTextEntry.getText().setLayoutData(gd);
		fTextEntry.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

}
