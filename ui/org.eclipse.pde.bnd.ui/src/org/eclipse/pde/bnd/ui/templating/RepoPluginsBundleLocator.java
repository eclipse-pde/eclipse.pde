/*******************************************************************************
 * Copyright (c) 2015, 2019 bndtools project and others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aQute.bnd.service.RepositoryPlugin;
import aQute.lib.io.IO;

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
		File tempFile = File.createTempFile("download", "jar");
		tempFile.deleteOnExit();

		IO.copy(location.toURL(), tempFile);
		return tempFile;
	}

}
