/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;


public class TemplateListSelectionPage extends WizardListSelectionPage
		implements ISelectionChangedListener, IExecutableExtension {
	private ContentPage fContentPage;
	private Button fUseTemplate;
	private String fInitialTemplateId;
	
	class WizardFilter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement,
				Object element) {
			IPluginFieldData data = (IPluginFieldData) fContentPage.getData();
			boolean simple = data.isSimple();
			boolean generate = false;
			boolean ui = false;
			if (data instanceof PluginFieldData){
				ui = ((PluginFieldData)data).isUIPlugin();
				generate = ((PluginFieldData)data).doGenerateClass();
			}
			WizardElement welement = (WizardElement)element;
			IConfigurationElement config = welement.getConfigurationElement();
			boolean uiFlag = getFlag(config, "ui-content", true); //$NON-NLS-1$
			boolean javaFlag = getFlag(config, "java", true); //$NON-NLS-1$
			boolean rcpFlag = getFlag(config, "rcp", false);
			
			//filter out java wizards for simple projects
			if (simple && javaFlag) return false;
			//filter out ui wizards for non-ui plug-ins
			if (uiFlag && (simple || (generate && !ui))) return false;
			// filter out non-RCP wizard if RCP option is selected
			if (data.isRCPApplicationPlugin() && !rcpFlag) return false;

			return true;
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
		setTitle(PDEPlugin.getResourceString("WizardListSelectionPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("WizardListSelectionPage.desc")); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.WizardListSelectionPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}
	
	public void createAbove(Composite container, int span) {
		fUseTemplate = new Button(container, SWT.CHECK);
		fUseTemplate.setText(PDEPlugin.getResourceString("WizardListSelectionPage.label")); //$NON-NLS-1$
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
		if (getInitialTemplateId()!=null)
			fUseTemplate.setSelection(true);
	}
	
	protected void initializeViewer() {
		wizardSelectionViewer.addFilter(new WizardFilter());
		if (getInitialTemplateId()==null) {
			wizardSelectionViewer.getControl().setEnabled(false);
			setDescriptionEnabled(false);
		}
		else
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
			return (IPluginContentWizard)super.getSelectedWizard();
		return null;
	}
	
	public boolean isPageComplete() {
		return !fUseTemplate.getSelection() || (fUseTemplate.getSelection() && getSelectedNode() != null);
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
