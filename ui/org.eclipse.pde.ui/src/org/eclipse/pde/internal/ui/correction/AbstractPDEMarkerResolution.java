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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	public static final int CREATE_TYPE = 1;
	public static final int RENAME_TYPE = 2;
	public static final int REMOVE_TYPE = 3;

	protected int fType;
	/**
	 * This variable will only be available after run() is called.
	 * ie. its not the be used in getImage()/getType()/getDesciption()
	 */
	protected IResource fResource;

	public AbstractPDEMarkerResolution(int type) {
		fType = type;
	}

	public Image getImage() {
		return null;
	}

	public int getType() {
		return fType;
	}

	public String getDescription() {
		return getLabel();
	}

	public void run(IMarker marker) {
		fResource = marker.getResource();
		ModelModification modification = new ModelModification((IFile) marker.getResource()) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				createChange(model);
			}
		};
		PDEModelUtility.modifyModel(modification, null);
	}

	protected abstract void createChange(IBaseModel model);

}
