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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Stream;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.bnd.ui.model.resolution.CapReq;
import org.eclipse.pde.bnd.ui.model.resolution.CapReqLoader;
import org.eclipse.pde.bnd.ui.model.resolution.ResourceCapReqLoader;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceBuilder;

class TargetCapReqLoader implements CapReqLoader {

	private ITargetDefinition target;

	public TargetCapReqLoader(ITargetDefinition target) {
		this.target = target;
	}

	@Override
	public void close() throws IOException {
		// nothing to do
	}

	@Override
	public String getShortLabel() {
		return target.getName();
	}

	@Override
	public String getLongLabel() {
		return getShortLabel();
	}

	@Override
	public CapReq loadCapReq() throws Exception {
		TargetBundle[] bundles = target.getBundles();
		if (bundles == null || bundles.length == 0) {
			// TODO need a way to trigger a reload when target changes!
			return new CapReq(Map.of(), Map.of());
		}
		Map<String, Collection<Capability>> capabilityResults = new LinkedHashMap<>();
		Map<String, Collection<Requirement>> requirementResults = new LinkedHashMap<>();
		for (TargetBundle targetBundle : bundles) {
			if (targetBundle.isSourceBundle()) {
				continue;
			}
			CapReqLoader loader = toLoader(targetBundle);
			if (loader != null) {
				CapReq result = loader.loadCapReq();
				mergeMaps(result.capabilities(), capabilityResults);
				mergeMaps(result.requirements(), requirementResults);
			}
		}
		return new CapReq(capabilityResults, requirementResults);
	}

	private CapReqLoader toLoader(TargetBundle targetBundle) throws IOException {
		BundleInfo info = targetBundle.getBundleInfo();
		// TODO check if we can get a jar as BND offers additional infos when
		// loading from a jar!
		String manifest = info.getManifest();
		if (manifest != null && !manifest.isBlank()) {
			// TODO use equinox manifest reader
			ResourceBuilder builder = new ResourceBuilder();
			Properties properties = new Properties();
			try (ByteArrayInputStream stream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8))) {
				properties.load(stream);
			}
			builder.addManifest(new Processor(properties));
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

}
