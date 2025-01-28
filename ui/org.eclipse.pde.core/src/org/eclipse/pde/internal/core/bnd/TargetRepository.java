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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.plugin.IPluginLibrary;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ClasspathUtilCore;
import org.eclipse.pde.internal.core.PDECore;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.repository.BaseRepository;
import aQute.bnd.osgi.resource.CapReqBuilder;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.service.RepositoryPlugin;

public class TargetRepository extends BaseRepository implements RepositoryPlugin {

	private static final TargetRepository instance = new TargetRepository();
	private static final Map<File, ContentCapabilityCache> contentCapabilityMap = new ConcurrentHashMap<>();

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
					IPluginModelBase model = PluginRegistry.findModel((Resource) description.get());
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
		return bundles(null).map(r -> new BundleDescriptionRepositoryResource(this, r))
				.flatMap(resource -> ResourceUtils.capabilityStream(resource, namespace))
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

	private static final class BundleDescriptionRepositoryResource implements RepositoryContent, Resource, IAdaptable {

		private final BundleDescription bundle;
		private final Repository repository;

		public BundleDescriptionRepositoryResource(Repository repository, BundleDescription bundle) {
			this.repository = repository;
			this.bundle = bundle;
		}

		@Override
		public List<Capability> getCapabilities(String namespace) {
			String location = bundle.getLocation();
			if (location != null && (namespace == null || ContentNamespace.CONTENT_NAMESPACE.equals(namespace))) {
				File file = new File(location);
				return Stream
						.concat(bundleRequirements(namespace),
								contentCapabilityMap.computeIfAbsent(file,
										f -> new ContentCapabilityCache(f, BundleDescriptionRepositoryResource.this))
										.capability())
						.toList();
			}
			return bundleRequirements(namespace).toList();
		}

		private Stream<Capability> bundleRequirements(String namespace) {
			return bundle.getCapabilities(namespace).stream().map(original -> new Capability() {

				@Override
				public Resource getResource() {
					return BundleDescriptionRepositoryResource.this;
				}

				@Override
				public String getNamespace() {
					return original.getNamespace();
				}

				@Override
				public Map<String, String> getDirectives() {
					return original.getDirectives();
				}

				@Override
				public Map<String, Object> getAttributes() {
					return original.getAttributes();
				}

				@Override
				public String toString() {
					return original.toString();
				}
			});
		}

		@Override
		public List<Requirement> getRequirements(String namespace) {
			return bundle.getRequirements(namespace);
		}

		@Override
		public InputStream getContent() {
			String location = bundle.getLocation();
			if (location != null) {
				try {
					return new FileInputStream(location);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				}
			}
			throw new RuntimeException(new FileNotFoundException());
		}

		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == BundleDescription.class) {
				return adapter.cast(bundle);
			}
			if (adapter == File.class) {
				String location = bundle.getLocation();
				if (location != null) {
					File file = new File(location);
					if (file.isFile()) {
						return adapter.cast(file);
					}
				}
			}
			if (adapter == Path.class) {
				String location = bundle.getLocation();
				if (location != null) {
					Path path = Path.of(location);
					if (Files.isRegularFile(path)) {
						return adapter.cast(path);
					}
				}
			}
			if (adapter == Repository.class) {
				return adapter.cast(repository);
			}
			return null;
		}

	}

	private static final class ContentCapabilityCache {

		private final File file;
		private Capability capability;
		private long lastLength;
		private long lastModified;
		private final Resource resource;

		public ContentCapabilityCache(File file, Resource resource) {
			this.file = file;
			this.resource = resource;
		}

		public synchronized Stream<Capability> capability() {
			if (isOutDated()) {
				CapReqBuilder content = new CapReqBuilder(resource, ContentNamespace.CONTENT_NAMESPACE);
				String sha;
				try {
					MessageDigest digest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
					if (file.isDirectory()) {
						// directories can not really have a SHA-256 ...
						digest.update(file.getAbsolutePath().getBytes());
					} else {
						try (DigestInputStream stream = new DigestInputStream(new FileInputStream(file), digest)) {
							stream.readAllBytes();
						} catch (IOException e) {
							return Stream.empty();
						}
					}
					byte[] bytes = digest.digest();
					sha = HexFormat.of().formatHex(bytes);
				} catch (NoSuchAlgorithmException e) {
					return Stream.empty();
				}
				content.addAttribute(ContentNamespace.CONTENT_NAMESPACE, sha);
				content.addAttribute(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, Long.valueOf(file.length()));
				content.addAttribute(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, "application/vnd.osgi.bundle"); //$NON-NLS-1$
				try {
					content.addAttribute(ContentNamespace.CAPABILITY_URL_ATTRIBUTE,
							file.toURI().toURL().toExternalForm());
				} catch (MalformedURLException e) {
					return Stream.empty();
				}
				capability = content.buildCapability();
			}
			return Stream.of(capability);
		}

		private boolean isOutDated() {
			if (file.isFile()) {
				long length = file.length();
				long modified = file.lastModified();
				if (length != lastLength || modified != lastModified) {
					lastLength = length;
					lastModified = modified;
					return true;
				}
			}
			return capability == null;
		}

	}

}
