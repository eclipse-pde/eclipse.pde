/*******************************************************************************
 * Copyright (c) 2008, 2015 Code 9 Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSComponentSection;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSOptionsSection;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSPropertiesSection;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DSOverviewPage extends PDEFormPage implements
		IModelChangedListener {

	public static final String PAGE_ID = "overview"; //$NON-NLS-1$

	public DSOverviewPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.DSSimpPage_title);
	}

	@Override
	public void modelChanged(IModelChangedEvent event) {
		// no op
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(SharedImages.getImage(SharedImages.DESC_DS));
		form.setText(Messages.DSSimpPage_title);
		fillBody(managedForm, toolkit);

		// PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody()C,
		// IHelpContextIds.TOC_EDITOR);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 4));

		Composite topLeft = toolkit.createComposite(body);
		topLeft.setLayout(GridLayoutFactory.fillDefaults().create());
		topLeft.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(2, 1).create());

		Composite topRight = toolkit.createComposite(body);
		topRight.setLayout(GridLayoutFactory.fillDefaults().create());
		topRight.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(2, 1).create());

		Composite bottom = toolkit.createComposite(body);
		bottom.setLayout(new GridLayout());
		bottom.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(4, 1).create());

		// Sections
		managedForm.addPart(new DSComponentSection(this, topLeft));
		managedForm.addPart(new DSOptionsSection(this, topRight));
		managedForm.addPart(new DSPropertiesSection(this, bottom));
	}

}
