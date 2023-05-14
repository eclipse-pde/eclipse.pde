/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

public class RegistryFilteredTree extends FilteredTree {

	private RegistryBrowser browser;

	public RegistryFilteredTree(RegistryBrowser browser, Composite parent, int treeStyle, PatternFilter filter) {
		super(parent, treeStyle, filter, true, true);
		this.browser = browser;
	}

	@Override
	protected void createControl(Composite parent, int treeStyle) {
		super.createControl(parent, treeStyle);

		// add 2px margin around filter text

		FormLayout layout = new FormLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);

		FormData data = new FormData();
		data.left = new FormAttachment(0, 0);
		data.right = new FormAttachment(100, 0);
		data.bottom = new FormAttachment(100, 0);
		if (showFilterControls) {
			FormData filterData= new FormData();
			filterData.top = new FormAttachment(0, 2);
			filterData.left = new FormAttachment(0, 2);
			filterData.right = new FormAttachment(100, -2);
			filterComposite.setLayoutData(filterData);
			data.top = new FormAttachment(filterComposite, 2);
		} else {
			data.top = new FormAttachment(0, 0);
		}
		treeComposite.setLayoutData(data);
	}

	@Override
	protected void updateToolbar(boolean visible) {
		super.updateToolbar(visible);

		// update view title on viewer's toolbar update
		browser.updateTitle();
	}
}
