/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 438509
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 *
 */
public class FeatureReferencePage extends PDEFormPage {
	public static final String PAGE_ID = "reference"; //$NON-NLS-1$

	private PluginSection fPluginSection;

	private PluginDetailsSection fPluginDetailsSection;

	private PluginPortabilitySection fPluginPortabilitySection;

	/**
	 *
	 * @param editor
	 * @param title
	 */
	public FeatureReferencePage(PDEFormEditor editor, String title) {
		super(editor, PAGE_ID, title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.MANIFEST_FEATURE_CONTENT;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(true, 2));

		// Set form header image
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PLUGINS_FRAGMENTS));

		GridData gd;

		Composite left = toolkit.createComposite(form.getBody());
		left.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		left.setLayoutData(gd);

		Composite right = toolkit.createComposite(form.getBody());
		right.setLayout(FormLayoutFactory.createFormPaneGridLayout(false, 1));
		gd = new GridData(GridData.FILL_BOTH);
		right.setLayoutData(gd);

		fPluginSection = new PluginSection(this, left);

		fPluginDetailsSection = new PluginDetailsSection(this, right);

		// Align the master and details section headers (misalignment caused
		// by section toolbar icons)
		alignSectionHeaders(fPluginSection.getSection(), fPluginDetailsSection.getSection());

		fPluginPortabilitySection = new PluginPortabilitySection(this, right);
		gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		fPluginPortabilitySection.getSection().setLayoutData(gd);

		managedForm.addPart(fPluginSection);
		managedForm.addPart(fPluginDetailsSection);
		managedForm.addPart(fPluginPortabilitySection);

		form.setText(PDEUIMessages.FeatureEditor_ReferencePage_heading);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FEATURE_CONTENT);
		fPluginSection.fireSelection();
		super.createFormContent(managedForm);
	}

	@Override
	public void setFocus() {
		fPluginSection.setFocus();
	}

	@Override
	public boolean selectReveal(final Object object) {
		if (object instanceof IFeaturePlugin) {
			// selecton has to be done by detecting item from content provider by id of feature
			// and using that in new selection 
			// because just using #setSelection(object) will not work
			final IFeaturePlugin featurePlugin = (IFeaturePlugin) object;
			final StructuredViewer fPluginViewer = fPluginSection.getStructuredViewerPart().getViewer();
			final IStructuredContentProvider provider = (IStructuredContentProvider) fPluginViewer.getContentProvider();
			for (Object o : provider.getElements(fPluginViewer.getInput())) {
				final IFeaturePlugin fp = (IFeaturePlugin) o;

				if (fp.getId().equals(featurePlugin.getId())) {
					fPluginViewer.setSelection(new StructuredSelection(fp), true);
					return true;
				}
			}
		}
		return false;
	}
}
