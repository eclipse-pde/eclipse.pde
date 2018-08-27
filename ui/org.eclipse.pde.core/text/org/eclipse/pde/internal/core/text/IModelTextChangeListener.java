/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.text;

import org.eclipse.pde.core.IModelChangedListener;
import org.eclipse.text.edits.TextEdit;

public interface IModelTextChangeListener extends IModelChangedListener {

	TextEdit[] getTextOperations();

	/**
	 * Get a human readable name for the given TextEdit for use in a refactoring
	 * preview, for instance.
	 *
	 * @param edit
	 * 			the edit to get a name for
	 * @return
	 * 			the name associated to the given edit, or null if there is none
	 */
	String getReadableName(TextEdit edit);
}
