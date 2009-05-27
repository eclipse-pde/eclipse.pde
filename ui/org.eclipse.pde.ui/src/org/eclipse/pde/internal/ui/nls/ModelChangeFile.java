/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
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
	private ArrayList fChanges = new ArrayList();
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

	protected ArrayList getChanges() {
		return fChanges;
	}
}
