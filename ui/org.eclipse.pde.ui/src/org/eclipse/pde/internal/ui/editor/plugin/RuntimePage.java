/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class RuntimePage extends PDEFormPage {
	public static final String PAGE_ID = "runtime"; //$NON-NLS-1$

	public RuntimePage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.RuntimePage_tabName);
	}

	@Override
	protected String getHelpResource() {
		IPluginModelBase base = (IPluginModelBase) getPDEEditor().getAggregateModel();
		if (base == null) {
			return null;
		}
		if (base.isFragmentModel())
			return IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME;
		return IHelpContextIds.MANIFEST_PLUGIN_RUNTIME;
	}

	@Override
	protected void createFormContent(IManagedForm mform) {
		super.createFormContent(mform);
		ScrolledForm form = mform.getForm();
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_JAVA_LIB_OBJ));
		form.setText(PDEUIMessages.ManifestEditor_RuntimeForm_title);
		form.getBody().setLayout(FormLayoutFactory.createFormGridLayout(false, 2));

		if (isBundle()) {
			mform.addPart(new ExportPackageSection(this, form.getBody()));
			if (((ManifestEditor) getEditor()).isEquinox())
				mform.addPart(new ExportPackageVisibilitySection(this, form.getBody()));
			mform.addPart(new LibrarySection(this, form.getBody()));
		} else {
			// No MANIFEST.MF (not a Bundle)
			// Create a new plug-in project targeted for 3.0 using the hello
			// world template to see this section (no MANIFEST.MF is created)
			mform.addPart(new LibrarySection(this, form.getBody()));
			mform.addPart(new LibraryVisibilitySection(this, form.getBody()));
		}

		IPluginModelBase base = (IPluginModelBase) getPDEEditor().getAggregateModel();
		if (base == null) {
			return;
		}
		if (base.isFragmentModel())
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_FRAGMENT_RUNTIME);
		else
			PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.MANIFEST_PLUGIN_RUNTIME);
	}

	private boolean isBundle() {
		return getPDEEditor().getContextManager().findContext(BundleInputContext.CONTEXT_ID) != null;
	}

}
