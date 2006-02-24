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
package org.eclipse.pde.internal.ui.tests.macro;

import org.eclipse.jface.wizard.Wizard;


public class IndexWizard extends Wizard {
	private IndexPage page;
	private MacroManager macroManager;
	
	public IndexWizard(MacroManager manager) {
		this.macroManager = manager;
		setWindowTitle("Macro Recorder");
	}
	
	public void addPages() {
		page = new IndexPage(macroManager.getExistingIndices());
		addPage(page);
	}
	
	public boolean performFinish() {
		String indexId = page.getIndexId();
		if (indexId!=null) {
			macroManager.addIndex(indexId);
		}
		return true;
	}
}