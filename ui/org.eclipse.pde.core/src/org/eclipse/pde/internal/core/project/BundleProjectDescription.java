/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.osgi.framework.*;

/**
 * A bundle project description contains the meta-data required to define
 * a bundle or plug-in project.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.6
 */
public class BundleProjectDescription implements IBundleProjectDescription {

	private IProject fProject;
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
	private Map fHeaders = new HashMap();
	private Map fReadHeaders = null;

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
			fService = (IBundleProjectService) PDECore.getDefault().acquireService(IBundleProjectService.class.getName());
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
	private String getHeaderValue(Map headers, String key) throws CoreException {
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
	private ManifestElement[] parseHeader(Map headers, String key) throws CoreException {
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
			Map headers;
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
				List collect = new ArrayList();
				for (int i = 0; i < elements.length; i++) {
					String libName = elements[i].getValue();
					IBundleClasspathEntry[] entries = getClasspathEntries(project, build, libName);
					if (entries != null) {
						for (int j = 0; j < entries.length; j++) {
							collect.add(entries[j]);
						}
					}
				}
				classpath = (IBundleClasspathEntry[]) collect.toArray(new IBundleClasspathEntry[collect.size()]);
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
						List strings = new ArrayList();
						for (int i = 0; i < tokens.length; i++) {
							strings.add(tokens[i]);
						}
						// remove the default entries
						strings.remove("META-INF/"); //$NON-NLS-1$
						String[] names = ProjectModifyOperation.getLibraryNames(this);
						if (names != null) {
							for (int i = 0; i < names.length; i++) {
								strings.remove(names[i]);
								// if the library is a folder, account for trailing slash - see bug 306991
								IPath path = new Path(names[i]);
								if (path.getFileExtension() == null) {
									strings.remove(names[i] + "/"); //$NON-NLS-1$
								}
							}
						}
						// set left overs
						if (!strings.isEmpty()) {
							IPath[] paths = new IPath[strings.size()];
							for (int i = 0; i < strings.size(); i++) {
								paths[i] = new Path((String) strings.get(i));
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
						for (int j = 0; j < rawClasspath.length; j++) {
							IClasspathEntry cpe = rawClasspath[j];
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

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#apply(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void apply(IProgressMonitor monitor) throws CoreException {
		ProjectModifyOperation operation = new ProjectModifyOperation();
		operation.execute(monitor, this);
		fService = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setSymbolicName(java.lang.String)
	 */
	public void setSymbolicName(String name) {
		fSymbolicName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getSymbolicName()
	 */
	public String getSymbolicName() {
		return fSymbolicName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setLocationURI(java.net.URI)
	 */
	public void setLocationURI(URI location) {
		fUri = location;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getLocationURI()
	 */
	public URI getLocationURI() {
		return fUri;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleName(java.lang.String)
	 */
	public void setBundleName(String name) {
		fBundleName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBundleName()
	 */
	public String getBundleName() {
		return fBundleName;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleVendor(java.lang.String)
	 */
	public void setBundleVendor(String name) {
		fBundleVendor = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBundleVendor()
	 */
	public String getBundleVendor() {
		return fBundleVendor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleVersion(org.osgi.framework.Version)
	 */
	public void setBundleVersion(Version version) {
		fVersion = version;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBundleVersion()
	 */
	public Version getBundleVersion() {
		return fVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setSingleton(boolean)
	 */
	public void setSingleton(boolean singleton) {
		fSingleton = singleton;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#isSingleton()
	 */
	public boolean isSingleton() {
		return fSingleton;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setLocalization(org.eclipse.core.runtime.IPath)
	 */
	public void setLocalization(IPath path) {
		fLocalization = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getLocalization()
	 */
	public IPath getLocalization() {
		return fLocalization;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getNatureIds()
	 */
	public String[] getNatureIds() {
		if (fNatures == null) {
			return new String[0];
		}
		String[] copy = new String[fNatures.length];
		System.arraycopy(fNatures, 0, copy, 0, fNatures.length);
		return copy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setNatureIds(java.lang.String[])
	 */
	public void setNatureIds(String[] natures) {
		String[] copy = null;
		if (natures != null) {
			copy = new String[natures.length];
			System.arraycopy(natures, 0, copy, 0, natures.length);
		}
		fNatures = copy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#hasNature(java.lang.String)
	 */
	public boolean hasNature(String natureId) {
		if (fNatures != null) {
			for (int i = 0; i < fNatures.length; i++) {
				if (fNatures[i].equals(natureId)) {
					return true;
				}
			}
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setHost(org.eclipse.pde.core.project.IHostDescription)
	 */
	public void setHost(IHostDescription host) {
		fHost = host;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getHost()
	 */
	public IHostDescription getHost() {
		return fHost;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setDefaultOutputFolder(org.eclipse.core.runtime.IPath)
	 */
	public void setDefaultOutputFolder(IPath output) {
		fDefaultOuputFolder = output;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getDefaultOutputFolder()
	 */
	public IPath getDefaultOutputFolder() {
		return fDefaultOuputFolder;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setExecutionEnvironments(java.lang.String[])
	 */
	public void setExecutionEnvironments(String[] environments) {
		fEEs = environments;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getExecutionEnvironments()
	 */
	public String[] getExecutionEnvironments() {
		return fEEs;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBundleClasspath()
	 */
	public IBundleClasspathEntry[] getBundleClasspath() {
		return fBundleClasspath;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setActivator(java.lang.String)
	 */
	public void setActivator(String className) {
		fActivator = className;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getActivator()
	 */
	public String getActivator() {
		return fActivator;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setTargetVersion(java.lang.String)
	 */
	public void setTargetVersion(String version) {
		fTargetVersion = version;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getTargetVersion()
	 */
	public String getTargetVersion() {
		return fTargetVersion;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setEqunioxHeaders(boolean)
	 */
	public void setEquinox(boolean equinox) {
		fIsEquinox = equinox;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#isEquinoxHeaders()
	 */
	public boolean isEquinox() {
		return fIsEquinox;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setRequiredBundles(org.eclipse.pde.core.project.IRequiredBundleDescription[])
	 */
	public void setRequiredBundles(IRequiredBundleDescription[] bundles) {
		fRequiredBundles = bundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getRequiredBundles()
	 */
	public IRequiredBundleDescription[] getRequiredBundles() {
		return fRequiredBundles;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setPackageImports(org.eclipse.pde.core.project.IPackageImportDescription[])
	 */
	public void setPackageImports(IPackageImportDescription[] imports) {
		fImports = imports;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getPackageImports()
	 */
	public IPackageImportDescription[] getPackageImports() {
		return fImports;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setPackageExports(org.eclipse.pde.core.project.IPackageExportDescription[])
	 */
	public void setPackageExports(IPackageExportDescription[] exports) {
		fExports = exports;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getPackageExports()
	 */
	public IPackageExportDescription[] getPackageExports() {
		return fExports;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getProject()
	 */
	public IProject getProject() {
		return fProject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleClassath(org.eclipse.pde.core.project.IBundleClasspathSpecification[])
	 */
	public void setBundleClassath(IBundleClasspathEntry[] entries) {
		setBundleClasspath(entries);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleClasspath(org.eclipse.pde.core.project.IBundleClasspathEntry[])
	 */
	public void setBundleClasspath(IBundleClasspathEntry[] entries) {
		fBundleClasspath = entries;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBinIncludes(org.eclipse.core.runtime.IPath[])
	 */
	public void setBinIncludes(IPath[] paths) {
		fBinIncludes = paths;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBinIncludes()
	 */
	public IPath[] getBinIncludes() {
		return fBinIncludes;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setBundleRoot(org.eclipse.core.runtime.IPath)
	 */
	public void setBundleRoot(IPath path) {
		fRoot = path;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getBundleRoot()
	 */
	public IPath getBundleRoot() {
		return fRoot;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#isExtensionRegistry()
	 */
	public boolean isExtensionRegistry() {
		return fIsExtensionRegistry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setExtensionRegistry(boolean)
	 */
	public void setExtensionRegistry(boolean supportExtensions) {
		fIsExtensionRegistry = supportExtensions;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getLaunchShortcuts()
	 */
	public String[] getLaunchShortcuts() {
		return fLaunchShortcuts;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setLaunchShortcuts(java.lang.String[])
	 */
	public void setLaunchShortcuts(String[] ids) {
		fLaunchShortcuts = ids;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getExportWizardId()
	 */
	public String getExportWizardId() {
		return fExportWizard;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setExportWizardId(java.lang.String)
	 */
	public void setExportWizardId(String id) {
		fExportWizard = id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setActivationPolicy(java.lang.String)
	 */
	public void setActivationPolicy(String policy) {
		if (Constants.ACTIVATION_LAZY.equals(policy)) {
			fActivationPolicy = policy;
		} else {
			fActivationPolicy = null;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getActivationPolicy()
	 */
	public String getActivationPolicy() {
		return fActivationPolicy;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String header, String value) {
		fHeaders.put(header, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#getHeader(java.lang.String)
	 */
	public String getHeader(String header) {
		if (fHeaders.containsKey(header)) { // might be null so check contains
			return (String) fHeaders.get(header);
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
	Map getExtraHeaders() {
		return fHeaders;
	}

}
