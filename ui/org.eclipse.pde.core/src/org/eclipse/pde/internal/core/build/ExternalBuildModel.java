/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;

public class ExternalBuildModel extends BuildModel {

	private static final long serialVersionUID = 1L;
	private String fInstallLocation;

	public ExternalBuildModel(String installLocation) {
		fInstallLocation = installLocation;
	}

	public String getInstallLocation() {
		return fInstallLocation;
	}

	public boolean isEditable() {
		return false;
	}

	public void load() {
		try {
			URL url = null;
			File file = new File(getInstallLocation());
			if (file.isFile() && file.getName().endsWith(".jar")) { //$NON-NLS-1$
				url = new URL("jar:file:" + file.getAbsolutePath() + "!/build.properties"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				url = new URL("file:" + file.getAbsolutePath() + Path.SEPARATOR + "build.properties"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			InputStream stream = url.openStream();
			load(stream, false);
			stream.close();
		} catch (IOException e) {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
	}

	protected void updateTimeStamp() {
		updateTimeStamp(getLocalFile());
	}

	private File getLocalFile() {
		File file = new File(getInstallLocation());
		return (file.isFile()) ? file : new File(file, "build.properties");		 //$NON-NLS-1$
	}

	public boolean isInSync() {
		return true;
	}
}
