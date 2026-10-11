/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.tools.emf.ui.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

/**
 * Reads the Bundle-SymbolicName of a bundle manifest, ignoring directives and attributes and handling wrapped
 * header lines.
 */
@SuppressWarnings("restriction") // uses org.eclipse.pde.internal
public final class BundleSymbolicName {

	private BundleSymbolicName() {
	}

	/** Returns the symbolic name of the project's bundle or null if it has none. */
	public static String of(IProject project) {
		final IFile manifest = PDEProject.getManifest(project);
		if (manifest == null || !manifest.exists()) {
			return null;
		}
		try (InputStream in = manifest.getContents()) {
			return parse(in);
		} catch (CoreException | IOException e) {
			return null;
		}
	}

	/** Returns the symbolic name of the bundle in the given directory or null if it has none. */
	public static String of(File bundleDirectory) {
		if (bundleDirectory == null || !bundleDirectory.isDirectory()) {
			return null;
		}
		final File manifest = new File(bundleDirectory, "META-INF/MANIFEST.MF"); //$NON-NLS-1$
		if (!manifest.isFile()) {
			return null;
		}
		try (InputStream in = Files.newInputStream(manifest.toPath())) {
			return parse(in);
		} catch (IOException e) {
			return null;
		}
	}

	/** Returns the symbolic name declared by the manifest in the stream or null if it has none. */
	public static String parse(InputStream manifest) throws IOException {
		final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		try {
			ManifestElement.parseBundleManifest(manifest, headers);
		} catch (BundleException e) {
			return null;
		}
		return fromHeader(headers.get(Constants.BUNDLE_SYMBOLICNAME));
	}

	/** Returns the symbolic name of a Bundle-SymbolicName header value, for example {@code a.b;singleton:=true}. */
	public static String fromHeader(String header) {
		if (header == null) {
			return null;
		}
		try {
			final ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
			return elements == null || elements.length == 0 ? null : elements[0].getValue();
		} catch (BundleException e) {
			return null;
		}
	}
}
