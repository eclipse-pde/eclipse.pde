/*******************************************************************************
 *  Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.plugin;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class JarSelectionValidator implements ISelectionStatusValidator {

	private Class<?>[] fAcceptedTypes;
	private boolean fAllowMultipleSelection;

	/**
	 * @param acceptedTypes The types accepted by the validator
	 * @param allowMultipleSelection If set to <code>true</code>, the validator
	 * allows multiple selection.
	 */
	public JarSelectionValidator(Class<?>[] acceptedTypes, boolean allowMultipleSelection) {
		Assert.isNotNull(acceptedTypes);
		fAcceptedTypes = acceptedTypes;
		fAllowMultipleSelection = allowMultipleSelection;
	}

	@Override
	public IStatus validate(Object[] elements) {
		if (isValidSelection(elements)) {
			return new Status(IStatus.OK, PDEPlugin.getPluginId(), IStatus.OK, "", //$NON-NLS-1$
					null);
		}
		return new Status(IStatus.ERROR, PDEPlugin.getPluginId(), IStatus.ERROR, "", //$NON-NLS-1$
				null);
	}

	private boolean isValidSelection(Object[] selection) {
		if (selection.length == 0) {
			return false;
		}

		if (!fAllowMultipleSelection && selection.length != 1) {
			return false;
		}

		for (Object o : selection) {
			if (!isValid(o)) {
				return false;
			}
		}
		return true;
	}

	public boolean isValid(Object element) {
		for (Class<?> acceptedType : fAcceptedTypes) {
			if (acceptedType.isInstance(element)) {
				return true;
			}
		}
		return false;
	}
}
