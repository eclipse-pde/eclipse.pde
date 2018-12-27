/*******************************************************************************
 * Copyright (c) 2018 Remain Software and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wim Jongman
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

public class FeatureProperties extends VersionDialog {

	private boolean fRootFeature;

	public FeatureProperties(Shell parent, boolean editable, String version, boolean rootFeature) {
		super(parent, editable, version);
		fRootFeature = rootFeature;
		setTitle(PDEUIMessages.FeatureProps_title);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		Button rootMode = new Button(comp, SWT.CHECK);
		rootMode.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
		rootMode.setText(PDEUIMessages.FeatureProps_rootFeature);
		rootMode.setSelection(fRootFeature);
		rootMode.addListener(SWT.Selection, e -> fRootFeature = rootMode.getSelection());
		return comp;
	}

	public boolean getRootInstallMode() {
		return fRootFeature;
	}
}