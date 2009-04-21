/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.editor.cheatsheet;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ua.ui.wizards.cheatsheet.RegisterCSWizard;
import org.eclipse.pde.internal.ui.editor.MultiSourceEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.ImageHyperlink;

public abstract class CSAbstractEditor extends MultiSourceEditor {

	private ImageHyperlink fImageHyperlinkRegisterCS;

	public void contributeToToolbar(IToolBarManager manager) {
		// Add the register cheat sheet link to the form title area
		if (WorkspaceModelManager.isPluginProject(getCommonProject())
				&& getAggregateModel().isEditable())
			manager.add(createUIControlConRegisterCS());
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
		fImageHyperlinkRegisterCS.setText(Messages.CSAbstractEditor_register);
		fImageHyperlinkRegisterCS.setUnderlined(true);
		fImageHyperlinkRegisterCS.setForeground(getToolkit()
				.getHyperlinkGroup().getForeground());
	}

	/**
	 * 
	 */
	private void createUIListenerImageHyperlinkRegisterCS() {
		fImageHyperlinkRegisterCS
				.addHyperlinkListener(new IHyperlinkListener() {
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
		fImageHyperlinkRegisterCS.setForeground(getToolkit()
				.getHyperlinkGroup().getActiveForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager().setMessage(
				message);
	}

	/**
	 *
	 */
	private void handleLinkExitedRegisterCS() {
		// Update colour
		fImageHyperlinkRegisterCS.setForeground(getToolkit()
				.getHyperlinkGroup().getForeground());
		// Update IDE status line
		getEditorSite().getActionBars().getStatusLineManager().setMessage(null);
	}

	/**
	 * 
	 */
	private void handleLinkActivatedRegisterCS() {
		RegisterCSWizard wizard = new RegisterCSWizard(
				(IModel) getAggregateModel());
		// Initialize the wizard
		wizard.init(PlatformUI.getWorkbench(), null);
		// Create the dialog for the wizard
		WizardDialog dialog = new WizardDialog(PDEUserAssistanceUIPlugin
				.getActiveWorkbenchShell(), wizard);
		dialog.create();
		// Configure the dialogs size
		dialog.getShell().setSize(400, 370);
		// Check the result
		if (dialog.open() == Window.OK) {
			// TODO: MP: COMPCS: HIGH: Automatic save of editor after creating
			// simple cheat sheet?
		}
	}

}
