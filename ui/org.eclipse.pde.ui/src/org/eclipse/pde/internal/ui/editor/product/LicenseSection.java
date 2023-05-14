/*******************************************************************************
 * Copyright (c) 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.equinox.bidi.StructuredTextTypeHandlerFactory;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.util.BidiUtils;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.ILicenseInfo;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
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

	@Override
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
		BidiUtils.applyBidiProcessing(fURLEntry.getText(), StructuredTextTypeHandlerFactory.URL);
		fURLEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				getLicenseInfo().setURL(entry.getValue());
			}

		});
		fURLEntry.setEditable(isEditable());

		fLicenseEntry = new FormEntry(client, toolkit, PDEUIMessages.LicenseSection_text, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
		fLicenseEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
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

	@Override
	public void refresh() {
		ILicenseInfo info = getLicenseInfo();
		fURLEntry.setValue(info.getURL(), true);
		fLicenseEntry.setValue(info.getLicense(), true);
		super.refresh();
	}

	@Override
	public void commit(boolean onSave) {
		fURLEntry.commit();
		fLicenseEntry.commit();
		super.commit(onSave);
	}

	@Override
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

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	@Override
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

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

}
