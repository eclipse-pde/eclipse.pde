/*******************************************************************************
 *  Copyright (c) 2007, 2008 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.pde.core.plugin.IPluginModelBase;

public abstract class RefactoringInfo {

	private boolean fUpdateReferences = true;

	private String fNewID;

	protected Object fSelection;

	public abstract IPluginModelBase getBase();

	public Object getSelection() {
		return fSelection;
	}

	public void setSelection(Object selection) {
		fSelection = selection;
	}

	public boolean isUpdateReferences() {
		return fUpdateReferences;
	}

	public void setUpdateReferences(boolean updateReferences) {
		fUpdateReferences = updateReferences;
	}

	public String getNewValue() {
		return fNewID;
	}

	public void setNewValue(String newName) {
		fNewID = newName;
	}

	public abstract String getCurrentValue();

}
