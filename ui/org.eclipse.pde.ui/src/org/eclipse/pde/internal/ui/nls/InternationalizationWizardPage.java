/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.nls;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

public abstract class InternationalizationWizardPage extends WizardPage {

	public InternationalizationWizardPage(String pageName) {
		super(pageName);
	}

	public InternationalizationWizardPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}

	protected Group createFilterContainer(Composite parent, String title, String label) {
		Group container = new Group(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = 6;
		container.setLayout(layout);

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 3;
		container.setLayoutData(gd);
		container.setText(title);

		Label templateLabel = new Label(container, SWT.NONE);
		templateLabel.setText(label);
		return container;
	}

	protected Text createFilterText(Composite parent, String initial) {
		Text text = new Text(parent, SWT.BORDER);
		text.setText(initial);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		text.setLayoutData(gd);
		return text;
	}

}
