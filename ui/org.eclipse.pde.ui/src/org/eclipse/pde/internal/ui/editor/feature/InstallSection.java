/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.RTFTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class InstallSection extends PDESection {
	public static final String SECTION_TITLE = "FeatureEditor.InstallSection.title"; //$NON-NLS-1$

	public static final String SECTION_DESC = "FeatureEditor.InstallSection.desc"; //$NON-NLS-1$

	public static final String SECTION_COLOCATION_DESC = "FeatureEditor.InstallSection.colocation.desc"; //$NON-NLS-1$

	public static final String SECTION_EXCLUSIVE = "FeatureEditor.InstallSection.exclusive"; //$NON-NLS-1$

	public static final String SECTION_COLOCATION = "FeatureEditor.InstallSection.colocation"; //$NON-NLS-1$

	private Button fExclusiveButton;

	private FormEntry fColocationText;

	private boolean fBlockNotification;

	public InstallSection(FeatureAdvancedPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		getSection().setText(PDEPlugin.getResourceString(SECTION_TITLE));
		getSection().setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public boolean canPaste(Clipboard clipboard) {
		TransferData[] types = clipboard.getAvailableTypes();
		Transfer[] transfers = new Transfer[] { TextTransfer.getInstance(),
				RTFTransfer.getInstance() };
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
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		IFeatureModel model = (IFeatureModel) getPage().getModel();
		final IFeature feature = model.getFeature();

		fExclusiveButton = toolkit.createButton(container, PDEPlugin
				.getResourceString(SECTION_EXCLUSIVE), SWT.CHECK);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
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

		Label colocationDescLabel = toolkit.createLabel(container,
				PDEPlugin.getResourceString(SECTION_COLOCATION_DESC));
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		colocationDescLabel.setLayoutData(gd);

		fColocationText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_COLOCATION), null, false);
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
		fColocationText.setValue(
				feature.getColocationAffinity() != null ? feature
						.getColocationAffinity() : "", true); //$NON-NLS-1$
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
