/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
* This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
*******************************************************************************/
package org.eclipse.pde.bnd.ui.model.resolution;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Manifest;

import aQute.bnd.osgi.resource.ResourceBuilder;

/**
 * Using a manifest file supply its cap and req
 */
public class ManifestCapReqLoader implements CapReqLoader {

	private final File manifestFile;
	private ResourceCapReqLoader resourceCapReqLoader;

	public ManifestCapReqLoader(File manifestFile) {
		this.manifestFile = manifestFile;
	}

	@Override
	public synchronized void close() throws IOException {
		if (resourceCapReqLoader != null) {
			resourceCapReqLoader.close();
			resourceCapReqLoader = null;
		}
	}

	@Override
	public String getShortLabel() {
		return manifestFile.getName();
	}

	@Override
	public String getLongLabel() {
		return manifestFile.getAbsolutePath();
	}

	@Override
	public CapReq loadCapReq() throws Exception {
		return loadManifest().loadCapReq();
	}

	private synchronized ResourceCapReqLoader loadManifest() throws IOException {
		if (resourceCapReqLoader == null) {
			Manifest manifest;
			try (FileInputStream stream = new FileInputStream(manifestFile)) {
				manifest = new Manifest(stream);
			}
			ResourceBuilder builder = new ResourceBuilder();
			builder.addManifest(manifest);
			resourceCapReqLoader = new ResourceCapReqLoader(builder.build());
		}
		return resourceCapReqLoader;
	}

}
