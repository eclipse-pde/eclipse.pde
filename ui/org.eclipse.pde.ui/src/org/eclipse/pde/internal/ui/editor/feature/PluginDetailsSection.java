/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
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
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginDetailsSection extends PDESection implements IFormPart,
		IPartSelectionListener {
	protected IFeaturePlugin fInput;

	private FormEntry fNameText;

	private FormEntry fdownloadSizeText;

	private FormEntry fInstallSizeText;

	private Button fUnpackButton;

	private boolean fBlockNotification;

	public PluginDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.SiteEditor_PluginDetailsSection_title,
				PDEUIMessages.SiteEditor_PluginDetailsSection_desc, SWT.NULL);
	}

	public PluginDetailsSection(PDEFormPage page, Composite parent,
			String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	public void cancelEdit() {
		fdownloadSizeText.cancelEdit();
		fInstallSizeText.cancelEdit();
		super.cancelEdit();
	}

	public void commit(boolean onSave) {
		fdownloadSizeText.commit();
		fInstallSizeText.commit();
		super.commit(onSave);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(section);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 5;
		layout.horizontalSpacing = 6;
		container.setLayout(layout);

		fNameText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_PluginDetailsSection_pluginLabel, null, false);
		limitTextWidth(fNameText);
		fNameText.setEditable(false);
		fNameText.getText().setEnabled(false);

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
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFeaturePlugin) {
				fInput = (IFeaturePlugin) o;
			} else {
				fInput = null;
			}
		} else
			fInput = null;
		update();
	}

	public void setFocus() {
		if (fdownloadSizeText != null)
			fdownloadSizeText.getText().setFocus();
	}

	private void update() {
		if (fInput != null) {
			fNameText.setValue(fInput.getLabel());
			fdownloadSizeText
					.setValue(
							fInput.getDownloadSize() >= 0 ? "" + fInput.getDownloadSize() : null, true); //$NON-NLS-1$
			fInstallSizeText
					.setValue(
							fInput.getInstallSize() >= 0 ? "" + fInput.getInstallSize() : null, true); //$NON-NLS-1$
			fBlockNotification = true;
			fUnpackButton.setSelection(fInput.isUnpack());
			fBlockNotification = false;

		} else {
			fNameText.setValue(null);
			fdownloadSizeText.setValue(null, true); 
			fInstallSizeText.setValue(null, true); 
			fBlockNotification = true;
			fUnpackButton.setSelection(true);
			fBlockNotification = false;
		}
		fdownloadSizeText.setEditable(fInput != null && isEditable());
		fInstallSizeText.setEditable(fInput != null && isEditable());
		fUnpackButton.setEnabled(fInput != null && isEditable());
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
