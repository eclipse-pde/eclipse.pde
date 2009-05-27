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
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class CategoryDetailsSection extends PDESection implements IFormPart, IPartSelectionListener {

	private static final String PROPERTY_DESC = "desc"; //$NON-NLS-1$

	private static final String PROPERTY_NAME = "url"; //$NON-NLS-1$

	private static final String PROPERTY_TYPE = "type"; //$NON-NLS-1$

	private ISiteCategoryDefinition fCurrentCategoryDefinition;

	private FormEntry fDescriptionText;

	private FormEntry fLabelText;

	private FormEntry fNameText;

	public CategoryDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.CategoryDetails_title, PDEUIMessages.CategoryDetails_sectionDescription, SWT.NULL);

	}

	public CategoryDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private boolean alreadyExists(String name) {
		ISiteCategoryDefinition[] defs = fCurrentCategoryDefinition.getModel().getSite().getCategoryDefinitions();
		for (int i = 0; i < defs.length; i++) {
			ISiteCategoryDefinition def = defs[i];
			if (def == fCurrentCategoryDefinition)
				continue;
			String dname = def.getName();
			if (dname != null && dname.equals(name))
				return true;
		}
		return false;
	}

	private void applyValue(String property, String value) throws CoreException {
		if (fCurrentCategoryDefinition == null)
			return;
		if (property.equals(PROPERTY_NAME)) {
			String oldName = fCurrentCategoryDefinition.getName();
			fCurrentCategoryDefinition.setName(value);
			bringFeatures(oldName);
		} else if (property.equals(PROPERTY_TYPE))
			fCurrentCategoryDefinition.setLabel(value);
		else if (property.equals(PROPERTY_DESC)) {
			if (value == null || value.length() == 0) {
				fCurrentCategoryDefinition.setDescription(null);
			} else {
				ISiteDescription siteDesc = fCurrentCategoryDefinition.getDescription();
				if (siteDesc == null) {
					siteDesc = fCurrentCategoryDefinition.getModel().getFactory().createDescription(fCurrentCategoryDefinition);
					siteDesc.setText(value);
					fCurrentCategoryDefinition.setDescription(siteDesc);
				} else {
					siteDesc.setText(value);
				}
			}
		}
	}

	public void cancelEdit() {
		fNameText.cancelEdit();
		fLabelText.cancelEdit();
		fDescriptionText.cancelEdit();
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
		if (property.equals(PROPERTY_NAME))
			fNameText.setValue(null, true);
		else if (property.equals(PROPERTY_TYPE))
			fLabelText.setValue(null, true);
		else if (property.equals(PROPERTY_DESC))
			fDescriptionText.setValue(null, true);
	}

	private void clearFields() {
		fNameText.setValue(null, true);
		fLabelText.setValue(null, true);
		fDescriptionText.setValue(null, true);
	}

	public void commit(boolean onSave) {
		fNameText.commit();
		fLabelText.commit();
		fDescriptionText.commit();

		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		fNameText = new FormEntry(container, toolkit, PDEUIMessages.CategoryDetails_name, null, false);
		fNameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					if (text.getValue().length() <= 0 || alreadyExists(text.getValue())) {
						setValue(PROPERTY_NAME);
						String message = PDEUIMessages.CategoryDetails_alreadyExists;
						MessageDialog.openError(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.CategoryDetails_alreadyExists_title, message);
					} else {
						applyValue(PROPERTY_NAME, text.getValue());
					}
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fNameText);
		fNameText.setEditable(isEditable());

		fLabelText = new FormEntry(container, toolkit, PDEUIMessages.CategoryDetails_label, null, false);
		fLabelText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(PROPERTY_TYPE, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fLabelText);
		fLabelText.setEditable(isEditable());

		fDescriptionText = new FormEntry(container, toolkit, PDEUIMessages.CategoryDetails_desc, SWT.WRAP | SWT.MULTI);
		fDescriptionText.getText().setLayoutData(new GridData(GridData.FILL_BOTH));

		fDescriptionText.getLabel().setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

		fDescriptionText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					applyValue(PROPERTY_DESC, text.getValue());
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		});
		limitTextWidth(fDescriptionText);
		fDescriptionText.setEditable(isEditable());
		toolkit.paintBordersFor(container);
		section.setClient(container);

		ISiteModel model = (ISiteModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
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

	private void bringFeatures(String oldCategory) {
		ISiteFeature[] siteFeatures = fCurrentCategoryDefinition.getModel().getSite().getFeatures();
		for (int i = 0; i < siteFeatures.length; i++) {
			ISiteCategory[] categories = siteFeatures[i].getCategories();
			for (int c = 0; c < categories.length; c++) {
				if (oldCategory.equals(categories[c].getName())) {
					try {
						categories[c].setName(fCurrentCategoryDefinition.getName());
					} catch (CoreException ce) {
					}
				}
			}
		}
	}

	public void refresh() {
		if (fCurrentCategoryDefinition == null) {
			clearFields();
			super.refresh();
			return;
		}
		setValue(PROPERTY_NAME);
		setValue(PROPERTY_TYPE);
		setValue(PROPERTY_DESC);
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof ISiteCategoryDefinition) {
				fCurrentCategoryDefinition = (ISiteCategoryDefinition) o;
			} else {
				fCurrentCategoryDefinition = null;
			}
		} else
			fCurrentCategoryDefinition = null;
		refresh();
	}

	public void setFocus() {
		if (fNameText != null)
			fNameText.getText().setFocus();
	}

	private void setValue(String property) {
		if (fCurrentCategoryDefinition == null) {
			clearField(property);
		} else {
			if (property.equals(PROPERTY_NAME))
				fNameText.setValue(fCurrentCategoryDefinition.getName(), true);
			else if (property.equals(PROPERTY_TYPE))
				fLabelText.setValue(fCurrentCategoryDefinition.getLabel(), true);
			else if (property.equals(PROPERTY_DESC)) {
				ISiteDescription siteDesc = fCurrentCategoryDefinition.getDescription();
				if (siteDesc == null) {
					clearField(property);
				} else {
					fDescriptionText.setValue(siteDesc.getText(), true);
				}

			}
		}
	}
}
