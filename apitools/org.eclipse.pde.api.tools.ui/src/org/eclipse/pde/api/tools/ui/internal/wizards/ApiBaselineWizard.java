/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;

/**
 * A wizard to create a new API profile
 * @since 1.0.0
 */
public class ApiBaselineWizard extends Wizard {

	private IApiBaseline profile = null;
	private boolean content = false;
	
	/**
	 * Constructor
	 * @param profile
	 */
	public ApiBaselineWizard(IApiBaseline profile) {
		this.profile = profile;
		if(profile == null) {
			setWindowTitle(WizardMessages.ApiProfileWizard_0);
		}
		else {
			setWindowTitle(WizardMessages.ApiProfileWizard_1);
		}
		setNeedsProgressMonitor(true);
	}
	
	/**
	 * @return the current profile in the wizard. The current profile 
	 * can be <code>null</code> if the wizard has just been opened to create a new API profile
	 */
	public IApiBaseline getProfile() {
		return profile;
	}
	
	/**
	 * @return if the underlying content of the baseline has changed and not just
	 * a change to the name
	 */
	public boolean contentChanged() {
		return content;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new ApiBaselineWizardPage(profile));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			ApiBaselineWizardPage page = (ApiBaselineWizardPage) getContainer().getCurrentPage();
			profile = page.finish();
			content = page.contentChanged();
			return profile != null;
		}
		catch(IOException e) {
			ApiUIPlugin.log(e);
		}
		catch(CoreException e) {
			ApiUIPlugin.log(e);
		}
		return false;
	}
	
	/**
	 * @see org.eclipse.jface.wizard.Wizard#performCancel()
	 */
	public boolean performCancel() {
		ApiBaselineWizardPage page = (ApiBaselineWizardPage) getContainer().getCurrentPage();
		page.cancel();
		return super.performCancel();
	}
}
