/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry;

import org.eclipse.jface.action.*;
import org.eclipse.pde.internal.runtime.*;


/**
 * Action that controls the appearance of the details pane in debug views such
 * as the VariablesView and the ExpressionsView.  Instances of this class can be
 * created to show the detail pane underneath the main tree, to the right of the
 * main tree, or not shown at all.
 * 
 * @since 3.0
 */
public class TogglePropertiesAction extends Action {
	private RegistryBrowser fViewer;
	private int fOrientation;

	public TogglePropertiesAction(RegistryBrowser view, int orientation) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		setRegistryBrowser(view);
		setOrientation(orientation);
				
		if (orientation == RegistryBrowser.VERTICAL_ORIENTATION) {
			setText(PDERuntimeMessages.RegistryView_verticalOrientation_label);  
			setToolTipText(PDERuntimeMessages.RegistryView_verticalOrientation_tooltip);  
			setDescription(PDERuntimeMessages.RegistryView_verticalOrientation_desc);  
			setImageDescriptor(PDERuntimePluginImages.DESC_VERTICAL_VIEW);
			setDisabledImageDescriptor(PDERuntimePluginImages.DESC_VERTICAL_VIEW_DISABLED);
		} else if (orientation == RegistryBrowser.HORIZONTAL_ORIENTATION) {
			setText(PDERuntimeMessages.RegistryView_horizontalOrientation_label);  
			setToolTipText(PDERuntimeMessages.RegistryView_horizontalOrientation_tooltip);  
			setDescription(PDERuntimeMessages.RegistryView_horizontalOrientation_desc);  
			setImageDescriptor(PDERuntimePluginImages.DESC_HORIZONTAL_VIEW);
			setDisabledImageDescriptor(PDERuntimePluginImages.DESC_HORIZONTAL_VIEW_DISABLED);
		} else {
			setText(PDERuntimeMessages.RegistryView_showPropertiesSheet_label);
			setToolTipText(PDERuntimeMessages.RegistryView_showPropertiesSheet_tooltip);  
			setDescription(PDERuntimeMessages.RegistryView_showPropertiesSheet_desc);  
			setImageDescriptor(PDERuntimePluginImages.DESC_HIDE_PANE);
		} 		

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() { // called when menu pressed
		getRegistryBrowser().setViewOrientation(getOrientation()); 
	}
	
	private RegistryBrowser getRegistryBrowser() {
		return fViewer;
	}

	private void setRegistryBrowser(RegistryBrowser view) {
		fViewer = view;
	}

	private void setOrientation(int orientation) {
		fOrientation = orientation;
	}

	public int getOrientation() {
		return fOrientation;
	}
}

