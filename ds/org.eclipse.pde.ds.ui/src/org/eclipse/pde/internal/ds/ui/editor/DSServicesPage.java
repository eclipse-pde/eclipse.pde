/*******************************************************************************
 * Copyright (c) 2009 EclipseSource Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     EclipseSource Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.editor;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSProvideSection;
import org.eclipse.pde.internal.ds.ui.editor.sections.DSReferenceSection;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

public class DSServicesPage extends PDEFormPage implements
		IModelChangedListener {

	public static final String PAGE_ID = "services"; //$NON-NLS-1$

	public DSServicesPage(FormEditor editor) {
		super(editor, PAGE_ID, Messages.DSServicesPage_title);
	}

	public void modelChanged(IModelChangedEvent event) {
		// no op
	}

	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setImage(SharedImages.getImage(SharedImages.DESC_DS));
		form.setText(Messages.DSServicesPage_title);
		fillBody(managedForm, toolkit);

		// PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody()C,
		// IHelpContextIds.TOC_EDITOR);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(true, 1));
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		body.setLayoutData(data);
		
		// Sections
		managedForm.addPart(new DSReferenceSection(this, body));
		managedForm.addPart(new DSProvideSection(this, body));
	}

}
