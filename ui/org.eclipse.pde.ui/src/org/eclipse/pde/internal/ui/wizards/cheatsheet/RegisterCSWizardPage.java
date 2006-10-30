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

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
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

/**
 * RegisterCSWizardPage
 *
 */
public class RegisterCSWizardPage extends WizardPage {

	public final static String F_PAGE_NAME = "register-cs"; //$NON-NLS-1$	
	
	public final static String F_CS_EXTENSION_POINT_ID = 
		"org.eclipse.ui.cheatsheets.cheatSheetContent"; //$NON-NLS-1$
	
	public final static String F_CS_ELEMENT_CATEGORY = "category"; //$NON-NLS-1$
	
	private Combo fCategoryCombo;
	
	private Button fCategoryButton;
	
	private Text fDescriptionText;
	
	private IModel fCheatSheetModel;
	
	private IPluginModelBase fPluginModelBase;
	
	private String fPluginProjectName;
	
	/**
	 * @param pageName
	 */
	public RegisterCSWizardPage(IModel model) {
		super(F_PAGE_NAME);

		fCheatSheetModel = model;
		initialize();
	}

	/**
	 * 
	 */
	private void initialize() {
		setTitle(PDEUIMessages.CheatSheetFileWizardPage_1);
		setDescription(PDEUIMessages.RegisterCSWizardPage_wizardPageDescription);		
		
		fCategoryCombo = null;
		fCategoryButton = null;
		fDescriptionText = null;
		
		fPluginProjectName = fCheatSheetModel.getUnderlyingResource()
				.getProject().getName();
		fPluginModelBase = PDECore.getDefault().getModelManager().findModel(
				fPluginProjectName);
		// TODO: MP: MED: CompCS: Check if model is null?
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		
		createUI(parent);
		createListeners();
		
		updateUI();
		validateUI();

	}

	/**
	 * @param parent
	 */
	private void createUI(Composite parent) {
		// Create the container
		Composite container = createUIContainer(parent);
		// TODO: MP: LOW: CompCS: Refactor into separate method
		Label label = new Label(container, SWT.WRAP);
		label.setText(PDEUIMessages.RegisterCSWizardPage_labelInstructionText);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.widthHint = 300;
		label.setLayoutData(data);
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
		// TODO: MP: MED: Update help context ID
		// Provide functionality for the help button
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container,
				IHelpContextIds.NEW_SCHEMA);
	}

	/**
	 * @param container
	 * @return
	 */
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
		group.setText(PDEUIMessages.RegisterCSWizardPage_groupRegistration);
		return group;
	}

	/**
	 * @param parent
	 * @return
	 */
	private Composite createUIContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		container.setLayout(layout);
		return container;
	}
	
	/**
	 * @param parent
	 */
	private void createUICategoryField(Composite parent) {
		// Create the label
		createUICategoryLabel(parent);
		// Create the combo
		createUICategoryCombo(parent);
		// Create the button
		createUICategoryButton(parent);
	}

	/**
	 * @param parent
	 */
	private void createUICategoryLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.RegisterCSWizardPage_labelCategory);
	}

	/**
	 * @param parent
	 */
	private void createUICategoryCombo(Composite parent) {
		int style = SWT.READ_ONLY | SWT.BORDER;
		fCategoryCombo = new Combo(parent, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		fCategoryCombo.setLayoutData(data);
		// TODO: MP: LOW: CompCS: Descide where to do this
		fCategoryCombo.add(PDEUIMessages.SimpleCSCommandDetails_6);
		fCategoryCombo.setText(PDEUIMessages.SimpleCSCommandDetails_6);
	}

	/**
	 * @param parent
	 */
	private void createUICategoryButton(Composite parent) {
		fCategoryButton = new Button(parent, SWT.PUSH);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_END);
		data.widthHint = 50;
		fCategoryButton.setLayoutData(data);
		fCategoryButton.setText(PDEUIMessages.RegisterCSWizardPage_buttonNew);
		fCategoryButton.setToolTipText(PDEUIMessages.RegisterCSWizardPage_toolTipNewCategory);
	}

	/**
	 * @param parent
	 */
	private void createUIDescriptionField(Composite parent) {
		// Create the label
		createUIDescriptionLabel(parent);
		// Create the text widget
		createUIDescriptionText(parent);
	}

	/**
	 * @param parent
	 */
	private void createUIDescriptionLabel(Composite parent) {
		Label label = new Label(parent, SWT.NONE);
		label.setText(PDEUIMessages.RegisterCSWizardPage_labelDescription);
		int style = GridData.VERTICAL_ALIGN_BEGINNING
				| GridData.HORIZONTAL_ALIGN_END;
		GridData data = new GridData(style);
		label.setLayoutData(data);
	}

	/**
	 * @param parent
	 */
	private void createUIDescriptionText(Composite parent) {
		int style = SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER;
		fDescriptionText = new Text(parent, style);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.heightHint = 60;
		data.horizontalSpan = 2;
		fDescriptionText.setLayoutData(data);		
	}

	/**
	 * 
	 */
	private void createListeners() {
		// Create listeners for the category button
		createListenersCategoryButton();
	}

	/**
	 * 
	 */
	private void createListenersCategoryButton() {
		fCategoryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleWidgetSelectedCategoryButton();
			}
		});
	}

	/**
	 * 
	 */
	private void handleWidgetSelectedCategoryButton() {
		// Create a dialog allowing the user to input the category name
		NewCategoryNameDialog dialog = new NewCategoryNameDialog(
				PDEPlugin.getActiveWorkbenchShell());
		dialog.create();
		dialog.getShell().setText(PDEUIMessages.RegisterCSWizardPage_dialogTitleNewCategory);

		if (dialog.open() == Window.OK) {
			// TODO: MP: LOW: CompCS: Track new categories added in array list here
			String newCategoryName = dialog.getNameText();
			if (PDETextHelper.isDefined(newCategoryName.trim())) {
				fCategoryCombo.add(newCategoryName);
				fCategoryCombo.setText(newCategoryName);
			}
		}
		
	}	

	/**
	 * 
	 */
	private void updateUI() {
		// Get the host plug-in file where the cheat sheet is stored
		IFile file = getPluginFile(fPluginModelBase);
		if (file.exists()) {
			// Final all cheat sheet extensions within the host plug-in
			IPluginExtension[] extensions = findExtensions(fPluginModelBase,
					F_CS_EXTENSION_POINT_ID);
			// Update the category combo
			updateUICategoryCombo(extensions);
			// Update the description text widget
			updateUIDescriptionText(extensions);
		}	
	}

	/**
	 * @param extensions
	 */
	private void updateUICategoryCombo(IPluginExtension[] extensions) {
		// Check cheat sheet extensions for defined categories and populate the
		// combo box with the choices accordingly
		for (int i = 0; i < extensions.length; i++) {
			if (extensions[i].getChildCount() == 0) {
				break;
			}
			IPluginExtension extension = extensions[i];
			// TODO: MP: MED: CompCS: This does not work, no children
			IPluginObject[] pluginObjects = extension.getChildren();
			for (int j = 0; j < pluginObjects.length; j++) {
				if (pluginObjects[j] instanceof IPluginElement) {
					IPluginElement element = (IPluginElement)pluginObjects[j];
					if (element.getName().equals(F_CS_ELEMENT_CATEGORY)) {
						IPluginAttribute attribute = element.getAttribute(ICompCSConstants.ATTRIBUTE_NAME);
						if ((attribute != null) && 
								(PDETextHelper.isDefined(attribute.getValue()))) {
							// TODO: MP: HIGH: CompCS: Reference translated value
							fCategoryCombo.add(attribute.getValue());
						}
					}
				}
			}
		}
		
	}

	/**
	 * @param extensions
	 */
	private void updateUIDescriptionText(IPluginExtension[] extensions) {
		// TODO: MP: MED: CompCS: Find the exact extension and get description if it exists
	}

	/**
	 * 
	 */
	private void validateUI() {
		setPageComplete(true);
	}	
	
	/**
	 * @param pluginID
	 * @return
	 */
	protected IFile getPluginFile(IPluginModelBase model) {
		IProject project = model.getUnderlyingResource().getProject();
		String filename = null;
		if (model instanceof IFragmentModel) {
			filename = "fragment.xml"; //$NON-NLS-1$
		} else {
			filename = "plugin.xml"; //$NON-NLS-1$
		}
		return project.getFile(filename);	
	}
	
	/**
	 * @param model
	 * @param extensionPointID
	 * @return
	 */
	protected IPluginExtension[] findExtensions(IPluginModelBase model,
			String extensionPointID) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		
		ArrayList csExtensions = new ArrayList();
		// TODO: MP: LOW: CompCS: Better mechanism for searching plugin extensions than linear?
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (extensionPointID.equals(point)) {
				csExtensions.add(extensions[i]);
			}
		}
		return (IPluginExtension[]) csExtensions.toArray(
				new IPluginExtension[csExtensions.size()]);
	}	
	
	
}
