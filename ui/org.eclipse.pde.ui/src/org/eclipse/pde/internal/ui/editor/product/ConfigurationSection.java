/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.iproduct.IConfigurationFileInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileNameFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;


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
		section.setText(PDEUIMessages.ConfigurationSection_title); 
		section.setDescription(PDEUIMessages.ConfigurationSection_desc); 
		section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		
		fDefault = toolkit.createButton(client, PDEUIMessages.ConfigurationSection_default, SWT.RADIO); 
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
		
		fCustom = toolkit.createButton(client, PDEUIMessages.ConfigurationSection_existing, SWT.RADIO); 
		gd = new GridData();
		gd.horizontalSpan = 3;
		fCustom.setLayoutData(gd);
		fCustom.setEnabled(isEditable());
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fCustomEntry = new FormEntry(client, toolkit, PDEUIMessages.ConfigurationSection_file, PDEUIMessages.ConfigurationSection_browse, isEditable(), 35); // 
		fCustomEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getConfigurationFileInfo().setPath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
			public void linkActivated(HyperlinkEvent e) {
				handleOpen();
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
		dialog.setTitle(PDEUIMessages.ConfigurationSection_selection);  
		dialog.setMessage(PDEUIMessages.ConfigurationSection_message);  
		dialog.addFilter(new FileNameFilter("config.ini")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
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
		super.refresh();
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
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	private void handleOpen() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		Path path = new Path(fCustomEntry.getValue());
		if(path.isEmpty()){
			MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_emptyPath); // 
			return;
		}
		IResource resource = root.findMember(path);
		try {
			if (resource != null && resource instanceof IFile)
				IDE.openEditor(PDEPlugin.getActivePage(), (IFile)resource, true);
			else
				MessageDialog.openWarning(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.WindowImagesSection_open, PDEUIMessages.WindowImagesSection_warning); // 
		} catch (PartInitException e) {
		}			
	}


}
