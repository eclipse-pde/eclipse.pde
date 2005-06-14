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

import org.eclipse.pde.internal.core.iproduct.IArgumentsInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class ArgumentsSection extends PDESection {

	private FormEntry fProgramArgs;
	private FormEntry fVMArgs;

	public ArgumentsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}
	
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.ArgumentsSection_title); //$NON-NLS-1$
		section.setDescription(PDEUIMessages.ArgumentsSection_desc); //$NON-NLS-1$
		section.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout());
		
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		
		fProgramArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_program, SWT.MULTI|SWT.WRAP); //$NON-NLS-1$
		fProgramArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));
		fProgramArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherArguments().setProgramArguments(entry.getValue());
			}
		});
		fProgramArgs.setEditable(isEditable());
		
		
		fVMArgs = new FormEntry(client, toolkit, PDEUIMessages.ArgumentsSection_vm, SWT.MULTI|SWT.WRAP); //$NON-NLS-1$
		fVMArgs.getText().setLayoutData(new GridData(GridData.FILL_BOTH));		
		fVMArgs.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLauncherArguments().setVMArguments(entry.getValue());
			}
		});
		fVMArgs.setEditable(isEditable());
		
		toolkit.paintBordersFor(client);
		section.setClient(client);	
	}
	
	public void refresh() {
		fProgramArgs.setValue(getLauncherArguments().getProgramArguments(), true);
		fVMArgs.setValue(getLauncherArguments().getVMArguments(), true);
		super.refresh();
	}
	
	public void commit(boolean onSave) {
		fProgramArgs.commit();
		fVMArgs.commit();
		super.commit(onSave);
	}
	
	public void cancelEdit() {
		fProgramArgs.cancelEdit();
		fVMArgs.cancelEdit();
		super.cancelEdit();
	}
	
	private IArgumentsInfo getLauncherArguments() {
		IArgumentsInfo info = getProduct().getLauncherArguments();
		if (info == null) {
			info = getModel().getFactory().createLauncherArguments();
			getProduct().setLauncherArguments(info);
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
