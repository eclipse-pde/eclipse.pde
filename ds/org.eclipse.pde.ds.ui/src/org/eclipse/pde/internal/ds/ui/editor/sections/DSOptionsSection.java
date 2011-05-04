/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.core.IDSProvide;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.parts.ComboPart;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import org.eclipse.jface.layout.GridDataFactory;

import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSOptionsSection extends PDESection {

	private IDSComponent fComponent;
	private FormEntry fFactoryEntry;
	private IDSModel fModel;
	private Button fImmediateButton;
	private Button fEnabledButton;
	private ComboPart fConfigurationPolicy;

	public DSOptionsSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		createClient(getSection(), page.getEditor().getToolkit());
	}

	protected void createClient(Section section, FormToolkit toolkit) {

		initializeAttributes();

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		section.setLayoutData(data);
		section.setText(Messages.DSOptionsSection_title);
		section.setDescription(Messages.DSOptionsSection_description);

		Composite client = toolkit.createComposite(section);
		client.setLayout(new GridLayout(3, false));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		// Attribute: factory
		fFactoryEntry = new FormEntry(client, toolkit,
				Messages.DSComponentDetails_factoryEntry, SWT.NONE);

		// Attribute: configuration policy
		Label label = toolkit.createLabel(client,
				Messages.DSComponentDetails_configurationPolicy,
				SWT.WRAP);
		label.setForeground(toolkit.getColors().getColor(
IFormColors.TITLE));
		fConfigurationPolicy = new ComboPart();
		fConfigurationPolicy.createControl(client, toolkit, SWT.READ_ONLY);

		String[] items = new String[] {
				"", //$NON-NLS-1$
				IDSConstants.VALUE_CONFIGURATION_POLICY_OPTIONAL,
				IDSConstants.VALUE_CONFIGURATION_POLICY_REQUIRE,
				IDSConstants.VALUE_CONFIGURATION_POLICY_IGNORE };
		fConfigurationPolicy.setItems(items);
		GridDataFactory.fillDefaults().grab(true, false).indent(3, 0).applyTo(
				fConfigurationPolicy.getControl());

		createButtons(client, toolkit);

		setListeners();
		updateUIFields();

		toolkit.paintBordersFor(client);
		section.setClient(client);

	}

	private void createButtons(Composite parent, FormToolkit toolkit) {
		fEnabledButton = toolkit.createButton(parent,
				Messages.DSServiceComponentSection_enabledButtonMessage,
				SWT.CHECK);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		fEnabledButton.setLayoutData(data);
		fEnabledButton.setEnabled(isEditable());
		fEnabledButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fModel.getDSComponent().setEnabled(
						fEnabledButton.getSelection());
			}
		});

		fImmediateButton = toolkit.createButton(parent,
				Messages.DSServiceComponentSection_immediateButtonMessage,
				SWT.CHECK);
		fImmediateButton.setLayoutData(data);
		fImmediateButton.setEnabled(isEditable());
		fImmediateButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fModel.getDSComponent().setImmediate(
						fImmediateButton.getSelection());
			}
		});
	}

	private void initializeAttributes() {
		fModel = (IDSModel) getPage().getModel();
		fModel.addModelChangedListener(this);
		fComponent = fModel.getDSComponent();
	}

	public void commit(boolean onSave) {
		fFactoryEntry.commit();
		super.commit(onSave);
	}

	public void modelChanged(IModelChangedEvent e) {
		fComponent = fModel.getDSComponent();

		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}

		Display display= fEnabledButton.getDisplay();
		if (display.getThread() == Thread.currentThread())
			updateUIFields();
		else
			display.asyncExec(new Runnable() {
				public void run() {
					if (!fEnabledButton.isDisposed())
						updateUIFields();
				}
			});
	}

	public void updateUIFields() {
		if (fComponent != null) {
			if (fComponent.getFactory() == null) {
				// Attribute: factory
				fFactoryEntry.setValue("", true); //$NON-NLS-1$
			} else {
				// Attribute: factory
				fFactoryEntry.setValue(fComponent.getFactory(), true);
			}

			fEnabledButton.setSelection(fComponent.getEnabled());
			fImmediateButton.setSelection(fComponent.getImmediate());
			enableOrDisableImmediate();
			
			// Attribute: Policy
			if (fComponent.getConfigurationPolicy() != null)
				fConfigurationPolicy.setText(fComponent
						.getConfigurationPolicy());
		}
	}

	private void enableOrDisableImmediate() {
		boolean isService = false;
		boolean isFactory = fComponent.getFactory() != null
				&& !fComponent.getFactory().equals(""); //$NON-NLS-1$
		boolean isImmediate = fComponent.getImmediate();
		boolean enabled = true;

		if (fComponent.getService() != null) {
			IDSProvide[] providedServices = fComponent.getService()
					.getProvidedServices();
			if (providedServices != null && providedServices.length > 0) {
				isService = true;
			}
		}
		if (!isService && !isFactory && !isImmediate) {
			enabled = false;
		}

		fImmediateButton.setEnabled(enabled);
	}

	public void setListeners() {
		// Attribute: factory
		fFactoryEntry.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry entry) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setFactory(fFactoryEntry.getValue());
			}
		});

		fConfigurationPolicy.addModifyListener(new ModifyListener(){
		
			public void modifyText(ModifyEvent e) {
				// Ensure data object is defined
				if (fComponent == null) {
					return;
				}
				fComponent.setConfigurationPolicy(fConfigurationPolicy
						.getSelection());
			}
		});
		
	}

}
