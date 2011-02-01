/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.frameworkadmin.BundleInfo;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.provisional.*;
import org.osgi.framework.*;

/**
 * Common function for bundle containers.
 * 
 * @since 3.5
 */
public abstract class AbstractBundleContainer implements IBundleContainer {

	/**
	 * Resolved bundles or <code>null</code> if unresolved
	 */
	protected IResolvedBundle[] fBundles;

	/**
	 * List of features contained in this bundle container or <code>null</code> if unresolved
	 */
	protected IFeatureModel[] fFeatures;

	/**
	 * Status generated when this container was resolved, possibly <code>null</code>
	 */
	private IStatus fResolutionStatus;

	/**
	 * A registry can be built to identify old school source bundles.
	 */
	private IExtensionRegistry fRegistry;

	/**
	 * Most recent source path detected from an old-style source bundle extension.
	 */
	private String fSourcePath;

	/**
	 * The Java VM Arguments specified by this bundle container 
	 */
	private String[] fVMArgs;

	/**
	 * Resolves any string substitution variables in the given text returning
	 * the result.
	 * 
	 * @param text text to resolve
	 * @return result of the resolution
	 * @throws CoreException if unable to resolve 
	 */
	protected String resolveVariables(String text) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		return manager.performStringSubstitution(text);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#isResolved()
	 */
	public final boolean isResolved() {
		return fResolutionStatus != null && fResolutionStatus.getSeverity() != IStatus.CANCEL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#resolve(org.eclipse.pde.internal.core.target.provisional.ITargetDefinition, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public final IStatus resolve(ITargetDefinition definition, IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 150);
		try {
			fBundles = resolveBundles(definition, subMonitor.newChild(100));
			fFeatures = resolveFeatures(definition, subMonitor.newChild(50));
			fResolutionStatus = Status.OK_STATUS;
			if (subMonitor.isCanceled()) {
				fBundles = null;
				fResolutionStatus = Status.CANCEL_STATUS;
			}
		} catch (CoreException e) {
			fBundles = new IResolvedBundle[0];
			fFeatures = new IFeatureModel[0];
			fResolutionStatus = e.getStatus();
		} finally {
			if (fRegistry != null) {
				fRegistry.stop(this);
				fRegistry = null;
			}
			subMonitor.done();
			if (monitor != null) {
				monitor.done();
			}
		}
		return fResolutionStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getStatus()
	 */
	public IStatus getStatus() {
		if (!isResolved()) {
			return null;
		}
		return fResolutionStatus;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getBundles()
	 */
	public final IResolvedBundle[] getBundles() {
		if (isResolved()) {
			return fBundles;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getFeatures()
	 */
	public IFeatureModel[] getFeatures() {
		if (isResolved()) {
			return fFeatures;
		}
		return null;
	}

	/**
	 * Resolves all source and executable bundles in this container
	 * <p>
	 * Subclasses must implement this method.
	 * </p><p>
	 * <code>beginTask()</code> and <code>done()</code> will be called on the given monitor by the caller. 
	 * </p>
	 * @param definition target context
	 * @param monitor progress monitor
	 * @return all source and executable bundles in this container
	 * @throws CoreException if an error occurs
	 */
	protected abstract IResolvedBundle[] resolveBundles(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/**
	 * Collects all of the features in this container.  May return an empty array if {@link #resolveBundles(ITargetDefinition, IProgressMonitor)}
	 * has not been called previously.
	 * <p>
	 * Subclasses must implement this method.
	 * </p><p>
	 * <code>beginTask()</code> and <code>done()</code> will be called on the given monitor by the caller. 
	 * </p>
	 * @param definition target context
	 * @param monitor progress monitor
	 * @return all features in this container
	 * @throws CoreException if an error occurs
	 */
	protected abstract IFeatureModel[] resolveFeatures(ITargetDefinition definition, IProgressMonitor monitor) throws CoreException;

	/**
	 * Returns a string that identifies the type of bundle container.  This type is persisted to xml
	 * so that the correct bundle container is created when deserializing the xml.  This type is also
	 * used to alter how the containers are presented to the user in the UI.
	 * 
	 * @return string identifier for the type of bundle container.
	 */
	public abstract String getType();

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * <p>
	 * TODO: Ideally we won't need this method. Currently the PDE target platform preferences are
	 * based on a home location and additional locations, so we need the information.
	 * </p>
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public abstract String getLocation(boolean resolve) throws CoreException;

	/**
	 * Returns whether this container has equivalent bundle content to the given container
	 * 
	 * @param container bundle container
	 * @return whether content is equivalent
	 */
	public abstract boolean isContentEqual(AbstractBundleContainer container);

	/**
	 * Sets the resolution status to null.  This container will be considered unresolved.
	 */
	protected void clearResolutionStatus() {
		fResolutionStatus = null;
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundle the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if unable to read
	 */
	protected Map loadManifest(BundleInfo bundle) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		try {
			String path = bundle.getLocation().toURL().getFile();
			File dirOrJar = new File(path);
			String extension = new Path(path).getFileExtension();
			if (extension != null && extension.equals("jar") && dirOrJar.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(dirOrJar, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(dirOrJar, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundle.getSymbolicName()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundle.getSymbolicName()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary
	 * @throws CoreException if manifest has invalid syntax or is missing
	 */
	protected Map loadManifest(File bundleLocation) throws CoreException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists()) {
					manifestStream = new FileInputStream(file);
				} else {
					Map map = loadPluginXML(bundleLocation);
					if (map != null) {
						return map; // else fall through to invalid manifest
					}
				}
			}
			if (manifestStream == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			Map map = ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
			// Validate manifest - BSN must be present.
			// Else look for plugin.xml in case it's an old style plug-in
			String bsn = (String) map.get(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn == null && bundleLocation.isDirectory()) {
				map = loadPluginXML(bundleLocation); // not a bundle manifest, try plugin.xml
			}
			if (map == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			return map;
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			PDECore.log(e);
		}
	}

	/**
	 * Parses an old style plug-in's (or fragment's) XML definition file into a dictionary.
	 * The plug-in must be in a directory at the specified location.
	 * 
	 * @param pluginDir root location of the plug-in
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws CoreException if manifest has invalid syntax
	 */
	private Map loadPluginXML(File pluginDir) throws CoreException {
		File pxml = new File(pluginDir, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
		File fxml = new File(pluginDir, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
		if (pxml.exists() || fxml.exists()) {
			// support classic non-OSGi plug-in
			PluginConverter converter = (PluginConverter) PDECore.getDefault().acquireService(PluginConverter.class.getName());
			if (converter != null) {
				try {
					Dictionary convert = converter.convertManifest(pluginDir, false, null, false, null);
					if (convert != null) {
						Map map = new HashMap(convert.size(), 1.0f);
						Enumeration keys = convert.keys();
						while (keys.hasMoreElements()) {
							Object key = keys.nextElement();
							map.put(key, convert.get(key));
						}
						return map;
					}
				} catch (PluginConversionException e) {
					throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.DirectoryBundleContainer_2, pluginDir.getAbsolutePath()), e));
				}
			}
		}
		return null;
	}

	protected IResolvedBundle resolveBundle(BundleInfo info, boolean isSource) {
		File file = null;
		Map manifest = null;
		boolean fragment = false;
		IStatus status = null;
		try {
			file = new File(info.getLocation());
			manifest = loadManifest(file);
			fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
		} catch (CoreException e) {
			status = e.getStatus();
		}
		return new ResolvedBundle(info, this, status, isSource ? getProvidedSource(file, info.getSymbolicName(), manifest) : null, false, fragment);

	}

	/**
	 * Disposes any registry created when resolving bundles.
	 */
	protected void disposeRegistry() {

	}

	/**
	 * Returns a resolved bundle for the given file or <code>null</code> if none.
	 * <p>
	 * Clients of this method must call 
	 * </p>
	 * @param file root jar or folder that contains a bundle
	 * @return resolved bundle or <code>null</code>
	 * @exception CoreException if not a valid bundle
	 */
	protected IResolvedBundle generateBundle(File file) throws CoreException {
		Map manifest = loadManifest(file);
		try {
			String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
			if (header != null) {
				ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
				if (elements != null) {
					String name = elements[0].getValue();
					if (name != null) {
						BundleInfo info = new BundleInfo();
						info.setSymbolicName(name);
						info.setLocation(file.toURI());
						header = (String) manifest.get(Constants.BUNDLE_VERSION);
						if (header != null) {
							elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
							if (elements != null) {
								info.setVersion(elements[0].getValue());
							}
						}
						BundleInfo source = getProvidedSource(file, name, manifest);
						boolean fragment = manifest.containsKey(Constants.FRAGMENT_HOST);
						ResolvedBundle rb = new ResolvedBundle(info, this, null, source, false, fragment);
						rb.setSourcePath(fSourcePath);
						return rb;
					}
				}
			}
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IResolvedBundle.STATUS_INVALID_MANIFEST, NLS.bind(Messages.DirectoryBundleContainer_3, file.getAbsolutePath()), e));
		}
		return null;
	}

	/**
	 * If the given bundle is a source bundle, the bundle that this bundle provides source for will be returned.
	 * If the given bundle is not a source bundle or there was a problem getting the source target, <code>null</code>
	 * will be returned.
	 * 
	 * @param bundle location of the bundle in the file system, can be <code>null</code> to skip searching plugin.xml
	 * @param symbolicName symbolic name of the bundle, can be <code>null</code> to skip searching of plugin.xml
	 * @param manifest the bundle's manifest, can be <code>null</code> to skip searching of manifest entries
	 * @return bundle for provided source or <code>null</code> if not a source bundle
	 */
	private BundleInfo getProvidedSource(File bundle, String symbolicName, Map manifest) {
		fSourcePath = null;
		if (manifest != null) {
			if (manifest.containsKey(ICoreConstants.ECLIPSE_SOURCE_BUNDLE)) {
				try {
					ManifestElement[] manifestElements = ManifestElement.parseHeader(ICoreConstants.ECLIPSE_SOURCE_BUNDLE, (String) manifest.get(ICoreConstants.ECLIPSE_SOURCE_BUNDLE));
					if (manifestElements != null) {
						for (int j = 0; j < manifestElements.length; j++) {
							ManifestElement currentElement = manifestElements[j];
							String binaryPluginName = currentElement.getValue();
							String versionEntry = currentElement.getAttribute(Constants.VERSION_ATTRIBUTE);
							// Currently the version attribute is required
							if (binaryPluginName != null && binaryPluginName.length() > 0 && versionEntry != null && versionEntry.length() > 0) {
								return new BundleInfo(binaryPluginName, versionEntry, null, BundleInfo.NO_LEVEL, false);
							}
						}
					}
				} catch (BundleException e) {
					PDECore.log(e);
					return null;
				}
			}
			// source bundles never have a class path
			if (manifest.containsKey(Constants.BUNDLE_CLASSPATH)) {
				return null;
			}
		}

		if (bundle != null && symbolicName != null) {
			// old source bundles were never jar'd
			if (bundle.isFile()) {
				return null;
			}

			// check for an "org.eclipse.pde.core.source" extension 
			File pxml = new File(bundle, ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			if (!pxml.exists()) {
				pxml = new File(bundle, ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR);
			}
			if (pxml.exists()) {
				IExtensionRegistry registry = getRegistry();
				RegistryContributor contributor = new RegistryContributor(symbolicName, symbolicName, null, null);
				try {
					registry.addContribution(new BufferedInputStream(new FileInputStream(pxml)), contributor, false, null, null, this);
					IExtension[] extensions = registry.getExtensions(contributor);
					for (int i = 0; i < extensions.length; i++) {
						IExtension extension = extensions[i];
						if (ICoreConstants.EXTENSION_POINT_SOURCE.equals(extension.getExtensionPointUniqueIdentifier())) {
							IConfigurationElement[] elements = extension.getConfigurationElements();
							if (elements.length == 1) {
								fSourcePath = elements[0].getAttribute("path"); //$NON-NLS-1$
							}
							return new BundleInfo(null, null, bundle.toURI(), BundleInfo.NO_LEVEL, false);
						}
					}
				} catch (FileNotFoundException e) {
				}
			}
		}
		return null;
	}

	/**
	 * Returns an extension registry used to identify source bundles.
	 * 
	 * @return extension registry
	 */
	private IExtensionRegistry getRegistry() {
		if (fRegistry == null) {
			fRegistry = RegistryFactory.createRegistry(null, this, this);
			// contribute PDE source extension point
			String bogusDef = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<?eclipse version=\"3.2\"?>\n<plugin><extension-point id=\"source\" name=\"source\"/>\n</plugin>"; //$NON-NLS-1$
			RegistryContributor contributor = new RegistryContributor(PDECore.PLUGIN_ID, PDECore.PLUGIN_ID, null, null);
			fRegistry.addContribution(new ByteArrayInputStream(bogusDef.getBytes()), contributor, false, null, null, this);
		}
		return fRegistry;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$

		if (fVMArgs == null) {
			try {
				FrameworkAdmin fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
				if (fwAdmin == null) {
					Bundle fwAdminBundle = Platform.getBundle(FWK_ADMIN_EQ);
					if (fwAdminBundle != null){
						fwAdminBundle.start();
						fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
					}
				}
				if (fwAdmin != null) {
					Manipulator manipulator = fwAdmin.getManipulator();
					ConfigData configData = new ConfigData(null, null, null, null);

					String home = getLocation(true);
					manipulator.getLauncherData().setLauncher(new File(home, "eclipse")); //$NON-NLS-1$
					File installDirectory = new File(home);
					if (Platform.getOS().equals(Platform.OS_MACOSX))
						installDirectory = new File(installDirectory, "Eclipse.app/Contents/MacOS"); //$NON-NLS-1$
					manipulator.getLauncherData().setLauncherConfigLocation(new File(installDirectory, "eclipse.ini")); //$NON-NLS-1$
					manipulator.getLauncherData().setHome(new File(home));

					manipulator.setConfigData(configData);
					manipulator.load();
					fVMArgs = manipulator.getLauncherData().getJvmArgs();
				}
			} catch (BundleException e) {
				PDECore.log(e);
			} catch (CoreException e) {
				PDECore.log(e);
			} catch (IOException e) {
				PDECore.log(e);
			}

		}
		if (fVMArgs == null || fVMArgs.length == 0) {
			return null;
		}
		return fVMArgs;
	}

	/**
	 * Associate this bundle container with the given target.  This allows for the container and 
	 * the target to share configuration information etc.  
	 * 
	 * @param target the target to which this container is being added.
	 */
	protected void associateWithTarget(ITargetDefinition target) {
		// Do nothing by default
	}
}
