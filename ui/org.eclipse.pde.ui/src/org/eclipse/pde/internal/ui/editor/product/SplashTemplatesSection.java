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

package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.eclipse.pde.internal.core.iproduct.ISplashInfo;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.FormLayoutFactory;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.pde.internal.ui.editor.PDESection;
import org.eclipse.pde.internal.ui.wizards.product.UpdateSplashHandlerInModelAction;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

/**
 * SplashTemplatesSection
 *
 */
public class SplashTemplatesSection extends PDESection {

	private Section fSection;
	
	private FormToolkit fToolkit;
	
	private Combo fFieldTemplateCombo;
	
	private boolean fBlockNotification;
	
	/**
	 * @param page
	 * @param parent
	 */
	public SplashTemplatesSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		fFieldTemplateCombo = null;
		fBlockNotification = true;
		createClient(getSection(), page.getEditor().getToolkit());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#createClient(org.eclipse.ui.forms.widgets.Section, org.eclipse.ui.forms.widgets.FormToolkit)
	 */
	protected void createClient(Section section, FormToolkit toolkit) {
		// Set globals
		fSection = section;
		fToolkit = toolkit;
		// Configure the section
		configureUISection();
		// Create the UI
		createUI();
		// Create the Listeners
		createUIListeners();
		// Note: Rely on refresh method to update the UI
	}

	/**
	 * 
	 */
	private void updateUI() {
		// Block fired events when updating the UI
		fBlockNotification = true;
		updateUIFieldTemplateCombo();
		fBlockNotification = false;
	}

	/**
	 * 
	 */
	private void updateUIFieldTemplateCombo() {
		// Get the splash info if any
		ISplashInfo info = getSplashInfo();
		if (info.isDefinedSplashHandlerType() == false) {
			// No splash handler type defined, set "none" in combo box
			fFieldTemplateCombo.setText(PDEUIMessages.SimpleCSCommandDetails_6);
			return;
		} 
		String splashHandlerType = info.getFieldSplashHandlerType();
		// Update the splash handler type in the combo box
		for (int i = 0; i < UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			String key = UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES[i][0];
			if (splashHandlerType.equals(key)) {
				String displayName = UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES[i][1];
				fFieldTemplateCombo.setText(displayName);
			}
		}
	}

	/**
	 * 
	 */
	private void createUIListeners() {
		// Create listener for the combo box
		createUIListenerFieldTemplateCombo();
	}

	/**
	 * 
	 */
	private void createUIListenerFieldTemplateCombo() {
		fFieldTemplateCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleTemplateComboWidgetSelected();
			}
		});
	}

	/**
	 * 
	 */
	private void handleTemplateComboWidgetSelected() {
		// Ignore event if notifications are blocked
		if (fBlockNotification) {
			return;
		}
		// Set the splash handler type in the model
		getSplashInfo().setFieldSplashHandlerType(getSelectedTemplate(), false);
		// TODO: MP: SPLASH: Implement functionality to remove existing splash handler type if "none" is selected
	}

	/**
	 * @return the associated key of the item selected in the combo box
	 */
	private String getSelectedTemplate() {
		int index = fFieldTemplateCombo.getSelectionIndex();
		int position = index - 1;
		if ((index <= 0) ||
				(index > UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES.length)) {
			return null;
		}
		return UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES[position][0];
	}	
	
	/**
	 * 
	 */
	private void createUI() {
		// Create the container
		Composite container = createUISectionContainer(fSection);
		// Create the label
		createUILabelType(container);
		// Create the template field
		createUIFieldTemplateCombo(container);
		// Paint the borders for the container
		fToolkit.paintBordersFor(container);
		// Set the container as the section client
		fSection.setClient(container);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);			
	}

	/**
	 * @param parent
	 */
	private void createUILabelType(Composite parent) {
		Color foreground = fToolkit.getColors().getColor(IFormColors.TITLE);		
		Label label = fToolkit.createLabel(parent, 
				PDEUIMessages.SplashTemplatesSection_typeName, SWT.WRAP);
		label.setForeground(foreground);		
	}	
	
	/**
	 * @param parent
	 */
	private void createUIFieldTemplateCombo(Composite parent) {
		int style = SWT.READ_ONLY | SWT.BORDER;
		fFieldTemplateCombo = new Combo(parent, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fFieldTemplateCombo.setLayoutData(data);
		// Add "none" element
		fFieldTemplateCombo.add(PDEUIMessages.SimpleCSCommandDetails_6, 0);
		// Add all splash screen types in exact order found
		for (int i = 0; i < UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			int position = i + 1;
			fFieldTemplateCombo.add(
					UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES[i][1], 
					position);
		}
	}
	
	/**
	 * @return
	 */
	private IProductModel getModel() {
		return (IProductModel)getPage().getPDEEditor().getAggregateModel();
	}	
	
	/**
	 * @param parent
	 * @return
	 */
	private Composite createUISectionContainer(Composite parent) {
		Composite client = fToolkit.createComposite(fSection);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, 2));
		client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));		
		return client;
	}		
	
	/**
	 * 
	 */
	private void configureUISection() {
		fSection.setLayout(FormLayoutFactory.createClearGridLayout(false, 1));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fSection.setLayoutData(data);			
		fSection.setText(PDEUIMessages.SplashTemplatesSection_templatesName); 
		fSection.setDescription(PDEUIMessages.SplashTemplatesSection_templatesSectionDesc); 		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		// Update the UI
		updateUI();
		super.refresh();
	}
	
	/**
	 * @return
	 */
	private IProduct getProduct() {
		return getModel().getProduct();
	}	
	
	/**
	 * @return
	 */
	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
 		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
 			handleModelEventWorldChanged(e);
 		}		
	}

	/**
	 * @param event
	 */
	private void handleModelEventWorldChanged(IModelChangedEvent event) {
		refresh();
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#dispose()
	 */
	public void dispose() {
		IProductModel model = getModel();
		if (model != null) {
			model.removeModelChangedListener(this);
		}
		super.dispose();
	}		
	
}
