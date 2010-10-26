/*******************************************************************************
 *  Copyright (c) 2005, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.nls;

import java.util.*;
import org.eclipse.core.resources.IFile;
import org.eclipse.pde.core.plugin.IPluginModelBase;

public class ModelChangeTable {

	private Hashtable fChangeTable = new Hashtable();
	private ArrayList fPreSelected = new ArrayList();

	public void addToChangeTable(IPluginModelBase model, IFile file, Object change, boolean selected) {
		if (change == null)
			return;
		ModelChange modelChange;
		if (fChangeTable.containsKey(model))
			modelChange = (ModelChange) fChangeTable.get(model);
		else {
			modelChange = new ModelChange(model, selected);
			fChangeTable.put(model, modelChange);
			if (selected)
				fPreSelected.add(modelChange);
		}
		modelChange.addChange(file, new ModelChangeElement(modelChange, change));
	}

	public Collection getAllModelChanges() {
		return fChangeTable.values();
	}

	public ModelChange getModelChange(IPluginModelBase modelKey) {
		if (fChangeTable.containsKey(modelKey))
			return (ModelChange) fChangeTable.get(modelKey);
		return null;
	}

	public Object[] getPreSelected() {
		return fPreSelected.toArray();
	}

	public boolean hasPreSelected() {
		return fPreSelected.size() > 0;
	}

	public boolean isEmpty() {
		return fChangeTable.size() == 0;
	}
}
