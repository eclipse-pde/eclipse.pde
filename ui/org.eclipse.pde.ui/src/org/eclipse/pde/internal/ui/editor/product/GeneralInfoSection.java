/*******************************************************************************
 * Copyright (c) 2009, 2015 EclipseSource Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation

 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.core.IModelChangedEvent;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.forms.IMessageManager;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class GeneralInfoSection extends PDESection {

	private FormEntry fIdEntry;
	private FormEntry fNameEntry;
	private FormEntry fVersionEntry;
	private Button fLaunchersButton;
	private Button fJreButton;

	private static int NUM_COLUMNS = 3;

	public GeneralInfoSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	@Override
	protected void createClient(Section section, FormToolkit toolkit) {
		section.setText(PDEUIMessages.GeneralInfoSection_title);
		section.setDescription(PDEUIMessages.GeneralInfoSection_desc);
		section.setLayout(FormLayoutFactory.createClearTableWrapLayout(false, 1));
		TableWrapData data = new TableWrapData(TableWrapData.FILL_GRAB);
		data.colspan = 2;
		section.setLayoutData(data);

		Composite client = toolkit.createComposite(section);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, NUM_COLUMNS));

		IActionBars actionBars = getPage().getPDEEditor().getEditorSite().getActionBars();

		// general section
		createIdEntry(client, toolkit, actionBars);
		createVersionEntry(client, toolkit, actionBars);
		createNameEntry(client, toolkit, actionBars);
		createLaunchersOption(client, toolkit);

		toolkit.paintBordersFor(client);
		section.setClient(client);

		getModel().addModelChangedListener(this);
	}

	@Override
	public void dispose() {
		IProductModel model = getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	private void createNameEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fNameEntry = new FormEntry(client, toolkit, PDEUIMessages.ProductInfoSection_productname, null, false);
		fNameEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				getProduct().setName(entry.getValue().trim());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		fNameEntry.getText().setLayoutData(gd);
		fNameEntry.setEditable(isEditable());
	}

	private void createIdEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fIdEntry = new FormEntry(client, toolkit, PDEUIMessages.ProductInfoSection_id, null, false);
		fIdEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				getProduct().setId(entry.getValue().trim());
				validateProductId();
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		fIdEntry.getText().setLayoutData(gd);
		fIdEntry.setEditable(isEditable());
		validateProductId();
	}

	private void validateProductId() {
		String pluginId = getProduct().getDefiningPluginId();
		IMessageManager messageManager = getManagedForm().getForm().getMessageManager();
		if (pluginId != null && pluginId.equals(getProduct().getId())) {
			messageManager.addMessage(PDEUIMessages.GeneralInfoSection_IdWarning, PDEUIMessages.GeneralInfoSection_IdWarning, null, IMessageProvider.WARNING);
		} else {
			messageManager.removeMessage(PDEUIMessages.GeneralInfoSection_IdWarning);
		}
	}

	private void createVersionEntry(Composite client, FormToolkit toolkit, IActionBars actionBars) {
		fVersionEntry = new FormEntry(client, toolkit, PDEUIMessages.ProductInfoSection_version, null, false);
		fVersionEntry.setFormEntryListener(new FormEntryAdapter(this, actionBars) {
			@Override
			public void textValueChanged(FormEntry entry) {
				getProduct().setVersion(entry.getValue().trim());
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalSpan = 2;
		fVersionEntry.getText().setLayoutData(gd);
		fVersionEntry.setEditable(isEditable());
	}

	private void createLaunchersOption(Composite client, FormToolkit toolkit) {
		// includes launcher
		fLaunchersButton = toolkit.createButton(client, PDEUIMessages.ProductInfoSection_launchers, SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fLaunchersButton.setLayoutData(data);
		fLaunchersButton.setEnabled(isEditable());
		fLaunchersButton.addSelectionListener(widgetSelectedAdapter(e -> getProduct().setIncludeLaunchers(fLaunchersButton.getSelection())));
		// includes JRE
		fJreButton = toolkit.createButton(client, PDEUIMessages.ProductInfoSection_jre, SWT.CHECK);
		fJreButton.setLayoutData(data);
		fJreButton.setEnabled(isEditable());
		fJreButton.addSelectionListener(
				widgetSelectedAdapter(e -> getProduct().setIncludeJre(fJreButton.getSelection())));

	}

	@Override
	public void commit(boolean onSave) {
		fIdEntry.commit();
		fNameEntry.commit();
		fVersionEntry.commit();
		super.commit(onSave);
	}

	@Override
	public void cancelEdit() {
		fIdEntry.cancelEdit();
		fNameEntry.cancelEdit();
		fVersionEntry.cancelEdit();
		super.cancelEdit();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	@Override
	public void refresh() {
		IProduct product = getProduct();
		if (product.getId() != null) {
			fIdEntry.setValue(product.getId(), true);
		}
		if (product.getName() != null) {
			fNameEntry.setValue(product.getName(), true);
		}
		if (product.getVersion() != null) {
			fVersionEntry.setValue(product.getVersion(), true);
		}
		fLaunchersButton.setSelection(product.includeLaunchers());
		fJreButton.setSelection(product.includeJre());
		super.refresh();
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
			return;
		}

		String prop = e.getChangedProperty();
		Object[] objects = e.getChangedObjects();
		if (prop == null || objects == null || !(objects[0] instanceof IProduct))
			return;
		if (prop.equals(IProduct.P_UID)) {
			fIdEntry.setValue(e.getNewValue().toString(), true);
		} else if (prop.equals(IProduct.P_NAME)) {
			fNameEntry.setValue(e.getNewValue().toString(), true);
		} else if (prop.equals(IProduct.P_VERSION)) {
			fVersionEntry.setValue(e.getNewValue().toString(), true);
		} else if (prop.equals(IProduct.P_INCLUDE_LAUNCHERS)) {
			fLaunchersButton.setSelection(Boolean.parseBoolean(e.getNewValue().toString()));
		}
	}

	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
		// Note:  A deferred selection event is fired from radio buttons when
		// their value is toggled, the user switches to another page, and the
		// user switches back to the same page containing the radio buttons
		// This appears to be a result of a SWT bug.
		// If the radio button is the last widget to have focus when leaving
		// the page, an event will be fired when entering the page again.
		// An event is not fired if the radio button does not have focus.
		// The solution is to redirect focus to a stable widget.
		getPage().setLastFocusControl(fIdEntry.getText());
	}

	@Override
	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

}
