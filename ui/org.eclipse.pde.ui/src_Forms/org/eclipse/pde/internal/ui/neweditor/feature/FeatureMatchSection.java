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
package org.eclipse.pde.internal.ui.neweditor.feature;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.manifest.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;

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
	public FeatureMatchSection(PDEFormPage formPage) {
		super(formPage, false);
	}

	public Composite createClient(
		Composite parent,
		FormWidgetFactory factory) {
		Composite client = super.createClient(parent, factory);
		patchButton = factory.createButton(client, "Patch", SWT.CHECK);
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

		return client;
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
		patchButton.setEnabled(!isReadOnly() && (ntrue > 0 || nfalse > 0));
		patchButton.setSelection(ntrue > 0);
	}

	private void update(IFeatureImport iimport) {
		super.update(iimport);
		if (patchButton == null)
			return;
		IFeatureImport fimport = (IFeatureImport) iimport;
		if (fimport == null || fimport.getType() == IFeatureImport.PLUGIN) {
			patchButton.setSelection(false);
			patchButton.setEnabled(false);
			return;
		}
		patchButton.setEnabled(!isReadOnly());
		patchButton.setSelection(fimport.isPatch());
	}
}
