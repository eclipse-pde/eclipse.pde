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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.pde.bnd.ui.model.resolution.RequirementWrapper;
import org.eclipse.pde.bnd.ui.tasks.CapReqLoader;
import org.eclipse.pde.bnd.ui.tasks.ResourceCapReqLoader;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.TargetBundle;
import org.osgi.resource.Capability;

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
	public Map<String, List<Capability>> loadCapabilities() throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		TargetBundle[] bundles = target.getBundles();
		if (bundles != null) {
			Map<String, List<Capability>> result = new LinkedHashMap<>();
			for (TargetBundle targetBundle : bundles) {
				CapReqLoader loader = toLoader(targetBundle);
				if (loader != null) {
					Map<String, List<Capability>> toMerge = loader.loadCapabilities();
					Set<Entry<String, List<Capability>>> set = toMerge.entrySet();
					for (Entry<String, List<Capability>> entry : set) {
						result.merge(entry.getKey(), entry.getValue(),
								(a, b) -> Stream.concat(a.stream(), b.stream()).toList());
					}
				}
			}
			return result;
		}
		return Map.of();
	}

	private CapReqLoader toLoader(TargetBundle targetBundle) throws IOException {
		BundleInfo info = targetBundle.getBundleInfo();
		String manifest = info.getManifest();
		if (manifest != null && !manifest.isBlank()) {
			ResourceBuilder builder = new ResourceBuilder();
			Properties properties = new Properties();
			try (ByteArrayInputStream stream = new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8))) {
				properties.load(stream);
			}
			builder.addManifest(new Processor(properties));
			return new ResourceCapReqLoader(builder.build());
		}
		// TODO use adapter pattern instead
		return null;
	}

	@Override
	public Map<String, List<RequirementWrapper>> loadRequirements() throws Exception {
		if (!target.isResolved()) {
			target.resolve(null);
		}
		TargetBundle[] bundles = target.getBundles();
		if (bundles != null) {
			Map<String, List<RequirementWrapper>> result = new LinkedHashMap<>();
			for (TargetBundle targetBundle : bundles) {
				CapReqLoader loader = toLoader(targetBundle);
				if (loader != null) {
					Map<String, List<RequirementWrapper>> toMerge = loader.loadRequirements();
					Set<Entry<String, List<RequirementWrapper>>> set = toMerge.entrySet();
					for (Entry<String, List<RequirementWrapper>> entry : set) {
						result.merge(entry.getKey(), entry.getValue(),
								(a, b) -> Stream.concat(a.stream(), b.stream()).toList());
					}
				}
			}
			return result;
		}
		return Map.of();
	}

}
