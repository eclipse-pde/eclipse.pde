/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;

public class IncludedFeaturesDetailsSection extends PDESection implements IPartSelectionListener {
	protected IFeatureChild fInput;

	private FormEntry fNameText;

	private FormEntry fVersionText;

	private Button fOptionalButton;

	private Button fSearchRootButton;

	private Button fSearchSelfButton;

	private Button fSearchBothButton;

	private boolean fBlockNotification;

	public IncludedFeaturesDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_title, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_desc, SWT.NULL);
	}

	public IncludedFeaturesDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	@Override
	public void cancelEdit() {
		fNameText.cancelEdit();
		fVersionText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public void commit(boolean onSave) {
		fNameText.commit();
		fVersionText.commit();
		super.commit(onSave);
	}

	@Override
	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientTableWrapLayout(false, 2));

		fNameText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_featureLabel, null, false);
		fNameText.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry text) {
				if (fInput != null)
					try {
						fInput.setName(text.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		fNameText.setEditable(isEditable());

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {
			@Override
			public void textValueChanged(FormEntry text) {
				if (fInput != null)
					try {
						fInput.setVersion(text.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		fVersionText.setEditable(isEditable());

		fOptionalButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_optional, SWT.CHECK);

		TableWrapData gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = 2;
		fOptionalButton.setLayoutData(gd);
		fOptionalButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockNotification) {
				try {
					fInput.setOptional(fOptionalButton.getSelection());
				} catch (CoreException ce) {
				}
			}
		}));
		Label fSearchLocationDescLabel = toolkit.createLabel(container, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_searchLocation, SWT.WRAP);
		gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = 2;
		fSearchLocationDescLabel.setLayoutData(gd);

		fSearchRootButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_root, SWT.RADIO);
		fSearchRootButton.setSelection(true);
		gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = 2;
		gd.indent = 5;
		fSearchRootButton.setLayoutData(gd);
		fSearchRootButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockNotification) {
				try {
					if (fSearchRootButton.getSelection())
						fInput.setSearchLocation(IFeatureChild.ROOT);
				} catch (CoreException ce) {
				}
			}
		}));

		fSearchSelfButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_self, SWT.RADIO);
		fSearchSelfButton.setSelection(true);
		gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = 2;
		gd.indent = 5;
		fSearchSelfButton.setLayoutData(gd);
		fSearchSelfButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockNotification) {
				try {
					if (fSearchSelfButton.getSelection())
						fInput.setSearchLocation(IFeatureChild.SELF);
				} catch (CoreException ce) {
				}
			}
		}));

		fSearchBothButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_IncludedFeaturesDetailsSection_both, SWT.RADIO);
		fSearchBothButton.setSelection(true);
		gd = new TableWrapData(TableWrapData.FILL);
		gd.colspan = 2;
		gd.indent = 5;
		fSearchBothButton.setLayoutData(gd);
		fSearchBothButton.addSelectionListener(widgetSelectedAdapter(e -> {
			if (!fBlockNotification) {
				try {
					if (fSearchBothButton.getSelection())
						fInput.setSearchLocation(IFeatureChild.BOTH);
				} catch (CoreException ce) {
				}
			}
		}));

		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	@Override
	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	@Override
	public void initialize(IManagedForm form) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
		super.initialize(form);
	}

	@Override
	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	@Override
	public void refresh() {
		update();
		super.refresh();
	}

	@Override
	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFeatureChild) {
				fInput = (IFeatureChild) o;
			} else {
				fInput = null;
			}
		} else
			fInput = null;
		update();
	}

	@Override
	public void setFocus() {
		if (fNameText != null)
			fNameText.getText().setFocus();
	}

	private void update() {
		fBlockNotification = true;

		if (fInput != null) {
			fNameText.setValue(fInput.getName(), true);
			fVersionText.setValue(fInput.getVersion(), true);
			fOptionalButton.setSelection(fInput.isOptional());
			int searchLocation = fInput.getSearchLocation();
			fSearchRootButton.setSelection(searchLocation == IFeatureChild.ROOT);
			fSearchSelfButton.setSelection(searchLocation == IFeatureChild.SELF);
			fSearchBothButton.setSelection(searchLocation == IFeatureChild.BOTH);
		} else {
			fNameText.setValue(null, true);
			fVersionText.setValue(null, true);
			fOptionalButton.setSelection(false);
			fSearchRootButton.setSelection(true);
			fSearchSelfButton.setSelection(false);
			fSearchBothButton.setSelection(false);
		}
		boolean editable = fInput != null && isEditable();
		fNameText.setEditable(editable);
		fVersionText.setEditable(editable);
		fOptionalButton.setEnabled(editable);
		fSearchRootButton.setEnabled(editable);
		fSearchSelfButton.setEnabled(editable);
		fSearchBothButton.setEnabled(editable);

		fBlockNotification = false;
	}
}
