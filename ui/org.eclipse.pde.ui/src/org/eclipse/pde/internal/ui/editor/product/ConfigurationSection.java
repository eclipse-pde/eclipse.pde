package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.widgets.*;


public class ConfigurationSection extends PDESection {

	private Button fDefault;
	private Button fCustom;
	private FormEntry fBrowseEntry;

	public ConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText("Configuration File");
		section.setDescription("An Eclipse product can be configured by setting properties in a config.ini file.  These properties are read by the runtime upon startup.");
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		fDefault = toolkit.createButton(client, "Generate a default config.ini file", SWT.RADIO);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fDefault.setLayoutData(gd);
		fDefault.setEnabled(isEditable());
		
		fCustom = toolkit.createButton(client, "Use an existing config.ini file", SWT.RADIO);
		gd = new GridData();
		gd.horizontalSpan = 3;
		fCustom.setLayoutData(gd);
		fCustom.setEnabled(isEditable());
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fBrowseEntry = new FormEntry(client, toolkit, "File:", "Browse...", true, 35);
		fBrowseEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
			public void browseButtonSelected(FormEntry entry) {				
			}
		});
		fBrowseEntry.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}

}
