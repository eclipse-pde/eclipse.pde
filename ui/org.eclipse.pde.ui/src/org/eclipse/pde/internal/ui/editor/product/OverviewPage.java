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
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.*;

public class OverviewPage extends LaunchShortcutOverviewPage {

	public static final String PAGE_ID = "overview"; //$NON-NLS-1$
	private ProductLauncherFormPageHelper fLauncherHelper;

	public OverviewPage(PDELauncherFormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.OverviewPage_title);
	}

	@Override
	protected String getHelpResource() {
		return IHelpContextIds.OVERVIEW_PAGE;
	}

	@Override
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.OverviewPage_title);
		form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_PRODUCT_DEFINITION));
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.OVERVIEW_PAGE);
	}

	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		body.setLayout(FormLayoutFactory.createFormTableWrapLayout(true, 2));

		GeneralInfoSection generalSection = new GeneralInfoSection(this, body);
		ProductInfoSection productSection = new ProductInfoSection(this, body);

		managedForm.addPart(generalSection);
		managedForm.addPart(productSection);
		if (getModel().isEditable()) {
			createTestingSection(body, toolkit);
			createExportingSection(body, toolkit);
		}
	}

	private void createTestingSection(Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.Product_OverviewPage_testing);
		FormText text = createClient(section, getLauncherText(getLauncherHelper().isOSGi(), PDEUIMessages.Product_overview_testing), toolkit);
		PDELabelProvider lp = PDEPlugin.getDefault().getLabelProvider();
		text.setImage("run", lp.get(PDEPluginImages.DESC_RUN_EXC)); //$NON-NLS-1$
		text.setImage("debug", lp.get(PDEPluginImages.DESC_DEBUG_EXC)); //$NON-NLS-1$
		text.setImage("profile", lp.get(PDEPluginImages.DESC_PROFILE_EXC)); //$NON-NLS-1$
		section.setClient(text);
	}

	private void createExportingSection(Composite parent, FormToolkit toolkit) {
		Section section = createStaticSection(toolkit, parent, PDEUIMessages.OverviewPage_exportingTitle);
		section.setClient(createClient(section, PDEUIMessages.Product_overview_exporting, toolkit));
	}

	@Override
	public void linkActivated(HyperlinkEvent e) {
		String href = (String) e.getHref();
		if (href.equals("action.synchronize")) { //$NON-NLS-1$
			((ProductLauncherFormPageHelper) getLauncherHelper()).handleSynchronize(true);
		} else if (href.equals("action.export")) { //$NON-NLS-1$
			if (getPDEEditor().isDirty())
				getPDEEditor().doSave(null);
			new ProductExportAction(getPDEEditor()).run();
		} else if (href.equals("configuration")) { //$NON-NLS-1$
			String pageId = ((ProductLauncherFormPageHelper) getLauncherHelper()).getProduct().useFeatures() ? DependenciesPage.FEATURE_ID : DependenciesPage.PLUGIN_ID;
			getEditor().setActivePage(pageId);
		} else if (href.equals("multi-platform-wiki")) { //$NON-NLS-1$
			openBrowser("https://wiki.eclipse.org/Building#Cross-platform_build"); //$NON-NLS-1$
		} else
			super.linkActivated(e);
	}

	private static void openBrowser(String url) {
		// Create the browser
		IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
		IWebBrowser browser;
		try {
			browser = support.createBrowser(null);
		} catch (PartInitException e) {
			PDEPlugin.log(e);
			return;
		}

		try {
			browser.openURL(new URL(url));
		} catch (PartInitException e) {
			PDEPlugin.log(e);
		} catch (MalformedURLException e) {
			PDEPlugin.log(e);
		}
	}

	protected ILauncherFormPageHelper getLauncherHelper() {
		if (fLauncherHelper == null)
			fLauncherHelper = new ProductLauncherFormPageHelper(getPDELauncherEditor());
		return fLauncherHelper;
	}

	@Override
	protected short getIndent() {
		return 35;
	}

}
