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
	public static final String SHOW_PROPERTIES_SHEET = "RegistryView.showPropertiesSheet.label"; //$NON-NLS-1$
	public static final String SHOW_PROPERTIES_SHEET_TOOLTIP = "RegistryView.showPropertiesSheet.tooltip"; //$NON-NLS-1$
	public static final String SHOW_PROPERTIES_SHEET_DESC = "RegistryView.showPropertiesSheet.desc"; //$NON-NLS-1$
	
	public static final String USE_VERTICAL_ORIENTATION = "RegistryView.verticalOrientation.label"; //$NON-NLS-1$
	public static final String USE_VERTICAL_ORIENTATION_TOOLTIP = "RegistryView.verticalOrientation.tooltip"; //$NON-NLS-1$
	public static final String USE_VERTICAL_ORIENTATION_DESC = "RegistryView.verticalOrientation.desc"; //$NON-NLS-1$
	
	public static final String USE_HORIZONTAL_ORIENTATION = "RegistryView.horizontalOrientation.label"; //$NON-NLS-1$
	public static final String USE_HORIZONTAL_ORIENTATION_TOOLTIP = "RegistryView.horizontalOrientation.tooltip"; //$NON-NLS-1$
	public static final String USE_HORIZONTAL_ORIENTATION_DESC = "RegistryView.horizontalOrientation.desc"; //$NON-NLS-1$
	
	private RegistryBrowser fViewer;
	private int fOrientation;

	public TogglePropertiesAction(RegistryBrowser view, int orientation) {
		super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		setRegistryBrowser(view);
		setOrientation(orientation);
				
		if (orientation == RegistryBrowser.VERTICAL_ORIENTATION) {
			setText(PDERuntimePlugin.getResourceString(USE_VERTICAL_ORIENTATION));  //$NON-NLS-1$
			setToolTipText(PDERuntimePlugin.getResourceString(USE_VERTICAL_ORIENTATION_TOOLTIP));  //$NON-NLS-1$
			setDescription(PDERuntimePlugin.getResourceString(USE_VERTICAL_ORIENTATION_DESC));  //$NON-NLS-1$
			setImageDescriptor(PDERuntimePluginImages.DESC_VERTICAL_VIEW);
			setDisabledImageDescriptor(PDERuntimePluginImages.DESC_VERTICAL_VIEW_DISABLED);
		} else if (orientation == RegistryBrowser.HORIZONTAL_ORIENTATION) {
			setText(PDERuntimePlugin.getResourceString(USE_HORIZONTAL_ORIENTATION));  //$NON-NLS-1$
			setToolTipText(PDERuntimePlugin.getResourceString(USE_HORIZONTAL_ORIENTATION_TOOLTIP));  //$NON-NLS-1$
			setDescription(PDERuntimePlugin.getResourceString(USE_HORIZONTAL_ORIENTATION_DESC));  //$NON-NLS-1$
			setImageDescriptor(PDERuntimePluginImages.DESC_HORIZONTAL_VIEW);
			setDisabledImageDescriptor(PDERuntimePluginImages.DESC_HORIZONTAL_VIEW_DISABLED);
		} else {
			setText(PDERuntimePlugin.getResourceString(SHOW_PROPERTIES_SHEET));
			setToolTipText(PDERuntimePlugin.getResourceString(SHOW_PROPERTIES_SHEET_TOOLTIP));  //$NON-NLS-1$
			setDescription(PDERuntimePlugin.getResourceString(SHOW_PROPERTIES_SHEET_DESC));  //$NON-NLS-1$
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

