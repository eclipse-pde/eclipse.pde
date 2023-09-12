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

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;

/**
 * Implementation of a <code>ISelectionValidator</code> to validate the
 * type of an element.
 * Empty selections are not accepted.
 */
public class JarSelectionValidator implements ISelectionStatusValidator {

	private final Class<?>[] fAcceptedTypes;
	private final boolean fAllowMultipleSelection;

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
			return Status.OK_STATUS;
		}
		return Status.error("", null); //$NON-NLS-1$
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
