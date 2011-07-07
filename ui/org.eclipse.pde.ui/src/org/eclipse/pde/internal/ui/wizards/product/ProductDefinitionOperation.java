/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.iproduct.IProduct;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.product.SplashInfo;
import org.eclipse.pde.internal.core.text.plugin.PluginElementNode;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.branding.IProductConstants;

public class ProductDefinitionOperation extends BaseManifestOperation {

	private String fProductId;
	private String fApplication;
	private IProduct fProduct;

	protected IProject fProject;

	private UpdateSplashHandlerAction fUpdateSplashAction;

	private RemoveSplashHandlerBindingAction fRemoveSplashAction;

	private UpdateSplashProgressOperation fUpdateSplashProgressOperation;

	public ProductDefinitionOperation(IProduct product, String pluginId, String productId, String application, Shell shell) {
		super(shell, pluginId);
		fProductId = productId;
		fApplication = application;
		fProduct = product;
		fProject = null;
	}

	public ProductDefinitionOperation(IProduct product, String pluginId, String productId, String application, Shell shell, IProject project) {
		super(shell, pluginId);
		fProductId = productId;
		fApplication = application;
		fProduct = product;
		// Needed for splash handler updates (file copying)
		fProject = project;
	}

	protected String getFormattedPackageName(String id) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < id.length(); i++) {
			char ch = id.charAt(i);
			if (buffer.length() == 0) {
				if (Character.isJavaIdentifierStart(ch))
					buffer.append(Character.toLowerCase(ch));
			} else {
				if (Character.isJavaIdentifierPart(ch) || ch == '.')
					buffer.append(ch);
			}
		}
		return buffer.toString().toLowerCase(Locale.ENGLISH);
	}

	protected String createTargetPackage() {
		// Package name addition to create a location for containing
		// any classes required by the splash handlers. 
		String packageName = getFormattedPackageName(fPluginId);
		// Unqualifed
		if (packageName.length() == 0) {
			return ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
		}
		// Qualified
		return packageName + '.' + ISplashHandlerConstants.F_UNQUALIFIED_EXTENSION_ID;
	}

	/**
	 * @return fully-qualified class (with package)
	 */
	private String createAttributeValueClass() {
		String targetPackage = createTargetPackage();
		String targetClass = createTargetClass();
		// Ensure target class is defined
		if (targetClass == null) {
			return null;
		}

		return targetPackage + "." + //$NON-NLS-1$
				targetClass;
	}

	/**
	 * @return unqualified class
	 */
	private String createTargetClass() {
		// Get the splash handler type
		String splashHandlerType = getSplashHandlerType();
		// Ensure splash handler type was specfied
		if (splashHandlerType == null) {
			return null;
		}
		// Update the class name depending on the splash screen type
		for (int i = 0; i < ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES.length; i++) {
			String choice = ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES[i][0];
			if (splashHandlerType.equals(choice)) {
				return ISplashHandlerConstants.F_SPLASH_SCREEN_CLASSES[i];
			}
		}
		return null;
	}

	/**
	 * @return splash screen type qualified with package name
	 */
	private String createAttributeValueID() {
		// Create the ID based on the splash screen type
		return createTargetPackage() + "." + //$NON-NLS-1$
				getSplashHandlerType();
	}

	private UpdateSplashProgressOperation getUpdateSplashProgressOperation() {
		if (fUpdateSplashProgressOperation == null) {
			fUpdateSplashProgressOperation = new UpdateSplashProgressOperation();
		} else {
			fUpdateSplashProgressOperation.reset();
		}
		return fUpdateSplashProgressOperation;
	}

	private void updateSplashProgress(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		// Sanity checks
		if (fProject == null) {
			return;
		} else if (model == null) {
			return;
		} else if (monitor == null) {
			return;
		}
		// Get the action
		UpdateSplashProgressOperation operation = getUpdateSplashProgressOperation();
		operation.setModel(model);
		operation.setShowProgress(isProgressDefined());
		operation.setProject(fProject);
		operation.setProductID(fProduct.getProductId());
		operation.setPluginID(fPluginId);
		// Execute the action
		operation.run(monitor);
	}

	private boolean isProgressDefined() {
		// Get the splash info from the model
		ISplashInfo info = fProduct.getProduct().getSplashInfo();
		// Ensure splash info was defined
		if (info == null) {
			return false;
		}
		// Ensure splash progress was defined
		return info.isDefinedGeometry();
	}

	private String getSplashHandlerType() {
		// Get the splash info from the model
		ISplashInfo info = fProduct.getProduct().getSplashInfo();
		// Ensure splash info was defined
		if (info == null) {
			return null;
		}
		// Ensure splash type was defined
		if (info.isDefinedSplashHandlerType() == false) {
			return null;
		}
		return info.getFieldSplashHandlerType();
	}

	private void updateSplashHandler(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		// Copy the applicable splash handler artifacts and perform parameter
		// substitution (like in templates plug-in)
		// Artifacts may include code, images and extension point schemas
		updateSplashHandlerFiles(model, monitor);
		// Update the plug-in model with the applicable splash handler extension
		// and extension point related mark-up
		updateSplashHandlerModel(model, monitor);
	}

	private void updateSplashHandlerFiles(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		// If the project is not defined, abort this operation
		if (fProject == null) {
			return;
		}
		// Get the splash handler type
		String splashHandlerType = getSplashHandlerType();
		// If the splash handler type was not defined, abort this operation
		if (splashHandlerType == null) {
			return;
		}
		// Create and configure the template file generator
		// Note: Plug-in ID must be passed in separately from model, because
		// the underlying model does not contain the ID (even when it is
		// a workspace model)
		TemplateFileGenerator generator = new TemplateFileGenerator(fProject, model, fPluginId, createTargetPackage(), createTargetClass(), splashHandlerType);
		// Generate the necessary files
		generator.generateFiles(monitor);
	}

	private void updateSplashHandlerModel(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		// Get the splash handler type
		String splashHandlerType = getSplashHandlerType();
		// If the splash handler type is not defined, abort this operation
		if (splashHandlerType == null) {
			runRemoveSplashAction(model, monitor);
		} else {
			runUpdateSplashAction(model, monitor, splashHandlerType);
		}
	}

	private void runRemoveSplashAction(IPluginModelBase model, IProgressMonitor monitor) throws CoreException {
		// Create the remove splash handler action
		fRemoveSplashAction = new RemoveSplashHandlerBindingAction();
		// Configure the action
		fRemoveSplashAction.setFieldProductID(fProduct.getProductId());
		fRemoveSplashAction.setFieldTargetPackage(createTargetPackage());

		fRemoveSplashAction.setModel(model);
		fRemoveSplashAction.setMonitor(monitor);
		// Execute the action
		fRemoveSplashAction.run();
		// If an core exception was thrown and caught, release it
		fRemoveSplashAction.hasException();
	}

	private void runUpdateSplashAction(IPluginModelBase model, IProgressMonitor monitor, String splashHandlerType) throws CoreException {
		// Create the update splash handler action
		fUpdateSplashAction = new UpdateSplashHandlerAction();
		// Configure the action
		String id = createAttributeValueID();
		fUpdateSplashAction.setFieldID(id);
		fUpdateSplashAction.setFieldClass(createAttributeValueClass());
		fUpdateSplashAction.setFieldSplashID(id);
		fUpdateSplashAction.setFieldProductID(fProduct.getProductId());
		fUpdateSplashAction.setFieldTemplate(splashHandlerType);
		fUpdateSplashAction.setFieldPluginID(fPluginId);

		fUpdateSplashAction.setModel(model);
		fUpdateSplashAction.setMonitor(monitor);
		// Execute the action
		fUpdateSplashAction.run();
		// If an core exception was thrown and caught, release it
		fUpdateSplashAction.hasException();
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			IFile file = getFile();
			if (!file.exists()) {
				createNewFile(file, monitor);
			} else {
				modifyExistingFile(file, monitor);
			}
			updateSingleton(monitor);
		} catch (CoreException e) {
			throw new InvocationTargetException(e);
		}
	}

	private void createNewFile(IFile file, IProgressMonitor monitor) throws CoreException {
		WorkspacePluginModelBase model = (WorkspacePluginModelBase) getModel(file);
		IPluginBase base = model.getPluginBase();
		base.setSchemaVersion(TargetPlatformHelper.getSchemaVersion());
		base.add(createExtension(model));
		// Update the splash handler.  Update plug-in model and copy files
		updateSplashHandler(model, monitor);
		// Update splash progress.  Update plug-in model and copy files
		updateSplashProgress(model, monitor);

		model.save();
	}

	private IPluginExtension createExtension(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = model.getFactory().createExtension();
		extension.setPoint("org.eclipse.core.runtime.products"); //$NON-NLS-1$
		extension.setId(fProductId);
		extension.add(createExtensionContent(extension));
		return extension;
	}

	private IPluginElement createExtensionContent(IPluginExtension extension) throws CoreException {
		IPluginElement element = extension.getModel().getFactory().createElement(extension);
		element.setName("product"); //$NON-NLS-1$
		element.setAttribute("name", fProduct.getName()); //$NON-NLS-1$
		element.setAttribute("application", fApplication); //$NON-NLS-1$

		IPluginElement child = createElement(element, IProductConstants.WINDOW_IMAGES, getWindowImagesString());
		if (child != null)
			element.add(child);

		child = createElement(element, IProductConstants.ABOUT_TEXT, getAboutText());
		if (child != null)
			element.add(child);

		child = createElement(element, IProductConstants.ABOUT_IMAGE, getAboutImage());
		if (child != null)
			element.add(child);

		child = createElement(element, IProductConstants.STARTUP_FOREGROUND_COLOR, getForegroundColor());
		if (child != null)
			element.add(child);

		child = createElement(element, IProductConstants.STARTUP_PROGRESS_RECT, getProgressRect());
		if (child != null)
			element.add(child);

		child = createElement(element, IProductConstants.STARTUP_MESSAGE_RECT, getMessageRect());
		if (child != null)
			element.add(child);

		return element;
	}

	private IPluginElement createElement(IPluginElement parent, String name, String value) throws CoreException {
		IPluginElement element = null;
		if (value != null && value.length() > 0) {
			element = parent.getModel().getFactory().createElement(parent);
			element.setName("property"); //$NON-NLS-1$
			element.setAttribute("name", name); //$NON-NLS-1$ 
			element.setAttribute("value", value); //$NON-NLS-1$ 
		}
		return element;
	}

	private String getAboutText() {
		IAboutInfo info = fProduct.getAboutInfo();
		if (info != null) {
			String text = info.getText();
			return text == null || text.length() == 0 ? null : text;
		}
		return null;
	}

	private String getAboutImage() {
		IAboutInfo info = fProduct.getAboutInfo();
		return info != null ? getURL(info.getImagePath()) : null;
	}

	private String getURL(String location) {
		if (location == null || location.trim().length() == 0)
			return null;
		IPath path = new Path(location);
		if (!path.isAbsolute())
			return location;
		String projectName = path.segment(0);
		IProject project = PDEPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				String id = model.getPluginBase().getId();
				if (fPluginId.equals(id))
					return path.removeFirstSegments(1).toString();
				return "platform:/plugin/" + id + "/" + path.removeFirstSegments(1); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return location;
	}

	private String getWindowImagesString() {
		IWindowImages images = fProduct.getWindowImages();
		StringBuffer buffer = new StringBuffer();
		if (images != null) {
			for (int i = 0; i < IWindowImages.TOTAL_IMAGES; i++) {
				String image = getURL(images.getImagePath(i));
				if (image != null) {
					if (buffer.length() > 0)
						buffer.append(","); //$NON-NLS-1$
					buffer.append(image);
				}

			}
		}
		return buffer.length() == 0 ? null : buffer.toString();
	}

	private String getForegroundColor() {
		ISplashInfo info = fProduct.getSplashInfo();
		return info != null ? info.getForegroundColor() : null;
	}

	private String getProgressRect() {
		ISplashInfo info = fProduct.getSplashInfo();
		return info != null ? SplashInfo.getGeometryString(info.getProgressGeometry()) : null;
	}

	private String getMessageRect() {
		ISplashInfo info = fProduct.getSplashInfo();
		return info != null ? SplashInfo.getGeometryString(info.getMessageGeometry()) : null;
	}

	private void modifyExistingFile(IFile file, IProgressMonitor monitor) throws CoreException {
		IStatus status = PDEPlugin.getWorkspace().validateEdit(new IFile[] {file}, getShell());
		if (status.getSeverity() != IStatus.OK)
			throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.pde.ui", IStatus.ERROR, NLS.bind(PDEUIMessages.ProductDefinitionOperation_readOnly, fPluginId), null)); //$NON-NLS-1$ 

		ModelModification mod = new ModelModification(file) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IPluginModelBase))
					return;
				IPluginExtension extension = findProductExtension((IPluginModelBase) model);
				if (extension == null)
					insertNewExtension((IPluginModelBase) model);
				else
					modifyExistingExtension(extension);
				// Update the splash handler.  Update plug-in model and copy files
				updateSplashHandler((IPluginModelBase) model, monitor);
				// Update splash progress.  Update plug-in model and copy files
				updateSplashProgress((IPluginModelBase) model, monitor);
			}
		};
		PDEModelUtility.modifyModel(mod, monitor);
	}

	private IPluginExtension findProductExtension(IPluginModelBase model) {
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			String id = extensions[i].getId();
			if (fProductId.equals(id) && "org.eclipse.core.runtime.products".equals(point)) { //$NON-NLS-1$
				return extensions[i];
			}
		}
		return null;
	}

	private void insertNewExtension(IPluginModelBase model) throws CoreException {
		IPluginExtension extension = createExtension(model);
		model.getPluginBase().add(extension);
	}

	private void modifyExistingExtension(IPluginExtension extension) throws CoreException {
		if (extension.getChildCount() == 0) {
			insertNewProductElement(extension);
			return;
		}

		PluginElementNode element = (PluginElementNode) extension.getChildren()[0];

		if (!"product".equals(element.getName())) { //$NON-NLS-1$
			insertNewProductElement(extension);
			return;
		}

		element.setAttribute("application", fApplication); //$NON-NLS-1$
		element.setAttribute("name", fProduct.getName()); //$NON-NLS-1$
		synchronizeChild(element, IProductConstants.APP_NAME, fProduct.getName());

		synchronizeChild(element, IProductConstants.ABOUT_IMAGE, getAboutImage());
		synchronizeChild(element, IProductConstants.ABOUT_TEXT, getAboutText());
		synchronizeChild(element, IProductConstants.WINDOW_IMAGES, getWindowImagesString());
		synchronizeChild(element, IProductConstants.STARTUP_FOREGROUND_COLOR, getForegroundColor());
		synchronizeChild(element, IProductConstants.STARTUP_MESSAGE_RECT, getMessageRect());
		synchronizeChild(element, IProductConstants.STARTUP_PROGRESS_RECT, getProgressRect());

	}

	private void synchronizeChild(IPluginElement element, String propertyName, String value) throws CoreException {
		IPluginElement child = null;
		IPluginObject[] children = element.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement candidate = (IPluginElement) children[i];
			if (candidate.getName().equals("property")) { //$NON-NLS-1$
				IPluginAttribute attr = candidate.getAttribute("name"); //$NON-NLS-1$
				if (attr != null && attr.getValue().equals(propertyName)) {
					child = candidate;
					break;
				}
			}
		}
		if (child != null && value == null)
			element.remove(child);

		if (value == null)
			return;

		if (child == null) {
			child = element.getModel().getFactory().createElement(element);
			child.setName("property"); //$NON-NLS-1$
			element.add(child);
		}
		child.setAttribute("value", value); //$NON-NLS-1$
		child.setAttribute("name", propertyName); //$NON-NLS-1$
	}

	private void insertNewProductElement(IPluginExtension extension) throws CoreException {
		IPluginElement element = createExtensionContent(extension);
		extension.add(element);
	}

}
