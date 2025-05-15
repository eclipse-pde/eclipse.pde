/*******************************************************************************
 * Copyright (c) 2016, 2023 bndtools project and others.
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
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.templating;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

// TODO need to use some kind of cache to avoid repeated downloads
public class DirectDownloadBundleLocator implements BundleLocator {

	@Override
	public File locate(String bsn, String hash, String algo, URI location) throws Exception {

		Path tempFile = Files.createTempFile("download", "jar");
		tempFile.toFile().deleteOnExit();

		try (InputStream stream = location.toURL().openStream()) {
			Files.copy(stream, tempFile);
		}
		return tempFile.toFile();
	}

}
