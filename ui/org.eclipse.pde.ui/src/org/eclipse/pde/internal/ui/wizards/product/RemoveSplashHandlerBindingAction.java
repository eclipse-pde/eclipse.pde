/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.product;

import java.util.ArrayList;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;

public class RemoveSplashHandlerBindingAction extends Action implements ISplashHandlerConstants {

	private IPluginModelBase fModel;

	private IProgressMonitor fMonitor;

	private CoreException fException;

	private String fFieldProductID;

	private String fFieldTargetPackage;

	public RemoveSplashHandlerBindingAction() {
		reset();
	}

	public void setFieldProductID(String fieldProductID) {
		fFieldProductID = fieldProductID;
	}

	public void setFieldTargetPackage(String fieldTargetPackage) {
		fFieldTargetPackage = fieldTargetPackage;
	}

	public void reset() {
		fModel = null;
		fMonitor = null;
		fException = null;

		fFieldProductID = null;
		fFieldTargetPackage = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		try {
			updateModel();
		} catch (CoreException e) {
			fException = e;
		}
	}

	public void hasException() throws CoreException {
		// Release any caught exceptions
		if (fException != null) {
			throw fException;
		}
	}

	public void setModel(IPluginModelBase model) {
		fModel = model;
	}

	public void setMonitor(IProgressMonitor monitor) {
		fMonitor = monitor;
	}

	private void updateModel() throws CoreException {
		// Find the first splash handler extension
		// We don't care about other splash handler extensions manually added
		// by the user
		IPluginExtension extension = findFirstExtension(F_SPLASH_HANDLERS_EXTENSION);
		// Check to see if one was found
		if (extension == null) {
			// None found, our job is already done
			return;
		}
		// Update progress work units
		fMonitor.beginTask(NLS.bind(PDEUIMessages.RemoveSplashHandlerBindingAction_msgProgressRemoveProductBindings, F_SPLASH_HANDLERS_EXTENSION), 1);
		// Find all product binding elements
		IPluginElement[] productBindingElements = findProductBindingElements(extension);
		// Remove all product binding elements that are malformed or match the
		// target product ID
		removeMatchingProductBindingElements(extension, productBindingElements);
		// Update progress work units
		fMonitor.done();
	}

	private void removeMatchingProductBindingElements(IPluginExtension extension, IPluginElement[] productBindingElements) throws CoreException {
		// If there are no product binding elements, then our job is done
		if ((productBindingElements == null) || (productBindingElements.length == 0)) {
			return;
		}
		// Process all product binding elements
		for (int i = 0; i < productBindingElements.length; i++) {
			IPluginElement element = productBindingElements[i];
			// Get the product ID attribute
			IPluginAttribute productIDAttribute = element.getAttribute(F_ATTRIBUTE_PRODUCT_ID);
			// Remove any product binding element that does not define a 
			// product ID 
			if ((productIDAttribute == null) || (PDETextHelper.isDefined(productIDAttribute.getValue()) == false)) {
				extension.remove(element);
				continue;
			}
			// Get the splash ID attribute
			IPluginAttribute splashIDAttribute = element.getAttribute(F_ATTRIBUTE_SPLASH_ID);
			// Remove any product binding element that does not define a 
			// splash ID 
			if ((splashIDAttribute == null) || (PDETextHelper.isDefined(splashIDAttribute.getValue()) == false)) {
				extension.remove(element);
				continue;
			}
			// Remove any product binding element whose product ID matches this
			// product's ID and whose splash ID match's a generated splash
			// handler template ID
			if (productIDAttribute.getValue().equals(fFieldProductID) && isGeneratedSplashID(splashIDAttribute.getValue())) {
				extension.remove(element);
			}
		}
	}

	private boolean isGeneratedSplashID(String value) {
		String[][] choices = ISplashHandlerConstants.F_SPLASH_SCREEN_TYPE_CHOICES;
		// Check to see if the splash ID matches any of the pre-generated
		// splash handler template IDs
		for (int i = 0; i < choices.length; i++) {
			String splashID = fFieldTargetPackage + '.' + choices[i][0];
			if (value.equals(splashID)) {
				return true;
			}
		}
		return false;
	}

	private IPluginElement[] findProductBindingElements(IPluginExtension extension) {
		ArrayList elements = new ArrayList();
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return null;
		}
		IPluginObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IPluginElement) {
				IPluginElement element = (IPluginElement) pluginObjects[j];
				// Find product binding elements
				if (element.getName().equals(F_ELEMENT_PRODUCT_BINDING)) {
					elements.add(element);
				}
			}
		}
		// No product binding elements found
		if (elements.size() == 0) {
			return null;
		}
		// Return product binding elements
		return (IPluginElement[]) elements.toArray(new IPluginElement[elements.size()]);
	}

	private IPluginExtension findFirstExtension(String extensionPointID) {
		// Get all the extensions
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		// Get the first extension matching the specified extension point ID
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (extensionPointID.equals(point)) {
				return extensions[i];
			}
		}
		return null;
	}

}
