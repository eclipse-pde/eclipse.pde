/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.FormPage;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * Second page for the target definition editor.  Provides ability to alter various
 * environment elements of the target.
 * @see TargetEditor
 * @see EnvironmentSection
 * @see JRESection
 * @see ArgumentsSection
 * @see ImplicitDependenciesSection
 */
public class EnvironmentPage extends FormPage {

	public static final String PAGE_ID = "environment"; //$NON-NLS-1$

	public EnvironmentPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.EnvironmentPage_title);
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		form.setText(PDEUIMessages.EnvironmentPage_title);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_TARGET_ENVIRONMENT));
		FormToolkit toolkit = managedForm.getToolkit();
		fillBody(managedForm, toolkit);
		toolkit.decorateFormHeading(form.getForm());
		((TargetEditor) getEditor()).contributeToToolbar(managedForm.getForm(), IHelpContextIds.TARGET_EDITOR_ENVIRONMENT_PAGE);
		((TargetEditor) getEditor()).addForm(managedForm);
		form.updateToolBar();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.TARGET_EDITOR_ENVIRONMENT_PAGE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form, IHelpContextIds.TARGET_EDITOR_ENVIRONMENT_PAGE);
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormGridLayout(false, 2));

		managedForm.addPart(new EnvironmentSection(this, body));
		managedForm.addPart(new JRESection(this, body));
		managedForm.addPart(new ArgumentsSection(this, body));
		managedForm.addPart(new ImplicitDependenciesSection(this, body));
	}

	@Override
	public boolean canLeaveThePage() {
		((TargetEditor) getEditor()).setDirty(isDirty());
		return true;
	}
}
