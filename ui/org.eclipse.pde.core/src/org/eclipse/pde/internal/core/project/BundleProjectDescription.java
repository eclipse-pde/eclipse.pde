/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.project;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.core.project.IBundleClasspathEntry;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.pde.core.project.IHostDescription;
import org.eclipse.pde.core.project.IPackageExportDescription;
import org.eclipse.pde.core.project.IPackageImportDescription;
import org.eclipse.pde.core.project.IRequiredBundleDescription;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * A bundle project description contains the meta-data required to define
 * a bundle or plug-in project.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.6
 */
public class BundleProjectDescription implements IBundleProjectDescription {

	private final IProject fProject;
	private IPath fRoot;
	private String fSymbolicName;
	private String fBundleName;
	private String fBundleVendor;
	private IHostDescription fHost;
	private URI fUri;
	private Version fVersion;
	private boolean fSingleton = false;
	private IPath fLocalization = null;
	private IPath fDefaultOuputFolder = null;
	private String[] fEEs;
	private String[] fNatures;
	private IBundleClasspathEntry[] fBundleClasspath = null;
	private String fActivator = null;
	private String fTargetVersion;
	private boolean fIsEquinox = false;
	private boolean fIsExtensionRegistry = false;
	private String fActivationPolicy;
	private IRequiredBundleDescription[] fRequiredBundles;
	private IPackageImportDescription[] fImports;
	private IPackageExportDescription[] fExports;
	private IPath[] fBinIncludes;
	private IBundleProjectService fService;
	private String[] fLaunchShortcuts;
	private String fExportWizard;
	private final Map<String, String> fHeaders = new HashMap<>();
	private Map<?, ?> fReadHeaders = null;

	/**
	 * Constructs a bundle description for the specified project.
	 *
	 * @param project project that may or may not exist
	 * @throws CoreException
	 */
	public BundleProjectDescription(IProject project) throws CoreException {
		fProject = project;
		if (project.exists() && project.isOpen()) {
			initialize(project);
		}
	}

	/**
	 * Returns the bundle project service.
	 *
	 * @return bundle project service
	 */
	IBundleProjectService getBundleProjectService() {
		if (fService == null) {
			fService = PDECore.getDefault().acquireService(IBundleProjectService.class);
		}
		return fService;
	}

	/**
	 * Returns the build model for the given project or <code>null</code>
	 * if none.
	 *
	 * @param project project
	 * @return build model or <code>null</code>
	 */
	private IBuild getBuildModel(IProject project) {
		IFile buildFile = PDEProject.getBuildProperties(project);
		if (buildFile.exists()) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
			return buildModel.getBuild();
		}
		return null;
	}

	/**
	 * Returns the header value from the Map of ManifestElement's or <code>null</code> if none.
	 *
	 * @param headers map of ManifestElement's
	 * @param key header name
	 * @return header value or <code>null</code>
	 */
	private String getHeaderValue(Map<?, ?> headers, String key) throws CoreException {
		ManifestElement[] elements = parseHeader(headers, key);
		if (elements != null) {
			if (elements.length > 0) {
				return elements[0].getValue();
			}
		}
		return null;
	}

	/**
	 * Parses the specified header.
	 *
	 * @param headers
	 * @param key
	 * @return elements or <code>null</code> if none
	 * @throws CoreException
	 */
	private ManifestElement[] parseHeader(Map<?, ?> headers, String key) throws CoreException {
		String value = (String) headers.get(key);
		if (value != null) {
			if (value.trim().length() > 0) {
				try {
					return ManifestElement.parseHeader(key, value);
				} catch (BundleException e) {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
				}
			}
			// empty header
			return new ManifestElement[0];
		}
		return null;
	}

	/**
	 * Initialize settings from the given project.
	 *
	 * @param project project
	 * @exception CoreException if unable to initialize
	 */
	private void initialize(IProject project) throws CoreException {
		IContainer root = PDEProject.getBundleRoot(project);
		if (root != project) {
			setBundleRoot(root.getProjectRelativePath());
		}
		IEclipsePreferences node = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
		if (node != null) {
			setExtensionRegistry(node.getBoolean(ICoreConstants.EXTENSIONS_PROPERTY, true));
			setEquinox(node.getBoolean(ICoreConstants.EQUINOX_PROPERTY, true));
		}
		// export wizard and launch shortcuts
		setExportWizardId(PDEProject.getExportWizard(project));
		setLaunchShortcuts(PDEProject.getLaunchShortcuts(project));
		// location and natures
		setLocationURI(project.getDescription().getLocationURI());
		setNatureIds(project.getDescription().getNatureIds());

		IFile manifest = PDEProject.getManifest(project);
		if (manifest.exists()) {
			Map<?, ?> headers;
			try {
				headers = ManifestElement.parseBundleManifest(manifest.getContents(), null);
				fReadHeaders = headers;
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
			} catch (BundleException e) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
			}
			setActivator(getHeaderValue(headers, Constants.BUNDLE_ACTIVATOR));
			setBundleName(getHeaderValue(headers, Constants.BUNDLE_NAME));
			setBundleVendor(getHeaderValue(headers, Constants.BUNDLE_VENDOR));
			String version = getHeaderValue(headers, Constants.BUNDLE_VERSION);
			if (version != null) {
				setBundleVersion(new Version(version));
			}
			IJavaProject jp = JavaCore.create(project);
			if (jp.exists()) {
				setDefaultOutputFolder(jp.getOutputLocation().removeFirstSegments(1));
			}
			ManifestElement[] elements = parseHeader(headers, Constants.FRAGMENT_HOST);
			if (elements != null && elements.length > 0) {
				setHost(getBundleProjectService().newHost(elements[0].getValue(), getRange(elements[0].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE))));
			}
			String value = getHeaderValue(headers, Constants.BUNDLE_LOCALIZATION);
			if (value != null) {
				setLocalization(new Path(value));
			}
			elements = parseHeader(headers, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			if (elements != null && elements.length > 0) {
				String[] keys = new String[elements.length];
				for (int i = 0; i < elements.length; i++) {
					keys[i] = elements[i].getValue();
				}
				setExecutionEnvironments(keys);
			}
			IBuild build = getBuildModel(project);
			elements = parseHeader(headers, Constants.BUNDLE_CLASSPATH);
			IBundleClasspathEntry[] classpath = null;
			if (elements != null && elements.length > 0) {
				List<IBundleClasspathEntry> collect = new ArrayList<>();
				for (ManifestElement element : elements) {
					String libName = element.getValue();
					IBundleClasspathEntry[] entries = getClasspathEntries(project, build, libName);
					if (entries != null) {
						for (IBundleClasspathEntry entry : entries) {
							collect.add(entry);
						}
					}
				}
				classpath = collect.toArray(new IBundleClasspathEntry[collect.size()]);
			} else if (elements == null) {
				// default bundle classpath of '.'
				classpath = getClasspathEntries(project, build, "."); //$NON-NLS-1$
			}
			setBundleClasspath(classpath);
			elements = parseHeader(headers, Constants.BUNDLE_SYMBOLICNAME);
			if (elements != null && elements.length > 0) {
				setSymbolicName(elements[0].getValue());
				String directive = elements[0].getDirective(Constants.SINGLETON_DIRECTIVE);
				if (directive == null) {
					directive = elements[0].getAttribute(Constants.SINGLETON_DIRECTIVE);
				}
				setSingleton("true".equals(directive)); //$NON-NLS-1$
			}
			elements = parseHeader(headers, Constants.IMPORT_PACKAGE);
			if (elements != null) {
				if (elements.length > 0) {
					IPackageImportDescription[] imports = new IPackageImportDescription[elements.length];
					for (int i = 0; i < elements.length; i++) {
						boolean optional = Constants.RESOLUTION_OPTIONAL.equals(elements[i].getDirective(Constants.RESOLUTION_DIRECTIVE)) || "true".equals(elements[i].getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
						String pv = elements[i].getAttribute(ICoreConstants.PACKAGE_SPECIFICATION_VERSION);
						if (pv == null) {
							pv = elements[i].getAttribute(Constants.VERSION_ATTRIBUTE);
						}
						imports[i] = getBundleProjectService().newPackageImport(elements[i].getValue(), getRange(pv), optional);
					}
					setPackageImports(imports);
				} else {
					// empty header - should be maintained
					setHeader(Constants.IMPORT_PACKAGE, ""); //$NON-NLS-1$
				}
			}
			elements = parseHeader(headers, Constants.EXPORT_PACKAGE);
			if (elements != null && elements.length > 0) {
				IPackageExportDescription[] exports = new IPackageExportDescription[elements.length];
				for (int i = 0; i < elements.length; i++) {
					ManifestElement exp = elements[i];
					String pv = exp.getAttribute(ICoreConstants.PACKAGE_SPECIFICATION_VERSION);
					if (pv == null) {
						pv = exp.getAttribute(Constants.VERSION_ATTRIBUTE);
					}
					String directive = exp.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
					boolean internal = "true".equals(exp.getDirective(ICoreConstants.INTERNAL_DIRECTIVE)) || directive != null; //$NON-NLS-1$
					String[] friends = null;
					if (directive != null) {
						friends = ManifestElement.getArrayFromList(directive);
					}
					exports[i] = getBundleProjectService().newPackageExport(exp.getValue(), getVersion(pv), !internal, friends);
				}
				setPackageExports(exports);
			}
			elements = parseHeader(headers, Constants.REQUIRE_BUNDLE);
			if (elements != null && elements.length > 0) {
				IRequiredBundleDescription[] req = new IRequiredBundleDescription[elements.length];
				for (int i = 0; i < elements.length; i++) {
					ManifestElement rb = elements[i];
					boolean reexport = Constants.VISIBILITY_REEXPORT.equals(rb.getDirective(Constants.VISIBILITY_DIRECTIVE)) || "true".equals(rb.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE)); //$NON-NLS-1$
					boolean optional = Constants.RESOLUTION_OPTIONAL.equals(rb.getDirective(Constants.RESOLUTION_DIRECTIVE)) || "true".equals(rb.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
					req[i] = getBundleProjectService().newRequiredBundle(rb.getValue(), getRange(rb.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)), optional, reexport);
				}
				setRequiredBundles(req);
			}
			String lazy = getHeaderValue(headers, ICoreConstants.ECLIPSE_AUTOSTART);
			if (lazy == null) {
				lazy = getHeaderValue(headers, ICoreConstants.ECLIPSE_LAZYSTART);
				if (lazy == null) {
					setActivationPolicy(getHeaderValue(headers, Constants.BUNDLE_ACTIVATIONPOLICY));
				}
			}
			if ("true".equals(lazy)) { //$NON-NLS-1$
				setActivationPolicy(Constants.ACTIVATION_LAZY);
			}
			String latest = TargetPlatformHelper.getTargetVersionString();
			IPluginModelBase model = PluginRegistry.findModel(project);
			if (model != null) {
				IPluginBase base = model.getPluginBase();
				String tv = TargetPlatformHelper.getTargetVersionForSchemaVersion(base.getSchemaVersion());
				if (!tv.equals(latest)) {
					setTargetVersion(tv);
				}
			}
			if (build != null) {
				IBuildEntry entry = build.getEntry(IBuildEntry.BIN_INCLUDES);
				if (entry != null) {
					String[] tokens = entry.getTokens();
					if (tokens != null && tokens.length > 0) {
						List<String> strings = new ArrayList<>();
						for (String token : tokens) {
							strings.add(token);
						}
						// remove the default entries
						strings.remove("META-INF/"); //$NON-NLS-1$
						String[] names = ProjectModifyOperation.getLibraryNames(this);
						if (names != null) {
							for (String name : names) {
								strings.remove(name);
								// if the library is a folder, account for trailing slash - see bug 306991
								IPath path = new Path(name);
								if (path.getFileExtension() == null) {
									strings.remove(name + "/"); //$NON-NLS-1$
								}
							}
						}
						// set left overs
						if (!strings.isEmpty()) {
							IPath[] paths = new IPath[strings.size()];
							for (int i = 0; i < strings.size(); i++) {
								paths[i] = new Path(strings.get(i));
							}
							setBinIncludes(paths);
						}
					}
				}
			}
		}
	}

	/**
	 * Returns bundle classpath specifications associated with the specified library or <code>null</code>.
	 *
	 * @param project associated project
	 * @param build build properties object or <code>null</code>
	 * @param libraryName name of library classpath is for
	 * @return bundle classpath specifications associated with the specified library or <code>null</code>
	 * @exception CoreException if unable to access associated Java project for source entries
	 */
	private IBundleClasspathEntry[] getClasspathEntries(IProject project, IBuild build, String libraryName) throws CoreException {
		if (build != null) {
			IBuildEntry entry = build.getEntry(IBuildEntry.JAR_PREFIX + libraryName);
			if (entry == null) {
				entry = build.getEntry(IBuildEntry.OUTPUT_PREFIX + libraryName);
				if (entry == null) {
					// no source or class file folder
					return new IBundleClasspathEntry[] {getBundleProjectService().newBundleClasspathEntry(null, null, new Path(libraryName))};
				}
				// base the entries on class file folders
				return getClasspathEntries(project, entry, true);
			}
			// base the entries on source folders
			return getClasspathEntries(project, entry, false);
		}
		return null;
	}

	/**
	 * Creates and returns a bundle claspath specifications for the given source.<library> build
	 * entry
	 *
	 * @param project
	 * @param entry
	 * @param binary whether a binary folder (<code>true</code>) or source folder (<code>false</code>)
	 * @return associated bundle classpath specifications or <code>null</code> if a malformed entry
	 * @throws CoreException if unable to access Java build path
	 */
	private IBundleClasspathEntry[] getClasspathEntries(IProject project, IBuildEntry entry, boolean binary) throws CoreException {
		String[] tokens = entry.getTokens();
		IPath lib = null;
		if (binary) {
			lib = new Path(entry.getName().substring(IBuildEntry.OUTPUT_PREFIX.length()));
		} else {
			lib = new Path(entry.getName().substring(IBuildEntry.JAR_PREFIX.length()));
		}
		if (tokens != null && tokens.length > 0) {
			IBundleClasspathEntry[] bces = new IBundleClasspathEntry[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				IPath path = new Path(tokens[i]);
				IBundleClasspathEntry spec = null;
				if (binary) {
					spec = getBundleProjectService().newBundleClasspathEntry(null, path, lib);
				} else {
					IJavaProject jp = JavaCore.create(project);
					IPath output = null;
					if (jp.exists()) {
						IClasspathEntry[] rawClasspath = jp.getRawClasspath();
						for (IClasspathEntry cpe : rawClasspath) {
							if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
								if (cpe.getPath().removeFirstSegments(1).equals(path)) {
									output = cpe.getOutputLocation();
									if (output != null) {
										output = output.removeFirstSegments(1);
									}
									break;
								}
							}
						}
					}
					spec = getBundleProjectService().newBundleClasspathEntry(path, output, lib);
				}
				bces[i] = spec;
			}
			return bces;
		}
		return null;
	}

	/**
	 * Create and return a version range from the given string or <code>null</code>.
	 *
	 * @param version version range string or <code>null</code>
	 * @return version range or <code>null</code>
	 */
	private VersionRange getRange(String version) {
		if (version != null) {
			return new VersionRange(version);
		}
		return null;
	}

	/**
	 * Creates and returns a version from the given string or <code>null</code>.
	 *
	 * @param version version string or <code>null</code>
	 * @return version or <code>null</code>
	 */
	private Version getVersion(String version) {
		if (version != null) {
			return new Version(version);
		}
		return null;
	}

	@Override
	public void apply(IProgressMonitor monitor) throws CoreException {
		ProjectModifyOperation operation = new ProjectModifyOperation();
		operation.execute(monitor, this);
		fService = null;
	}

	@Override
	public void setSymbolicName(String name) {
		fSymbolicName = name;
	}

	@Override
	public String getSymbolicName() {
		return fSymbolicName;
	}

	@Override
	public void setLocationURI(URI location) {
		fUri = location;
	}

	@Override
	public URI getLocationURI() {
		return fUri;
	}

	@Override
	public void setBundleName(String name) {
		fBundleName = name;
	}

	@Override
	public String getBundleName() {
		return fBundleName;
	}

	@Override
	public void setBundleVendor(String name) {
		fBundleVendor = name;
	}

	@Override
	public String getBundleVendor() {
		return fBundleVendor;
	}

	@Override
	public void setBundleVersion(Version version) {
		fVersion = version;
	}

	@Override
	public Version getBundleVersion() {
		return fVersion;
	}

	@Override
	public void setSingleton(boolean singleton) {
		fSingleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return fSingleton;
	}

	@Override
	public void setLocalization(IPath path) {
		fLocalization = path;
	}

	@Override
	public IPath getLocalization() {
		return fLocalization;
	}

	@Override
	public String[] getNatureIds() {
		if (fNatures == null) {
			return new String[0];
		}
		String[] copy = new String[fNatures.length];
		System.arraycopy(fNatures, 0, copy, 0, fNatures.length);
		return copy;
	}

	@Override
	public void setNatureIds(String[] natures) {
		String[] copy = null;
		if (natures != null) {
			copy = new String[natures.length];
			System.arraycopy(natures, 0, copy, 0, natures.length);
		}
		fNatures = copy;
	}

	@Override
	public boolean hasNature(String natureId) {
		if (fNatures != null) {
			for (String nature : fNatures) {
				if (nature.equals(natureId)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void setHost(IHostDescription host) {
		fHost = host;
	}

	@Override
	public IHostDescription getHost() {
		return fHost;
	}

	@Override
	public void setDefaultOutputFolder(IPath output) {
		fDefaultOuputFolder = output;
	}

	@Override
	public IPath getDefaultOutputFolder() {
		return fDefaultOuputFolder;
	}

	@Override
	public void setExecutionEnvironments(String[] environments) {
		fEEs = environments;
	}

	@Override
	public String[] getExecutionEnvironments() {
		return fEEs;
	}

	@Override
	public IBundleClasspathEntry[] getBundleClasspath() {
		return fBundleClasspath;
	}

	@Override
	public void setActivator(String className) {
		fActivator = className;
	}

	@Override
	public String getActivator() {
		return fActivator;
	}

	@Override
	public void setTargetVersion(String version) {
		fTargetVersion = version;
	}

	@Override
	public String getTargetVersion() {
		return fTargetVersion;
	}

	@Override
	public void setEquinox(boolean equinox) {
		fIsEquinox = equinox;
	}

	@Override
	public boolean isEquinox() {
		return fIsEquinox;
	}

	@Override
	public void setRequiredBundles(IRequiredBundleDescription[] bundles) {
		fRequiredBundles = bundles;
	}

	@Override
	public IRequiredBundleDescription[] getRequiredBundles() {
		return fRequiredBundles;
	}

	@Override
	public void setPackageImports(IPackageImportDescription[] imports) {
		fImports = imports;
	}

	@Override
	public IPackageImportDescription[] getPackageImports() {
		return fImports;
	}

	@Override
	public void setPackageExports(IPackageExportDescription[] exports) {
		fExports = exports;
	}

	@Override
	public IPackageExportDescription[] getPackageExports() {
		return fExports;
	}

	@Override
	public IProject getProject() {
		return fProject;
	}

	@Override
	@Deprecated
	public void setBundleClassath(IBundleClasspathEntry[] entries) {
		setBundleClasspath(entries);
	}

	@Override
	public void setBundleClasspath(IBundleClasspathEntry[] entries) {
		fBundleClasspath = entries;
	}

	@Override
	public void setBinIncludes(IPath[] paths) {
		fBinIncludes = paths;
	}

	@Override
	public IPath[] getBinIncludes() {
		return fBinIncludes;
	}

	@Override
	public void setBundleRoot(IPath path) {
		fRoot = path;
	}

	@Override
	public IPath getBundleRoot() {
		return fRoot;
	}

	@Override
	public boolean isExtensionRegistry() {
		return fIsExtensionRegistry;
	}

	@Override
	public void setExtensionRegistry(boolean supportExtensions) {
		fIsExtensionRegistry = supportExtensions;
	}

	@Override
	public String[] getLaunchShortcuts() {
		return fLaunchShortcuts;
	}

	@Override
	public void setLaunchShortcuts(String[] ids) {
		fLaunchShortcuts = ids;
	}

	@Override
	public String getExportWizardId() {
		return fExportWizard;
	}

	@Override
	public void setExportWizardId(String id) {
		fExportWizard = id;
	}

	@Override
	public void setActivationPolicy(String policy) {
		if (Constants.ACTIVATION_LAZY.equals(policy)) {
			fActivationPolicy = policy;
		} else {
			fActivationPolicy = null;
		}
	}

	@Override
	public String getActivationPolicy() {
		return fActivationPolicy;
	}

	@Override
	public void setHeader(String header, String value) {
		fHeaders.put(header, value);
	}

	@Override
	public String getHeader(String header) {
		if (fHeaders.containsKey(header)) { // might be null so check contains
			return fHeaders.get(header);
		}
		if (fReadHeaders != null) {
			if (fReadHeaders.containsKey(header)) {
				String value = (String) fReadHeaders.get(header);
				if (value == null) {
					// Return the empty string for present empty headers (instead of null - which means missing)
					return ""; //$NON-NLS-1$
				}
				return value;
			}
		}
		return null;
	}

	/**
	 * Returns any extra headers that have been specified. A map of header names to header
	 * values.
	 *
	 * @return a map of header names to header values, possible empty
	 */
	Map<String, String> getExtraHeaders() {
		return fHeaders;
	}

}
