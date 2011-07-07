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

package org.eclipse.pde.internal.ui.editor.validation;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.builders.CompilerFlags;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.osgi.framework.InvalidSyntaxException;

/**
 * ControlValidationUtility
 *
 */
public class ControlValidationUtility {

	public static boolean validateRequiredField(String value, IValidatorMessageHandler validator, int messageType) {
		// Check to see if a value was specified
		if (value.length() == 0) {
			validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgValueMustBeSpecified, messageType);
			return false;
		}
		return true;
	}

	public static boolean validateTranslatableField(String value, IValidatorMessageHandler validator, IPluginModelBase model, IProject project) {

		// Check the compiler flag and translate it into a message type
		int messageType = AbstractControlValidator.getMessageType(project, CompilerFlags.P_NOT_EXTERNALIZED);
		// If the message type is none, no validation is required
		// Same as IGNORE
		if (messageType == IMessageProvider.NONE) {
			return true;
		}

		// Check to see if the name has been externalized
		if (value.startsWith("%") == false) { //$NON-NLS-1$
			validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgValueNotExternalized, messageType);
			return false;
		}

		// Check to see if the key is in the plugin's property file
		if (model instanceof AbstractNLModel) {
			NLResourceHelper helper = ((AbstractNLModel) model).getNLResourceHelper();
			if ((helper == null) || (helper.resourceExists(value) == false)) {
				validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgKeyNotFound, messageType);
				return false;
			}
		}
		return true;
	}

	public static boolean validateVersionField(String value, IValidatorMessageHandler validator) {
		// Check for invalid version
		IStatus status = VersionUtil.validateVersion(value);
		if (status.isOK() == false) {
			validator.addMessage(status.getMessage(), AbstractControlValidator.getMessageType(status));
			return false;
		}
		return true;
	}

	public static boolean validatePlatformFilterField(String value, IValidatorMessageHandler validator) {
		// Check to see if the platform filter syntax is valid
		try {
			PDECore.getDefault().getBundleContext().createFilter(value);
		} catch (InvalidSyntaxException ise) {
			validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgFilterInvalidSyntax, IMessageProvider.ERROR);
			return false;
		}

		return true;
	}

	public static boolean validateActivatorField(String value, IValidatorMessageHandler validator, IProject project) {

		// Check the compiler flag and translate it into a message type
		int messageType = AbstractControlValidator.getMessageType(project, CompilerFlags.P_UNKNOWN_CLASS);
		// If the message type is none, no validation is required
		// Same as IGNORE
		if (messageType == IMessageProvider.NONE) {
			return true;
		}

		// Check to see if the class is on the plug-in classpath
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				// Look for this activator in the project's classpath
				if (!PDEJavaHelper.isOnClasspath(value, javaProject)) {
					validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgNotOnClasspath, messageType);
					return false;
				}
			}
		} catch (CoreException ce) {
			// Ignore
		}

		return true;
	}

	public static boolean validateFragmentHostPluginField(String value, IValidatorMessageHandler validator, IProject project) {

		// Check the compiler flag and translate it into a message type
		// If the message type is none, it is the same as IGNORE
		int reqAttMessageType = AbstractControlValidator.getMessageType(project, CompilerFlags.P_NO_REQUIRED_ATT);
		// Check to see if the host plug-in was defined
		if ((reqAttMessageType != IMessageProvider.NONE) && validateRequiredField(value, validator, reqAttMessageType) == false) {
			return false;
		}
		// Check the compiler flag and translate it into a message type
		int unresImpMessageType = AbstractControlValidator.getMessageType(project, CompilerFlags.P_UNRESOLVED_IMPORTS);
		// If the message type is none, no validation is required
		// Same as IGNORE		
		if (unresImpMessageType == IMessageProvider.NONE) {
			return true;
		}
		// Check to see if the host plugin is defined, enabled and not a 
		// fragment itself
		IPluginModelBase hostModel = PluginRegistry.findModel(value);
		if ((hostModel == null) || (hostModel instanceof IFragmentModel) || (hostModel.isEnabled() == false)) {
			validator.addMessage(PDEUIMessages.ControlValidationUtility_errorMsgPluginUnresolved, unresImpMessageType);
			return false;
		}

		return true;
	}

}
