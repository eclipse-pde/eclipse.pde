package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;


public class SplashSection extends PDESection {

	private FormEntry fPluginEntry;

	public SplashSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION|Section.TWISTIE|Section.EXPANDED);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Splash Screen");
		section.setDescription("The splash screen appears when the product launches.  It must be named 'splash.bmp' and is typically located in the product's defining plug-in.");

		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout(3, false);
		layout.marginTop = 5;
		client.setLayout(layout);
		
		Label label = toolkit.createLabel(client, "Specify the plug-in in which the splash screen is located:");
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		label.setLayoutData(gd);
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fPluginEntry = new FormEntry(client, toolkit, "Plug-in:", "Browse...", false);
		fPluginEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fPluginEntry.setEditable(isEditable());
				
		toolkit.paintBordersFor(client);
		section.setClient(client);
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
	}

}
