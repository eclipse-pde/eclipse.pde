/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
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
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;

/**
 * RegisterTocWizardPage
 */
public class RegisterTocWizardPage extends WizardPage implements IRegisterTOCData {

	public static final int NUM_COLUMNS = 2;

	public final static String F_PAGE_NAME = "register-toc"; //$NON-NLS-1$

	public final static String F_TOC_ELEMENT_TOC = "toc"; //$NON-NLS-1$

	private Button fPrimaryChkBox;

	protected IModel fTocModel;

	private ISharedExtensionsModel fExtensionsModel;

	private IProject fPluginProject;

	private boolean fDataIsPrimary;

	public RegisterTocWizardPage(IModel model) {
		super(F_PAGE_NAME);

		fTocModel = model;
		initialize();
	}

	private void initialize() {

		setTitle(TocWizardMessages.RegisterTocWizardPage_title);
		setDescription(TocWizardMessages.RegisterTocWizardPage_description);

		fPrimaryChkBox = null;

		fDataIsPrimary = true;

		// Get the project the TOC is stored in
		fPluginProject = fTocModel.getUnderlyingResource().getProject();

		initializePluginModel();
	}

	private void initializePluginModel() {
		IPluginModelBase base = PluginRegistry.findModel(getPluginProject());

		if (base == null)
			return;
		if (base instanceof IBundlePluginModelBase)
			fExtensionsModel = ((IBundlePluginModelBase) base).getExtensionsModel();
		else
			fExtensionsModel = base;
	}

	private void updateUI() {

		if (fExtensionsModel != null) {
			// Find all TOC extensions within the host plug-in
			IPluginExtension[] extensions = RegisterTocOperation.findTOCExtensions(fExtensionsModel);

			// Process all TOC elements
			processTocElements(extensions);
		}
	}

	private void processTocElements(IPluginExtension[] extensions) {
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
					if (element.getName().equals(F_TOC_ELEMENT_TOC)) {
						// TOC element
						processTocElement(element, getDataTocFile());
					}

				}
			}
		}
	}

	private void processTocElement(IPluginElement parentElement, String generatedID) {
		// Get the id attribute
		IPluginAttribute fileAttribute = parentElement.getAttribute(RegisterTocOperation.F_TOC_ATTRIBUTE_FILE);

		// Check for the generated ID for this TOC
		// If a TOC exists with the generated ID already, read its
		// description and populate the description text accordingly
		if ((fileAttribute != null) && PDETextHelper.isDefined(fileAttribute.getValue()) && generatedID.equals(fileAttribute.getValue())) {
			// Matching TOC extension found
			// Process children if any
			if (parentElement.getChildCount() > 0) {
				// Update the description text widget
				updateUIPrimary(parentElement);
			}
		}
	}

	private void updateUIPrimary(IPluginElement parentElement) {
		IPluginObject pluginObject = parentElement.getChildren()[0];
		if (pluginObject instanceof IPluginElement) {
			IPluginElement element = (IPluginElement) pluginObject;
			if (element.getName().equals(RegisterTocOperation.F_TOC_ATTRIBUTE_PRIMARY) && PDETextHelper.isDefinedAfterTrim(element.getText())) {
				// Triggers listener to update data description on load
				fPrimaryChkBox.setSelection(Boolean.getBoolean(element.getText().trim()));
			}
		}
	}

	@Override
	public boolean getDataPrimary() {
		return fDataIsPrimary;
	}

	@Override
	public String getDataTocFile() {
		return fTocModel.getUnderlyingResource().getProjectRelativePath().toPortableString();
	}

	@Override
	public IProject getPluginProject() {
		return fPluginProject;
	}

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

		// Create the primary checkbox
		createUIPrimaryChkBox(container);

		// Set the control for the reciever
		// Must be done otherwise a null assertion error is generated
		setControl(container);
		// Apply the dialog font to all controls using the default font
		Dialog.applyDialogFont(container);
		// Provide functionality for the help button
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.REGISTER_TOC);
	}

	private Composite createUIContainer(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = NUM_COLUMNS;
		container.setLayout(layout);
		return container;
	}

	private void createUIPrimaryChkBox(Composite parent) {
		fPrimaryChkBox = new Button(parent, SWT.CHECK);
		fPrimaryChkBox.setText(TocWizardMessages.RegisterTocWizardPage_makePrimary);
		GridData data = new GridData();
		data.horizontalSpan = NUM_COLUMNS;
		fPrimaryChkBox.setLayoutData(data);
		fPrimaryChkBox.setSelection(true);
	}

	private void createUIListeners() {
		// Create listeners for the primary check box
		createUIListenersPrimaryChkBox();
	}

	private void createUIListenersPrimaryChkBox() {
		fPrimaryChkBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fDataIsPrimary = fPrimaryChkBox.getSelection();
			}
		});
	}

	private void validateUI() {
		setPageComplete(true);
	}

	public IPluginExtension[] findExtensions(IPluginModelBase model, String extensionPointID) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();

		ArrayList<IPluginExtension> tocExtensions = new ArrayList<>();
		for (IPluginExtension extension : extensions) {
			String point = extension.getPoint();
			if (extensionPointID.equals(point)) {
				tocExtensions.add(extension);
			}
		}
		return tocExtensions.toArray(new IPluginExtension[tocExtensions.size()]);
	}
}
