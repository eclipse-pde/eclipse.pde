/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.bnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.jar.Manifest;

import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.osgi.framework.BundleException;

import aQute.bnd.osgi.ManifestResource;

public class FormatedManifestResource extends ManifestResource {

	public FormatedManifestResource(Manifest manifest) {
		super(manifest);
	}

	@Override
	public void write(OutputStream out) throws IOException {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		super.write(bout);
		try {
			Map<String, String> map = ManifestElement.parseBundleManifest(new ByteArrayInputStream(bout.toByteArray()),
					null);
			try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
				ManifestUtils.writeManifest(map, writer);
			}
		} catch (BundleException e) {
			throw new IOException("invalid manifest", e); //$NON-NLS-1$
		}
	}

}
