/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet;

import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.cheatsheet.RegisterCSWizard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormText;

/**
 * CompCSDeployDetails
 *
 */
public class CSRegisterCSDetails implements ICSDetails {

	private ICSDetailsSurrogate fDetails;	
	
	private FormText fRegisterCSFormText;
	
	private IModel fModel;
	
	/**
	 * 
	 */
	public CSRegisterCSDetails(ICSDetailsSurrogate details, IModel model) {
		fDetails = details;
		fModel = model;
		
		fRegisterCSFormText = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#createDetails(org.eclipse.swt.widgets.Composite)
	 */
	public void createDetails(Composite parent) {
		// Create the container for the hyperlink
		Composite container = fDetails.getToolkit().createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginTop = 10;
		layout.marginLeft = 8;
		container.setLayout(layout);
		// Create the register cheat sheet form text
		createUIRegisterCSFormText(container);
	}

	/**
	 * @param parent
	 */
	private void createUIRegisterCSFormText(Composite parent) {
		fRegisterCSFormText = fDetails.getToolkit().createFormText(parent, true);
		fRegisterCSFormText.setImage("register", PDEPlugin.getDefault().getLabelProvider().get( //$NON-NLS-1$
				PDEPluginImages.DESC_DEPLOYCS_TOOL));
		String text = PDEUIMessages.CSRegisterCSDetails_linkFormattingTags;
		fRegisterCSFormText.setText(text, true, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#hookListeners()
	 */
	public void hookListeners() {
		// Create listeners for the register cheat sheet form text
		createListenersRegisterCSFormText();
	}

	/**
	 * 
	 */
	private void createListenersRegisterCSFormText() {
		fRegisterCSFormText.addHyperlinkListener(new HyperlinkAdapter() {
			public void linkActivated(HyperlinkEvent e) {
				handleLinkActivatedRegisterCS();
			}
			public void linkEntered(HyperlinkEvent e) {
				handleLinkEntryRegisterCS(e.getLabel());
			}
			public void linkExited(HyperlinkEvent e) {
				handleLinkEntryRegisterCS(null);
			}
		});			
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.cheatsheet.ICSDetails#updateFields()
	 */
	public void updateFields() {
		// NO-OP
	}

	/**
	 * 
	 */
	private void handleLinkActivatedRegisterCS() {
		RegisterCSWizard wizard = 
			new RegisterCSWizard(fModel);
		//
		wizard.init(PlatformUI.getWorkbench(), null);
		// Create the dialog for the wizard
		WizardDialog dialog = 
			new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		SWTUtil.setDialogSize(dialog, 400, 300);
		// Check the result
		if (dialog.open() == Window.OK) {
			
		}			
	}	

	/**
	 * @param message
	 */
	private void handleLinkEntryRegisterCS(String message) {
		IEditorSite site = fDetails.getPage().getEditor().getEditorSite();
		site.getActionBars().getStatusLineManager().setMessage(message);
	}	
	
}
