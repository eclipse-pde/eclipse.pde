/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;

public abstract class AbstractPDEMarkerResolution implements IMarkerResolution2 {

	protected int fType;

	public AbstractPDEMarkerResolution(int type) {
		fType = type;
	}
	
	public Image getImage() {
		return null;
	}

}
