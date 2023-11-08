/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public abstract class RegisterCSWizardPage extends WizardPage implements IRegisterCSData {

	public final static String F_PAGE_NAME = "register-cs"; //$NON-NLS-1$

	public final static String F_CS_ELEMENT_CATEGORY = "category"; //$NON-NLS-1$

	public final static String F_CS_ELEMENT_CHEATSHEET = "cheatsheet"; //$NON-NLS-1$

	public final static String F_CS_ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$

	private final static String F_LOCALE_VARIABLE = "$nl$/"; //$NON-NLS-1$

	private Combo fCategoryCombo;

	private Button fCategoryButton;

	private Text fDescriptionText;

	protected IModel fCheatSheetModel;

	private ISharedExtensionsModel fExtensionsModel;

	private IProject fPluginProject;

	private String fDataCategoryName;

	private String fDataDescription;

	private String fDataCheatSheetID;

	private CSCategoryTrackerUtil fCategoryTrackerUtil;

	public RegisterCSWizardPage(IModel model) {
		super(F_PAGE_NAME);

		fCheatSheetModel = model;
		initialize();
	}

	private void initialize() {

		setTitle(CSWizardMessages.RegisterCSWizardPage_title);
		setDescription(CSWizardMessages.RegisterCSWizardPage_description);

		fCategoryCombo = null;
		fCategoryButton = null;
		fDescriptionText = null;

		fCategoryTrackerUtil = new CSCategoryTrackerUtil();

		fDataCategoryName = null;
		fDataDescription = null;

		// Get the project the cheat sheet is stored in
		fPluginProject = fCheatSheetModel.getUnderlyingResource().getProject();

		fDataCheatSheetID = generateCheatSheetID();

		initializePluginModel();
	}

	private void initializePluginModel() {
		IPluginModelBase base = PluginRegistry.findModel(getPluginProject());
		// should never happen
		if (base == null)
			return;
		if (base instanceof IBundlePluginModelBase)
			fExtensionsModel = ((IBundlePluginModelBase) base).getExtensionsModel();
		else
			fExtensionsModel = base;
	}

	@Override
	public String getDataDescription() {
		return fDataDescription;
	}

	@Override
	public String getDataCategoryName() {
		return fDataCategoryName;
	}

	@Override
	public int getDataCategoryType() {
		String categoryID = getDataCategoryID();
		if (categoryID == null) {
			return CSCategoryTrackerUtil.F_TYPE_NO_CATEGORY;
		}
		return fCategoryTrackerUtil.getCategoryType(categoryID);
	}

	@Override
	public String getDataCategoryID() {
		if (fDataCategoryName != null) {
			return fCategoryTrackerUtil.getCategoryID(fDataCategoryName);
		}
		return null;
	}

	@Override
	public String getDataContentFile() {
		// Retrieve the project relative path to the cheat sheet
		String portablePath = fCheatSheetModel.getUnderlyingResource().getProjectRelativePath().toPortableString();
		// Prepend the locale specific variable
		return F_LOCALE_VARIABLE + portablePath;
	}

	@Override
	public String getDataCheatSheetID() {
		return fDataCheatSheetID;
	}

	@Override
	public IProject getPluginProject() {
		return fPluginProject;
	}

	@Override
	public abstract String getDataCheatSheetName();

	@Override
	public abstract boolean isCompositeCheatSheet();

	@Override
	public void createControl(Composite parent) {

		createUI(parent);
		createUIListeners();

		updateUI();
		validateUI();

	}

	private void createUI(Composite parent) {
		// Create the container
		Composite container = createUIContainer(parent);
		// Create the label
		createUILabel(container);
		// Create the group
		Group group = createUIGroup(container);
		// Create the category field
		createUICategoryField(group);
		// Create the description field
		createUIDescriptionField(group);
		// Set the control for the reciever
		// Must be done otherwise a null assertion error is generated
		setControl(container);
		// Apply the dialog font to all controls using the default font
		Dialog.applyDialogFont(container);

		// Provide functionality for the help button
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.REGISTER_CS);
	}

	private void createUILabel(Composite container) {
		Label label = new Label(container, SWT.WRAP);
		label.setText(CSWizardMessages.RegisterCSWizardPage_label);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		label.setLayoutData(data);
	}

	private Group createUIGroup(Composite container) {
		Group group = new Group(container, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = 6;
		layout.marginHeight = 6;
		group.setLayout(layout);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		data.verticalIndent = 10;
		group.setLayoutData(data);
		group.setText(CSWizardMessages.RegisterCSWizardPage_group);
		return group;
	}

	private Composite createUIContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		container.setLayout(layout);
		return container;
	}

	private void createUICategoryField(Composite parent) {
		// Create the label
		createUICategoryLabel(parent);
		// Create the combo
		createUICategoryCombo(parent);
		// Create the button
		createUICategoryButton(parent);
	}

	private void createUICategoryLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(CSWizardMessages.RegisterCSWizardPage_category);
	}

	private void createUICategoryCombo(Composite parent) {
		int style = SWT.READ_ONLY | SWT.BORDER;
		fCategoryCombo = new Combo(parent, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fCategoryCombo.setLayoutData(data);
		fCategoryCombo.add(CSWizardMessages.RegisterCSWizardPage_none);
		fCategoryCombo.setText(CSWizardMessages.RegisterCSWizardPage_none);
	}

	private void createUICategoryButton(Composite parent) {
		fCategoryButton = new Button(parent, SWT.PUSH);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = 50;
		fCategoryButton.setLayoutData(data);
		fCategoryButton.setText(CSWizardMessages.RegisterCSWizardPage_new);
		fCategoryButton.setToolTipText(CSWizardMessages.RegisterCSWizardPage_newTooltip);
		SWTUtil.setButtonDimensionHint(fCategoryButton);
	}

	private void createUIDescriptionField(Composite parent) {
		// Create the label
		createUIDescriptionLabel(parent);
		// Create the text widget
		createUIDescriptionText(parent);
	}

	private void createUIDescriptionLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(CSWizardMessages.RegisterCSWizardPage_desc);
		int style = GridData.VERTICAL_ALIGN_BEGINNING | GridData.HORIZONTAL_ALIGN_END;
		GridData data = new GridData(style);
		label.setLayoutData(data);
	}

	private void createUIDescriptionText(Composite parent) {
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER;
		fDescriptionText = new Text(parent, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		data.horizontalSpan = 2;
		fDescriptionText.setLayoutData(data);
	}

	private void createUIListeners() {
		// Create listeners for the category button
		createUIListenersCategoryButton();
		// Create listeners for the category combo box
		createUIListenersCategoryCombo();
		// Create listeners for the description text
		createUIListenersDescriptionText();
	}

	private void createUIListenersCategoryButton() {
		fCategoryButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelectedCategoryButton();
			}
		});
	}

	private void createUIListenersCategoryCombo() {
		fCategoryCombo.addModifyListener(e -> fDataCategoryName = fCategoryCombo.getText());
	}

	private void createUIListenersDescriptionText() {
		fDescriptionText.addModifyListener(e -> fDataDescription = fDescriptionText.getText());
	}

	private void handleWidgetSelectedCategoryButton() {
		// Create a dialog allowing the user to input the category name
		NewCategoryNameDialog dialog = new NewCategoryNameDialog(PDEUserAssistanceUIPlugin.getActiveWorkbenchShell());
		dialog.create();
		dialog.getShell().setText(CSWizardMessages.RegisterCSWizardPage_descTooltip);

		if (dialog.open() == Window.OK) {
			String newCategoryName = dialog.getNameText();

			if (PDETextHelper.isDefinedAfterTrim(newCategoryName)) {
				String trimmedText = newCategoryName.trim();
				fCategoryCombo.add(trimmedText);
				fCategoryCombo.setText(trimmedText);
				fCategoryCombo.setFocus();
				String id = generateCategoryID(trimmedText);
				fCategoryTrackerUtil.associate(id, trimmedText, CSCategoryTrackerUtil.F_TYPE_NEW_CATEGORY);
			}
		}

	}

	private void updateUI() {

		if (fExtensionsModel != null) {
			// Find all cheat sheet extensions within the host plug-in
			IPluginExtension[] extensions = RegisterCSOperation.findCheatSheetExtensions(fExtensionsModel);
			// Process all category elements
			processCategoryElements(extensions);
			// Process all cheat sheet elements
			processCheatSheetElements(extensions);
		}
	}

	private void processCategoryElements(IPluginExtension[] extensions) {
		// Linear search:  Process all cheat sheet extensions found
		for (IPluginExtension extension : extensions) {
			if (extension.getChildCount() == 0) {
				// Extension has no children, skip to the next extension
				continue;
			}
			IPluginObject[] pluginObjects = extension.getChildren();
			// Process all children
			for (IPluginObject pluginObject : pluginObjects) {
				if (pluginObject instanceof IPluginElement) {
					IPluginElement element = (IPluginElement) pluginObject;
					if (element.getName().equals(F_CS_ELEMENT_CATEGORY)) {
						// Category element
						// Update the category combo
						updateUICategoryComboElement(element);
					}
				}
			}
		}
	}

	private void processCheatSheetElements(IPluginExtension[] extensions) {
		// Query cheat sheet extensions for information required to update
		// the description text and category combo widgets
		// Linear search:  Process all cheat sheet extensions found
		for (IPluginExtension extension : extensions) {
			if (extension.getChildCount() == 0) {
				// Extension has no children, skip to the next extension
				continue;
			}
			IPluginObject[] pluginObjects = extension.getChildren();
			// Process all children
			for (IPluginObject pluginObject : pluginObjects) {
				if (pluginObject instanceof IPluginElement) {
					IPluginElement element = (IPluginElement) pluginObject;
					if (element.getName().equals(F_CS_ELEMENT_CHEATSHEET)) {
						// Cheat sheet element
						processCheatSheetElement(element, fDataCheatSheetID);
					}

				}
			}
		}
	}

	/**
	 * Process category elements
	 * @param parentElement
	 */
	private void updateUICategoryComboElement(IPluginElement parentElement) {
		// Get the id attribute
		IPluginAttribute idAttribute = parentElement.getAttribute(ICompCSConstants.ATTRIBUTE_ID);
		// Get the name attribute
		IPluginAttribute nameAttribute = parentElement.getAttribute(ICompCSConstants.ATTRIBUTE_NAME);
		// Add the category to the combo box only if
		// (1) the category name is defined
		// (2) the category has not already been added to the combo box
		if ((nameAttribute != null) && PDETextHelper.isDefined(nameAttribute.getValue()) && (idAttribute != null) && PDETextHelper.isDefined(idAttribute.getValue()) && (fCategoryTrackerUtil.containsCategoryName(nameAttribute.getValue()) == false)) {
			// TODO: MP: LOW: CompCS: Reference translated value
			fCategoryCombo.add(nameAttribute.getValue());
			// Assocate the category ID with the category name
			fCategoryTrackerUtil.associate(idAttribute.getValue(), nameAttribute.getValue(), CSCategoryTrackerUtil.F_TYPE_OLD_CATEGORY);
		}
	}

	/**
	 * Process cheatsheet elements with a category attribute
	 * @param parentElement
	 */
	private void updateUICategoryComboAttribute(IPluginElement element) {
		// Get the category attribute
		IPluginAttribute categoryAttribute = element.getAttribute(F_CS_ELEMENT_CATEGORY);
		// Process the category attribute
		if ((categoryAttribute != null) && PDETextHelper.isDefined(categoryAttribute.getValue())) {
			String id = categoryAttribute.getValue();
			// Check to see if the category ID has been defined
			if (fCategoryTrackerUtil.containsCategoryID(id)) {
				// Update the category combo selection
				String name = fCategoryTrackerUtil.getCategoryName(id);
				fCategoryCombo.setText(name);
			} else {
				// Add the category ID to the combo box (no assoicated name)
				// This can only happen if the category is defined outside of
				// the plug-in the cheat sheet is stored in
				fCategoryCombo.add(id);
				fCategoryCombo.setText(id);
				fCategoryTrackerUtil.associate(id, id, CSCategoryTrackerUtil.F_TYPE_OLD_CATEGORY);
			}
		}
	}

	private void processCheatSheetElement(IPluginElement parentElement, String generatedID) {
		// Get the id attribute
		IPluginAttribute idAttribute = parentElement.getAttribute(ICompCSConstants.ATTRIBUTE_ID);

		// Check for the generated ID for this cheat sheet
		// If a cheat sheet exists with the generated ID already, read its
		// description and populate the description text accordingly
		if ((idAttribute != null) && PDETextHelper.isDefined(idAttribute.getValue()) && generatedID.equals(idAttribute.getValue())) {
			// Matching cheat sheet extension found
			// Process children if any
			if (parentElement.getChildCount() > 0) {
				// Update the description text widget
				updateUIDescriptionText(parentElement);
			}
			updateUICategoryComboAttribute(parentElement);
		}
	}

	private void updateUIDescriptionText(IPluginElement parentElement) {
		IPluginObject pluginObject = parentElement.getChildren()[0];
		if (pluginObject instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) pluginObject;
			if (element.getName().equals(F_CS_ELEMENT_DESCRIPTION) && PDETextHelper.isDefinedAfterTrim(element.getText())) {
				// Triggers listener to update data description on load
				fDescriptionText.setText(element.getText().trim());
			}
		}
	}

	private void validateUI() {
		setPageComplete(true);
	}

	public IPluginExtension[] findExtensions(ISharedExtensionsModel model, String extensionPointID) {
		IPluginExtension[] extensions = model.getExtensions().getExtensions();

		ArrayList<IPluginExtension> csExtensions = new ArrayList<>();
		for (IPluginExtension extension : extensions) {
			String point = extension.getPoint();
			if (extensionPointID.equals(point)) {
				csExtensions.add(extension);
			}
		}
		return csExtensions.toArray(new IPluginExtension[csExtensions.size()]);
	}

	private String generateCheatSheetID() {
		// Generate the hash code using the full path
		long uniqueID = hash(fCheatSheetModel.getUnderlyingResource().getFullPath().toPortableString());
		// Qualify with the project name
		// Append the hash code to make the name unique and allow cheat sheets
		// with the same name (but different directories) be registered
		// individually
		String result = fPluginProject.getName() + '.' + F_CS_ELEMENT_CHEATSHEET + uniqueID;
		return result;
	}

	private String generateCategoryID(String name) {
		// Generate the hash code using the category name
		long uniqueID = hash(name);
		// Qualify with the project name
		// Append the hash code to make the name unique
		String result = fPluginProject.getName() + '.' + F_CS_ELEMENT_CATEGORY + uniqueID;
		return result;
	}

	private long hash(String string) {
		int b = 378551;
		int a = 63689;
		long hash = 0;

		for (int i = 0; i < string.length(); i++) {
			hash = hash * a + string.charAt(i);
			a = a * b;
		}
		return (hash & 0x7FFFFFFF);
	}

}
