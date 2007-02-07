/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.iproduct.IAboutInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.EditorUtilities;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.editor.validation.TextValidator;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.util.FileExtensionFilter;
import org.eclipse.pde.internal.ui.util.FileValidator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class AboutSection extends PDESection {

	private FormEntry fImageEntry;
	private FormEntry fTextEntry;
	
	private TextValidator fImageEntryValidator;

	public AboutSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		data.verticalSpan = 2;
		section.setLayoutData(data);			
		
		section.setText(PDEUIMessages.AboutSection_title); 
		section.setDescription(PDEUIMessages.AboutSection_desc); 
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 3));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fImageEntry = new FormEntry(client, toolkit, PDEUIMessages.AboutSection_image, PDEUIMessages.AboutSection_browse, isEditable());
		fImageEntry.setEditable(isEditable());
		// Create validator
		fImageEntryValidator = new TextValidator(
				getManagedForm(), fImageEntry.getText(), getProject(), true) {
			protected boolean validateControl() {
				return validateImageEntry();
			}
		};
		fImageEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getAboutInfo().setImagePath(entry.getValue());
			}
			public void browseButtonSelected(FormEntry entry) {
				handleBrowse();
			}
			public void linkActivated(HyperlinkEvent e) {
				EditorUtilities.openImage(fImageEntry.getValue(), getProduct().getDefiningPluginId());
			}
		});
		
		fTextEntry = new FormEntry(client, toolkit, PDEUIMessages.AboutSection_text, SWT.MULTI|SWT.WRAP); 
		fTextEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getAboutInfo().setText(entry.getValue());
			}
		});

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		fTextEntry.getText().setLayoutData(gd);
		fTextEntry.setEditable(isEditable());
		gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END);
		fTextEntry.getLabel().setLayoutData(gd);
		
		toolkit.paintBordersFor(client);
		section.setClient(client);

	}
	
	/**
	 * @return
	 */
	private boolean validateImageEntry() {
		return EditorUtilities.imageEntrySizeDoesNotExceed(
				fImageEntryValidator, fImageEntry, getProduct(),
				500, 330, 250, 330);
	}
	
	private void handleBrowse() {
		ElementTreeSelectionDialog dialog =
			new ElementTreeSelectionDialog(
				getSection().getShell(),
				new WorkbenchLabelProvider(),
				new WorkbenchContentProvider());
				
		dialog.setValidator(new FileValidator());
		dialog.setAllowMultiple(false);
		dialog.setTitle(PDEUIMessages.AboutSection_imgTitle);  
		dialog.setMessage(PDEUIMessages.AboutSection_imgMessage);  
		dialog.addFilter(new FileExtensionFilter("gif")); //$NON-NLS-1$
		dialog.setInput(PDEPlugin.getWorkspace().getRoot());

		if (dialog.open() == Window.OK) {
			IFile file = (IFile)dialog.getFirstResult();
			fImageEntry.setValue(file.getFullPath().toString());
		}
	}
	
	public void refresh() {
		fImageEntry.setValue(getAboutInfo().getImagePath(), true);
		fTextEntry.setValue(getAboutInfo().getText(), true);
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fImageEntry.commit();
		fTextEntry.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fImageEntry.cancelEdit();
		fTextEntry.cancelEdit();
		super.cancelEdit();
	}
	
	private IAboutInfo getAboutInfo() {
		IAboutInfo info = getProduct().getAboutInfo();
		if (info == null) {
			info = getModel().getFactory().createAboutInfo();
			getProduct().setAboutInfo(info);
		}
		return info;
	}
	
	private IProduct getProduct() {
		return getModel().getProduct();
	}
	
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}
	
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}
}
