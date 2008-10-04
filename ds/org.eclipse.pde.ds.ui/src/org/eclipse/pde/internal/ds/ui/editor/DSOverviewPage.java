/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
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

	public void modelChanged(IModelChangedEvent event) {
		// no op
	}

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
		
		Composite top = toolkit.createComposite(body);
		top.setLayout(GridLayoutFactory.fillDefaults().create());
		top.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(2, 1).create());

		Composite topRight = toolkit.createComposite(body);
		topRight.setLayout(GridLayoutFactory.fillDefaults().create());
		topRight.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(2, 1).create());
		
		Composite left = toolkit.createComposite(body);
		left.setLayout(new GridLayout());
		left.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.span(2, 1).create());
		
		Composite right = toolkit.createComposite(body);
		right.setLayout(new GridLayout());
		right.setLayoutData(GridDataFactory.fillDefaults().grab(true, true)
				.span(2, 1).create());
		
		Composite bottom = toolkit.createComposite(body);
		bottom.setLayout(new GridLayout());
		bottom.setLayoutData(GridDataFactory.fillDefaults().grab(true, false)
				.span(4, 1).create());

		// Sections
		managedForm.addPart(new DSComponentSection(this, top));
		managedForm.addPart(new DSOptionsSection(this, topRight));
		managedForm.addPart(new DSReferenceSection(this, left));
		managedForm.addPart(new DSProvideSection(this, right));
		managedForm.addPart(new DSPropertiesSection(this, bottom));
	}

}
