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
package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.isite.ISiteFeature;
import org.eclipse.pde.internal.core.isite.ISiteModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
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
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureDetailsSection extends PDESection implements IFormPart,
		IPartSelectionListener {

	private static final String PROPERTY_TYPE = "type"; //$NON-NLS-1$

	private static final String PROPERTY_URL = "url"; //$NON-NLS-1$

	private static final String SECTION_DESC = "FeatureDetailsSection.desc"; //$NON-NLS-1$

	private static final String SECTION_PATH = "FeatureDetailsSection.patch"; //$NON-NLS-1$

	private static final String SECTION_TITLE = "FeatureDetailsSection.title"; //$NON-NLS-1$

	private static final String SECTION_URL = "FeatureDetailsSection.url"; //$NON-NLS-1$

	private ISiteFeature currentSiteFeature;

	private Button patchCheckBox;

	private FormEntry urlText;

	public FeatureDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEPlugin.getResourceString(SECTION_TITLE),
				PDEPlugin.getResourceString(SECTION_DESC), SWT.NULL);
	}

	public FeatureDetailsSection(PDEFormPage page, Composite parent,
			String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private void applyIsPatch(boolean patch) throws CoreException {
		if (currentSiteFeature == null)
			return;
		currentSiteFeature.setIsPatch(patch);
	}

	private void applyValue(String property, String value) throws CoreException {
		if (currentSiteFeature == null)
			return;
		if (property.equals(PROPERTY_URL))
			currentSiteFeature.setURL(value);
		else if (property.equals(PROPERTY_TYPE))
			currentSiteFeature.setType(value);
	}

	public void cancelEdit() {
		urlText.cancelEdit();
		super.cancelEdit();
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

	private void clearField(String property) {
		if (property.equals(PROPERTY_URL))
			urlText.setValue(null, true);
	}

	private void clearFields() {
		urlText.setValue(null, true);
		patchCheckBox.setSelection(false);
	}

	public void commit(boolean onSave) {
		try {
			applyIsPatch(patchCheckBox.getSelection());
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		urlText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_URL), null, false);
		urlText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					if (text.getValue().length() <= 0) {
						setValue(PROPERTY_URL);
						String message = PDEPlugin
								.getResourceString("FeatureDetailsSection.requiredURL"); //$NON-NLS-1$
						MessageDialog
								.openError(
										PDEPlugin.getActiveWorkbenchShell(),
										PDEPlugin
												.getResourceString("FeatureDetailsSection.requiredURL.title"), //$NON-NLS-1$
										message);
					} else {
						applyValue(PROPERTY_URL, text.getValue());
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(urlText);
		urlText.getText().setEnabled(false);
		
		createPatchButton(toolkit, container);

		toolkit.paintBordersFor(container);
		section.setClient(container);

		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
	}

	private void createPatchButton(FormToolkit toolkit, Composite container) {
		patchCheckBox = toolkit.createButton(container, PDEPlugin
				.getResourceString(SECTION_PATH), SWT.CHECK);
		patchCheckBox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					applyIsPatch(patchCheckBox.getSelection());
				} catch (CoreException ce) {
					PDEPlugin.logException(ce);
				}
			}
		});
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		patchCheckBox.setLayoutData(gd);
		patchCheckBox.setEnabled(isEditable());
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
		if (currentSiteFeature == null) {
			clearFields();
			super.refresh();
			return;
		}
		setValue(PROPERTY_URL);
		setValue(PROPERTY_TYPE);
		patchCheckBox.setSelection(currentSiteFeature.isPatch());
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof SiteFeatureAdapter) {
				currentSiteFeature = ((SiteFeatureAdapter) o).feature;
			} else {
				currentSiteFeature = null;
			}
		} else
			currentSiteFeature = null;
		refresh();
	}

	public void setFocus() {
		if (urlText != null)
			urlText.getText().setFocus();
	}

	private void setValue(String property) {
		if (currentSiteFeature == null) {
			clearField(property);
		} else {
			if (property.equals(PROPERTY_URL))
				urlText.setValue(currentSiteFeature.getURL(), true);
		}
	}
}
