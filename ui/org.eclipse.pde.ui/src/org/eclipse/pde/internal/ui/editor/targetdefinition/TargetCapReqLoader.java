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
package org.eclipse.pde.internal.ui.editor.targetdefinition;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.bnd.ui.model.resolution.CapReq;
import org.eclipse.pde.bnd.ui.model.resolution.CapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.JarFileCapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.ResourceCapReqLoader;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.osgi.framework.BundleException;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceBuilder;

class TargetCapReqLoader implements CapReqLoader {

	private ITargetDefinition target;
	private IEditorInput editorInput;

	public TargetCapReqLoader(ITargetDefinition target, IEditorInput editorInput) {
		this.target = target;
		this.editorInput = editorInput;
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

	@Override
	public String getShortLabel() {
		return String.format("Target Platform '%s'", target.getName()); //$NON-NLS-1$
	}

	@Override
	public String getLongLabel() {
		if (editorInput instanceof IPathEditorInput path) {
			return String.format("%s (%s)", getShortLabel(), path.getPath()); //$NON-NLS-1$
		}
		return getShortLabel();
	}

	@Override
	public CapReq loadCapReq(IProgressMonitor monitor) throws Exception {
		TargetBundle[] bundles = target.getBundles();
		if (bundles == null || bundles.length == 0) {
			// TODO need a way to trigger a reload when target changes!
			return new CapReq(Map.of(), Map.of());
		}
		Map<String, Collection<Capability>> capabilityResults = new LinkedHashMap<>();
		Map<String, Collection<Requirement>> requirementResults = new LinkedHashMap<>();
		monitor.beginTask("read bundles", bundles.length); //$NON-NLS-1$
		for (int i = 0; i < bundles.length; i++) {
			TargetBundle targetBundle = bundles[i];
			if (targetBundle.isSourceBundle()) {
				continue;
			}
			monitor.subTask((i + 1) + " / " + bundles.length); //$NON-NLS-1$
			CapReqLoader loader = toLoader(targetBundle);
			if (loader != null) {
				CapReq result = loader.loadCapReq(null);
				mergeMaps(result.capabilities(), capabilityResults);
				mergeMaps(result.requirements(), requirementResults);
			}
		}
		return new CapReq(capabilityResults, requirementResults);
	}

	private CapReqLoader toLoader(TargetBundle targetBundle) throws IOException, BundleException {
		BundleInfo info = targetBundle.getBundleInfo();
		URI location = info.getLocation();
		if (location != null && "file".equals(location.getScheme())) { //$NON-NLS-1$
			File jarFile = new File(location);
			if (jarFile.isFile()) {
				return new JarFileCapReqLoader(jarFile);
			}
		}
		String manifest = info.getManifest();
		if (manifest != null && !manifest.isBlank()) {
			// TODO use equinox manifest reader
			ResourceBuilder builder = new ResourceBuilder();
			Map<String, String> bundleManifest;
			try (ByteArrayInputStream stream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8))) {
				bundleManifest = ManifestElement.parseBundleManifest(stream);
			}
			Processor processor = new Processor();
			processor.addProperties(bundleManifest);
			builder.addManifest(processor);
			return new ResourceCapReqLoader(builder.build());
		}
		return null;
	}

	private static <K, V> void mergeMaps(Map<K, Collection<V>> from, Map<K, Collection<V>> into) {
		for (Entry<K, Collection<V>> entry : from.entrySet()) {
			K key = entry.getKey();
			into.merge(key, entry.getValue(), (a, b) -> Stream.concat(a.stream(), b.stream()).distinct().toList());
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(editorInput, target);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TargetCapReqLoader other = (TargetCapReqLoader) obj;
		return Objects.equals(editorInput, other.editorInput) && Objects.equals(target, other.target);
	}

}
