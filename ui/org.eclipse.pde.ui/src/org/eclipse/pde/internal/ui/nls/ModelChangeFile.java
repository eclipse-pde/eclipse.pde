/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
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

import java.util.ArrayList;
import org.eclipse.core.resources.IFile;

public class ModelChangeFile {
	private IFile fFile;
	private ModelChange fModel;
	private ArrayList<ModelChangeElement> fChanges = new ArrayList<>();
	private int fNumChanges = 0;

	public ModelChangeFile(IFile file, ModelChange model) {
		fFile = file;
		fModel = model;
	}

	protected IFile getFile() {
		return fFile;
	}

	protected ModelChange getModel() {
		return fModel;
	}

	public void add(ModelChangeElement element) {
		if (fChanges.add(element))
			fNumChanges += 1;
	}

	protected int getNumChanges() {
		return fNumChanges;
	}

	protected ArrayList<ModelChangeElement> getChanges() {
		return fChanges;
	}
}
