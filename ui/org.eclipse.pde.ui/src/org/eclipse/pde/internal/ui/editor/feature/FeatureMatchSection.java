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
package org.eclipse.pde.internal.ui.editor.feature;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.plugin.MatchSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class FeatureMatchSection extends MatchSection {
	private Button fPatchButton;

	/**
	 * Constructor for FeatureMatchSection.
	 * 
	 * @param formPage
	 */
	public FeatureMatchSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, false);
	}

	public void createClient(Section section, FormToolkit toolkit) {
		super.createClient(section, toolkit);
		Composite client = (Composite) section.getClient();
		fPatchButton = toolkit.createButton(client, PDEPlugin
				.getResourceString("FeatureMatchSection.patch"), SWT.CHECK); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		fPatchButton.setLayoutData(gd);
		fPatchButton.setEnabled(false);
		fPatchButton.setSelection(false);
	}

	protected void update(IStructuredSelection selection) {
		super.update(selection);
		if (fPatchButton == null)
			return;
		if (selection.isEmpty()) {
			update((IFeatureImport) null);
			return;
		}
		if (!(selection.getFirstElement() instanceof IFeatureImport))
			return;

		if (selection.size() == 1) {
			update((IFeatureImport) selection.getFirstElement());
			return;
		}
	}

	protected void update(IPluginReference reference) {
		super.update(reference);
		if (fPatchButton == null)
			return;
		IFeatureImport fimport = (IFeatureImport) reference;
		if (fimport == null || fimport.getType() == IFeatureImport.PLUGIN) {
			fPatchButton.setSelection(false);
			return;
		}
		fPatchButton.setSelection(fimport.isPatch());
	}
}
