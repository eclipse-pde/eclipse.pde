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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.*;
/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class GeneralInfoSection extends PDESection {
	private Button olderVersions;
	private FormEntry idEntry;
	private FormEntry versionEntry;
	private FormEntry nameEntry;
	private FormEntry providerEntry;
	private FormEntry classEntry;
	/**
	 * @param page
	 * @param parent
	 * @param style
	 */
	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION | Section.EXPANDED
				| Section.TWISTIE);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.neweditor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section,
	 *      org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin
				.getResourceString("ManifestEditor.PluginSpecSection.title"));
		toolkit.createCompositeSeparator(section);
		section.setDescription(PDEPlugin
				.getResourceString("ManifestEditor.PluginSpecSection.desc"));
		createEntries(section, toolkit);
	}
	private void createEntries(Section section, FormToolkit toolkit) {
		Composite client = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		client.setLayout(layout);
		section.setClient(client);
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite()
				.getActionBars();
		idEntry = new FormEntry(client, toolkit, "Id:", null, false);
		idEntry
				.setFormEntryListener(new FormEntryAdapter(getForm(),
						actionBars) {
					public void textValueChanged(FormEntry entry) {
					}
				});
		versionEntry = new FormEntry(client, toolkit, "Version:", null, false);
		versionEntry.setFormEntryListener(new FormEntryAdapter(getForm(),
				actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		nameEntry = new FormEntry(client, toolkit, "Name:", null, false);
		nameEntry.setFormEntryListener(new FormEntryAdapter(getForm(),
				actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		providerEntry = new FormEntry(client, toolkit, "Provider:", null, false);
		providerEntry.setFormEntryListener(new FormEntryAdapter(getForm(),
				actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		classEntry = new FormEntry(client, toolkit, "Class:", "Browse...", true);
		classEntry.setFormEntryListener(new FormEntryAdapter(getForm(),
				actionBars) {
			public void textValueChanged(FormEntry entry) {
			}
		});
		olderVersions = toolkit
				.createButton(
						client,
						PDEPlugin
								.getFormattedMessage(
										"ManifestEditor.PluginSpecSection.isCompatible",
										isFragment()
												? PDEPlugin
														.getResourceString("ManifestEditor.PluginSpecSection.fragment")
												: PDEPlugin
														.getResourceString("ManifestEditor.PluginSpecSection.plugin")),
						SWT.CHECK);
		olderVersions.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				markDirty();
			}
		});
		GridData gd = new GridData();
		gd.horizontalSpan = layout.numColumns;
		olderVersions.setLayoutData(gd);
	}
	private boolean isFragment() {
		//TODO replace this with a real code
		return false;
	}
	public void commit(boolean onSave) {
		idEntry.commit();
		nameEntry.commit();
		providerEntry.commit();
		classEntry.commit();
		//olderVersions.getSelection();
		super.commit(onSave);
	}
	public boolean isDirty() {
		return idEntry.isDirty() || nameEntry.isDirty()
				|| providerEntry.isDirty() || classEntry.isDirty();
	}
	public void refresh() {
		idEntry.setValue("com.example.xyz", true);
		nameEntry.setValue("XYZ Plug-in", true);
		providerEntry.setValue("XYZ Inc.", true);
		classEntry.setValue("com.example.xyz.XYZPlugin", true);
		super.refresh();
	}
}