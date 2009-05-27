/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.exports;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class FeatureDestinationTab extends ExportDestinationTab {

	public FeatureDestinationTab(AbstractExportWizardPage page) {
		super(page);
	}

	protected void hookListeners() {
		super.hookListeners();
		fDirectoryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				((FeatureExportWizardPage) fPage).adjustJNLPTabVisibility();
				fPage.pageChanged();
			}
		});
	}

}
