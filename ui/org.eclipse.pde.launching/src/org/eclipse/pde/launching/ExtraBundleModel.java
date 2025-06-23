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
package org.eclipse.pde.launching;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.plugin.AbstractPluginModelBase;
import org.eclipse.pde.internal.core.plugin.PluginBase;
import org.osgi.framework.BundleException;

/**
 * Extra bundle model can be used in case where we want to inject additional jars that are not managed by the current workspace anywhere.
 */
class ExtraBundleModel extends AbstractPluginModelBase {

	private static final long serialVersionUID = 1L;
	private final Path path;

	public ExtraBundleModel(Path path, Map<String, String> manifest) throws BundleException {
		this.path = path;
		BundleDescription bundleDescription = StateObjectFactory.defaultFactory.createBundleDescription(null, manifestToProperties(manifest), path.toString(), 0);
		setBundleDescription(bundleDescription);
	}

	private static Dictionary<String, String> manifestToProperties(Map<String, String> d) {
		Dictionary<String, String> result = new Hashtable<>();
		for (String key : d.keySet()) {
			result.put(key, d.get(key));
		}
		return result;
	}

	@Override
	public BundleDescription getBundleDescription() {
		return super.getBundleDescription();
	}

	@Override
	public void load() throws CoreException {

	}

	@Override
	public boolean isEditable() {
		return false;
	}

	@Override
	public String getInstallLocation() {
		return path.toString();
	}

	@Override
	public IPluginBase createPluginBase() {
		PluginBase pluginBase = new PluginBase(true) {

			private static final long serialVersionUID = 1L;

			@Override
			public void write(String indent, PrintWriter writer) {
			}

			@Override
			public String getId() {
				return getBundleDescription().getSymbolicName();
			}

			@Override
			public String getVersion() {
				return getBundleDescription().getVersion().toString();
			}
		};
		pluginBase.setModel(this);
		return pluginBase;
	}

	@Override
	protected NLResourceHelper createNLResourceHelper() {
		return null;
	}

}
