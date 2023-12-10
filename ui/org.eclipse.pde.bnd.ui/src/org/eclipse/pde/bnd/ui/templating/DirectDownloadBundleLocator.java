/*******************************************************************************
 * Copyright (c) 2016, 2019 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Neil Bartlett <njbartlett@gmail.com>  - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *******************************************************************************/
package org.bndtools.core.templating.repobased;

import java.io.File;
import java.net.URI;

import aQute.lib.io.IO;

// TODO need to use some kind of cache to avoid repeated downloads
public class DirectDownloadBundleLocator implements BundleLocator {

	@Override
	public File locate(String bsn, String hash, String algo, URI location) throws Exception {
		File tempFile = File.createTempFile("download", "jar");
		tempFile.deleteOnExit();

		IO.copy(location.toURL(), tempFile);
		return tempFile;
	}

}
