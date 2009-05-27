/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class InstallSection extends PDESection {
	private Button fExclusiveButton;

	private FormEntry fColocationText;

	private boolean fBlockNotification;

	public InstallSection(FeatureAdvancedPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEUIMessages.FeatureEditor_InstallSection_title);
		getSection().setDescription(PDEUIMessages.FeatureEditor_InstallSection_desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] {TextTransfer.getInstance(), RTFTransfer.getInstance()};
		for (int i = 0; i < types.length; i++) {
			for (int j = 0; j < transfers.length; j++) {
				if (transfers[j].isSupportedType(types[i]))
					return true;
			}
		}
		return false;
	}

	public void commit(boolean onSave) {
		fColocationText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		fExclusiveButton = toolkit.createButton(container, PDEUIMessages.FeatureEditor_InstallSection_exclusive, SWT.CHECK);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fExclusiveButton.setLayoutData(gd);
		fExclusiveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (!fBlockNotification)
						feature.setExclusive(fExclusiveButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		});

		Label colocationDescLabel = toolkit.createLabel(container, PDEUIMessages.FeatureEditor_InstallSection_colocation_desc, SWT.WRAP);
		gd = new GridData(GridData.FILL);
		gd.horizontalSpan = 2;
		gd.widthHint = 250;
		colocationDescLabel.setLayoutData(gd);

		fColocationText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_InstallSection_colocation, null, false);
		fColocationText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				IFeatureModel model = (IFeatureModel) getPage().getModel();
				IFeature feature = model.getFeature();
				try {
					feature.setColocationAffinity(fColocationText.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});

		toolkit.paintBordersFor(container);
		section.setClient(container);
		initialize();
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	public void initialize() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		refresh();
		if (model.isEditable() == false) {
			fColocationText.getText().setEditable(false);
			fExclusiveButton.setEnabled(false);
		}
		model.addModelChangedListener(this);
	}

	public void modelChanged(IModelChangedEvent e) {
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		}
	}

	public void setFocus() {
		if (fExclusiveButton != null)
			fExclusiveButton.setFocus();
	}

	public void refresh() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		IFeature feature = model.getFeature();
		fColocationText.setValue(feature.getColocationAffinity() != null ? feature.getColocationAffinity() : "", true); //$NON-NLS-1$
		fBlockNotification = true;
		fExclusiveButton.setSelection(feature.isExclusive());
		fBlockNotification = false;
		super.refresh();
	}

	public void cancelEdit() {
		fColocationText.cancelEdit();
		super.cancelEdit();
	}
}
