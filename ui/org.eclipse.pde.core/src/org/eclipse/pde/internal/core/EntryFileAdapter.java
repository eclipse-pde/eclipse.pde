/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;

public class EntryFileAdapter extends FileAdapter {
	private ModelEntry entry;

	/**
	 * Constructor for EntryFileAdapter.
	 * @param parent
	 * @param file
	 */
	public EntryFileAdapter(ModelEntry entry, File file, IFileAdapterFactory factory) {
		super(null, file, factory);
		this.entry = entry;
	}
	
	public ModelEntry getEntry() {
		return entry;
	}
}
