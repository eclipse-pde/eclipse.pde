/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.target;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardSelectionPage;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetPlatformService;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;

/**
 * First wizard page used to create a target definition. Defines the location where
 * the definition will be created and how to seed the definition.
 */
public class TargetCreationPage extends WizardSelectionPage {

	protected static final int USE_EMPTY = 0;
	protected static final int USE_DEFAULT = 1;
	protected static final int USE_CURRENT_TP = 2;
	protected static final int USE_EXISTING_TARGET = 3;

	private Button fEmptyButton;
	private Button fDefaultButton;
	private Button fCurrentTPButton;
	private Button fExistingTargetButton;
	private Combo fTargets;
	private String[] fTargetIds;
	private String templateTargetId;
	private ITargetDefinition[] fTargetDefs = new ITargetDefinition[4];

	public TargetCreationPage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.TargetProfileWizardPage_title);
		setDescription(PDEUIMessages.TargetProfileWizardPage_description);
	}

	/**
	 * Returns the target service or <code>null</code> if none.
	 * 
	 * @return target service or <code>null</code>
	 */
	protected ITargetPlatformService getTargetService() {
		return (ITargetPlatformService) PDECore.getDefault().acquireService(ITargetPlatformService.class.getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH);
		SWTFactory.createLabel(comp, PDEUIMessages.TargetCreationPage_0, 3);

		fEmptyButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_1, 2);
		fDefaultButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_2, 2);
		fCurrentTPButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_3, 2);
		fExistingTargetButton = SWTFactory.createRadioButton(comp, PDEUIMessages.TargetCreationPage_4, 1);
		fExistingTargetButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enabled = fExistingTargetButton.getSelection();
				fTargets.setEnabled(enabled);
			}
		});

		fEmptyButton.setSelection(true);

		fTargets = SWTFactory.createCombo(comp, SWT.SINGLE | SWT.READ_ONLY, 1, GridData.BEGINNING, null);
		fTargets.setEnabled(false);
		initializeTargetCombo();
		fTargets.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(SelectionEvent e) {
				templateTargetId = fTargetIds[fTargets.getSelectionIndex()];

			}
		});

		Dialog.applyDialogFont(comp);
		setSelectedNode(new EditTargetNode());
		setControl(comp);
		setPageComplete(true);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IHelpContextIds.NEW_TARGET_WIZARD);
	}

	protected void initializeTargetCombo() {
		IConfigurationElement[] elements = PDECore.getDefault().getTargetProfileManager().getSortedTargets();
		fTargetIds = new String[elements.length];
		for (int i = 0; i < elements.length; i++) {
			String name = elements[i].getAttribute("name"); //$NON-NLS-1$
			if (fTargets.indexOf(name) == -1)
				fTargets.add(name);
			fTargetIds[i] = elements[i].getAttribute("id"); //$NON-NLS-1$
		}
		if (elements.length > 0) {
			fTargets.select(0);
			setTargetId(fTargetIds[fTargets.getSelectionIndex()]);
		}
	}

	protected int getInitializationOption() {
		if (fEmptyButton.getSelection())
			return USE_EMPTY;
		if (fDefaultButton.getSelection())
			return USE_DEFAULT;
		else if (fCurrentTPButton.getSelection())
			return USE_CURRENT_TP;
		return USE_EXISTING_TARGET;
	}

	protected String getTargetId() {
		return templateTargetId;
	}

	protected void setTargetId(String targetId) {
		templateTargetId = targetId;
	}

	protected ITargetDefinition createTarget(int targetOption) {
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetDefinition definition = service.newTarget();
			switch (targetOption) {
				case USE_EMPTY :
					definition.setName(PDEUIMessages.TargetCreationPage_6);
					break;
				case USE_DEFAULT :
					try {
						populateBasicTarget(definition);
					} catch (CoreException e) {
						setErrorMessage(e.getMessage());
						return null;
					}
					break;
				case USE_CURRENT_TP :
					try {
						populateFromCurrentTargetPlatform(definition);
					} catch (CoreException e) {
						setErrorMessage(e.getMessage());
						return null;
					}
					break;
				case USE_EXISTING_TARGET :
					try {
						populateFromTemplate(definition, getTargetId());
					} catch (CoreException e) {
						setErrorMessage(e.getMessage());
						return null;
					}
					break;
			}
			return definition;
		}
		return null;
	}

	/**
	 * Applies basic target settings to the given target definition.
	 * 
	 * @param definition
	 * @throws CoreException 
	 */
	private void populateBasicTarget(ITargetDefinition definition) throws CoreException {
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			ITargetDefinition def = service.newDefaultTarget();
			service.copyTargetDefinition(def, definition);
		}
	}

	/**
	 * Populates the given definition from current target platform settings.
	 * 
	 * @param definition
	 * @throws CoreException 
	 */
	private void populateFromCurrentTargetPlatform(ITargetDefinition definition) throws CoreException {
		ITargetPlatformService service = getTargetService();
		if (service instanceof TargetPlatformService) {
			TargetPlatformService ts = (TargetPlatformService) service;
			ts.loadTargetDefinitionFromPreferences(definition);
		}
	}

	/**
	 * Populates the given definition from the specified target template.
	 * 
	 * @param definition
	 * @param id target extension identifier
	 * @exception CoreException if unable to complete
	 */
	private void populateFromTemplate(ITargetDefinition definition, String id) throws CoreException {
		ITargetPlatformService service = getTargetService();
		if (service != null) {
			service.loadTargetDefinition(definition, id);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardSelectionPage#getNextPage()
	 */
	public IWizardPage getNextPage() {
		ITargetDefinition target = null;
		int option = getInitializationOption();
		if (fTargetDefs[option] == null) {
			fTargetDefs[option] = createTarget(option);
		}
		target = fTargetDefs[option];
		if (target != null) {
			((NewTargetDefinitionWizard2) getWizard()).setTargetDefinition(target);
			((EditTargetNode) getSelectedNode()).setTargetDefinition(target);
			return super.getNextPage();
		}
		return null;
	}
}
