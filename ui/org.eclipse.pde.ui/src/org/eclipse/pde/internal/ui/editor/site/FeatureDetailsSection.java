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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureDetailsSection extends PDESection implements IFormPart, IPartSelectionListener {

	private static final String PROPERTY_TYPE = "type"; //$NON-NLS-1$

	private static final String PROPERTY_URL = "url"; //$NON-NLS-1$

	private ISiteFeature fCurrentSiteFeature;

	private Button fPatchCheckBox;

	private FormEntry fUrlText;

	public FeatureDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.FeatureDetailsSection_title, PDEUIMessages.FeatureDetailsSection_desc, SWT.NULL);
	}

	public FeatureDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyIsPatch(boolean patch) throws CoreException {
		if (fCurrentSiteFeature == null)
			return;
		fCurrentSiteFeature.setIsPatch(patch);
	}

	private void applyValue(String property, String value) throws CoreException {
		if (fCurrentSiteFeature == null)
			return;
		if (property.equals(PROPERTY_URL))
			fCurrentSiteFeature.setURL(value);
		else if (property.equals(PROPERTY_TYPE))
			fCurrentSiteFeature.setType(value);
	}

	public void cancelEdit() {
		fUrlText.cancelEdit();
		super.cancelEdit();
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

	private void clearField(String property) {
		if (property.equals(PROPERTY_URL))
			fUrlText.setValue(null, true);
	}

	private void clearFields() {
		fUrlText.setValue(null, true);
		fPatchCheckBox.setSelection(false);
	}

	public void commit(boolean onSave) {
		try {
			applyIsPatch(fPatchCheckBox.getSelection());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		section.setLayoutData(data);

		fUrlText = new FormEntry(container, toolkit, PDEUIMessages.FeatureDetailsSection_url, null, false);
		fUrlText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					if (text.getValue().length() <= 0) {
						setValue(PROPERTY_URL);
						MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.FeatureDetailsSection_requiredURL_title, PDEUIMessages.FeatureDetailsSection_requiredURL);
					} else {
						applyValue(PROPERTY_URL, text.getValue());
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fUrlText);
		fUrlText.getText().setEnabled(false);

		createPatchButton(toolkit, container);

		toolkit.paintBordersFor(container);
		section.setClient(container);

		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
	}

	private void createPatchButton(FormToolkit toolkit, Composite container) {
		fPatchCheckBox = toolkit.createButton(container, PDEUIMessages.FeatureDetailsSection_patch, SWT.CHECK);
		fPatchCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					applyIsPatch(fPatchCheckBox.getSelection());
				} catch (CoreException ce) {
					PDEPlugin.logException(ce);
				}
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fPatchCheckBox.setLayoutData(gd);
		fPatchCheckBox.setEnabled(isEditable());
	}

	public void dispose() {
		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	private void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	public void refresh() {
		if (fCurrentSiteFeature == null) {
			clearFields();
			super.refresh();
			return;
		}
		setValue(PROPERTY_URL);
		setValue(PROPERTY_TYPE);
		fPatchCheckBox.setSelection(fCurrentSiteFeature.isPatch());
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof SiteFeatureAdapter) {
				fCurrentSiteFeature = ((SiteFeatureAdapter) o).feature;
			} else {
				fCurrentSiteFeature = null;
			}
		} else
			fCurrentSiteFeature = null;
		refresh();
	}

	public void setFocus() {
		if (fUrlText != null)
			fUrlText.getText().setFocus();
	}

	private void setValue(String property) {
		if (fCurrentSiteFeature == null) {
			clearField(property);
		} else {
			if (property.equals(PROPERTY_URL))
				fUrlText.setValue(fCurrentSiteFeature.getURL(), true);
		}
	}
}
