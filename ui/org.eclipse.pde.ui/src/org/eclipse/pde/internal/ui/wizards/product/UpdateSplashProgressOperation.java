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
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 274107
 *     Johannes Ahlers <Johannes.Ahlers@gmx.de> - bug 477677
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.product;

import java.nio.charset.Charset;
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

	private static final String PLUGIN_URL_PREFIX = "platform:/plugin/"; //$NON-NLS-1$

	private IPluginModelBase fModel;
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

	public void setShowProgress(boolean showProgress) {
		fShowProgress = showProgress;
	}

	public void setProductID(String productID) {
		fProductID = productID;
	}

	public void setProject(IProject project) {
		fProject = project;
	}

	@Override
	public void run(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor,
				PDEUIMessages.UpdateSplashProgressAction_msgProgressCustomizingSplash, 1);
		// Perform the operation
		update(subMonitor.split(1));
	}

	private void update(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 4);
		// Find the product extension
		IPluginExtension productExtension = findProductExtension();
		subMonitor.split(1);
		// Ensure product extension exists
		if (productExtension == null) {
			// Something is seriously wrong
			return;
		}
		// Find the product element
		IPluginElement productElement = findProductElement(productExtension);
		subMonitor.split(1);
		// Ensure product element exists
		if (productElement == null) {
			// Something is seriously wrong
			return;
		}
		// Find the preference customization property
		IPluginElement propertyElement = findPrefCustPropertyElement(productElement);
		subMonitor.split(1);
		if ((propertyElement == null) && fShowProgress) {
			// Operation: Add progress
			// The preference customization property does not exist
			// Create it
			addPreferenceCustomizationElement(productElement, subMonitor.split(1));
		} else if (propertyElement == null) {
			// Operation: Remove progress
			// The preference customization property does not exist
			// NO-OP
			// Note: If plugin_customization.ini exists in the root of the
			// plug-in, this is the default file name in the default location
			// Its values will be loaded.
			// Therefore, since it is possible for a the show progress on
			// startup key to be present and true, make it false
			updateDefaultPluginCustomizationFile(subMonitor.split(1));
		} else {
			// Operations: Add progress, Remove progress
			// The preference customization property exists
			// Update it
			updatePreferenceCustomizationElement(propertyElement, subMonitor.split(1));
		}
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

	private void updatePreferenceCustomizationElement(IPluginElement propertyElement, IProgressMonitor monitor)
			throws CoreException {
		// Get the plug-in customization ini file name
		IPluginAttribute valueAttribute = propertyElement.getAttribute(F_ATTRIBUTE_VALUE);
		// Ensure we have a plug-in customization ini file value
		boolean isAttributeValueNotDefined = !isAttributeValueDefined(valueAttribute);
		if (isAttributeValueNotDefined && fShowProgress) {
			// Operation: Add progress
			// Value is not defined
			// Create the default plugin customization ini file
			createDefaultPluginCustomizationFile(propertyElement, monitor);
			return;
		} else if (isAttributeValueNotDefined) {
			// Operation: Remove progress
			// Fall-back to the default plugin customization ini file
			updateDefaultPluginCustomizationFile(monitor);
			return;
		}
		// Get the plugin customization ini file name
		String pluginCustomizationFileName = valueAttribute.getValue();

		// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=435452
		// Qualified paths should be searched in the workspace, and non-qualified paths
		// should be assumed to refer to the product's defining plugin project.
		int index = pluginCustomizationFileName.indexOf(PLUGIN_URL_PREFIX);
		if (index >= 0) {
			pluginCustomizationFileName = pluginCustomizationFileName.substring(index + PLUGIN_URL_PREFIX.length());
		} else if (pluginCustomizationFileName.equals(F_FILE_NAME_PLUGIN_CUSTOM)) {
			pluginCustomizationFileName = fPluginId + IPath.SEPARATOR + pluginCustomizationFileName;
		}
		// Find the file in the workspace
		IResource resource = fProject.getWorkspace().getRoot().findMember(pluginCustomizationFileName);
		// Ensure the plug-in customization ini file exists
		boolean isFileNotExist = !isFileExist(resource);
		if (isFileNotExist && fShowProgress) {
			// Operation: Add progress
			// File does not exist in the project
			// Create the default plugin customization ini file
			createDefaultPluginCustomizationFile(propertyElement, monitor);
			return;
		} else if (isFileNotExist) {
			// Operation: Remove progress
			// NO-OP
			return;
		}
		// Operations:  Add progress, Remove progress
		// File exists in the project
		// Update it
		updatePluginCustomizationFile((IFile) resource, monitor);
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
		pluginCustomModel.setCharset(Charset.forName(file.getCharset()));
		// Create the listener to listen to text edit operations
		// (Operations need to be collected and applied to the document before
		// saving)
		fPropertiesListener = new PropertiesTextChangeListener(document);
		pluginCustomModel.addModelChangedListener(fPropertiesListener);

		return pluginCustomModel;
	}

	private void updatePluginCustomizationFile(IFile file, IProgressMonitor monitor) throws CoreException {
		IPath path = file.getFullPath();
		LocationKind kind = LocationKind.IFILE;
		// Connect to the text file buffer manager
		SubMonitor subMonitor = SubMonitor.convert(monitor, 3);
		getTextFileBufferManager().connect(path, kind, subMonitor.split(1));
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
			savePluginCustomFileChanges(pluginCustomModel, subMonitor.split(1));
		} catch (MalformedTreeException e) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorCustomFileSaveFailed, e);
		} catch (BadLocationException e) {
			throw createCoreException(PDEUIMessages.UpdateSplashProgressAction_msgErrorCustomFileSaveFailed, e);
		} finally {
			// Disconnect from the text file buffer manager
			getTextFileBufferManager().disconnect(path, kind, subMonitor.split(1));
		}
	}

	private void savePluginCustomFileChanges(BuildModel pluginCustomModel, IProgressMonitor monitor)
			throws CoreException, MalformedTreeException, BadLocationException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
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
		fTextFileBuffer.commit(subMonitor.split(1), true);
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
		for (String value : values) {
			showProgressEntry.removeToken(value);
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

	private void addPreferenceCustomizationElement(IPluginElement productElement, IProgressMonitor monitor)
			throws CoreException {
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
		createDefaultPluginCustomizationFile(propertyElement, monitor);
	}

	private void createDefaultPluginCustomizationFile(IPluginElement propertyElement, IProgressMonitor monitor)
			throws CoreException {
		// Define the value as the default plugin customization ini file name
		propertyElement.setAttribute(F_ATTRIBUTE_VALUE, F_FILE_NAME_PLUGIN_CUSTOM);
		// Check to see if the default file already exists in the project
		IResource resource = fProject.findMember(F_FILE_NAME_PLUGIN_CUSTOM);
		// Ensure the plug-in customization ini file exists
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		if (isFileExist(resource)) {
			// File exists in the project
			// Update it
			updatePluginCustomizationFile((IFile) resource, subMonitor.split(1));
		} else {
			subMonitor.split(1);
			// File does not exist in the project
			// Create the plugin customization ini file
			createPluginCustomizationFile();
		}
	}

	private void updateDefaultPluginCustomizationFile(IProgressMonitor monitor) throws CoreException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		// Check to see if the default file already exists in the project
		IResource resource = fProject.findMember(F_FILE_NAME_PLUGIN_CUSTOM);
		if (isFileExist(resource)) {
			// File exists in the project
			// Update it
			updatePluginCustomizationFile((IFile) resource, subMonitor.split(1));
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
		for (IPluginObject object : objects) {
			// Ensure we have an element
			if ((object instanceof IPluginElement) == false) {
				continue;
			}
			// Property elements are the only legitimate children of product elements
			if (object.getName().equals(F_ELEMENT_PROPERTY) == false) {
				continue;
			}
			IPluginElement element = (IPluginElement) object;
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
		for (IPluginExtension extension : extensions) {
			// Get the extension point
			String point = extension.getPoint();
			// Ensure we have a product extension
			if (point.equals(F_EXTENSION_PRODUCT) == false) {
				continue;
			}
			// Ensure we have the exact product
			// Get the fully qualified product ID
			String id = fPluginId + '.' + extension.getId();
			if (id.equals(fProductID) == false) {
				continue;
			}
			return extension;
		}
		return null;
	}

}
