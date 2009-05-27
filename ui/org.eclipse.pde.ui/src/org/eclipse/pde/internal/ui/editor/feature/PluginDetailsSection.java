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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginDetailsSection extends PDESection implements IFormPart, IPartSelectionListener {
	protected IFeaturePlugin fInput;

	private FormEntry fNameText;

	private FormEntry fVersionText;

	private FormEntry fdownloadSizeText;

	private FormEntry fInstallSizeText;

	private Button fUnpackButton;

	private boolean fBlockNotification;

	public PluginDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.SiteEditor_PluginDetailsSection_title, PDEUIMessages.SiteEditor_PluginDetailsSection_desc, SWT.NULL);
	}

	public PluginDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void cancelEdit() {
		fVersionText.cancelEdit();
		fdownloadSizeText.cancelEdit();
		fInstallSizeText.cancelEdit();
		super.cancelEdit();
	}

	public void commit(boolean onSave) {
		fVersionText.commit();
		fdownloadSizeText.commit();
		fInstallSizeText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fNameText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PluginDetailsSection_pluginLabel, null, false);
		limitTextWidth(fNameText);
		fNameText.setEditable(false);
		fNameText.getText().setEnabled(false);

		fVersionText = new FormEntry(container, toolkit, PDEUIMessages.FeatureEditor_SpecSection_version, null, false);
		fVersionText.setFormEntryListener(new FormEntryAdapter(this) {
			public void textValueChanged(FormEntry text) {
				if (fInput != null)
					try {
						fInput.setVersion(text.getValue());
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		limitTextWidth(fVersionText);
		fVersionText.setEditable(isEditable());

		fdownloadSizeText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PluginDetailsSection_downloadSize, null, false);
		fdownloadSizeText.setFormEntryListener(new FormEntryAdapter(this) {

			public void textValueChanged(FormEntry text) {
				if (fInput != null)
					try {
						fInput.setDownloadSize(getLong(text.getValue()));
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		limitTextWidth(fdownloadSizeText);
		fdownloadSizeText.setEditable(isEditable());

		fInstallSizeText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PluginDetailsSection_installSize, null, false);
		fInstallSizeText.setFormEntryListener(new FormEntryAdapter(this) {

			public void textValueChanged(FormEntry text) {
				if (fInput != null)
					try {
						fInput.setInstallSize(getLong(text.getValue()));
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					}
			}
		});
		limitTextWidth(fInstallSizeText);
		fInstallSizeText.setEditable(isEditable());

		fUnpackButton = toolkit.createButton(container, PDEUIMessages.SiteEditor_PluginDetailsSection_unpack, SWT.CHECK);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fUnpackButton.setLayoutData(gd);
		fUnpackButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					if (!fBlockNotification)
						fInput.setUnpack(fUnpackButton.getSelection());
				} catch (CoreException ex) {
					PDEPlugin.logException(ex);
				}
			}
		});

		toolkit.paintBordersFor(container);
		section.setClient(container);
	}

	public void dispose() {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.removeModelChangedListener(this);
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.forms.AbstractFormPart#initialize(org.eclipse.ui.forms.IManagedForm)
	 */
	public void initialize(IManagedForm form) {
		IFeatureModel model = (IFeatureModel) getPage().getModel();
		if (model != null)
			model.addModelChangedListener(this);
		super.initialize(form);
	}

	protected void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
	}

	public void modelChanged(IModelChangedEvent e) {
		markStale();
	}

	public void refresh() {
		update();
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() == 1) {
			IStructuredSelection s = ((IStructuredSelection) selection);
			Object o = s.getFirstElement();
			if (o instanceof IFeaturePlugin) {
				fInput = (IFeaturePlugin) o;
			} else {
				fInput = null;
			}
		} else {
			fInput = null;
		}
		update();
	}

	public void setFocus() {
		if (fdownloadSizeText != null)
			fdownloadSizeText.getText().setFocus();
	}

	private void update() {
		if (fInput != null) {
			fNameText.setValue(fInput.getLabel());
			fVersionText.setValue(fInput.getVersion(), true);
			fdownloadSizeText.setValue(fInput.getDownloadSize() >= 0 ? "" + fInput.getDownloadSize() : null, true); //$NON-NLS-1$
			fInstallSizeText.setValue(fInput.getInstallSize() >= 0 ? "" + fInput.getInstallSize() : null, true); //$NON-NLS-1$
			fBlockNotification = true;
			fUnpackButton.setSelection(fInput.isUnpack());
			fBlockNotification = false;

		} else {
			fNameText.setValue(null);
			fVersionText.setValue(null, true);
			fdownloadSizeText.setValue(null, true);
			fInstallSizeText.setValue(null, true);
			fBlockNotification = true;
			fUnpackButton.setSelection(true);
			fBlockNotification = false;
		}
		boolean editable = fInput != null && isEditable();
		fVersionText.setEditable(editable);
		fdownloadSizeText.setEditable(editable);
		fInstallSizeText.setEditable(editable);
		fUnpackButton.setEnabled(editable);
	}

	public boolean isEditable() {
		return getPage().getPDEEditor().getAggregateModel().isEditable();
	}

	private long getLong(String svalue) {
		if (svalue == null)
			return 0;
		try {
			return Long.parseLong(svalue);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
