/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.zip.CRC32;

import org.eclipse.core.runtime.CoreException;


/**
 * Class file that uses a CRC for its mod stamp.
 */
public abstract class CRCClassFile extends AbstractClassFile {

	/**
	 * Used to compute CRC codes
	 */
	private static CRC32 fgCRC32 = new CRC32();
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractClassFile#getModificationStamp()
	 */
	public IModificationStamp getModificationStamp() {
		try {
			byte[] contents = getContents();
			fgCRC32.reset();
			fgCRC32.update(contents);
			return new ModificationStamp(fgCRC32.getValue(), contents);
		} catch (CoreException e) {
		}
		return new ModificationStamp(0L, null);
	}


}
