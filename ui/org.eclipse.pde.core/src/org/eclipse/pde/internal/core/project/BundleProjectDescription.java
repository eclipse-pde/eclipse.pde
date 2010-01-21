/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.project;

import java.net.URI;
import java.util.*;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.project.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.bundle.BundleFragment;
import org.eclipse.pde.internal.core.bundle.BundlePluginBase;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.pde.internal.core.text.bundle.*;
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
	private IRequiredBundleDescription[] fRequiredBundles;
	private IPackageImportDescription[] fImports;
	private IPackageExportDescription[] fExports;
	private IPath[] fBinIncludes;
	private WorkspacePluginModelBase fModel;
	private IBundleProjectService fService;

	private static final String[] EQUINOX_HEADERS = new String[] {ICoreConstants.ECLIPSE_AUTOSTART, Constants.BUNDLE_ACTIVATIONPOLICY, ICoreConstants.ECLIPSE_LAZYSTART};

	/**
	 * Constructs a bundle description for the specified project.
	 * 
	 * @param project project that may or may not exist
	 * @throws CoreException 
	 */
	public BundleProjectDescription(IProject project) throws CoreException {
		fProject = project;
		if (project.exists() && project.isOpen()) {
			initiaize(project);
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
	 * Initialize settings from the given project.
	 * 
	 * @param project project
	 * @exception CoreException if unable to initialize
	 */
	private void initiaize(IProject project) throws CoreException {
		IContainer root = PDEProject.getBundleRoot(project);
		if (root != project) {
			setBundleRoot(root.getProjectRelativePath());
		}
		IPluginModelBase model = PluginRegistry.findModel(project);
		if (model != null) {
			IPluginBase base = model.getPluginBase();
			if (base instanceof IPlugin) {
				IPlugin plugin = (IPlugin) base;
				setActivator(plugin.getClassName());
			}
			IBuild build = ClasspathUtilCore.getBuild(model);
			setBundleName(base.getName());
			setBundleVendor(base.getProviderName());
			String version = base.getVersion();
			if (version != null) {
				setBundleVersion(new Version(version));
			}
			IJavaProject jp = JavaCore.create(project);
			if (jp.exists()) {
				setDefaultOutputFolder(jp.getOutputLocation().removeFirstSegments(1));
			}
			if (model.isFragmentModel()) {
				IFragmentModel fragModel = (IFragmentModel) model;
				IFragment frag = fragModel.getFragment();
				if (frag instanceof BundleFragment) {
					// use header since IFragment implementation returns 0.0.0 even when unspecified
					BundleFragment bf = (BundleFragment) frag;
					String header = bf.getBundle().getHeader(Constants.FRAGMENT_HOST);
					ManifestElement[] elements;
					try {
						elements = ManifestElement.parseHeader(Constants.FRAGMENT_HOST, header);
					} catch (BundleException e) {
						throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, e.getMessage(), e));
					}
					setHost(getBundleProjectService().newHost(frag.getPluginId(), getRange(elements[0].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE))));
				} else {
					setHost(getBundleProjectService().newHost(frag.getPluginId(), getRange(frag.getPluginVersion())));
				}
			}
			if (base instanceof BundlePluginBase) {
				IBundle bundle = ((BundlePluginBase) base).getBundle();
				String value = bundle.getHeader(Constants.BUNDLE_LOCALIZATION);
				if (value != null) {
					setLocalization(new Path(value));
				}
				IManifestHeader header = createHeader(bundle, Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
				if (header instanceof RequiredExecutionEnvironmentHeader) {
					ExecutionEnvironment[] environments = ((RequiredExecutionEnvironmentHeader) header).getEnvironments();
					if (environments != null && environments.length > 0) {
						String[] keys = new String[environments.length];
						for (int i = 0; i < keys.length; i++) {
							keys[i] = environments[i].getName();
						}
						setExecutionEnvironments(keys);
					}
				}
				header = createHeader(bundle, Constants.BUNDLE_CLASSPATH);
				IBundleClasspathEntry[] classpath = null;
				if (header instanceof BundleClasspathHeader) {
					Vector libNames = ((BundleClasspathHeader) header).getElementNames();
					if (!libNames.isEmpty()) {
						List collect = new ArrayList();
						Iterator iterator = libNames.iterator();
						while (iterator.hasNext()) {
							String libName = (String) iterator.next();
							IBundleClasspathEntry[] entries = getClasspathEntries(project, build, libName);
							if (entries != null) {
								for (int i = 0; i < entries.length; i++) {
									collect.add(entries[i]);
								}
							}
						}
						classpath = (IBundleClasspathEntry[]) collect.toArray(new IBundleClasspathEntry[collect.size()]);
					}
				} else if (header == null) {
					// default bundle classpath of '.'
					classpath = getClasspathEntries(project, build, "."); //$NON-NLS-1$
				}
				setBundleClassath(classpath);
				header = createHeader(bundle, Constants.BUNDLE_SYMBOLICNAME);
				if (header instanceof BundleSymbolicNameHeader) {
					setSingleton(((BundleSymbolicNameHeader) header).isSingleton());
				}
				for (int i = 0; i < EQUINOX_HEADERS.length; i++) {
					if (bundle.getHeader(EQUINOX_HEADERS[i]) != null) {
						setEqunioxHeaders(true);
						break;
					}
				}
				header = createHeader(bundle, Constants.IMPORT_PACKAGE);
				if (header instanceof ImportPackageHeader) {
					ImportPackageObject[] packages = ((ImportPackageHeader) header).getPackages();
					if (packages != null && packages.length > 0) {
						IPackageImportDescription[] imports = new IPackageImportDescription[packages.length];
						for (int i = 0; i < packages.length; i++) {
							ImportPackageObject pkg = packages[i];
							imports[i] = getBundleProjectService().newPackageImport(pkg.getName(), getRange(pkg.getVersion()), pkg.isOptional());
						}
						setPackageImports(imports);
					}
				}
				header = createHeader(bundle, Constants.EXPORT_PACKAGE);
				if (header instanceof ExportPackageHeader) {
					ExportPackageObject[] packages = ((ExportPackageHeader) header).getPackages();
					if (packages != null && packages.length > 0) {
						IPackageExportDescription[] exports = new IPackageExportDescription[packages.length];
						for (int i = 0; i < packages.length; i++) {
							ExportPackageObject exp = packages[i];
							String[] friends = null;
							PackageFriend[] pfs = exp.getFriends();
							if (pfs != null && pfs.length > 0) {
								friends = new String[pfs.length];
								for (int j = 0; j < pfs.length; j++) {
									friends[j] = pfs[j].getName();
								}
							}
							exports[i] = getBundleProjectService().newPackageExport(exp.getName(), getVersion(exp.getVersion()), !exp.isInternal(), friends);
						}
						setPackageExports(exports);
					}
				}
				header = createHeader(bundle, Constants.REQUIRE_BUNDLE);
				if (header instanceof RequireBundleHeader) {
					RequireBundleObject[] bundles = ((RequireBundleHeader) header).getRequiredBundles();
					if (bundles != null && bundles.length > 0) {
						IRequiredBundleDescription[] req = new IRequiredBundleDescription[bundles.length];
						for (int i = 0; i < bundles.length; i++) {
							RequireBundleObject rb = bundles[i];
							req[i] = getBundleProjectService().newRequiredBundle(rb.getId(), getRange(rb.getVersion()), rb.isOptional(), rb.isReexported());
						}
						setRequiredBundles(req);
					}
				}
			}
			setLocationURI(project.getDescription().getLocationURI());
			setNatureIds(project.getDescription().getNatureIds());
			setSymbolicName(base.getId());
			String latest = TargetPlatformHelper.getTargetVersionString();
			String tv = TargetPlatformHelper.getTargetVersionForSchemaVersion(base.getSchemaVersion());
			if (!tv.equals(latest)) {
				setTargetVersion(tv);
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
	 * Returns a structured header from a bundle model
	 * 
	 * @param bundle the bundle
	 * @param header header name/key
	 * @return header or <code>null</code>
	 */
	private IManifestHeader createHeader(IBundle bundle, String header) {
		BundleModelFactory factory = new BundleModelFactory(bundle.getModel());
		String headerValue = bundle.getHeader(header);
		if (headerValue == null) {
			return null;
		}
		return factory.createHeader(header, headerValue);
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
		fModel = operation.getModel();
		fService = null;
	}

	/**
	 * Returns the model created by this operation.
	 * 
	 * @return model
	 */
	public WorkspacePluginModelBase getModel() {
		return fModel;
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
		return fNatures;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#setNatureIds(java.lang.String[])
	 */
	public void setNatureIds(String[] natures) {
		fNatures = natures;
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
	public void setEqunioxHeaders(boolean equinox) {
		fIsEquinox = equinox;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.project.IBundleProjectDescription#isEquinoxHeaders()
	 */
	public boolean isEquinoxHeaders() {
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

}
