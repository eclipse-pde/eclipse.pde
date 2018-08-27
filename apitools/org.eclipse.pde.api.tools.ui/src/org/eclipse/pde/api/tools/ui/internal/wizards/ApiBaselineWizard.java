/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
 *     Manumitting Technologies Inc - bug 324310
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;

/**
 * A wizard to create a new API profile
 *
 * @since 1.0.0
 */
public class ApiBaselineWizard extends Wizard {

	private IApiBaseline profile = null;
	private boolean content = false;

	/**
	 * Constructor
	 *
	 * @param profile
	 */
	public ApiBaselineWizard(IApiBaseline profile) {
		this.profile = profile;
		if (profile == null) {
			setWindowTitle(WizardMessages.ApiProfileWizard_0);
		} else {
			setWindowTitle(WizardMessages.ApiProfileWizard_1);
		}
		setNeedsProgressMonitor(true);
	}

	/**
	 * @return the current profile in the wizard. The current profile can be
	 *         <code>null</code> if the wizard has just been opened to create a
	 *         new API profile
	 */
	public IApiBaseline getProfile() {
		return profile;
	}

	/**
	 * @return if the underlying content of the baseline has changed and not
	 *         just a change to the name
	 */
	public boolean contentChanged() {
		return content;
	}

	@Override
	public void addPages() {
		if (profile != null) {
			profile.getApiComponents(); // XXX: necc to load baseline cache
			if (TargetBasedApiBaselineWizardPage.isApplicable(profile)) {
				addPage(new TargetBasedApiBaselineWizardPage(profile));
			} else /* if(ApiBaselineWizardPage.isApplicable(profile)) */ {
				addPage(new DirectoryBasedApiBaselineWizardPage(profile));
			}
		} else {
			setForcePreviousAndNextButtons(true);
			addPage(new SelectApiBaselineTypeWizardPage());
		}
	}

	@Override
	public boolean canFinish() {
		return super.canFinish() && getApiBaselineWizardPage() != null;
	}

	private ApiBaselineWizardPage getApiBaselineWizardPage() {
		// Assumes that the AbstractApiBaselineWizardPage page
		// is the current page
		IWizardPage page = getContainer().getCurrentPage();
		if (page instanceof ApiBaselineWizardPage) {
			return (ApiBaselineWizardPage) page;
		}
		return null;
	}

	@Override
	public boolean performFinish() {
		try {
			ApiBaselineWizardPage page = getApiBaselineWizardPage();
			if (page != null) {
				profile = page.finish();
				content = page.contentChanged();
				return profile != null;
			}
			return true;
		} catch (IOException e) {
			ApiUIPlugin.log(e);
		} catch (CoreException e) {
			ApiUIPlugin.log(e);
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.wizard.Wizard#performCancel()
	 */
	@Override
	public boolean performCancel() {
		ApiBaselineWizardPage page = getApiBaselineWizardPage();
		if (page != null) {
			page.cancel();
		}
		return super.performCancel();
	}
}
