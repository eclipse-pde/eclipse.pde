/*******************************************************************************
 *  Copyright (c) 2003, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.pde.core.IModelChangedListener;

public interface IContextPart extends IModelChangedListener {
	boolean isEditable();

	PDEFormPage getPage();

	String getContextId();

	void fireSaveNeeded();

	void cancelEdit();
}
