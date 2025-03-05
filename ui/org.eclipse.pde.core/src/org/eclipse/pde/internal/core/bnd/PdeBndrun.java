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
package org.eclipse.pde.internal.core.bnd;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import aQute.bnd.build.Container;
import aQute.bnd.build.Container.TYPE;
import aQute.bnd.build.DownloadBlocker;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Verifier;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.Strategy;
import aQute.bnd.version.Version;
import aQute.bnd.version.VersionRange;
import biz.aQute.resolve.Bndrun;

class PdeBndrun extends Bndrun {

	private String name;

	PdeBndrun(Workspace workspace, File propertiesFile, String name) throws Exception {
		super(workspace, propertiesFile);
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Container getBundle(String bsn, String range, Strategy strategy, Map<String, String> attrs)
			throws Exception {
		Container bundle = super.getBundle(bsn, range, strategy, attrs);
		if (bundle == null || bundle.getType() == TYPE.ERROR) {
			// workaround for https://github.com/bndtools/bnd/issues/6481
			// and https://github.com/bndtools/bnd/issues/6482
			// derived from the super class
			List<RepositoryPlugin> plugins = getPlugins(RepositoryPlugin.class);
			range = Objects.requireNonNullElse(range, "0"); //$NON-NLS-1$
			attrs = Objects.requireNonNullElse(attrs, Collections.emptyMap());
			if (strategy == Strategy.EXACT) {
				if (!Verifier.isVersion(range)) {
					return bundle;
				}
				Version version = new Version(range);
				for (RepositoryPlugin plugin : plugins) {
					DownloadBlocker blocker = new DownloadBlocker(this);
					File result = plugin.get(bsn, version, attrs, blocker);
					if (result != null) {
						return toContainer(bsn, range, attrs, result, blocker);
					}
				}
			} else {
				VersionRange versionRange = VERSION_ATTR_LATEST.equals(range) ? new VersionRange("0") //$NON-NLS-1$
						: new VersionRange(range);
				SortedMap<Version, RepositoryPlugin> versions = new TreeMap<>();
				for (RepositoryPlugin plugin : plugins) {
					try {
						SortedSet<Version> vs = plugin.versions(bsn);
						if (vs != null) {
							for (Version v : vs) {
								if (!versions.containsKey(v) && versionRange.includes(v)) {
									versions.put(v, plugin);
								}
							}
						}
					} catch (UnsupportedOperationException ose) {
					}
				}
				if (!versions.isEmpty()) {
					Version provider = switch (strategy) {
					case HIGHEST -> versions.lastKey();
					case LOWEST -> versions.firstKey();
					case EXACT -> null;
					};
					if (provider != null) {
						RepositoryPlugin repo = versions.get(provider);
						if (repo != null) {
							String version = provider.toString();
							DownloadBlocker blocker = new DownloadBlocker(this);
							File result = repo.get(bsn, provider, attrs, blocker);
							if (result != null) {
								return toContainer(bsn, version, attrs, result, blocker);
							}
						}
					}
				}
			}
		}
		return bundle;
	}

}
