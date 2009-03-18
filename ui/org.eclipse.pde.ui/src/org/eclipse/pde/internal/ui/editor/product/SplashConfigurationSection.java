/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.jface.fieldassist.*;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.product.SplashInfo;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.parts.ComboPart;
import org.eclipse.pde.internal.ui.wizards.product.ISplashHandlerConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

public class SplashConfigurationSection extends PDESection {

	private static final int F_NUM_COLUMNS = 10;

	private ColorSelector fColorSelector;
	private boolean fBlockNotification;

	private Button fAddBarButton;
	// spinners controlling the progress bar geometry
	private Spinner[] fBarSpinners = new Spinner[4];
	// all swt controls under the progress bar checkbox
	private Control[] fBarControls = new Control[8];

	private Button fAddMessageButton;
	// spinners controlling the progress message geometry
	private Spinner[] fMessageSpinners = new Spinner[4];
	// all swt controls under the progress message checkbox
	private Control[] fMessageControls = new Control[10];

	private Section fSection;

	private FormToolkit fToolkit;

	private ComboPart fFieldTemplateCombo;

	private ControlDecoration fControlDecoration;

	public SplashConfigurationSection(PDEFormPage page, Composite parent) {
		super(page, parent, Section.DESCRIPTION);
		fFieldTemplateCombo = null;
		fControlDecoration = null;
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
		// Create listeners for the combo box
		createUIListenersFieldTemplateCombo();
		// Note: Rely on refresh method to update the UI
	}

	/**
	 * 
	 */
	private void createUIListenersFieldTemplateCombo() {
		// Selection listener
		fFieldTemplateCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleTemplateComboWidgetSelected();
			}
		});
		// Focus listener
		fFieldTemplateCombo.getControl().addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				double currentTarget = TargetPlatformHelper.getTargetVersion();
				if (currentTarget <= 3.2) {
					fControlDecoration.show();
				}
			}

			public void focusLost(FocusEvent e) {
				fControlDecoration.hide();
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
		String template = getSelectedTemplate();
		getSplashInfo().setFieldSplashHandlerType(template, false);
		// Update this sections enablement
		updateFieldEnablement();
	}

	/**
	 * @return the associated key of the item selected in the combo box
	 */
	private String getSelectedTemplate() {
		int index = fFieldTemplateCombo.getSelectionIndex();
		int position = index - 1;
		if ((index <= 0) || (index > ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES.length)) {
			return null;
		}
		return ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[position][0];
	}

	/**
	 * 
	 */
	private void createUI() {
		// Create the container
		Composite container = createUISectionContainer(fSection);
		// Create the template field label
		createUILabelType(container);
		// Create the template field 
		createUIFieldTemplateCombo(container);
		// Create the template field decoration
		createUIFieldDecorationTemplate();
		// Create the progress field label
		createUILabelProgress(container);
		// Create the progress bar fields
		createProgressBarConfig(container);
		// Create the message bar fields
		createProgressMessageConfig(container);
		// Paint the borders for the container
		fToolkit.paintBordersFor(container);
		// Set the container as the section client
		fSection.setClient(container);
		// Register to be notified when the model changes
		getModel().addModelChangedListener(this);
	}

	/**
	 * 
	 */
	private void createUIFieldDecorationTemplate() {
		// Decorate the combo with the info image
		int bits = SWT.TOP | SWT.LEFT;
		fControlDecoration = new ControlDecoration(fFieldTemplateCombo.getControl(), bits);
		// Configure decoration
		// No margin
		fControlDecoration.setMarginWidth(0);
		// Custom hover tip text
		fControlDecoration.setDescriptionText(PDEUIMessages.SplashConfigurationSection_msgDecorationTemplateSupport);
		// Custom hover properties
		fControlDecoration.setShowHover(true);
		// Hover image to use
		FieldDecoration contentProposalImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION);
		fControlDecoration.setImage(contentProposalImage.getImage());
		// Hide the decoration initially
		fControlDecoration.hide();
	}

	/**
	 * @param parent
	 */
	private void createUILabelType(Composite parent) {
		Color foreground = fToolkit.getColors().getColor(IFormColors.TITLE);
		Label label = fToolkit.createLabel(parent, PDEUIMessages.SplashTemplatesSection_typeName, SWT.WRAP);
		label.setForeground(foreground);
	}

	/**
	 * @param parent
	 */
	private void createUILabelProgress(Composite parent) {
		Label label = fToolkit.createLabel(parent, PDEUIMessages.SplashConfigurationSection_sectionDescCustomization, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = F_NUM_COLUMNS;
		data.verticalIndent = 5;
		label.setLayoutData(data);
	}

	/**
	 * @param parent
	 */
	private void createUIFieldTemplateCombo(Composite parent) {
		int style = SWT.READ_ONLY | SWT.BORDER;
		fFieldTemplateCombo = new ComboPart();
		fFieldTemplateCombo.createControl(parent, fToolkit, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = F_NUM_COLUMNS - 1;
		fFieldTemplateCombo.getControl().setLayoutData(data);
		// Add "none" element
		fFieldTemplateCombo.add(PDEUIMessages.SplashConfigurationSection_none, 0);
		// Add all splash screen types in exact order found
		for (int i = 0; i < ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			int position = i + 1;
			fFieldTemplateCombo.add(ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[i][1], position);
		}
	}

	/**
	 * @param parent
	 */
	private Composite createUISectionContainer(Composite parent) {
		Composite client = fToolkit.createComposite(fSection);
		client.setLayout(FormLayoutFactory.createSectionClientGridLayout(false, F_NUM_COLUMNS));
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
		fSection.setText(PDEUIMessages.SplashProgressSection_progressName);
		fSection.setDescription(PDEUIMessages.SplashProgressSection_progressSectionDesc);
	}

	private void createProgressBarConfig(Composite parent) {
		fAddBarButton = createButton(parent, fToolkit, PDEUIMessages.SplashSection_progressBar);
		fAddBarButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enable = fAddBarButton.getSelection();
				getSplashInfo().addProgressBar(enable, false);
				updateFieldEnablement();
			}
		});
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.verticalIndent = 5;
		data.horizontalSpan = F_NUM_COLUMNS;
		fAddBarButton.setLayoutData(data);
		fAddBarButton.setEnabled(isEditable());

		Color foreground = fToolkit.getColors().getColor(IFormColors.TITLE);

		fBarControls[0] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_progressX);
		fBarControls[1] = fBarSpinners[0] = createSpinner(parent, fToolkit);
		fBarControls[2] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_progressY);
		fBarControls[3] = fBarSpinners[1] = createSpinner(parent, fToolkit);
		fBarControls[4] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_progressWidth);
		fBarControls[5] = fBarSpinners[2] = createSpinner(parent, fToolkit);
		fBarControls[6] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_progressHeight);
		fBarControls[7] = fBarSpinners[3] = createSpinner(parent, fToolkit);
		// Add tooltips to coordinate controls
		addOffsetTooltips(fBarControls);

		for (int i = 0; i < fBarSpinners.length; i++) {
			fBarSpinners[i].setEnabled(isEditable());
			fBarSpinners[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					applySpinners(true);
				}
			});
		}

		Composite filler = fToolkit.createComposite(parent);
		filler.setLayout(new GridLayout());
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		filler.setLayoutData(gd);
	}

	private void createProgressMessageConfig(Composite parent) {
		fAddMessageButton = createButton(parent, fToolkit, PDEUIMessages.SplashSection_progressMessage);
		fAddMessageButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean enable = fAddMessageButton.getSelection();
				getSplashInfo().addProgressMessage(enable, false);
				updateFieldEnablement();
			}
		});
		fAddMessageButton.setEnabled(false);

		Color foreground = fToolkit.getColors().getColor(IFormColors.TITLE);

		fMessageControls[0] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_messageX);
		fMessageControls[1] = fMessageSpinners[0] = createSpinner(parent, fToolkit);
		fMessageControls[2] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_messageY);
		fMessageControls[3] = fMessageSpinners[1] = createSpinner(parent, fToolkit);

		fMessageControls[4] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_messageWidth);
		fMessageControls[5] = fMessageSpinners[2] = createSpinner(parent, fToolkit);
		fMessageControls[6] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_messageHeight);
		fMessageControls[7] = fMessageSpinners[3] = createSpinner(parent, fToolkit);

		fMessageControls[8] = createLabel(parent, fToolkit, foreground, PDEUIMessages.SplashSection_messageColor);
		fColorSelector = new ColorSelector(parent);
		fColorSelector.addListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				if (!event.getNewValue().equals(event.getOldValue()))
					applyColor();
			}
		});
		fToolkit.adapt(fColorSelector.getButton(), true, true);
		fMessageControls[9] = fColorSelector.getButton();
		// Add tooltips to coordinate controls
		addOffsetTooltips(fMessageControls);

		for (int i = 0; i < fMessageSpinners.length; i++) {
			fMessageSpinners[i].setEnabled(isEditable());
			fMessageSpinners[i].addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					applySpinners(false);
				}
			});
		}
	}

	private void applyColor() {
		if (fBlockNotification)
			return;
		RGB rgb = fColorSelector.getColorValue();
		getSplashInfo().setForegroundColor(rgbToHEX(rgb), false);
	}

	private void applySpinners(boolean bar) {
		if (fBlockNotification)
			return;
		Spinner[] spinners = bar ? fBarSpinners : fMessageSpinners;
		int[] geo = new int[] {spinners[0].getSelection(), spinners[1].getSelection(), spinners[2].getSelection(), spinners[3].getSelection()};
		if (bar)
			getSplashInfo().setProgressGeometry(geo, false);
		else
			getSplashInfo().setMessageGeometry(geo, false);
	}

	private Label createLabel(Composite parent, FormToolkit toolkit, Color color, String labelName) {
		Label label = toolkit.createLabel(parent, labelName);
		label.setForeground(color);
		GridData gd = new GridData();
		gd.horizontalIndent = 10;
		label.setLayoutData(gd);
		return label;
	}

	private Button createButton(Composite parent, FormToolkit toolkit, String label) {
		Button button = toolkit.createButton(parent, label, SWT.CHECK);
		GridData gd = new GridData();
		gd.horizontalSpan = F_NUM_COLUMNS;
		button.setLayoutData(gd);
		return button;
	}

	private Spinner createSpinner(Composite parent, FormToolkit toolkit) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		spinner.setMinimum(0);
		spinner.setMaximum(9999);
		toolkit.adapt(spinner, false, false);
		return spinner;
	}

	/**
	 * 
	 */
	private void resetProgressBarGeometry() {
		// X Offset
		fBarSpinners[0].setSelection(SplashInfo.F_DEFAULT_BAR_X_OFFSET);
		// Y Offset
		fBarSpinners[1].setSelection(SplashInfo.F_DEFAULT_BAR_Y_OFFSET);
		// Width
		fBarSpinners[2].setSelection(SplashInfo.F_DEFAULT_BAR_WIDTH);
		// Height
		fBarSpinners[3].setSelection(SplashInfo.F_DEFAULT_BAR_HEIGHT);
	}

	/**
	 * 
	 */
	private void resetProgressMessageGeometry() {
		// X Offset
		fMessageSpinners[0].setSelection(SplashInfo.F_DEFAULT_MESSAGE_X_OFFSET);
		// Y Offset
		fMessageSpinners[1].setSelection(SplashInfo.F_DEFAULT_MESSAGE_Y_OFFSET);
		// Width
		fMessageSpinners[2].setSelection(SplashInfo.F_DEFAULT_MESSAGE_WIDTH);
		// Height
		fMessageSpinners[3].setSelection(SplashInfo.F_DEFAULT_MESSAGE_HEIGHT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.forms.AbstractFormPart#refresh()
	 */
	public void refresh() {
		ISplashInfo info = getSplashInfo();
		fBlockNotification = true;

		fColorSelector.setColorValue(hexToRGB(info.getForegroundColor()));

		int[] pgeo = info.getProgressGeometry();
		boolean addProgress = pgeo != null;
		info.addProgressBar(addProgress, fBlockNotification);
		if (addProgress) {
			for (int i = 0; i < pgeo.length; i++) {
				fBarSpinners[i].setSelection(pgeo[i]);
			}
		} else {
			resetProgressBarGeometry();
		}

		fAddBarButton.setSelection(addProgress);

		int[] mgeo = info.getMessageGeometry();
		boolean addMessage = mgeo != null;
		info.addProgressMessage(addMessage, fBlockNotification);
		if (addMessage) {
			for (int i = 0; i < mgeo.length; i++) {
				fMessageSpinners[i].setSelection(mgeo[i]);
			}
		} else {
			resetProgressMessageGeometry();
		}
		fColorSelector.setColorValue(addMessage ? hexToRGB(info.getForegroundColor()) : new RGB(0, 0, 0));

		fAddMessageButton.setSelection(addMessage);

		// Update the UI
		updateUIFieldTemplateCombo();
		fBlockNotification = false;
		super.refresh();
		// Update this sections enablement
		updateFieldEnablement();
	}

	/**
	 * 
	 */
	private void updateUIFieldTemplateCombo() {
		// Update this sections enablement
		updateFieldEnablement();
		// Get the splash info if any
		ISplashInfo info = getSplashInfo();
		if (info.isDefinedSplashHandlerType() == false) {
			// No splash handler type defined, set "none" in combo box
			fFieldTemplateCombo.setText(PDEUIMessages.SplashConfigurationSection_none);
			return;
		}
		String splashHandlerType = info.getFieldSplashHandlerType();
		// Update the splash handler type in the combo box
		for (int i = 0; i < ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			String key = ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[i][0];
			if (splashHandlerType.equals(key)) {
				String displayName = ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[i][1];
				fFieldTemplateCombo.setText(displayName);
			}
		}
	}

	private ISplashInfo getSplashInfo() {
		ISplashInfo info = getProduct().getSplashInfo();
		if (info == null) {
			info = getModel().getFactory().createSplashInfo();
			getProduct().setSplashInfo(info);
		}
		return info;
	}

	private IProduct getProduct() {
		return getModel().getProduct();
	}

	private IProductModel getModel() {
		return (IProductModel) getPage().getPDEEditor().getAggregateModel();
	}

	public boolean canPaste(Clipboard clipboard) {
		Display d = getSection().getDisplay();
		Control c = d.getFocusControl();
		if (c instanceof Text)
			return true;
		return false;
	}

	private String rgbToHEX(RGB rgb) {
		if (rgb == null)
			return null;
		return rgbToHex(rgb.red) + rgbToHex(rgb.green) + rgbToHex(rgb.blue);
	}

	private String rgbToHex(int value) {
		value = Math.max(0, value);
		value = Math.min(value, 255);
		String hex = Integer.toHexString(value).toUpperCase();
		if (hex.length() == 1)
			hex = '0' + hex;
		return hex;
	}

	private RGB hexToRGB(String hexValue) {
		if (hexValue == null || hexValue.length() < 6)
			return new RGB(0, 0, 0);
		return new RGB(Integer.parseInt(hexValue.substring(0, 2), 16), Integer.parseInt(hexValue.substring(2, 4), 16), Integer.parseInt(hexValue.substring(4, 6), 16));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDESection#modelChanged(org.eclipse.pde.core.IModelChangedEvent)
	 */
	public void modelChanged(IModelChangedEvent e) {
		// No need to call super, handling world changed event here
		if (e.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			handleModelEventWorldChanged(e);
		} else if (e.getChangeType() == IModelChangedEvent.CHANGE) {
			handleModelEventChange(e);
		}
	}

	/**
	 * @param e
	 */
	private void handleModelEventChange(IModelChangedEvent event) {
		// Change event
		Object[] objects = event.getChangedObjects();
		IProductObject object = (IProductObject) objects[0];
		if (object == null) {
			// Ignore
		} else if ((object instanceof IProduct) && (event.getChangedProperty() == IProduct.P_ID)) {
			updateFieldEnablement();
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

	/**
	 * 
	 */
	private void updateFieldEnablement() {
		// Enable / disable progress and message bar fields
		updateFieldProgressEnablement();
		// Enable / disable template field
		updateFieldTemplateEnablement();
	}

	/**
	 * 
	 */
	private void updateFieldProgressEnablement() {
		// Get the splash info if any
		ISplashInfo info = getSplashInfo();
		// Enable section under the following conditions:
		// (1) Product ID is defined
		// (2) Progress geometry is defined
		// (3) Splash handler type is NOT defined
		if ((PDETextHelper.isDefined(getProduct().getProductId()) == false) || ((info.isDefinedGeometry() == false) && (info.isDefinedSplashHandlerType() == true))) {
			fAddBarButton.setEnabled(false);
			fAddMessageButton.setEnabled(false);
			updateFieldProgressBarEnablement(false);
			updateFieldProgressMessageEnablement(false);
		} else {
			fAddBarButton.setEnabled(isEditable());
			fAddMessageButton.setEnabled(isEditable());
			updateFieldProgressBarEnablement(isEditable());
			updateFieldProgressMessageEnablement(isEditable());
		}
	}

	/**
	 * @param buttonEnabled
	 */
	private void updateFieldProgressBarEnablement(boolean buttonEnabled) {
		boolean enable = (fAddBarButton.getSelection() && buttonEnabled);
		for (int i = 0; i < fBarControls.length; i++) {
			fBarControls[i].setEnabled(enable);
		}
	}

	/**
	 * @param buttonEnabled
	 */
	private void updateFieldProgressMessageEnablement(boolean buttonEnabled) {
		boolean enable = (fAddMessageButton.getSelection() && buttonEnabled);
		for (int i = 0; i < fMessageControls.length; i++) {
			fMessageControls[i].setEnabled(enable);
		}
	}

	/**
	 * 
	 */
	private void updateFieldTemplateEnablement() {
		// Get the splash info if any
		ISplashInfo info = getSplashInfo();
		// Enable section under the following conditions:
		// (1) Product ID is defined
		// (2) Progress geometry is NOT defined
		// (3) Progress geometry is defined and splash handler type is defined
		if ((PDETextHelper.isDefined(getProduct().getProductId()) == false) || ((info.isDefinedGeometry() == true) && (info.isDefinedSplashHandlerType() == false))) {
			fFieldTemplateCombo.setEnabled(false);
		} else {
			fFieldTemplateCombo.setEnabled(true);
		}
	}

	/**
	 * @param controls
	 */
	private void addOffsetTooltips(Control[] controls) {
		// Limit includes X, Y spinners and labels
		int limit = 4;
		// Ensure we have that many controls
		if (controls.length < limit) {
			// Something seriously wrong
			return;
		}
		// Set the tooltips
		for (int i = 0; i < limit; i++) {
			controls[i].setToolTipText(PDEUIMessages.SplashConfigurationSection_msgTooltipOffsetRelative);
		}
	}

}
