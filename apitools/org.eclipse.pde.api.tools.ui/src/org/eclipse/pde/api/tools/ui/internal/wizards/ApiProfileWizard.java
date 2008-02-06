/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;

/**
 * A wizard to create a new API profile
 * @since 1.0.0
 */
public class ApiProfileWizard extends Wizard {

	private IApiProfile profile = null;
	
	/**
	 * Constructor
	 * @param profile
	 */
	public ApiProfileWizard(IApiProfile profile) {
		this.profile = profile;
		if(profile == null) {
			setWindowTitle(WizardMessages.ApiProfileWizard_0);
		}
		else {
			setWindowTitle(WizardMessages.ApiProfileWizard_1);
		}
	}
	
	/**
	 * @return the current profile in the wizard. The current profile 
	 * can be <code>null</code> if the wizard has just been opened to create a new API profile
	 */
	public IApiProfile getProfile() {
		return profile;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		addPage(new ApiProfileWizardPage(profile));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	public boolean performFinish() {
		try {
			ApiProfileWizardPage page = (ApiProfileWizardPage) getContainer().getCurrentPage();
			profile = page.finish();
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
}
