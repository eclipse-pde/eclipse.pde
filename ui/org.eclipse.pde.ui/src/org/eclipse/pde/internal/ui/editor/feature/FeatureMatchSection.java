/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.util.Iterator;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.core.plugin.IPluginReference;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.plugin.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class FeatureMatchSection extends MatchSection {
	private Button patchButton;

	/**
	 * Constructor for FeatureMatchSection.
	 * @param formPage
	 */
	public FeatureMatchSection(PDEFormPage formPage, Composite parent) {
		super(formPage, parent, false);
	}

	public void createClient(
		Section section,
		FormToolkit toolkit) {
		super.createClient(section, toolkit);
		Composite client = (Composite)section.getClient();
		patchButton = toolkit.createButton(client, PDEPlugin.getResourceString("FeatureMatchSection.patch"), SWT.CHECK); //$NON-NLS-1$
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		patchButton.setLayoutData(gd);
		patchButton.setEnabled(false);
		patchButton.setSelection(false);
		patchButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handlePatchChange(patchButton.getSelection());
			}
		});
	}

	private void handlePatchChange(boolean patch) {
		if (currentImport != null) {
			IFeatureImport iimport = (IFeatureImport) currentImport;
			if (iimport.getType() == IFeatureImport.FEATURE) {
				try {
					iimport.setPatch(patch);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				}
			}
		}
		if (multiSelection != null) {
			for (Iterator iter = multiSelection.iterator(); iter.hasNext();) {
				IFeatureImport iimport = (IFeatureImport) iter.next();
				try {
					iimport.setPatch(patch);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
					break;
				}
			}
		}
	}

	protected void update(IStructuredSelection selection) {
		super.update(selection);
		if (patchButton == null)
			return;
		if (selection.isEmpty()) {
			update((IFeatureImport) null);
			return;
		}
		if (selection.size() == 1) {
			update((IFeatureImport) selection.getFirstElement());
			return;
		}
		int ntrue = 0, nfalse = 0;

		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			IFeatureImport iimport = (IFeatureImport) iter.next();
			if (iimport.getType() == IFeatureImport.FEATURE) {
				if (iimport.isPatch())
					ntrue++;
				else
					nfalse++;
			}
		}
		patchButton.setEnabled(isEditable() && (ntrue > 0 || nfalse > 0));
		patchButton.setSelection(ntrue > 0);
	}

	protected void update(IPluginReference reference) {
		super.update(reference);
		if (patchButton == null)
			return;
		IFeatureImport fimport = (IFeatureImport)reference;
		if (fimport == null || fimport.getType() == IFeatureImport.PLUGIN) {
			patchButton.setSelection(false);
			patchButton.setEnabled(false);
			return;
		}
		patchButton.setEnabled(getPage().getModel().isEditable());
		patchButton.setSelection(fimport.isPatch());
	}
}