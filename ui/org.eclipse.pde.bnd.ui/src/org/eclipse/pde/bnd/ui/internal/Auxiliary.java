/*******************************************************************************
 * Copyright (c) 2015, 2024 bndtools project and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Kriens <Peter.Kriens@aqute.biz> - initial API and implementation
 *     BJ Hargrave <bj@hargrave.dev> - ongoing enhancements
 *     Christoph Läubrich - adapt to PDE codebase
 *******************************************************************************/
package org.eclipse.pde.bnd.ui.internal;

import java.io.IOException;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;

import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Verifier;

/**
 * This class extends the dynamic imports of bndlib with any exported package
 * from OSGi that specifies a 'bnd-plugins' attribute. Its value is either true
 * or a version range on the bnd version.
 */
@Component(service=WeavingHook.class)
public class Auxiliary implements WeavingHook {
	private final BundleTracker<Bundle>				tracker;
	private final Deque<String>						delta	= new ConcurrentLinkedDeque<>();
	
	@Activate
	public Auxiliary(BundleContext context) {
		this.tracker = new BundleTracker<Bundle>(context, Bundle.RESOLVED + Bundle.ACTIVE + Bundle.STARTING, null) {
			@Override
			public Bundle addingBundle(Bundle bundle, BundleEvent event) {
				if (!doImport(bundle.getHeaders()
					.get(Constants.EXPORT_PACKAGE)))
					return null;

				return super.addingBundle(bundle, event);
			}

		};
		this.tracker.open();
	}

	/*
	 * Parse the exports and see
	 */
	private boolean doImport(String exports) {
		if (exports == null || exports.isEmpty())
			return false;

		Parameters out = new Parameters();

		Parameters p = new Parameters(exports);
		for (Entry<String, Attrs> e : p.entrySet()) {
			Attrs attrs = e.getValue();
			if (attrs == null)
				continue;

			String plugins = attrs.get("bnd-plugins");
			if (plugins == null)
				continue;

			if (!(plugins.isEmpty() || "true".equalsIgnoreCase(plugins))) {
				if (Verifier.isVersionRange(plugins)) {
					VersionRange range = new VersionRange(plugins);
					if (!range.includes(new Version(3, 0, 0)))
						continue;
				}
			}

			//
			// Ok, matched
			//

			String v = attrs.getVersion();
			if (v == null)
				v = "0";

			for (Iterator<String> i = attrs.keySet()
				.iterator(); i.hasNext();) {
				String key = i.next();
				if (key.endsWith(":"))
					i.remove();
			}

			if (Verifier.isVersion(v)) {
				Version version = new Version(v);
				attrs.put("version", new VersionRange(VersionRange.LEFT_CLOSED, version, version, VersionRange.RIGHT_CLOSED).toString());
			}
			out.put(e.getKey(), attrs);
		}
		if (out.isEmpty())
			return false;

		delta.offerLast(out.toString());
		return true;
	}

	@Override
	public void weave(WovenClass wovenClass) {
		if (delta.isEmpty()) {
			return;
		}
		BundleWiring wiring = wovenClass.getBundleWiring();
		if (wiring == null)
			return;

		if (wiring.getBundle() != FrameworkUtil.getBundle(Workspace.class))
			return;

		List<String> dynamicImports = wovenClass.getDynamicImports();
		for (String dynamicImport; (dynamicImport = delta.pollFirst()) != null;) {
			dynamicImports.add(dynamicImport);
		}
	}

	@Deactivate
	public void close() throws IOException {
		tracker.close();
	}

}
