/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.logview;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.Assert;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.pde.internal.runtime.PDERuntimePlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.internal.WorkbenchMessages;


public class EvenDetailsDialogAction extends SelectionProviderAction{

	/**
	 * The shell in which to open the property dialog
	 */
	private Shell shell;
	private ISelectionProvider provider;
	private EventDetailsDialog propertyDialog;
	/**
	 * Creates a new action for opening a property dialog
	 * on the elements from the given selection provider
	 * @param shell - the shell in which the dialog will open
	 * @param provider - the selection provider whose elements
	 * the property dialog will describe
	 */
	public EvenDetailsDialogAction(Shell shell, ISelectionProvider provider){
		super(provider, WorkbenchMessages.getString("PropertyDialog.text"));
		Assert.isNotNull(shell);
		this.shell = shell;
		this.provider = provider;
		// setToolTipText
		//WorkbenchHelp.setHelp
	}
	
	public void resetSelection(byte sortType, int sortOrder){
		IAdaptable element = (IAdaptable) getStructuredSelection().getFirstElement();
		if (element == null)
			return;
		if (propertyDialog != null && propertyDialog.isOpen())
			propertyDialog.resetSelection(element, sortType, sortOrder);
	}
	public void resetSelection(){
		IAdaptable element = (IAdaptable) getStructuredSelection().getFirstElement();
		if (element == null)
			return;
		if (propertyDialog != null && propertyDialog.isOpen())
			propertyDialog.resetSelection(element);
	}
	
	public void run(){
		if (propertyDialog != null && propertyDialog.isOpen()){
			resetSelection();
			return;
		}
		
		//get initial selection
		IAdaptable element = (IAdaptable) getStructuredSelection().getFirstElement();
		if (element == null)
			return;
		
		propertyDialog = new EventDetailsDialog(shell, element, provider);
		propertyDialog.create();
		propertyDialog.getShell().setText(PDERuntimePlugin.getResourceString("EventDetailsDialog.title"));
		propertyDialog.getShell().setSize(500,550);
		propertyDialog.open();
	}
}
