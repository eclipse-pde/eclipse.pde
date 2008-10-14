/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.pde.api.tools.internal.provisional.IClassFile.IModificationStamp;

/**
 * Implementation of a modification stamp.
 */
public class ModificationStamp implements IModificationStamp {
	private long fStamp;
	private byte[] fContents;
	
	public ModificationStamp(long stamp, byte[] contents) {
		fStamp = stamp;
		fContents = contents;
	}

	public byte[] getContents() {
		return fContents;
	}

	public long getModificationStamp() {
		return fStamp;
	}
	
}