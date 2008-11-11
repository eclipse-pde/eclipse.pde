/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class LicenseSection extends PDESection {

	private FormEntry fURLEntry;
	private FormEntry fLicenseEntry;

	public LicenseSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {

		// Configure section
		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);
		section.setText(PDEUIMessages.LicenseSection_title);
		section.setDescription(PDEUIMessages.LicenseSection_description);
		// Create and configure client
		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_BOTH));
		// Create form entry
		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();
		fURLEntry = new FormEntry(client, toolkit, PDEUIMessages.LicenseSection_url, SWT.NONE);
		fURLEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLicenseInfo().setURL(entry.getValue());
			}

		});
		fURLEntry.setEditable(isEditable());

		fLicenseEntry = new FormEntry(client, toolkit, PDEUIMessages.LicenseSection_text, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		fLicenseEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			public void textValueChanged(FormEntry entry) {
				getLicenseInfo().setLicense(entry.getValue());
			}

		});
		fLicenseEntry.setEditable(isEditable());
		GridDataFactory.fillDefaults().grab(true, true).indent(FormLayoutFactory.CONTROL_HORIZONTAL_INDENT, 0).applyTo(fLicenseEntry.getText());
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).grab(false, false).applyTo(fLicenseEntry.getLabel());

		toolkit.paintBordersFor(client);
		section.setClient(client);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ILicenseInfo info = getLicenseInfo();
		fURLEntry.setValue(info.getURL(), true);
		fLicenseEntry.setValue(info.getLicense(), true);
		super.refresh();
	}

	public void commit(boolean onSave) {
		fURLEntry.commit();
		fLicenseEntry.commit();
		super.commit(onSave);
	}

	public void cancelEdit() {
		fURLEntry.cancelEdit();
		fLicenseEntry.cancelEdit();
		super.cancelEdit();
	}

	private ILicenseInfo getLicenseInfo() {
		ILicenseInfo info = getProduct().getLicenseInfo();
		if (info == null) {
			info = getModel().getFactory().createLicenseInfo();
			getProduct().setLicenseInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		}
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

}
