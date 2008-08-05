/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 242028
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DSOverviewPage extends PDEFormPage implements
		IModelChangedListener {

	public static final String PAGE_ID = Messages.DSSimpPage_pageId;

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
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 1));

		// Sections
		
		managedForm.addPart(new DSSection(this, body));
		managedForm.addPart(new DSReferenceSection(this, body));
		managedForm.addPart(new DSProvideSection(this, body));
	}

}
