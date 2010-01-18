package org.eclipse.pde.internal.core.target;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.*;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.equinox.internal.p2.artifact.repository.simple.SimpleArtifactDescriptor;
import org.eclipse.equinox.internal.p2.metadata.ArtifactKey;
import org.eclipse.equinox.internal.provisional.frameworkadmin.*;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.repository.artifact.IArtifactDescriptor;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.osgi.framework.*;

/**
 * Abstract base for bundle containers that provide bundles from the local file system.  Because
 * they are local they can provide a location and likely need to generate metadata.
 * 
 * @see DirectoryBundleContainer
 * @see ProfileBundleContainer
 * @see FeatureBundleContainer
 */
public abstract class AbstractLocalBundleContainer implements IBundleContainer {

	private static final String BUNDLE_ARTIFACT_CLASSIFIER = "osgi.bundle"; //$NON-NLS-1$

	/**
	 * The Java VM Arguments specified by this bundle container 
	 */
	private String[] fVMArgs;

	/**
	 * Returns a path in the local file system to the root of the bundle container.
	 * 
	 * @param resolve whether to resolve variables in the path
	 * @return home location
	 * @exception CoreException if unable to resolve the location
	 */
	public abstract String getLocation(boolean resolve) throws CoreException;

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
	 * @see org.eclipse.pde.internal.core.target.provisional.IBundleContainer#getVMArguments()
	 */
	public String[] getVMArguments() {
		String FWK_ADMIN_EQ = "org.eclipse.equinox.frameworkadmin.equinox"; //$NON-NLS-1$

		if (fVMArgs == null) {
			try {
				FrameworkAdmin fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
				if (fwAdmin == null) {
					Bundle fwAdminBundle = Platform.getBundle(FWK_ADMIN_EQ);
					fwAdminBundle.start();
					fwAdmin = (FrameworkAdmin) PDECore.getDefault().acquireService(FrameworkAdmin.class.getName());
				}
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

	protected IInstallableUnit[] generateMetadata(File[] files, IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 50);

		StateObjectFactory stateFactory = Platform.getPlatformAdmin().getFactory();
		State state = stateFactory.createState(false);

		SubMonitor loopProgress = subMon.newChild(25).setWorkRemaining(files.length);
		List bundleDescriptions = new ArrayList();
		List sourceDescriptions = new ArrayList();
		for (int i = 0; i < files.length; i++) {
			if (subMon.isCanceled()) {
				return new IInstallableUnit[0];
			}
			try {
				Map manifest = loadManifest(files[i]);
				String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
				if (header != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
					if (elements != null) {
						String name = elements[0].getValue();
						if (name != null) {
							Hashtable dictionary = new Hashtable(manifest.size());
							dictionary.putAll(manifest);
							BundleDescription bd = stateFactory.createBundleDescription(state, (Dictionary) manifest, (files[i].toURL()).toExternalForm(), (long) i);
							if ((String) manifest.get(ICoreConstants.ECLIPSE_SOURCE_BUNDLE) != null) {
								sourceDescriptions.add(bd);
							} else {
								bundleDescriptions.add(bd);
							}
						}
					}
				}
			} catch (MalformedURLException e) {
				// ignore invalid bundles
			} catch (BundleException e) {
				// ignore invalid bundles
			} catch (CoreException e) {
				// ignore invalid bundles
			}
			loopProgress.worked(1);
		}

		if (subMon.isCanceled()) {
			return new IInstallableUnit[0];
		}

		// Create metadata for the bundles
		IInstallableUnit[] ius = P2Utils.createInstallableUnits(bundleDescriptions, false);
		IInstallableUnit[] sourceIus = P2Utils.createInstallableUnits(sourceDescriptions, true);
		subMon.worked(25);
		subMon.done();

		if (sourceIus.length == 0) {
			return ius;
		}

		IInstallableUnit[] allIus = new IInstallableUnit[ius.length + sourceIus.length];
		System.arraycopy(ius, 0, allIus, 0, ius.length);
		System.arraycopy(sourceIus, 0, allIus, ius.length, sourceIus.length);
		return allIus;
	}

	protected IArtifactDescriptor[] generateArtifactDescriptors(File[] files, IProgressMonitor monitor) {
		// TODO This could be optimized by combining with generateMetadata

		SubMonitor subMon = SubMonitor.convert(monitor, 50);

		List descriptors = new ArrayList();

		SubMonitor loopProgress = subMon.newChild(50).setWorkRemaining(files.length);
		for (int i = 0; i < files.length; i++) {
			if (subMon.isCanceled()) {
				return new IArtifactDescriptor[0];
			}
			try {
				Map manifest = loadManifest(files[i]);
				String header = (String) manifest.get(Constants.BUNDLE_SYMBOLICNAME);
				if (header != null) {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_SYMBOLICNAME, header);
					if (elements != null) {
						String name = elements[0].getValue();
						if (name != null) {
							header = (String) manifest.get(Constants.BUNDLE_VERSION);
							elements = ManifestElement.parseHeader(Constants.BUNDLE_VERSION, header);
							if (elements != null) {
								String version = elements[0].getValue();
								if (version != null) {
									ArtifactKey key = new ArtifactKey(BUNDLE_ARTIFACT_CLASSIFIER, name, Version.parseVersion(version));
									SimpleArtifactDescriptor descriptor = new SimpleArtifactDescriptor(key);
									descriptor.setRepositoryProperty(SimpleArtifactDescriptor.ARTIFACT_REFERENCE, files[i].toURI().toString());
									descriptors.add(descriptor);
								}
							}
						}
					}
				}
			} catch (BundleException e) {
				// ignore invalid bundles
			} catch (CoreException e) {
				// ignore invalid bundles
			}
			loopProgress.worked(1);
		}

		if (subMon.isCanceled()) {
			return new IArtifactDescriptor[0];
		}

		subMon.done();
		return (IArtifactDescriptor[]) descriptors.toArray(new IArtifactDescriptor[descriptors.size()]);
	}

	/**
	 * Parses a bundle's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary
	 * @throws CoreException if manifest has invalid syntax or is missing
	 */
	private Map loadManifest(File bundleLocation) throws CoreException {
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
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			Map map = ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
			// Validate manifest - BSN must be present.
			// Else look for plugin.xml in case it's an old style plug-in
			String bsn = (String) map.get(Constants.BUNDLE_SYMBOLICNAME);
			if (bsn == null && bundleLocation.isDirectory()) {
				map = loadPluginXML(bundleLocation); // not a bundle manifest, try plugin.xml
			}
			if (map == null) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), null));
			}
			return map;
		} catch (BundleException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, 0, NLS.bind(Messages.DirectoryBundleContainer_3, bundleLocation.getAbsolutePath()), e));
		} finally {
			closeZipFileAndStream(manifestStream, jarFile);
		}
	}

	private void closeZipFileAndStream(InputStream stream, ZipFile jarFile) {
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

}
