/*******************************************************************************
 *  Copyright (c) 2005, 2018 IBM Corporation and others.
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
package org.eclipse.pde.internal.ui.nls;

import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelChangeTable {

	private Map<IPluginModelBase, ModelChange> fChangeTable = new LinkedHashMap<>();
	private ArrayList<ModelChange> fPreSelected = new ArrayList<>();

	public void addToChangeTable(IPluginModelBase model, IFile file, Object change, boolean selected) {
		if (change == null)
			return;
		ModelChange modelChange;
		if (fChangeTable.containsKey(model))
			modelChange = fChangeTable.get(model);
		else {
			modelChange = new ModelChange(model, selected);
			fChangeTable.put(model, modelChange);
			if (selected)
				fPreSelected.add(modelChange);
		}
		modelChange.addChange(file, new ModelChangeElement(modelChange, change));
	}

	public Collection<ModelChange> getAllModelChanges() {
		return fChangeTable.values();
	}

	public ModelChange getModelChange(IPluginModelBase modelKey) {
		if (fChangeTable.containsKey(modelKey))
			return fChangeTable.get(modelKey);
		return null;
	}

	public Object[] getPreSelected() {
		return fPreSelected.toArray();
	}

	public boolean hasPreSelected() {
		return !fPreSelected.isEmpty();
	}

	public boolean isEmpty() {
		return fChangeTable.isEmpty();
	}
}
