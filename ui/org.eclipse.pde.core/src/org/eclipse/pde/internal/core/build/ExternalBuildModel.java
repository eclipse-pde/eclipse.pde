/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     EclipseSource Corporation - ongoing enhancements
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.*;
import java.net.URL;
import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.internal.core.ICoreConstants;

public class ExternalBuildModel extends BuildModel {

	private static final long serialVersionUID = 1L;
	private String fInstallLocation;

	public ExternalBuildModel(String installLocation) {
		fInstallLocation = installLocation;
	}

	@Override
	public String getInstallLocation() {
		return fInstallLocation;
	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public void load() {
		try {
			URL url = null;
			File file = new File(getInstallLocation());
			if (file.isFile()) {
				url = new URL("jar:file:" + file.getAbsolutePath() + "!/build.properties"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				url = new URL("file:" + file.getAbsolutePath() + IPath.SEPARATOR + ICoreConstants.BUILD_FILENAME_DESCRIPTOR); //$NON-NLS-1$
			}
			try (InputStream stream = url.openStream()) {
				load(stream, false);
			}
		} catch (IOException e) {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
	}

	@Override
	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		return (file.isFile()) ? file : new File(file, ICoreConstants.BUILD_FILENAME_DESCRIPTOR);
	}

	@Override
	public boolean isInSync() {
		return true;
	}
}
