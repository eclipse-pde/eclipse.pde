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
import org.eclipse.pde.internal.core.isite.ISiteCategory;
import org.eclipse.pde.internal.core.isite.ISiteCategoryDefinition;
import org.eclipse.pde.internal.core.isite.ISiteDescription;
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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class CategoryDetailsSection extends PDESection implements IFormPart,
		IPartSelectionListener {

	private static final String PROPERTY_DESC = "desc"; //$NON-NLS-1$

	private static final String PROPERTY_NAME = "url"; //$NON-NLS-1$

	private static final String PROPERTY_TYPE = "type"; //$NON-NLS-1$

	public static final String SECTION_DESC = "CategoryDetails.desc"; //$NON-NLS-1$

	public static final String SECTION_LABEL = "CategoryDetails.label"; //$NON-NLS-1$

	public static final String SECTION_NAME = "CategoryDetails.name"; //$NON-NLS-1$

	public static final String SECTION_SECT_DESC = "CategoryDetails.sectionDescription"; //$NON-NLS-1$

	public static final String SECTION_TITLE = "CategoryDetails.title"; //$NON-NLS-1$

	private ISiteCategoryDefinition fCurrentCategoryDefinition;

	private FormEntry fDescriptionText;

	private FormEntry fLabelText;

	private FormEntry fNameText;

	public CategoryDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEPlugin.getResourceString(SECTION_TITLE),
				PDEPlugin.getResourceString(SECTION_SECT_DESC), SWT.NULL);
		getSection().setLayoutData(new GridData(GridData.FILL_BOTH));

	}

	public CategoryDetailsSection(PDEFormPage page, Composite parent,
			String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	private boolean alreadyExists(String name) {
		ISiteCategoryDefinition[] defs = fCurrentCategoryDefinition.getModel()
				.getSite().getCategoryDefinitions();
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
		if (property.equals(PROPERTY_NAME)){
			String oldName = fCurrentCategoryDefinition.getName();
			fCurrentCategoryDefinition.setName(value);
			bringFeatures(oldName);
		} else if (property.equals(PROPERTY_TYPE))
			fCurrentCategoryDefinition.setLabel(value);
		else if (property.equals(PROPERTY_DESC)) {
			if (value == null || value.length() == 0) {
				fCurrentCategoryDefinition.setDescription(null);
			} else {
				ISiteDescription siteDesc = fCurrentCategoryDefinition
						.getDescription();
				if (siteDesc == null) {
					siteDesc = fCurrentCategoryDefinition.getModel()
							.getFactory().createDescription(
									fCurrentCategoryDefinition);
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
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);
		
		fNameText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_NAME), null, false);
		fNameText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				try {
					if (text.getValue().length() <= 0
							|| alreadyExists(text.getValue())) {
						setValue(PROPERTY_NAME);
						String message = PDEPlugin
								.getResourceString("CategoryDetails.alreadyExists"); //$NON-NLS-1$
						MessageDialog
								.openError(
										PDEPlugin.getActiveWorkbenchShell(),
										PDEPlugin
												.getResourceString("CategoryDetails.alreadyExists.title"), //$NON-NLS-1$
										message);
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

		fLabelText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_LABEL), null, false);
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

		fDescriptionText = new FormEntry(container, toolkit, PDEPlugin
				.getResourceString(SECTION_DESC), SWT.WRAP | SWT.MULTI);
		fDescriptionText.getText().setLayoutData(
				new GridData(GridData.FILL_BOTH));

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

	private void bringFeatures(String oldCategory){
		ISiteFeature[] siteFeatures = fCurrentCategoryDefinition.getModel()
				.getSite().getFeatures();
		for (int i = 0; i < siteFeatures.length; i++) {
			ISiteCategory[] categories = siteFeatures[i].getCategories();
			for (int c = 0; c < categories.length; c++) {
				if (oldCategory.equals(categories[c].getName())) {
					try {
						categories[c].setName(fCurrentCategoryDefinition
								.getName());
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
				ISiteDescription siteDesc = fCurrentCategoryDefinition
						.getDescription();
				if (siteDesc == null) {
					clearField(property);
				} else {
					fDescriptionText.setValue(siteDesc.getText(), true);
				}

			}
		}
	}
}
