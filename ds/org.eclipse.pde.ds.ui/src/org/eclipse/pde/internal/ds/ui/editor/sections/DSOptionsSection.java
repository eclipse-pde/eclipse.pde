/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <zx@code9.com>
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor.sections;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSImplementation;
import org.eclipse.pde.internal.ds.core.IDSModel;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ds.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ds.ui.parts.FormEntry;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DSOptionsSection extends PDESection {

	private IDSComponent fComponent;
	private IDSImplementation fImplementation;
	private FormEntry fFactoryEntry;
	private IDSModel fModel;
	private Button fImmediateButton;
	private Button fEnabledButton;

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
		if (fComponent != null) {
			fImplementation = fComponent.getImplementation();
		}
	}

	public void commit(boolean onSave) {
		fFactoryEntry.commit();
		super.commit(onSave);
	}

	public void modelChanged(IModelChangedEvent e) {
		fComponent = fModel.getDSComponent();
		if (fComponent != null)
			fImplementation = fComponent.getImplementation();

		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}

		Display.getDefault().syncExec(new Runnable() {
			public void run() {
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
		}


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

	}

}
