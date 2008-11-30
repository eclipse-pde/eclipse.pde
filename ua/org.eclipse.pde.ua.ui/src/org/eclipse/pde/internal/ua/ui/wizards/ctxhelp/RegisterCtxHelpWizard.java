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
package org.eclipse.pde.internal.ua.ui.wizards.ctxhelp;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPluginImages;
import org.eclipse.pde.internal.ua.ui.editor.ctxhelp.CtxHelpEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * Wizard to register a context help file in plugin.xml
 * @since 3.4
 * @see RegisterCtxHelpOperation
 * @see CtxHelpEditor
 */
public class RegisterCtxHelpWizard extends Wizard {

	private RegisterCtxHelpWizardPage fMainPage;
	private IModel fWizModel;

	public RegisterCtxHelpWizard(IModel model) {
		fWizModel = model;
		setWindowTitle(CtxWizardMessages.RegisterCtxHelpWizard_title);
		setDefaultPageImageDescriptor(PDEUserAssistanceUIPluginImages.DESC_CHEATSHEET_WIZ);
		setNeedsProgressMonitor(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new RegisterCtxHelpWizardPage(CtxWizardMessages.RegisterCtxHelpWizard_title);
		addPage(fMainPage);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			getContainer().run(false, true, new RegisterCtxHelpOperation(getShell(), fWizModel, fMainPage.getPluginText()));
		} catch (InvocationTargetException e) {
			PDEUserAssistanceUIPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Main page of the wizard
	 * @since 3.4
	 */
	class RegisterCtxHelpWizardPage extends WizardPage {

		private Text fPluginText;

		protected RegisterCtxHelpWizardPage(String pageName) {
			super(pageName);
			setTitle(CtxWizardMessages.RegisterCtxHelpWizard_pageTitle);
			setMessage(CtxWizardMessages.RegisterCtxHelpWizard_pageMessage);
		}

		public void createControl(Composite parent) {
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setFont(parent.getFont());
			composite.setLayoutData(new GridData(GridData.FILL_BOTH));

			Label label = new Label(composite, SWT.NONE);
			label.setFont(composite.getFont());
			label.setLayoutData(new GridData());
			label.setText(CtxWizardMessages.RegisterCtxHelpWizard_plugin);

			fPluginText = new Text(composite, SWT.SINGLE | SWT.BORDER);
			fPluginText.setFont(composite.getFont());
			fPluginText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			Label label2 = new Label(composite, SWT.NONE);
			label2.setFont(composite.getFont());
			GridData data = new GridData();
			data.horizontalSpan = 2;
			label2.setLayoutData(data);
			label2.setText(CtxWizardMessages.RegisterCtxHelpWizard_pluginDesc);

			setControl(composite);
		}

		public String getPluginText() {
			return fPluginText.getText().trim();
		}

	}

}
