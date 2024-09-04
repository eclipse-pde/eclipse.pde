/*******************************************************************************
 * Copyright (c) 2007, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.BundleVersionRange;
import org.eclipse.pde.api.tools.internal.CompositeApiDescription;
import org.eclipse.pde.api.tools.internal.FilterStore;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.RequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAccess;
import org.eclipse.pde.api.tools.internal.provisional.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.util.SourceDefaultHandler;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.internal.core.TargetWeaver;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of an API component based on a bundle in the file system.
 *
 * @since 1.0.0
 */
public class BundleComponent extends Component {

	private static final String[] NO_EXECUTION_ENVIRONMENTS = new String[0];

	static final String TMP_API_FILE_PREFIX = "api"; //$NON-NLS-1$

	/**
	 * Dictionary parsed from MANIFEST.MF
	 */
	private volatile Map<String, String> fManifest;

	/**
	 * Manifest headers that are maintained after {@link BundleDescription}
	 * creation. Only these headers are maintained in the manifest dictionary to
	 * reduce footprint.
	 */
	private static final String[] MANIFEST_HEADERS = new String[] {
			IApiCoreConstants.ECLIPSE_SOURCE_BUNDLE,
			Constants.BUNDLE_CLASSPATH, Constants.BUNDLE_NAME,
			Constants.BUNDLE_VERSION };

	/**
	 * Whether there is an underlying .api_description file
	 */
	private volatile boolean fHasApiDescription;

	/**
	 * Root location of component in the file system
	 */
	private final String fLocation;

	/**
	 * Underlying bundle description (OSGi model of a bundle)
	 */
	private volatile BundleDescription fBundleDescription;

	/**
	 * Symbolic name of this bundle
	 */
	private String fSymbolicName;

	/**
	 * Bundle version
	 */
	private volatile Version fVersion;

	/**
	 * Cached value for the lowest EEs
	 */
	private volatile String[] lowestEEs;

	/**
	 * Flag to know if this component is a binary bundle in the workspace i.e.
	 * an imported binary bundle
	 */
	private final boolean fWorkspaceBinary;

	/**
	 * The id of this component
	 */
	private final long fBundleId;

	/**
	 * Tracks where/when the component was disposed to give better error message
	 */
	private volatile RuntimeException disposeSource;

	/**
	 * Whether the manifest declares an explicit execution environment requirement
	 *
	 * @see #getExecutionEnvironments()
	 * @see #hasDeclaredRequiredEE(Map)
	 */
	private volatile String[] fdeclaredRequiredEE;

	/**
	 * Constructs a new API component from the specified location in the file
	 * system in the given baseline.
	 *
	 * @param baseline owning API baseline
	 * @param location directory or jar file
	 * @exception CoreException if unable to create a component from the
	 *                specified location
	 */
	public BundleComponent(IApiBaseline baseline, String location, long bundleid) throws CoreException {
		super(baseline);
		fLocation = location;
		fBundleId = bundleid;
		fWorkspaceBinary = isBinary() && ApiBaselineManager.WORKSPACE_API_BASELINE_ID.equals(baseline.getName())
		/*
		 * Workaround for bad architecture, see bug 488694: We don't know if the
		 * bundle is from the workspace , from a host workspace, or from a
		 * target platform. If it's a file, then it's certainly not from a
		 * workspace project, and hence can't be an imported binary bundle.
		 */
				&& !new File(location).isFile();
	}

	@Override
	public void dispose() {
		if (isDisposed()) {
			return;
		}
		try {
			super.dispose();
		} finally {
			synchronized (this) {
				fManifest = null;
				fBundleDescription = null;
				String message = String.format("Component was disposed here [%s]", Thread.currentThread().getName()); //$NON-NLS-1$
				disposeSource = new RuntimeException(message);
			}
		}
	}

	/**
	 * Returns this bundle's manifest as a dictionary or <code>null</code> if no
	 * manifest was found.
	 *
	 * @return manifest dictionary or <code>null</code>
	 * @exception CoreException if something goes terribly wrong
	 */
	protected Map<String, String> getManifest() throws CoreException {
		if (fManifest != null) {
			return fManifest;
		}
		Map<String, String> manifest = loadManifest(new File(fLocation), isWorkspaceBinary());
		synchronized (this) {
			if (fManifest == null) {
				fManifest = manifest;
			}
			return fManifest;
		}
	}

	private static Map<String, String> loadManifest(File bundleLocation, boolean isWorkspaceBinary)
			throws CoreException {
		try {
			Map<String, String> manifest = ManifestUtils.loadManifest(bundleLocation);
			if (isWorkspaceBinary) {
				// must account for bundles in development mode - look for class
				// files in output
				// folders rather than jars
				TargetWeaver.weaveManifest(manifest, bundleLocation);
			}
			return manifest;
		} catch (CoreException e) {
			if (e.getStatus().getCode() == ManifestUtils.STATUS_CODE_NOT_A_BUNDLE_MANIFEST) {
				// If we load a component with a manifest file that isn't a
				// bundle, ignore it
				return null;
			} else {
				throw e;
			}
		}
	}

	/**
	 * Reduce the manifest to only contain required headers after
	 * {@link BundleDescription} creation.
	 */
	protected synchronized void doManifestCompaction() {
		Map<String, String> temp = fManifest;
		fManifest = new Hashtable<>(MANIFEST_HEADERS.length, 1);
		for (String header : MANIFEST_HEADERS) {
			String value = temp.get(header);
			if (value != null) {
				fManifest.put(header, value);
			}
		}
	}

	/**
	 * Returns if the bundle at the specified location is a valid bundle or not.
	 * Validity is determined via the existence of a readable manifest file
	 *
	 * @return true if the bundle at the given location is valid false otherwise
	 */
	public boolean isValidBundle() {
		try {
			Map<String, String> manifest = getManifest();
			return manifest != null
					&& (manifest.get(Constants.BUNDLE_NAME) != null && manifest.get(Constants.BUNDLE_VERSION) != null);
		} catch (CoreException e) {
			// if loading the manifest fails, this is NOT a valid bundle obviously...
		}
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BundleComponent comp) {
			return getName().equals(comp.getName()) && getSymbolicName().equals(comp.getSymbolicName()) && getVersion().equals(comp.getVersion());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getName().hashCode() + getSymbolicName().hashCode() + getVersion().hashCode();
	}

	/**
	 * Initializes the component
	 *
	 * @throws CoreException on failure
	 */
	protected void init() throws CoreException {
		if (isDisposed() || fBundleDescription != null) {
			return;
		}
		synchronized (this) {
			try {
				Map<String, String> manifest = getManifest();
				if (manifest == null) {
					throw new CoreException(
							Status.error("Unable to find a manifest for the component from: " + fLocation, //$NON-NLS-1$
							null));
				}
				BundleDescription bundleDescription = getBundleDescription(manifest, fLocation, fBundleId);
				fSymbolicName = bundleDescription.getSymbolicName();
				fVersion = bundleDescription.getVersion();
				if (hasDeclaredRequiredEE(manifest)) {
					fdeclaredRequiredEE = ManifestUtils.getRequiredExecutionEnvironments(bundleDescription)
							.toArray(String[]::new);
				} else {
					fdeclaredRequiredEE = NO_EXECUTION_ENVIRONMENTS;
				}
				setName(manifest.get(Constants.BUNDLE_NAME));
				fBundleDescription = bundleDescription;
			} catch (BundleException e) {
				throw new CoreException(
						Status.error("Unable to create API component from specified location: " + fLocation, //$NON-NLS-1$
						e));
			}
			// compact manifest after initialization - only keep used headers
			doManifestCompaction();
		}
	}

	/**
	 * Returns if this component is a a binary bundle in the workspace i.e. an
	 * imported binary bundle
	 *
	 * @return true if the component is a binary bundle in the workspace, false
	 *         otherwise
	 */
	public boolean isWorkspaceBinary() {
		return fWorkspaceBinary;
	}

	/**
	 * Returns the {@link State} from the backing baseline
	 *
	 * @return the state from the backing {@link ApiBaseline}
	 */
	protected State getState() {
		return ((ApiBaseline) getBaseline()).getState();
	}

	/**
	 * Returns the {@link BundleDescription} for the given manifest + state or
	 * throws an exception, never returns <code>null</code>
	 *
	 * @param location to be used by override
	 *
	 * @return the {@link BundleDescription} or throws an exception
	 */
	protected BundleDescription getBundleDescription(Map<String, String> manifest, String location, long id)
			throws BundleException {
		State state = getState();
		BundleDescription bundle = lookupBundle(state, manifest);
		if (bundle != null) {
			return bundle;
		}
		StateObjectFactory factory = StateObjectFactory.defaultFactory;
		bundle = factory.createBundleDescription(state, FrameworkUtil.asDictionary(manifest), fLocation, id);
		state.addBundle(bundle);
		return bundle;
	}

	/**
	 * Tries to look up the bundle described by the given manifest in the given
	 * state
	 *
	 * @return the bundle for the given manifest, <code>null</code> otherwise
	 */
	protected static BundleDescription lookupBundle(State state, Map<String, String> manifest) throws BundleException {
		Version version = null;
		try {
			// just in case the version is not a number
			String ver = manifest.get(Constants.BUNDLE_VERSION);
			version = ver != null ? new Version(ver) : null;
		} catch (NumberFormatException nfe) {
			// ignore
		}
		ManifestElement[] name = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, manifest.get(Constants.BUNDLE_SYMBOLICNAME));
		if (name.length < 1) {
			return null;
		}
		return state.getBundle(name[0].getValue(), version);
	}

	/**
	 * Returns whether this API component represents a binary bundle versus a
	 * project bundle.
	 *
	 * @return whether this API component represents a binary bundle
	 */
	protected boolean isBinary() {
		return true;
	}

	@Override
	protected IApiDescription createApiDescription() throws CoreException {
		BundleDescription[] fragments = getBundleDescription().getFragments();
		if (fragments.length == 0) {
			return createLocalApiDescription();
		}
		// build a composite description
		ArrayList<IApiDescription> descriptions = new ArrayList<>(fragments.length);
		descriptions.add(createLocalApiDescription());
		for (BundleDescription fragment : fragments) {
			if (!fragment.isResolved()) {
				continue;
			}
			IApiComponent component = getBaseline().getApiComponent(fragment.getSymbolicName());
			if (component != null) {
				descriptions.add(component.getApiDescription());
			} else {
				ApiPlugin.log(Status.warning(
						NLS.bind(Messages.BundleComponent_failed_to_lookup_fragment, fragment.getSymbolicName())));
			}
		}
		return new CompositeApiDescription(descriptions.toArray(new IApiDescription[descriptions.size()]));
	}

	/**
	 * Creates and returns this component's API description based on packages
	 * supplied by this component, exported packages, and associated directives.
	 *
	 * @return API description
	 * @throws CoreException if unable to initialize
	 */
	protected IApiDescription createLocalApiDescription() throws CoreException {
		IApiDescription apiDesc = new ApiDescription(getSymbolicName());
		// first mark all packages as internal
		initializeApiDescription(apiDesc, getBundleDescription(), getLocalPackageNames());
		try {
			String xml = loadApiDescription(new File(fLocation));
			setHasApiDescription(xml != null);
			if (xml != null) {
				ApiDescriptionProcessor.annotateApiSettings(null, apiDesc, xml);
			}
		} catch (IOException e) {
			abort("Unable to load .api_description file ", e); //$NON-NLS-1$
		}
		return apiDesc;
	}

	/**
	 * Returns the names of all packages that originate from this bundle. Does
	 * not include packages that originate from fragments or a host.
	 *
	 * @return local package names
	 */
	protected Set<String> getLocalPackageNames() throws CoreException {
		Set<String> names = new HashSet<>();
		IApiTypeContainer[] containers = getApiTypeContainers();
		for (IApiTypeContainer container : containers) {
			IApiComponent comp = (IApiComponent) container.getAncestor(IApiElement.COMPONENT);
			if (comp != null && comp.getSymbolicName().equals(getSymbolicName())) {
				String[] packageNames = container.getPackageNames();
				Collections.addAll(names, packageNames);
			}
		}
		return names;
	}

	/**
	 * Initializes the given API description based on package exports in the
	 * manifest. The API description for a bundle only contains packages that
	 * originate from this bundle (so a host will not contain API descriptions
	 * for packages that originate from fragments). However, a host's API
	 * description will be represented by a proxy that delegates to the host and
	 * all of its fragments to provide a complete description of the host.
	 *
	 * @param apiDesc API description to initialize
	 * @param bundle the bundle to load from
	 * @param packages the complete set of packages names originating from the
	 *            backing component
	 * @throws CoreException if an error occurs
	 */
	public static void initializeApiDescription(IApiDescription apiDesc, BundleDescription bundle,
			Set<String> packages) {
		for (String name : packages) {
			apiDesc.setVisibility(Factory.packageDescriptor(name), VisibilityModifiers.PRIVATE);
		}
		// then process exported packages that originate from this bundle
		// considering host and fragment package exports
		List<ExportPackageDescription> supplied = new ArrayList<>();
		ExportPackageDescription[] exportPackages = bundle.getExportPackages();
		addSuppliedPackages(packages, supplied, exportPackages);
		HostSpecification host = bundle.getHost();
		if (host != null) {
			BundleDescription[] hosts = host.getHosts();
			for (BundleDescription bundleDescription : hosts) {
				addSuppliedPackages(packages, supplied, bundleDescription.getExportPackages());
			}
		}
		BundleDescription[] fragments = bundle.getFragments();
		for (BundleDescription fragment : fragments) {
			if (!fragment.isResolved()) {
				continue;
			}
			addSuppliedPackages(packages, supplied, fragment.getExportPackages());
		}

		annotateExportedPackages(apiDesc, supplied.toArray(new ExportPackageDescription[supplied.size()]));
	}

	/**
	 * Adds package exports to the given list if the associated package
	 * originates from this bundle.
	 *
	 * @param packages names of packages supplied by this bundle
	 * @param supplied list to append package exports to
	 * @param exportPackages package exports to consider
	 */
	protected static void addSuppliedPackages(Set<String> packages, List<ExportPackageDescription> supplied, ExportPackageDescription[] exportPackages) {
		for (ExportPackageDescription pkg : exportPackages) {
			String name = pkg.getName();
			if (name.equals(".")) { //$NON-NLS-1$
				// translate . to default package
				name = Util.DEFAULT_PACKAGE_NAME;
			}
			if (packages.contains(name)) {
				supplied.add(pkg);
			}
		}
	}

	/**
	 * Annotates the API description with exported packages.
	 *
	 * @param apiDesc description to annotate
	 * @param exportedPackages packages that are exported
	 */
	protected static void annotateExportedPackages(IApiDescription apiDesc, ExportPackageDescription[] exportedPackages) {
		for (ExportPackageDescription pkg : exportedPackages) {
			boolean internal = ((Boolean) pkg.getDirective("x-internal")).booleanValue(); //$NON-NLS-1$
			String[] friends = (String[]) pkg.getDirective("x-friends"); //$NON-NLS-1$
			String pkgName = pkg.getName();
			if (pkgName.equals(".")) { //$NON-NLS-1$
				// default package
				pkgName = ""; //$NON-NLS-1$
			}
			IPackageDescriptor pkgDesc = Factory.packageDescriptor(pkgName);
			if (internal) {
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.PRIVATE);
			}
			if (friends != null) {
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.PRIVATE);
				for (String friend : friends) {
					// annotate the api description for x-friends access levels
					apiDesc.setAccessLevel(Factory.componentDescriptor(friend), Factory.packageDescriptor(pkgName), IApiAccess.FRIEND);
				}
			}
			if (!internal && friends == null) {
				// there could have been directives that have nothing to do with
				// visibility, so we need to add the package as API in that case
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.API);
			}
		}
	}

	@Override
	protected IApiFilterStore createApiFilterStore() throws CoreException {
		return new FilterStore(this);
	}

	/**
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiTypeContainer#createApiTypeContainers()
	 */
	@Override
	protected List<IApiTypeContainer> createApiTypeContainers() throws CoreException {
		List<IApiTypeContainer> containers = new ArrayList<>(5);
		List<BundleComponent> all = new ArrayList<>();
		// build the classpath from bundle and all fragments
		all.add(this);
		BundleDescription[] fragments = getBundleDescription().getFragments();
		for (BundleDescription fragment : fragments) {
			if (!fragment.isResolved()) {
				continue;
			}
			IApiComponent component = getBaseline().getApiComponent(fragment.getSymbolicName());
			if (component != null) {
				// force initialization of the fragment so we can
				// retrieve its class file containers
				component.getApiTypeContainers();
				all.add((BundleComponent) component);
			}
		}

		Set<String> entryNames = new HashSet<>(5);
		for (BundleComponent component : all) {
			Map<String, String> manifest = component.getManifest();
			if (manifest != null) {
				try {
					List<String> paths = getClasspathEntries(manifest);
					for (String path : paths) {
						// don't re-process the same entry twice (except default
						// entries ".")
						if (!".".equals(path) && entryNames.contains(path)) { //$NON-NLS-1$
							continue;
						}
						IApiTypeContainer container = component.createApiTypeContainer(path);
						if (container == null) {
							for (BundleComponent other : all) {
								if (other != component) {
									container = other.createApiTypeContainer(path);
								}
							}
						}
						if (container != null) {
							containers.add(container);
							if (!(".".equals(path))) { //$NON-NLS-1$
								entryNames.add(path);
							}
						}
					}
				} catch (BundleException e) {
					abort("Unable to parse bundle-classpath entry of manifest for bundle at " + component.getLocation(), e); //$NON-NLS-1$
				}

			}
		}
		return containers;
	}

	/**
	 * Returns whether this API component is enabled for API analysis by the API
	 * builder.
	 *
	 * @return whether this API component is enabled for API analysis by the API
	 *         builder.
	 */
	protected boolean isApiEnabled() {
		return false;
	}

	/**
	 * Returns classpath entries defined in the given manifest.
	 *
	 * @return classpath entries as bundle relative paths
	 */
	private static List<String> getClasspathEntries(Map<String, String> manifest) throws BundleException {
		ManifestElement[] classpath = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, manifest.get(Constants.BUNDLE_CLASSPATH));
		if (classpath == null) {
			// default classpath is '.'
			return List.of("."); //$NON-NLS-1$
		} else {
			return Arrays.stream(classpath).map(ManifestElement::getValue).toList();
		}
	}

	/**
	 * Creates and returns an {@link IApiTypeContainer} at the specified path in
	 * this bundle, or <code>null</code> if the {@link IApiTypeContainer} does
	 * not exist. The path is the name (path) of entries specified by the
	 * <code>Bundle-ClassPath:</code> header.
	 *
	 * @param path relative path to a class file container in this bundle
	 * @return {@link IApiTypeContainer} or <code>null</code>
	 * @throws CoreException if something goes wrong while creating the
	 *             container
	 */
	protected IApiTypeContainer createApiTypeContainer(String path) throws CoreException {
		try {
			File bundle = new File(fLocation);
			if (bundle.isDirectory()) {
				// bundle is folder
				File entry = new File(bundle, path);
				if (entry.exists()) {
					if (entry.isFile()) {
						return new ArchiveApiTypeContainer(this, entry.getCanonicalPath());
					} else {
						return new DirectoryApiTypeContainer(this, entry.getCanonicalPath());
					}
				}
			} else {
				// bundle is jar'd
				if (path.equals(".")) { //$NON-NLS-1$
					return new ArchiveApiTypeContainer(this, fLocation);
				} else {
					// classpath element can be jar or folder
					// https://bugs.eclipse.org/bugs/show_bug.cgi?id=279729
					try (ZipFile zip = new ZipFile(fLocation)) {
						ZipEntry entry = zip.getEntry(path);
						if (entry != null) {
							if (entry.isDirectory()) {
								// extract the dir and all children
								Path dir = Util.createTempDirectory(TMP_API_FILE_PREFIX).toRealPath();
								extractDirectory(zip, entry.getName(), dir);
								return new DirectoryApiTypeContainer(this, dir.toString());
							} else {
								if (Util.isArchive(path)) {
									// Create a uniquely named temp-directory and extract the file inside it.
									// Without that additional parent directory, this could clash, e.g.
									// due to pre-existing files in the temp dir or when comparing with
									// a baseline during API analysis: one file would simply overwrite
									// the other one and thus a difference cannot be found.
									Path dir = Util.createTempDirectory(TMP_API_FILE_PREFIX).toRealPath();
									Path file = extractEntry(zip, entry, dir);
									return new ArchiveApiTypeContainer(this, file.toRealPath().toString());
								}
							}
						}
					}
				}
			}
		} catch (IOException e) {
			abort("Problem creating api type container for path " + path + " in bundle at " + fLocation, e); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	/**
	 * Extracts a directory from the archive given a path prefix for entries to
	 * retrieve. <code>null</code> can be passed in as a prefix, causing all
	 * entries to be be extracted from the archive.
	 *
	 * @param zip the {@link ZipFile} to extract from
	 * @param pathprefix the prefix'ing path to include for extraction
	 * @param parent the parent directory to extract to
	 * @throws IOException if the {@link ZipFile} cannot be read or extraction
	 *             fails to write the file(s)
	 * @throws IOException, CoreException
	 */
	private static void extractDirectory(ZipFile zip, String pathprefix, Path parent)
			throws IOException {
		String prefix = (pathprefix == null ? Util.EMPTY_STRING : pathprefix);
		for (Enumeration<? extends ZipEntry> entries = zip.entries(); entries.hasMoreElements();) {
			ZipEntry entry = entries.nextElement();
			String name = entry.getName();
			if (name.startsWith(prefix)) {
				extractEntry(zip, entry, parent);
			}
		}
	}

	/**
	 * Extracts a non-directory entry from a zip file and returns the File
	 * handle
	 *
	 * @param zip the zip to extract from
	 * @param entry the entry to extract
	 * @param parent the parent directory to add the extracted entry to
	 * @return the file handle to the extracted entry, <code>null</code>
	 *         otherwise
	 */
	private static Path extractEntry(ZipFile zip, ZipEntry entry, Path parent) throws IOException {
		try (InputStream inputStream = zip.getInputStream(entry)) {
			String name = entry.getName();
			Path file = parent.resolve(name).normalize();
			if (!file.startsWith(parent)) {
				throw new ZipException("Bad zip entry: " + name); //$NON-NLS-1$
			}
			Files.createDirectories(file.getParent());
			Files.copy(inputStream, file);
			return file;
		}
	}

	public static void closingZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Reads and returns the file contents corresponding to the given file name.
	 * The bundle may be in a jar or in a directory at the specified location.
	 *
	 * @param xmlFileName the given file name
	 * @param bundleLocation the root location of the bundle
	 * @return the file contents or <code>null</code> if not present
	 */
	protected static String readFileContents(String xmlFileName, File bundleLocation) {
		ZipFile jarFile = null;
		InputStream stream = null;
		try {
			String extension = IPath.fromOSString(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(xmlFileName);
				if (manifestEntry != null) {
					stream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, xmlFileName);
				if (file.exists()) {
					stream = new FileInputStream(file);
				}
			}
			if (stream == null) {
				return null;
			}
			return new String(Util.getInputStreamAsCharArray(stream, StandardCharsets.UTF_8));
		} catch (IOException e) {
			ApiPlugin.log(e);
		} finally {
			closingZipFileAndStream(stream, jarFile);
		}
		return null;
	}

	// Content is from 3.125.0.v20240206-1259, id's are from 4.31 release
	private static final List<String> FIXED_API_DESCRIPTIONS = Arrays.asList(
			"org.eclipse.swt.win32.win32.x86-64-3.125.0.v20240227-1638", //$NON-NLS-1$
			"org.eclipse.swt.gtk.linux.x86-64-3.125.0.v20240227-1638", //$NON-NLS-1$
			"org.eclipse.swt.gtk.linux.ppc64le-3.125.0.v20240227-1638", //$NON-NLS-1$
			"org.eclipse.swt.gtk.linux.aarch64-3.125.0.v20240227-1638", //$NON-NLS-1$
			"org.eclipse.swt.cocoa.macosx.aarch64-3.125.0.v20240227-1638", //$NON-NLS-1$
			"org.eclipse.swt.cocoa.macosx.x86-64-3.125.0.v20240227-1638" //$NON-NLS-1$
	);

	/**
	 * Parses a bundle's .api_description XML into a string. The file may be in
	 * a jar or in a directory at the specified location.
	 *
	 * @param bundleLocation root location of the bundle
	 * @return API description XML as a string or <code>null</code> if none
	 * @throws IOException if unable to parse
	 */
	protected static String loadApiDescription(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream stream = null;
		String contents;
		try {
			String fileName = bundleLocation.getName();
			String extension = IPath.fromOSString(fileName).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				// TODO: remove this if(FIXED_API_DESCRIPTIONS) branch after switching to 4.32
				// baseline (assuming it will have proper SWT API descriptions, see
				// https://github.com/eclipse-pde/eclipse.pde/pull/1191)
				String bundleAndVersion = fileName.substring(0, fileName.length() - ".jar".length()).replace('_', '-'); //$NON-NLS-1$
				if (FIXED_API_DESCRIPTIONS.contains(bundleAndVersion)) {
					stream = loadFixedBundleApiDescription(bundleAndVersion);
				} else {
					jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
					ZipEntry manifestEntry = jarFile.getEntry(IApiCoreConstants.API_DESCRIPTION_XML_NAME);
					if (manifestEntry != null) {
						// new file is present
						stream = jarFile.getInputStream(manifestEntry);
					}
				}
			} else {
				File file = new File(bundleLocation, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
				if (file.exists()) {
					// use new file
					stream = new FileInputStream(file);
				}
			}
			if (stream == null) {
				return null;
			}
			char[] charArray = Util.getInputStreamAsCharArray(stream, StandardCharsets.UTF_8);
			contents = new String(charArray);
		} finally {
			closingZipFileAndStream(stream, jarFile);
		}
		return contents;
	}

	/**
	 * See https://github.com/eclipse-platform/eclipse.platform.swt/issues/1093 and
	 * https://github.com/eclipse-pde/eclipse.pde/issues/1187.
	 *
	 * @param bundleAndVersion platform specific bundle name with version
	 * @return stream opened for reading api description
	 * @throws IOException
	 */
	private static InputStream loadFixedBundleApiDescription(String bundleAndVersion) throws IOException {
		Bundle bundle = Platform.getBundle("org.eclipse.pde.api.tools"); //$NON-NLS-1$
		IPath pathInBundle = IPath.fromOSString("fixed_api_descriptions/" + bundleAndVersion + ".api_description"); //$NON-NLS-1$ //$NON-NLS-2$
		return FileLocator.openStream(bundle, pathInBundle, false);
	}

	@Override
	public String[] getExecutionEnvironments() throws CoreException {
		// Return the EE from the description only if explicitly specified in the
		// manifest.
		return fdeclaredRequiredEE;
	}

	@Override
	public final String getSymbolicName() {
		try {
			init();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return fSymbolicName;
	}

	@Override
	public IRequiredComponentDescription[] getRequiredComponents() throws CoreException {
		BundleSpecification[] requiredBundles = getBundleDescription().getRequiredBundles();
		return Arrays.stream(requiredBundles).map( //
				b -> new RequiredComponentDescription(b.getName(), new BundleVersionRange(b.getVersionRange()), b.isOptional(), b.isExported()))
				.toArray(IRequiredComponentDescription[]::new);
	}

	@Override
	public String getVersion() {
		try {
			init();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		Version version = fVersion;
		if (version== null) {
			return null;
		}
		// without the qualifier
		return version.getMajor() + "." + version.getMinor() + "." + version.getMicro(); //$NON-NLS-1$//$NON-NLS-2$
	}

	@Override
	public String getName() {
		try {
			init();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return super.getName();
	}

	/**
	 * Returns this component's bundle description.
	 *
	 * @return bundle description, never null
	 * @throws CoreException if this component or the baseline is already disposed
	 */
	public BundleDescription getBundleDescription() throws CoreException {
		init();
		BundleDescription description = fBundleDescription;
		if (isDisposed() || description == null) {
			baselineDisposed(getBaseline());
		}
		return description;
	}

	@Override
	public String toString() {
		BundleDescription description = fBundleDescription;
		if (description != null) {
			try {
				StringBuilder buffer = new StringBuilder();
				buffer.append(description.toString());
				buffer.append(" - "); //$NON-NLS-1$
				buffer.append("[fragment: ").append(isFragment()).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("[host: ").append(description.getFragments().length > 0).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("[system bundle: ").append(isSystemComponent()).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("[source bundle: ").append(isSourceComponent()).append("] "); //$NON-NLS-1$ //$NON-NLS-2$
				buffer.append("[dev bundle: ").append(fWorkspaceBinary).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
				return buffer.toString();
			} catch (CoreException ce) {
				return super.toString();
			}
		} else {
			StringBuilder buffer = new StringBuilder();
			if (isDisposed()) {
				buffer.append("Disposed "); //$NON-NLS-1$
			} else {
				buffer.append("Un-initialized "); //$NON-NLS-1$
			}
			buffer.append("Bundle Component"); //$NON-NLS-1$
			buffer.append("[location: ").append(fLocation).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			buffer.append("[dev bundle: ").append(fWorkspaceBinary).append("]"); //$NON-NLS-1$ //$NON-NLS-2$
			return buffer.toString();
		}
	}

	@Override
	public String getLocation() {
		return fLocation;
	}

	@Override
	public boolean isSystemComponent() {
		return false;
	}

	@Override
	public boolean isSourceComponent() throws CoreException {
		Map<String, String> manifest = getManifest();
		if (manifest == null) {
			baselineDisposed(getBaseline());
		}
		return isSourceComponent(manifest, new File(getLocation()));
	}

	private static boolean isSourceComponent(Map<String, String> manifest, File location) {
		ManifestElement[] sourceBundle = null;
		try {
			sourceBundle = ManifestElement.parseHeader(IApiCoreConstants.ECLIPSE_SOURCE_BUNDLE, manifest.get(IApiCoreConstants.ECLIPSE_SOURCE_BUNDLE));
		} catch (BundleException e) {
			// ignore
		}
		if (sourceBundle != null) {
			// this is a source bundle with the new format
			return true;
		}
		// check for the old format
		String pluginXMLContents = readFileContents(IApiCoreConstants.PLUGIN_XML_NAME, location);
		if (pluginXMLContents != null && containsSourceExtensionPoint(pluginXMLContents)) {
			return true;
		}
		// check if it contains a fragment.xml with the appropriate extension
		// point
		pluginXMLContents = readFileContents(IApiCoreConstants.FRAGMENT_XML_NAME, location);
		// if false, parse XML contents to find extension points
		return pluginXMLContents != null && containsSourceExtensionPoint(pluginXMLContents);
	}

	/**
	 * Check if the given source contains an source extension point.
	 *
	 * @param pluginXMLContents the given file contents
	 * @return true if it contains a source extension point, false otherwise
	 */
	private static boolean containsSourceExtensionPoint(String pluginXMLContents) {
		try {
			@SuppressWarnings("restriction")
			SAXParser saxParser = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createSAXParserWithErrorOnDOCTYPE();
			SourceDefaultHandler defaultHandler = new SourceDefaultHandler();
			saxParser.parse(new InputSource(new StringReader(pluginXMLContents)), defaultHandler);
			return defaultHandler.isSource();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			return false;
		}
	}

	@Override
	public boolean isFragment() throws CoreException {
		return getBundleDescription().getHost() != null;
	}

	@Override
	public IApiComponent getHost() throws CoreException {
		HostSpecification host = getBundleDescription().getHost();
		if (host != null) {
			return getBaseline().getApiComponent(host.getName());
		}
		return null;
	}

	@Override
	public boolean hasFragments() throws CoreException {
		return getBundleDescription().getFragments().length != 0;
	}

	/**
	 * Sets whether this bundle has an underlying API description file.
	 *
	 * @param hasApiDescription whether this bundle has an underlying API
	 *            description file
	 */
	protected void setHasApiDescription(boolean hasApiDescription) {
		fHasApiDescription = hasApiDescription;
	}

	@Override
	public boolean hasApiDescription() {
		// ensure initialized
		try {
			getApiDescription();
		} catch (CoreException e) {
			ApiPlugin.log("Failed to create API description for " + getName(), e); //$NON-NLS-1$
		}
		return fHasApiDescription;
	}

	@Override
	public String[] getLowestEEs() throws CoreException {
		if (lowestEEs != null) {
			return lowestEEs;
		}
		String[] executionEnvironments = getExecutionEnvironments();
		String[] ees = computeLowestEEs(executionEnvironments);
		synchronized (this) {
			lowestEEs = ees;
			return lowestEEs;
		}
	}

	private static String[] computeLowestEEs(String[] executionEnvironments) {
		String[] temp = null;

		int length = executionEnvironments.length;
		switch (length) {
			case 0:
				return null;
			case 1:
				temp = new String[] { executionEnvironments[0] };
				break;
			default:
				int values = ProfileModifiers.NO_PROFILE_VALUE;
				for (int i = 0; i < length; i++) {
					values |= ProfileModifiers.getValue(executionEnvironments[i]);
				}
				if (ProfileModifiers.isJRE(values)) {
					if (ProfileModifiers.isJRE_1_1(values)) {
						temp = new String[] { ProfileModifiers.JRE_1_1_NAME };
					} else if (ProfileModifiers.isJ2SE_1_2(values)) {
						temp = new String[] { ProfileModifiers.J2SE_1_2_NAME };
					} else if (ProfileModifiers.isJ2SE_1_3(values)) {
						temp = new String[] { ProfileModifiers.J2SE_1_3_NAME };
					} else if (ProfileModifiers.isJ2SE_1_4(values)) {
						temp = new String[] { ProfileModifiers.J2SE_1_4_NAME };
					} else if (ProfileModifiers.isJ2SE_1_5(values)) {
						temp = new String[] { ProfileModifiers.J2SE_1_5_NAME };
					} else if (ProfileModifiers.isJAVASE_1_6(values)) {
						temp = new String[] { ProfileModifiers.JAVASE_1_6_NAME };
					} else if (ProfileModifiers.isJAVASE_1_7(values)) {
						temp = new String[] { ProfileModifiers.JAVASE_1_7_NAME };
					} else if (ProfileModifiers.isJAVASE_1_8(values)) {
						temp = new String[] { ProfileModifiers.JAVASE_1_8_NAME };
					} else {
						temp = new String[] { ProfileModifiers.JAVASE_9_NAME };
					}
				}
				if (ProfileModifiers.isCDC_Foundation(values)) {
					if (ProfileModifiers.isCDC_1_0_FOUNDATION_1_0(values)) {
						if (temp != null) {
							temp = new String[] {
									temp[0],
									ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME };
						} else {
							temp = new String[] { ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME };
						}
					} else {
						if (temp != null) {
							temp = new String[] {
									temp[0],
									ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME };
						} else {
							temp = new String[] { ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME };
						}
					}
				}
				if (ProfileModifiers.isOSGi(values)) {
					if (ProfileModifiers.isOSGI_MINIMUM_1_0(values)) {
						if (temp != null) {
							int tempLength = temp.length;
							System.arraycopy(temp, 0, (temp = new String[tempLength + 1]), 0, tempLength);
							temp[tempLength] = ProfileModifiers.OSGI_MINIMUM_1_0_NAME;
						} else {
							temp = new String[] { ProfileModifiers.OSGI_MINIMUM_1_0_NAME };
						}
					} else if (ProfileModifiers.isOSGI_MINIMUM_1_1(values)) {
						if (temp != null) {
							int tempLength = temp.length;
							System.arraycopy(temp, 0, (temp = new String[tempLength + 1]), 0, tempLength);
							temp[tempLength] = ProfileModifiers.OSGI_MINIMUM_1_1_NAME;
						} else {
							temp = new String[] { ProfileModifiers.OSGI_MINIMUM_1_1_NAME };
						}
					} else {
						// OSGI_MINIMUM_1_2
						if (temp != null) {
							int tempLength = temp.length;
							System.arraycopy(temp, 0, (temp = new String[tempLength + 1]), 0, tempLength);
							temp[tempLength] = ProfileModifiers.OSGI_MINIMUM_1_2_NAME;
						} else {
							temp = new String[] { ProfileModifiers.OSGI_MINIMUM_1_2_NAME };
						}
					}
				}
		}
		return temp;
	}

	@Override
	public ResolverError[] getErrors() throws CoreException {
		ApiBaseline baseline = (ApiBaseline) getBaseline();
		if (baseline != null) {
			BundleDescription bundleDescription = getBundleDescription();
			ResolverError[] resolverErrors = baseline.getState().getResolverErrors(bundleDescription);
			if (resolverErrors.length == 0) {
				return null;
			}
			return resolverErrors;
		}
		return null;
	}

	/**
	 * @param baseline the baseline that is disposed
	 * @throws CoreException with the baseline disposed information
	 */
	protected void baselineDisposed(IApiBaseline baseline) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ApiPlugin.REPORT_BASELINE_IS_DISPOSED,
				NLS.bind(Messages.BundleApiComponent_baseline_disposed,
						new Object[] { getName(), baseline.getName(), Thread.currentThread().getName() }),
				disposeSource));
	}

	/*
	 * This is a copy of
	 * org.eclipse.pde.internal.core.MinimalState.hasDeclaredRequiredEE(Map<String,
	 * String>). PDE ends up adding a synthetic EE to the manifest when that method
	 * returns false, and that ends up in the bundle description such that we cannot
	 * determine whether the EE was actually present or was synthesized. So we
	 * repeat that computation here
	 */
	@SuppressWarnings("deprecation")
	private boolean hasDeclaredRequiredEE(Map<String, String> manifest) {
		if (manifest.containsKey(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT)) {
			return true;
		}
		try {
			String capability = manifest.get(Constants.REQUIRE_CAPABILITY);
			ManifestElement[] header = ManifestElement.parseHeader(Constants.REQUIRE_CAPABILITY, capability);
			return header != null && Arrays.stream(header).map(ManifestElement::getValue)
					.anyMatch(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE::equals);
		} catch (BundleException e) {
			return false; // ignore
		}
	}

}
