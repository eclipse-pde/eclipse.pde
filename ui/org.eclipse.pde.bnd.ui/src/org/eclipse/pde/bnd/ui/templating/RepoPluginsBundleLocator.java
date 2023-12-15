/*******************************************************************************
 * Copyright (c) 2015, 2023 bndtools project and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;

public class RepoPluginsBundleLocator implements BundleLocator {

	private final List<RepositoryPlugin> plugins;

	public RepoPluginsBundleLocator(List<RepositoryPlugin> plugins) {
		this.plugins = plugins;
	}

	@Override
	public File locate(String bsn, String hash, String algo, URI location) throws Exception {
		Map<String, String> searchProps = new HashMap<>();
		searchProps.put("version", "hash");
		searchProps.put("hash", algo + ":" + hash);

		for (RepositoryPlugin plugin : plugins) {
			try {
				File file = plugin.get(bsn, null, searchProps);
				if (file != null) {
					return file;
				}
			} catch (Exception e) {
				// ignore
			}
		}

		// Fall back to direct download
		// TODO: need some kind of download/cache service to avoid repeated
		// downloads
		Path tempFile = Files.createTempFile("download", "jar");
		tempFile.toFile().deleteOnExit();

		try (InputStream stream = location.toURL().openStream()) {
			Files.copy(stream, tempFile);
		}
		return tempFile.toFile();
	}

}
