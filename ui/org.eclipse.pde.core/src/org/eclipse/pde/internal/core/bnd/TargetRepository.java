/*******************************************************************************
 *  Copyright (c) 2023 Christoph Läubrich and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.bnd;

import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;
import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class TargetRepository extends BaseRepository implements RepositoryPlugin {

	private static final TargetRepository instance = new TargetRepository();

	private TargetRepository() {
	}

	@Override
	public File get(String bsn, aQute.bnd.version.Version version, Map<String, String> properties,
			DownloadListener... listeners) throws Exception {
		Optional<BundleDescription> description = getTargetPlatformState()
				.map(state -> state.getBundle(bsn, convert(version)));
		if (description.isEmpty()) {
			// not found!
			return null;
		}
		Optional<File> bundle = description.map(BundleDescription::getLocation).map(location -> new File(location))
				.filter(File::isFile).or(() -> {
					IPluginModelBase model = PluginRegistry.findModel(description.get());
					if (model != null) {
						IPluginLibrary[] libraries = model.getPluginBase().getLibraries();
						String installLocation = model.getInstallLocation();
						if (libraries.length == 0) {
							return Optional.of(new File(installLocation));
						}
						for (IPluginLibrary library : libraries) {
							if (IPluginLibrary.RESOURCE.equals(library.getType())) {
								continue;
							}
							String name = library.getName();
							String expandedName = ClasspathUtilCore.expandLibraryName(name);
							return Optional.of(new File(installLocation, expandedName));
						}
					}
					return Optional.empty();
				});
		if (bundle.isPresent()) {
			File file = bundle.get();
			for (DownloadListener l : listeners) {
				try {
					l.success(file);
				} catch (Exception e) {
				}
			}
			return file;
		}
		return null;
	}

	@Override
	public boolean canWrite() {
		return true;
	}

	@Override
	public PutResult put(InputStream stream, PutOptions options) throws Exception {
		State state = getTargetPlatformState().get();
		Dictionary<String, String> headers = new Hashtable<>();
		try (JarInputStream jar = new JarInputStream(stream)) {
			Manifest manifest = jar.getManifest();
			Attributes attributes = manifest.getMainAttributes();
			attributes.entrySet().forEach(e -> headers.put(e.getKey().toString(), e.getValue().toString()));
		}
		BundleDescription description = state.getFactory().createBundleDescription(state, headers, null,
				state.getHighestBundleId() + 1);
		PutResult result = new PutResult();
		result.alreadyReleased = state.updateBundle(description);
		if (!result.alreadyReleased) {
			state.addBundle(description);
		}
		result.digest = options.digest;
		return result;
	}

	@Override
	public List<String> list(String glob) throws Exception {

		Stream<String> stream = bundles(null).map(BundleDescription::getSymbolicName).distinct();
		if (glob != null) {
			Instruction pattern = new Instruction(glob);
			stream = stream.filter(bsn -> pattern.matches(bsn));
		}
		return stream.collect(Collectors.toList());
	}

	@Override
	public SortedSet<aQute.bnd.version.Version> versions(String bsn) throws Exception {
		return bundles(bsn).filter(bd -> bd.getLocation() != null).map(bundle -> bundle.getVersion())
				.map(v -> convert(v)).collect(Collectors.toCollection(TreeSet::new));
	}

	@Override
	public String getName() {
		return "PDE Target Platform State"; //$NON-NLS-1$
	}

	@Override
	public String getLocation() {
		return "pde-target-state"; //$NON-NLS-1$
	}

	@Override
	public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements) {
		return ResourceUtils.findProviders(requirements, this::findProvider);
	}

	public List<Capability> findProvider(Requirement requirement) {
		String namespace = requirement.getNamespace();
		return bundles(null).flatMap(resource -> ResourceUtils.capabilityStream(resource, namespace))
				.filter(ResourceUtils.matcher(requirement, ResourceUtils::filterPredicate))
				.collect(ResourceUtils.toCapabilities());
	}

	/**
	 * Aquires a stream of bundles from the current state
	 *
	 * @param bsn
	 *            the bsn to find (exact match) or <code>null</code> to return
	 *            all aviable bundles
	 * @return A stream of bundles from the current PDE state that match the
	 *         given bsn
	 */
	private static Stream<BundleDescription> bundles(String bsn) {
		Optional<State> state = getTargetPlatformState();
		if (state.isEmpty()) {
			return Stream.empty();
		}
		BundleDescription[] bundles;
		if (bsn == null) {
			bundles = state.get().getBundles();
		} else {
			bundles = state.get().getBundles(bsn);
		}
		return Arrays.stream(bundles);
	}

	/**
	 * Aquires the current PDE target platform state
	 *
	 * @return an {@link Optional} describing the current PDE target platform
	 *         state or an empty optional if no state is currently aviable
	 */
	private static Optional<State> getTargetPlatformState() {
		PDECore pde = PDECore.getDefault();
		if (pde == null) {
			// This seems to be called outside of PDE running, so simply assume
			// there is no state..
			return Optional.empty();
		}
		try {
			return Optional.of(pde.getModelManager().getState().getState());
		} catch (RuntimeException e) {
			// PDE is active but there is still a small chance that something
			// still goes wrong, so in this case we return an empty optional,
			// later calls to this method still might succeed then.
			return Optional.empty();
		}
	}

	private static org.osgi.framework.Version convert(aQute.bnd.version.Version version) {
		return new org.osgi.framework.Version(version.getMajor(), version.getMinor(), version.getMicro(),
				version.getQualifier());
	}

	private static aQute.bnd.version.Version convert(org.osgi.framework.Version v) {
		return new aQute.bnd.version.Version(v.getMajor(), v.getMinor(), v.getMicro(), v.getQualifier());
	}

	public static TargetRepository getTargetRepository() {
		return instance;
	}

}
