/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Jakub Jurkiewicz <jakub.jurkiewicz@pl.ibm.com> - bug 185995
 *     Rudiger Herrmann <rherrmann@innoopract.com> - bug 249707
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

public class TemplateListSelectionPage extends WizardListSelectionPage {
	private ContentPage fContentPage;
	private Button fUseTemplate;
	private String fInitialTemplateId;

	class WizardFilter extends ViewerFilter {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			PluginFieldData data = (PluginFieldData) fContentPage.getData();
			boolean simple = data.isSimple();
			boolean generate = data.doGenerateClass();
			boolean ui = data.isUIPlugin();
			boolean rcp = data.isRCPApplicationPlugin();
			boolean osgi = data.getOSGiFramework() != null;
			WizardElement welement = (WizardElement) element;
			boolean active = TemplateWizardHelper.isActive(welement);
			boolean uiFlag = TemplateWizardHelper.getFlag(welement, TemplateWizardHelper.FLAG_UI, true);
			boolean javaFlag = TemplateWizardHelper.getFlag(welement, TemplateWizardHelper.FLAG_JAVA, true);
			boolean rcpFlag = TemplateWizardHelper.getFlag(welement, TemplateWizardHelper.FLAG_RCP, false);
			boolean osgiFlag = TemplateWizardHelper.getFlag(welement, TemplateWizardHelper.FLAG_OSGI, false);
			boolean activatorFlag = TemplateWizardHelper.getFlag(welement, TemplateWizardHelper.FLAG_ACTIVATOR, false);

			//filter out wizards from disabled activities
			if (!active)
				return false;
			//osgi projects need java
			if (osgi && simple)
				return false;
			//filter out java wizards for simple projects
			if (simple)
				return !javaFlag;
			//filter out ui wizards for non-ui plug-ins
			if (uiFlag && !ui)
				return false;
			//filter out wizards that require an activator when the user specifies not to generate a class
			if (activatorFlag && !generate)
				return false;
			//filter out non-RCP wizard if RCP option is selected
			if (!osgi && (rcp != rcpFlag))
				return false;
			//filter out non-UI wizards if UI option is selected for rcp and osgi projects
			return (osgi == osgiFlag && ((!osgiFlag && !rcpFlag) || ui == uiFlag));
		}

	}

	/**
	 * Constructor
	 * @param wizardElements a list of TemplateElementWizard objects
	 * @param page content wizard page
	 * @param message message to provide to the user
	 */
	public TemplateListSelectionPage(ElementList wizardElements, ContentPage page, String message) {
		super(wizardElements, message);
		fContentPage = page;
		setTitle(PDEUIMessages.WizardListSelectionPage_title);
		setDescription(PDEUIMessages.WizardListSelectionPage_desc);
	}

	@Override
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}

	@Override
	public void createAbove(Composite container, int span) {
		fUseTemplate = new Button(container, SWT.CHECK);
		fUseTemplate.setText(PDEUIMessages.WizardListSelectionPage_label);
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		fUseTemplate.setLayoutData(gd);
		fUseTemplate.addSelectionListener(widgetSelectedAdapter(e -> {
			wizardSelectionViewer.getControl().setEnabled(fUseTemplate.getSelection());
			if (!fUseTemplate.getSelection())
				setDescription(""); //$NON-NLS-1$
			else
				setDescription(PDEUIMessages.WizardListSelectionPage_desc);
			setDescriptionEnabled(fUseTemplate.getSelection());
			getContainer().updateButtons();
		}));
		fUseTemplate.setSelection(false);
	}

	@Override
	protected void initializeViewer() {
		wizardSelectionViewer.addFilter(new WizardFilter());
		if (getInitialTemplateId() != null)
			selectInitialTemplate();
	}

	private void selectInitialTemplate() {
		Object[] children = wizardElements.getChildren();
		for (Object child : children) {
			WizardElement welement = (WizardElement) child;
			if (welement.getID().equals(getInitialTemplateId())) {
				wizardSelectionViewer.setSelection(new StructuredSelection(welement), true);
				setSelectedNode(createWizardNode(welement));
				setDescriptionText(welement.getDescription());
				break;
			}
		}
	}

	@Override
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			@Override
			public IBasePluginWizard createWizard() throws CoreException {
				IPluginContentWizard wizard = (IPluginContentWizard) wizardElement.createExecutableExtension();
				wizard.init(fContentPage.getData());
				return wizard;
			}
		};
	}

	@Override
	public IPluginContentWizard getSelectedWizard() {
		if (fUseTemplate.getSelection())
			return super.getSelectedWizard();
		return null;
	}

	@Override
	public boolean isPageComplete() {
		PluginFieldData data = (PluginFieldData) fContentPage.getData();
		boolean rcp = data.isRCPApplicationPlugin();

		return !rcp || (fUseTemplate.getSelection() && rcp && getSelectedNode() != null);
	}

	@Override
	public boolean canFlipToNextPage() {
		IStructuredSelection ssel = wizardSelectionViewer.getStructuredSelection();
		return fUseTemplate.getSelection() && ssel != null && !ssel.isEmpty();
	}

	/**
	 * @return Returns the fInitialTemplateId.
	 */
	public String getInitialTemplateId() {
		return fInitialTemplateId;
	}

	/**
	 * @param initialTemplateId The fInitialTemplateId to set.
	 */
	public void setInitialTemplateId(String initialTemplateId) {
		fInitialTemplateId = initialTemplateId;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible) {
			fContentPage.updateData();
			if (((PluginFieldData) fContentPage.getData()).isRCPApplicationPlugin()) {
				fUseTemplate.setSelection(true);
				fUseTemplate.setEnabled(false);
				wizardSelectionViewer.getControl().setEnabled(true);

			} else {
				if (fUseTemplate.getSelection() == false)
					wizardSelectionViewer.getControl().setEnabled(false);
				fUseTemplate.setEnabled(true);
			}
			wizardSelectionViewer.refresh();
		}
		super.setVisible(visible);
	}

	/**
	 * @return Returns <code>false</code> if no Template is available,
	 * and <code>true</code> otherwise.
	 */
	public boolean isAnyTemplateAvailable() {
		if (wizardSelectionViewer != null) {
			wizardSelectionViewer.refresh();
			Object firstElement = wizardSelectionViewer.getElementAt(0);
			if (firstElement != null) {
				return true;
			}
		}
		return false;
	}
}
