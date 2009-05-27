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
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.*;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class DataDetailsSection extends PDESection implements IFormPart, IPartSelectionListener {
	protected IFeatureEntry fInput;

	private FormEntry fdownloadSizeText;

	private FormEntry fInstallSizeText;

	public DataDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.SiteEditor_DataDetailsSection_title, PDEUIMessages.SiteEditor_DataDetailsSection_desc, SWT.NULL);
	}

	public DataDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
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

		section.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		section.setLayoutData(data);

		Composite container = toolkit.createComposite(section);
		container.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		container.setLayoutData(new GridData(GridData.FILL_BOTH));

		fdownloadSizeText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_DataDetailsSection_downloadSize, null, false);
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

		fInstallSizeText = new FormEntry(container, toolkit, PDEUIMessages.SiteEditor_DataDetailsSection_installSize, null, false);
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
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			markStale();
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE && e.getChangedObjects().length > 0 && e.getChangedObjects()[0] instanceof IFeatureData && e.getChangedObjects()[0] == fInput) {
			markStale();
		}
	}

	public void refresh() {
		update();
		super.refresh();
	}

	public void selectionChanged(IFormPart part, ISelection selection) {
		if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
			Object o = ((IStructuredSelection) selection).getFirstElement();
			if (o instanceof IFeatureData) {
				fInput = (IFeatureData) o;
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
			fdownloadSizeText.setValue(fInput.getDownloadSize() >= 0 ? "" + fInput.getDownloadSize() : null, true); //$NON-NLS-1$
			fInstallSizeText.setValue(fInput.getInstallSize() >= 0 ? "" + fInput.getInstallSize() : null, true); //$NON-NLS-1$
		} else {
			fdownloadSizeText.setValue(null, true);
			fInstallSizeText.setValue(null, true);
		}
		fdownloadSizeText.setEditable(fInput != null && isEditable());
		fInstallSizeText.setEditable(fInput != null && isEditable());
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
