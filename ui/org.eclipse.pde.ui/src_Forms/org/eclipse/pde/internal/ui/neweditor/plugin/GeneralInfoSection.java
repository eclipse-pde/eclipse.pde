/*
 * Created on Feb 26, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.plugin;

import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.newparts.FormEntry;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class GeneralInfoSection extends PDESection {
	private FormEntry idEntry;
	private FormEntry nameEntry;
	private FormEntry providerEntry;
	private FormEntry classEntry;
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
		createEntries(section, toolkit);
	}
	private void createEntries(Section section, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		client.setLayout(layout);
		section.setClient(client);
		
		idEntry = new FormEntry(client, toolkit, "Id:", null, false);
		idEntry.setFormEntryListener(new EditorEntryAdapter(getPage().getPDEEditor()) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		nameEntry = new FormEntry(client, toolkit, "Name:", null, false);
		nameEntry.setFormEntryListener(new EditorEntryAdapter(getPage().getPDEEditor()) {
			public void textValueChanged(FormEntry entry) {
			}
		});		
		providerEntry = new FormEntry(client, toolkit, "Provider:", null, false);
		providerEntry.setFormEntryListener(new EditorEntryAdapter(getPage().getPDEEditor()) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		classEntry = new FormEntry(client, toolkit, "Class:", "Browse...", true);
		classEntry.setFormEntryListener(new EditorEntryAdapter(getPage().getPDEEditor()) {
			public void textValueChanged(FormEntry entry) {
			}
		});
	}
	public void refresh() {
		idEntry.setValue("com.example.xyz", true);
		nameEntry.setValue("XYZ Plug-in", true);
		providerEntry.setValue("XYZ Inc.", true);
		classEntry.setValue("com.example.xyz.XYZPlugin", true);
	}
}