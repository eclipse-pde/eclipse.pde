package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.model.*;


public class ConfigurationSection extends PDESection {

	private Button fDefault;
	private Button fCustom;
	private FormEntry fCustomEntry;
	
	public ConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEPlugin.getResourceString("ConfigurationSection.title")); //$NON-NLS-1$
		section.setDescription(PDEPlugin.getResourceString("ConfigurationSection.desc")); //$NON-NLS-1$
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		fDefault = toolkit.createButton(client, PDEPlugin.getResourceString("ConfigurationSection.default"), SWT.RADIO); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		fDefault.setLayoutData(gd);
		fDefault.setEnabled(isEditable());
		fDefault.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean selected = fDefault.getSelection();
				getConfigurationFileInfo().setUse(selected ? "default" : "custom"); //$NON-NLS-1$ //$NON-NLS-2$
				fCustomEntry.setEditable(!selected);
			}
		});
		
		fCustom = toolkit.createButton(client, PDEPlugin.getResourceString("ConfigurationSection.existing"), SWT.RADIO); //$NON-NLS-1$
		gd = new GridData();
		gd.horizontalSpan = 3;
		fCustom.setLayoutData(gd);
		fCustom.setEnabled(isEditable());
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fCustomEntry = new FormEntry(client, toolkit, PDEPlugin.getResourceString("ConfigurationSection.file"), PDEPlugin.getResourceString("ConfigurationSection.browse"), true, 35); //$NON-NLS-1$ //$NON-NLS-2$
		fCustomEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getConfigurationFileInfo().setPath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
		});
		fCustomEntry.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);
	}
	
	private void handleBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEPlugin.getResourceString("ConfigurationSection.selection"));  //$NON-NLS-1$
		dialog.setMessage(PDEPlugin.getResourceString("ConfigurationSection.message"));  //$NON-NLS-1$
		dialog.addFilter(new FileNameFilter("config.ini")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == ElementTreeSelectionDialog.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			fCustomEntry.setValue(file.getFullPath().toString());
		}
	}
	
	public void refresh() {
		IConfigurationFileInfo info = getConfigurationFileInfo();
		if (info == null) {
			fDefault.setSelection(true);
			fCustomEntry.setEditable(false);
		} else {
			boolean custom = "custom".equals(info.getUse()); //$NON-NLS-1$
			fDefault.setSelection(!custom);
			fCustom.setSelection(custom);
			fCustomEntry.setValue(info.getPath(), true);
			fCustomEntry.setEditable(custom);
		}
	}
	
	private IConfigurationFileInfo getConfigurationFileInfo() {
		IConfigurationFileInfo info = getProduct().getConfigurationFileInfo();
		if (info == null) {
			info = getModel().getFactory().createConfigFileInfo();
			getProduct().setConfigurationFileInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public void commit(boolean onSave) {
		fCustomEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fCustomEntry.cancelEdit();
		super.cancelEdit();
	}

}
