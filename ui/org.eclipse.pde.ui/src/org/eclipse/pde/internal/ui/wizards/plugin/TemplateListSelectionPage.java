/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.IWizardNode;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.WizardElement;
import org.eclipse.pde.internal.ui.wizards.WizardListSelectionPage;
import org.eclipse.pde.internal.ui.wizards.WizardNode;
import org.eclipse.pde.ui.IBasePluginWizard;
import org.eclipse.pde.ui.IPluginContentWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;


public class TemplateListSelectionPage extends WizardListSelectionPage
		implements ISelectionChangedListener, IExecutableExtension {
	private ContentPage fContentPage;
	private Button fUseTemplate;
	private String fInitialTemplateId;
	
	class WizardFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			PluginFieldData data = (PluginFieldData) fContentPage.getData();
			boolean simple = data.isSimple();
			boolean generate = data.doGenerateClass();
			boolean ui = data.isUIPlugin();
			boolean rcp = data.isRCPApplicationPlugin();
			boolean osgi = data.getOSGiFramework() != null;
			WizardElement welement = (WizardElement)element;
			IConfigurationElement config = welement.getConfigurationElement();
			boolean uiFlag = getFlag(config, "ui-content", true); //$NON-NLS-1$
			boolean javaFlag = getFlag(config, "java", true); //$NON-NLS-1$
			boolean rcpFlag = getFlag(config, "rcp", false); //$NON-NLS-1$
			boolean osgiFlag = getFlag(config, "pureOSGi", false); //$NON-NLS-1$
			boolean activatorFlag = getFlag(config, "requiresActivator", false); //$NON-NLS-1$
			
			//osgi projects need java
			if (osgi && simple) return false;
			//filter out java wizards for simple projects
			if (simple) return !javaFlag;
			//filter out ui wizards for non-ui plug-ins
			if (uiFlag && generate && !ui) return false;
			//filter out wizards that require an activator when the user specifies not to generate a class
			if (activatorFlag && !generate) return false;
			//filter out non-RCP wizard if RCP option is selected
			if (!osgi && (rcp != rcpFlag)) return false;
			//filter out non-UI wizards if UI option is selected for rcp and osgi projects
			return (osgi == osgiFlag && ((!osgiFlag && !rcpFlag) || ui == uiFlag));
		}
		private boolean getFlag(IConfigurationElement config, String name, boolean defaultValue) {
			String value = config.getAttribute(name);
			if (value==null) return defaultValue;
			return value.equalsIgnoreCase("true"); //$NON-NLS-1$
		}
}
	
	public TemplateListSelectionPage(ElementList wizardElements, ContentPage page, String message) {
		super(wizardElements, message);
		fContentPage = page;
		setTitle(PDEUIMessages.WizardListSelectionPage_title); 
		setDescription(PDEUIMessages.WizardListSelectionPage_desc); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.WizardListSelectionPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(getControl(), IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}
	
	public void createAbove(Composite container, int span) {
		fUseTemplate = new Button(container, SWT.CHECK);
		fUseTemplate.setText(PDEUIMessages.WizardListSelectionPage_label); 
		GridData gd = new GridData();
		gd.horizontalSpan = span;
		fUseTemplate.setLayoutData(gd);
		fUseTemplate.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				wizardSelectionViewer.getControl().setEnabled(fUseTemplate.getSelection());
				if (!fUseTemplate.getSelection())
					setDescription(""); //$NON-NLS-1$
				setDescriptionEnabled(fUseTemplate.getSelection());				
				getContainer().updateButtons();
			}
		});
		fUseTemplate.setSelection(true);
	}
	
	protected void initializeViewer() {
		wizardSelectionViewer.addFilter(new WizardFilter());
		if (getInitialTemplateId() != null) 
			selectInitialTemplate();
	}
	
	private void selectInitialTemplate() {
		Object [] children = wizardElements.getChildren();
		for (int i=0; i<children.length; i++) {
			WizardElement welement = (WizardElement)children[i];
			if (welement.getID().equals(getInitialTemplateId())) {
				wizardSelectionViewer.setSelection(new StructuredSelection(welement), true);
				setSelectedNode(createWizardNode(welement));
				setDescriptionText(welement.getDescription());	
				break;
			}
		}
	}
	
	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IPluginContentWizard wizard =
					(IPluginContentWizard) wizardElement.createExecutableExtension();
				wizard.init(fContentPage.getData());
				return wizard;
			}
		};
	}
	
	public IPluginContentWizard getSelectedWizard() {
		if (fUseTemplate.getSelection()) 
			return super.getSelectedWizard();
		return null;
	}
	
	public boolean isPageComplete() {
		PluginFieldData data = (PluginFieldData)fContentPage.getData();
		boolean rcp = data.isRCPApplicationPlugin();
		
		return !rcp || (fUseTemplate.getSelection() && rcp && getSelectedNode() != null);
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardSelectionPage#canFlipToNextPage()
	 */
	public boolean canFlipToNextPage() {
		IStructuredSelection ssel = (IStructuredSelection)wizardSelectionViewer.getSelection();
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
	public void setVisible(boolean visible) {
		if (visible) {
			fContentPage.updateData();
			wizardSelectionViewer.refresh();
		}
		super.setVisible(visible);
	}
}
