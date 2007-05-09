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

package org.eclipse.pde.internal.ui.editor.cheatsheet;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.cheatsheet.RegisterCSWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.ScrolledForm;

/**
 * CSAbstractPage
 *
 */
public abstract class CSAbstractPage extends PDEFormPage {

	private ImageHyperlink fImageHyperlinkRegisterCS;
	
	private FormToolkit fToolkit;
	
	private IModel fModel;
	
	/**
	 * @param editor
	 * @param id
	 * @param title
	 */
	public CSAbstractPage(FormEditor editor, String id, String title) {
		super(editor, id, title);
	}

	/**
	 * @param managedForm
	 * @param model
	 */
	protected void createUIFormTitleRegisterCSLink(IManagedForm managedForm,
			IModel model) {
		// Set globals
		fToolkit = managedForm.getToolkit();
		fModel = model;
		// Add the register cheat sheet link to the form title area
		ScrolledForm form = managedForm.getForm();
		if (fModel.isEditable()) {
			form.getToolBarManager().add(createUIControlConRegisterCS());
			form.getToolBarManager().update(true);
		}
	}
	
	/**
	 * @return
	 */
	private ControlContribution createUIControlConRegisterCS() {
		return new ControlContribution("Register") { //$NON-NLS-1$
			protected Control createControl(Composite parent) {
				// Create UI
				createUIImageHyperlinkRegisterCS(parent);
				// Create Listener
				createUIListenerImageHyperlinkRegisterCS();
				return fImageHyperlinkRegisterCS;
			}
		};			
	}

	/**
	 * @param parent
	 */
	private void createUIImageHyperlinkRegisterCS(Composite parent) {
		fImageHyperlinkRegisterCS = new ImageHyperlink(parent, SWT.NONE);
		fImageHyperlinkRegisterCS.setText(
				PDEUIMessages.CSAbstractPage_msgRegisterThisCheatSheet);
		fImageHyperlinkRegisterCS.setUnderlined(true);
		fImageHyperlinkRegisterCS.setForeground(
				fToolkit.getHyperlinkGroup().getForeground());
	}

	/**
	 * 
	 */
	private void createUIListenerImageHyperlinkRegisterCS() {
		fImageHyperlinkRegisterCS.addHyperlinkListener(new IHyperlinkListener() {
			public void linkActivated(HyperlinkEvent e) {
				handleLinkActivatedRegisterCS();
			}
			public void linkEntered(HyperlinkEvent e) {
				handleLinkEnteredRegisterCS(e.getLabel());
			}
			public void linkExited(HyperlinkEvent e) {
				handleLinkExitedRegisterCS();
			}
		});	
	}
	
	/**
	 * @param message
	 */
	private void handleLinkEnteredRegisterCS(String message) {
		// Update colour
		fImageHyperlinkRegisterCS.setForeground(
				fToolkit.getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(message);
	}	
	
	/**
	 *
	 */
	private void handleLinkExitedRegisterCS() {
		// Update colour
		fImageHyperlinkRegisterCS.setForeground(
				fToolkit.getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditor().getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}		
	
	/**
	 * 
	 */
	private void handleLinkActivatedRegisterCS() {
		RegisterCSWizard wizard = new RegisterCSWizard(fModel);
		// Initialize the wizard
		wizard.init(PlatformUI.getWorkbench(), null);
		// Create the dialog for the wizard
		WizardDialog dialog = 
			new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
		dialog.create();
		// Configure the dialogs size
		SWTUtil.setDialogSize(dialog, 400, 300);
		// Check the result
		if (dialog.open() == Window.OK) {
			// TODO: MP: COMPCS: HIGH: Automatic save of editor after creating simple cheat sheet?
		}			
	}	
	
}
