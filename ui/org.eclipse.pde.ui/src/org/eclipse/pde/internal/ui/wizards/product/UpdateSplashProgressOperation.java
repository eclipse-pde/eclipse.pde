/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 274107 
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.filebuffers.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.text.edits.*;

public class UpdateSplashProgressOperation implements IWorkspaceRunnable {

	public static final String F_EXTENSION_PRODUCT = "org.eclipse.core.runtime.products"; //$NON-NLS-1$
	public static final String F_ELEMENT_PRODUCT = "product"; //$NON-NLS-1$
	public static final String F_ELEMENT_PROPERTY = "property"; //$NON-NLS-1$
	public static final String F_ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	public static final String F_ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	public static final String F_ATTRIBUTE_NAME_PREFCUST = "preferenceCustomization"; //$NON-NLS-1$
	public static final String F_KEY_SHOW_PROGRESS = "org.eclipse.ui/SHOW_PROGRESS_ON_STARTUP"; //$NON-NLS-1$
	public static final String F_FILE_NAME_PLUGIN_CUSTOM = "plugin_customization.ini"; //$NON-NLS-1$

	private IPluginModelBase fModel;
	private IProgressMonitor fMonitor;
	private boolean fShowProgress;
	private IProject fProject;
	private String fProductID;
	protected String fPluginId;
	private ITextFileBufferManager fTextFileBufferManager;
	private ITextFileBuffer fTextFileBuffer;
	private PropertiesTextChangeListener fPropertiesListener;

	public UpdateSplashProgressOperation() {
		reset();
	}

	public void reset() {
		// External Fields
		fModel = null;
		fMonitor = null;
		fProductID = null;
		fShowProgress = true;
		fProject = null;
		fPluginId = null;
		// Internal Fields
		fTextFileBufferManager = null;
		fPropertiesListener = null;
		fTextFileBuffer = null;
	}

	public void setPluginID(String pluginID) {
		fPluginId = pluginID;
	}

	public void setModel(IPluginModelBase model) {
		fModel = model;
	}

	private void setMonitor(IProgressMonitor monitor) {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		fMonitor = monitor;
	}

	public void setShowProgress(boolean showProgress) {
		fShowProgress = showProgress;
	}

	public void setProductID(String productID) {
		fProductID = productID;
	}

	public void setProject(IProject project) {
		fProject = project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void run(IProgressMonitor monitor) throws CoreException {
		// Set the progress monitor
		setMonitor(monitor);
		// Perform the operation
		fMonitor.beginTask(PDEUIMessages.UpdateSplashProgressAction_msgProgressCustomizingSplash, 10);
		try {
			update();
		} finally {
			fMonitor.done();
		}
	}

	private void update() throws CoreException {
		// Find the product extension
		IPluginExtension productExtension = findProductExtension();
		fMonitor.worked(1);
		// Ensure product extension exists
		if (productExtension == null) {
			// Something is seriously wrong
			return;
		}
		// Find the product element
		IPluginElement productElement = findProductElement(productExtension);
		fMonitor.worked(1);
		// Ensure product element exists
		if (productElement == null) {
			// Something is seriously wrong
			return;
		}
		// Find the preference customization property
		IPluginElement propertyElement = findPrefCustPropertyElement(productElement);
		fMonitor.worked(1);
		if ((propertyElement == null) && fShowProgress) {
			// Operation: Add progress
			// The preference customization property does not exist
			// Create it
			addPreferenceCustomizationElement(productElement);
		} else if (propertyElement == null) {
			// Operation: Remove progress
			// The preference customization property does not exist
			// NO-OP
			// Note: If plugin_customization.ini exists in the root of the 
			// plug-in, this is the default file name in the default location
			// Its values will be loaded.
			// Therefore, since it is possible for a the show progress on
			// startup key to be present and true, make it false
			updateDefaultPluginCustomizationFile();
		} else {
			// Operations: Add progress, Remove progress
			// The preference customization property exists
			// Update it
			updatePreferenceCustomizationElement(propertyElement);
		}
		fMonitor.worked(4);
	}

	private boolean isAttributeValueDefined(IPluginAttribute valueAttribute) {
		if (valueAttribute == null) {
			return false;
		}
		return PDETextHelper.isDefined(valueAttribute.getValue());
	}

	private boolean isFileExist(IResource resource) {
		if (resource == null) {
			return false;
		}
		return (resource instanceof IFile);
	}

	private void updatePreferenceCustomizationElement(IPluginElement propertyElement) throws CoreException {
		// Get the plug-in customization ini file name
		IPluginAttribute valueAttribute = propertyElement.getAttribute(F_ATTRIBUTE_VALUE);
		// Ensure we have a plug-in customization ini file value
		boolean isAttributeValueNotDefined = !isAttributeValueDefined(valueAttribute);
		if (isAttributeValueNotDefined && fShowProgress) {
			// Operation: Add progress
			// Value is not defined
			// Create the default plugin customization ini file
			createDefaultPluginCustomizationFile(propertyElement);
			return;
		} else if (isAttributeValueNotDefined) {
			// Operation: Remove progress
			// Fall-back to the default plugin customization ini file
			updateDefaultPluginCustomizationFile();
			return;
		}
		// Get the plugin customization ini file name
		String pluginCustomizationFileName = valueAttribute.getValue();
		// Find the file in the project
		IResource resource = fProject.findMember(pluginCustomizationFileName);
		// Ensure the plug-in customization ini file exists
		boolean isFileNotExist = !isFileExist(resource);
		if (isFileNotExist && fShowProgress) {
			// Operation: Add progress
			// File does not exist in the project
			// Create the default plugin customization ini file
			createDefaultPluginCustomizationFile(propertyElement);
			return;
		} else if (isFileNotExist) {
			// Operation: Remove progress
			// NO-OP
			return;
		}
		// Operations:  Add progress, Remove progress
		// File exists in the project
		// Update it
		updatePluginCustomizationFile((IFile) resource);
	}

	private CoreException createCoreException(String message, Throwable exception) {
		IStatus status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, message, exception);
		return new CoreException(status);
	}

	private CoreException createCoreException(String message) {
		IStatus status = new Status(IStatus.ERROR, IPDEUIConstants.PLUGIN_ID, message);
		return new CoreException(status);
	}

	private ITextFileBufferManager getTextFileBufferManager() throws CoreException {
		if (fTextFileBufferManager == null) {
			// Get the text file buffer manager
			fTextFileBufferManager = FileBuffers.getTextFileBufferManager();
		}
		// Ensure manager is defined
		if (fTextFileBufferManager == null) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorTextFileBufferManager);
		}
		return fTextFileBufferManager;
	}

	private ITextFileBuffer getPluginCustomizationBuffer(IFile file) throws CoreException {
		IPath path = file.getFullPath();
		LocationKind kind = LocationKind.IFILE;
		// Get the text file buffer
		fTextFileBuffer = getTextFileBufferManager().getTextFileBuffer(path, kind);
		// Ensure buffer is defined
		if (fTextFileBuffer == null) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorTextFileBuffer);
		}
		return fTextFileBuffer;
	}

	private BuildModel getBuildModel(IFile file) throws CoreException {
		// Convert the file to a document
		// Defines a text file buffer
		IDocument document = getPluginCustomizationBuffer(file).getDocument();
		// Create the plugin customization model
		BuildModel pluginCustomModel = new BuildModel(document, false);
		pluginCustomModel.setUnderlyingResource(file);
		pluginCustomModel.setCharset(file.getCharset());
		// Create the listener to listen to text edit operations
		// (Operations need to be collected and applied to the document before
		// saving)
		fPropertiesListener = new PropertiesTextChangeListener(document);
		pluginCustomModel.addModelChangedListener(fPropertiesListener);

		return pluginCustomModel;
	}

	private void updatePluginCustomizationFile(IFile file) throws CoreException {
		IPath path = file.getFullPath();
		LocationKind kind = LocationKind.IFILE;
		// Connect to the text file buffer manager
		getTextFileBufferManager().connect(path, kind, new SubProgressMonitor(fMonitor, 1));
		try {
			// Create the plugin customization model
			BuildModel pluginCustomModel = getBuildModel(file);
			// Load the plugin customization file
			pluginCustomModel.load();
			// Find the show progress on startup key
			IBuildEntry showProgressEntry = pluginCustomModel.getBuild().getEntry(F_KEY_SHOW_PROGRESS);
			// Check to see if we found the entry
			if (showProgressEntry == null) {
				// No show progress entry
				// Create one
				addShowProgressEntry(pluginCustomModel);
			} else {
				// Show progress entry exists
				// Update it
				updateShowProgressEntry(showProgressEntry);
			}
			// Save plugin customization file changes
			savePluginCustomFileChanges(pluginCustomModel);
		} catch (MalformedTreeException e) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorCustomFileSaveFailed, e);
		} catch (BadLocationException e) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorCustomFileSaveFailed, e);
		} finally {
			// Disconnect from the text file buffer manager
			getTextFileBufferManager().disconnect(path, kind, new SubProgressMonitor(fMonitor, 1));
		}
	}

	private void savePluginCustomFileChanges(BuildModel pluginCustomModel) throws CoreException, MalformedTreeException, BadLocationException {
		// Ensure there is something to save
		if (pluginCustomModel.isDirty() == false) {
			// Nothing to save
			return;
		} else if (fPropertiesListener == null) {
			// Prereq: Serious setup problem
			return;
		} else if (fTextFileBuffer == null) {
			// Prereq: Serious setup problem
			return;
		}
		// Get the accumulated text operations (if any)
		TextEdit[] edits = fPropertiesListener.getTextOperations();
		if (edits.length == 0) {
			// Nothing to save
			return;
		}
		// Apply text editor operations to the document
		MultiTextEdit multi = new MultiTextEdit();
		multi.addChildren(edits);
		multi.apply(pluginCustomModel.getDocument());
		// Ensure there is something to save
		if (fTextFileBuffer.isDirty() == false) {
			// Nothing to save
			return;
		}
		// Perform the actual save
		fTextFileBuffer.commit(new SubProgressMonitor(fMonitor, 1), true);
	}

	private String getBooleanValue(boolean value) {
		if (value) {
			return Boolean.TRUE.toString();
		}
		return Boolean.FALSE.toString();
	}

	private void updateShowProgressEntry(IBuildEntry showProgressEntry) throws CoreException {
		// Convert boolean to String
		String newBooleanValue = getBooleanValue(fShowProgress);
		// Get the value of the show progress entry
		String[] values = showProgressEntry.getTokens();
		// There should only be one value (the first one)
		if (values.length == 0) {
			// No values
			// Define true value
			showProgressEntry.addToken(newBooleanValue);
			return;
		} else if (values.length > 1) {
			// Too many values
			// Remove all values and add the true value
			removeEntryTokens(showProgressEntry, values);
			showProgressEntry.addToken(newBooleanValue);
			return;
		}
		// Get the boolean value
		String oldBooleanValue = values[0];
		// If the old value is not the same as the new value, replace the old
		// with the new
		if (oldBooleanValue.equals(newBooleanValue) == false) {
			showProgressEntry.renameToken(oldBooleanValue, newBooleanValue);
		}
		// Nothing to do if the value is the same already
	}

	private void removeEntryTokens(IBuildEntry showProgressEntry, String[] values) throws CoreException {
		// Remove each token
		for (int i = 0; i < values.length; i++) {
			showProgressEntry.removeToken(values[i]);
		}
	}

	private void addShowProgressEntry(IBuildModel pluginCustomModel) throws CoreException {
		// Create the show progress key
		IBuildEntry showProgressEntry = pluginCustomModel.getFactory().createEntry(F_KEY_SHOW_PROGRESS);
		// Set the show progress value
		showProgressEntry.addToken(getBooleanValue(fShowProgress));
		// Add the show progress entry to the model
		pluginCustomModel.getBuild().add(showProgressEntry);
	}

	private void createPluginCustomizationFile() throws CoreException {
		// Create a handle to the workspace file
		// (Does not exist yet)
		IFile file = fProject.getFile(F_FILE_NAME_PLUGIN_CUSTOM);
		// Create the plugin customization model
		WorkspaceBuildModel pluginCustomModel = new WorkspaceBuildModel(file);
		// Add the show progress entry to the model
		addShowProgressEntry(pluginCustomModel);
		// Create the file by saving the model
		pluginCustomModel.save();

		// add the file to build.properties
		IFile buildProps = PDEProject.getBuildProperties(fProject);
		if (buildProps.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			model.load();
			if (model.isLoaded()) {
				IBuildEntry entry = model.getBuild().getEntry("bin.includes"); //$NON-NLS-1$
				if (entry == null) {
					entry = model.getFactory().createEntry("bin.includes"); //$NON-NLS-1$
					model.getBuild().add(entry);
				}
				if (!entry.contains(F_FILE_NAME_PLUGIN_CUSTOM))
					entry.addToken(F_FILE_NAME_PLUGIN_CUSTOM);
				model.save();
			}
		}
	}

	private void addPreferenceCustomizationElement(IPluginElement productElement) throws CoreException {
		// Get the factory
		IExtensionsModelFactory factory = productElement.getModel().getFactory();
		// Create a property element
		IPluginElement propertyElement = factory.createElement(productElement);
		propertyElement.setName(F_ELEMENT_PROPERTY);
		// Create the name attribute
		propertyElement.setAttribute(F_ATTRIBUTE_NAME, F_ATTRIBUTE_NAME_PREFCUST);
		// Add the property element to the product element
		productElement.add(propertyElement);
		// Create the default plugin customization ini file
		createDefaultPluginCustomizationFile(propertyElement);
	}

	private void createDefaultPluginCustomizationFile(IPluginElement propertyElement) throws CoreException {
		// Define the value as the default plugin customization ini file name
		propertyElement.setAttribute(F_ATTRIBUTE_VALUE, F_FILE_NAME_PLUGIN_CUSTOM);
		// Check to see if the default file already exists in the project
		IResource resource = fProject.findMember(F_FILE_NAME_PLUGIN_CUSTOM);
		// Ensure the plug-in customization ini file exists
		if (isFileExist(resource)) {
			// File exists in the project
			// Update it
			updatePluginCustomizationFile((IFile) resource);
		} else {
			// File does not exist in the project
			// Create the plugin customization ini file
			createPluginCustomizationFile();
		}
	}

	private void updateDefaultPluginCustomizationFile() throws CoreException {
		// Check to see if the default file already exists in the project
		IResource resource = fProject.findMember(F_FILE_NAME_PLUGIN_CUSTOM);
		if (isFileExist(resource)) {
			// File exists in the project
			// Update it
			updatePluginCustomizationFile((IFile) resource);
		}
	}

	private IPluginElement findPrefCustPropertyElement(IPluginElement productElement) {
		// Ensure the produce element has children
		if (productElement.getChildCount() == 0) {
			return null;
		}
		// Get the product element children
		IPluginObject[] objects = productElement.getChildren();
		// Process all children
		for (int i = 0; i < objects.length; i++) {
			// Ensure we have an element
			if ((objects[i] instanceof IPluginElement) == false) {
				continue;
			}
			// Property elements are the only legitimate children of product elements
			if (objects[i].getName().equals(F_ELEMENT_PROPERTY) == false) {
				continue;
			}
			IPluginElement element = (IPluginElement) objects[i];
			// Get the name
			IPluginAttribute nameAttribute = element.getAttribute(F_ATTRIBUTE_NAME);
			// Ensure we have a preference customization property
			if (nameAttribute == null) {
				continue;
			} else if (PDETextHelper.isDefined(nameAttribute.getValue()) == false) {
				continue;
			} else if (nameAttribute.getValue().equals(F_ATTRIBUTE_NAME_PREFCUST) == false) {
				continue;
			}

			return element;
		}
		return null;
	}

	private IPluginElement findProductElement(IPluginExtension extension) {
		// The product extension is only allowed one child
		if (extension.getChildCount() != 1) {
			return null;
		}
		// Get the one child
		IPluginObject pluginObject = extension.getChildren()[0];
		// Ensure that the child is an element
		if ((pluginObject instanceof IPluginElement) == false) {
			return null;
		}
		// Ensure that the child is a product element
		if (pluginObject.getName().equals(F_ELEMENT_PRODUCT) == false) {
			return null;
		}
		return (IPluginElement) pluginObject;
	}

	private IPluginExtension findProductExtension() {
		// Get all the extensions
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		// Get the extension matching the product extension point ID
		// and product ID
		for (int i = 0; i < extensions.length; i++) {
			// Get the extension point
			String point = extensions[i].getPoint();
			// Ensure we have a product extension
			if (point.equals(F_EXTENSION_PRODUCT) == false) {
				continue;
			}
			// Ensure we have the exact product
			// Get the fully qualified product ID
			String id = fPluginId + '.' + extensions[i].getId();
			if (id.equals(fProductID) == false) {
				continue;
			}
			return extensions[i];
		}
		return null;
	}

}
