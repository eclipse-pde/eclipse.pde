/*******************************************************************************
 *  Copyright (c) 2006, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.plugin;

import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class AbbreviatedFragmentModel extends WorkspaceFragmentModel {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private String[] fExtensionPointIDs;

	/**
	 * @param file
	 * @param extensionPointIDs
	 */
	public AbbreviatedFragmentModel(IFile file, String[] extensionPointIDs) {
		super(file, true);

		fExtensionPointIDs = extensionPointIDs;
	}

	/**
	 * @param file
	 * @param extensionPointID
	 */
	public AbbreviatedFragmentModel(IFile file, String extensionPointID) {
		super(file, true);

		fExtensionPointIDs = new String[] {extensionPointID};
	}

	@Override
	public void load(InputStream stream, boolean outOfSync) throws CoreException {
		load(stream, outOfSync, new AbbreviatedPluginHandler(fExtensionPointIDs));
	}

}
