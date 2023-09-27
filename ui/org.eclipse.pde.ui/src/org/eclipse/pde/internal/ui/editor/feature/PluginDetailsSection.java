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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormEntryAdapter;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.parts.FormEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.IPartSelectionListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class PluginDetailsSection extends PDESection implements IPartSelectionListener {
	protected IFeaturePlugin fInput;

	private FormEntry fNameText;

	private FormEntry fVersionText;

	public PluginDetailsSection(PDEFormPage page, Composite parent) {
		this(page, parent, PDEUIMessages.SiteEditor_PluginDetailsSection_title, PDEUIMessages.SiteEditor_PluginDetailsSection_desc, SWT.NULL);
	}

	public PluginDetailsSection(PDEFormPage page, Composite parent, String title, String desc, int toggleStyle) {
		super(page, parent, Section.DESCRIPTION | toggleStyle);
		getSection().setText(title);
		getSection().setDescription(desc);
		createClient(getSection(), page.getManagedForm().getToolkit());
	}

	@Override
	public void cancelEdit() {
		fVersionText.cancelEdit();
		super.cancelEdit();
	}

	@Override
	public void commit(boolean onSave) {
		fVersionText.commit();
		super.commit(onSave);
	}

	@Override
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
		limitTextWidth(fVersionText);
		fVersionText.setEditable(isEditable());
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

	protected void limitTextWidth(FormEntry entry) {
		GridData gd = (GridData) entry.getText().getLayoutData();
		gd.widthHint = 30;
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
		if (selection instanceof IStructuredSelection s && ((IStructuredSelection) selection).size() == 1) {
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

	private void update() {
		if (fInput != null) {
			fNameText.setValue(fInput.getLabel());
			fVersionText.setValue(fInput.getVersion(), true);

		} else {
			fNameText.setValue(null);
			fVersionText.setValue(null, true);
		}
		boolean editable = fInput != null && isEditable();
		fVersionText.setEditable(editable);
	}
}
