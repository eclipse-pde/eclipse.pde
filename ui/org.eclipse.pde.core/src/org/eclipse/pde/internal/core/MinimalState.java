/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateDelta;
import org.eclipse.osgi.service.resolver.StateHelper;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

public class MinimalState {

	protected State fState;
		
	private String fTargetMode = null;

	protected long fId;

	private PluginConverter fConverter = null;

	private boolean fJavaProfileChanged = false; // indicates that the java
													// profile has changed

	private String fJavaProfile; // the currently selected java profile

	private String[] fJavaProfiles; // the list of available java profiles

	private static final String SYSTEM_BUNDLE = "org.eclipse.osgi"; //$NON-NLS-1$

	protected static boolean DEBUG = false;

	protected static StateObjectFactory stateObjectFactory;

	protected static String DIR;

	static {
		DEBUG = PDECore.getDefault().isDebugging()
				&& "true".equals(Platform.getDebugOption("org.eclipse.pde.core/cache")); //$NON-NLS-1$ //$NON-NLS-2$
		DIR = PDECore.getDefault().getStateLocation().toOSString();
		stateObjectFactory = Platform.getPlatformAdmin().getFactory();
	}
	
	public MinimalState() {
		fState = stateObjectFactory.createState();
		fState.setResolver(Platform.getPlatformAdmin().getResolver());
        fState.setPlatformProperties(TargetPlatform.getTargetEnvironment());		
	}
	
	public void addBundle(IPluginModelBase model, boolean update) {
		if (!update) {
			BundleDescription[] bundles = fState.getBundles(model.getPluginBase().getId());
			IPath path = new Path(model.getInstallLocation());
			for (int i = 0; i < bundles.length; i++) {
				if (ExternalModelManager.arePathsEqual(path, new Path(bundles[i].getLocation()))) {
					model.setBundleDescription(bundles[i]);
					return;
				}
			}
		}
		
		BundleDescription desc = model.getBundleDescription();
		long bundleId = desc == null || !update ? -1 : desc.getBundleId();
		try {
			model.setBundleDescription(
					addBundle(new File(model.getInstallLocation()), false,  bundleId));
		} catch (IOException e) {			
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
			PDECore.log(e);
		} 
	}
	
	public BundleDescription addBundle(IPluginModelBase model, long bundleId) {
		try {
			return addBundle(new File(model.getInstallLocation()), false, -1);
		} catch (IOException e) {			
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		}
		return null;
	}

	public BundleDescription addBundle(Dictionary manifest, File bundleLocation, boolean keepLibraries, long bundleId) {
		try {
			BundleDescription descriptor = stateObjectFactory.createBundleDescription(
					fState, manifest, bundleLocation.getAbsolutePath(),
					bundleId == -1 ? getNextId() : bundleId);
			// new bundle
			if (bundleId == -1) {
				fState.addBundle(descriptor);
			} else if (!fState.updateBundle(descriptor)) {
				fState.addBundle(descriptor);
			}
			return descriptor;
		} catch (BundleException e) {
		} catch (NumberFormatException e) {
		}
		return null;
	}

	public BundleDescription addBundle(File bundleLocation, boolean keepLibraries, long bundleId) throws PluginConversionException, CoreException, IOException {
		Dictionary manifest = loadManifest(bundleLocation);
		if (manifest == null || manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null) {
			if (!bundleLocation.isFile() 
					&& !new File(bundleLocation, "plugin.xml").exists() //$NON-NLS-1$
					&& !new File(bundleLocation, "fragment.xml").exists()) //$NON-NLS-1$
				return null;
			PluginConverter converter = acquirePluginConverter();
			manifest = converter.convertManifest(bundleLocation, false, getTargetMode(), false, null);
			if (manifest == null
					|| manifest.get(Constants.BUNDLE_SYMBOLICNAME) == null)
				throw new CoreException(new Status(
						IStatus.ERROR,
						PDECore.PLUGIN_ID,
						IStatus.ERROR,
						"Error parsing plug-in manifest file at " + bundleLocation.toString(), null)); //$NON-NLS-1$
		}
		BundleDescription desc = addBundle(manifest, bundleLocation, keepLibraries, bundleId);
		if (desc != null && SYSTEM_BUNDLE.equals(desc.getSymbolicName())) {
			// if this is the system bundle then reset the java profile and
			// indicate that the javaProfile has changed
			setJavaProfiles(bundleLocation);
		}
		return desc;
	}

	protected void saveState(File dir) {
		saveState(fState, dir);
	}
	
	protected void saveState(State state, File dir) {
		try {
			if (!dir.exists())
				dir.mkdirs();
			stateObjectFactory.writeState(state, dir);
		} catch (FileNotFoundException e) {
			PDECore.log(e);
		} catch (IOException e) {
			PDECore.log(e);
		} finally {
		}
	}

	public static Dictionary loadManifest(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
		} catch (IOException e) {
		}
		if (manifestStream == null)
			return null;
		try {
			Manifest m = new Manifest(manifestStream);
			return manifestToProperties(m.getMainAttributes());
		} finally {
			try {
				manifestStream.close();
			} catch (IOException e1) {
			}
			try {
				if (jarFile != null)
					jarFile.close();
			} catch (IOException e2) {
			}
		}
	}

	private static Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}

	public StateDelta resolveState(boolean incremental) {
		return internalResolveState(incremental);
	}

	private synchronized StateDelta internalResolveState(boolean incremental) {
		if (fJavaProfile == null) {
			fJavaProfile = getDefaultJavaProfile();
			fJavaProfileChanged = true;
		}
		if (fJavaProfileChanged) {
			incremental = !fState.setPlatformProperties(getProfilePlatformProperties());
			fJavaProfileChanged = false;
		}
		return fState.resolve(incremental);
	}

	private Dictionary getProfilePlatformProperties() {
		// get the target platform properties
		Dictionary props = TargetPlatform.getTargetEnvironment();
		// add the selected java profile
		Properties profileProps = getJavaProfileProperties();
		if (profileProps != null) {
			String systemPackages = profileProps.getProperty(Constants.FRAMEWORK_SYSTEMPACKAGES);
			if (systemPackages != null)
				props.put(Constants.FRAMEWORK_SYSTEMPACKAGES, systemPackages);
			String ee = profileProps.getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
			if (ee != null)
				props.put(Constants.FRAMEWORK_EXECUTIONENVIRONMENT, ee);
		}
		return props;
	}

	private File getOSGiLocation() {
		// return the File location of the system bundle
		BundleDescription osgiBundle = fState.getBundle(SYSTEM_BUNDLE, null);
		return (osgiBundle == null) ? null : new File(osgiBundle.getLocation());
	}

	private Properties getJavaProfileProperties() {
		// returns the list of packages in the selected java profile
		if (fJavaProfile == null)
			return null;
		File location = getOSGiLocation();
		if (location == null)
			return null;
		InputStream is = null;
		ZipFile zipFile = null;
		try {
			// find the input stream to the profile properties file
			if (location.isDirectory()) {
				is = new FileInputStream(new File(location, fJavaProfile));
			} else {
				zipFile = null;
				try {
					zipFile = new ZipFile(location, ZipFile.OPEN_READ);
					ZipEntry entry = zipFile.getEntry(fJavaProfile);
					if (entry != null)
						is = zipFile.getInputStream(entry);
				} catch (IOException e) {
					// nothing to do
				}
			}
			Properties profile = new Properties();
			profile.load(is);
			return profile;
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return null;
	}

	public String getDefaultJavaProfile() {
		// if the java profiles list is not set then find the list
		if (fJavaProfiles == null)
			setJavaProfiles(getOSGiLocation());
		// the javaProfiles list is sorted in descending order; return the first
		// profile in the list (highest available profile)
		if (fJavaProfiles != null && fJavaProfiles.length > 0)
			return fJavaProfiles[0];
		return null;
	}

	public void removeBundleDescription(BundleDescription description) {
		fState.removeBundle(description);
	}

	public State getState() {
		return fState;
	}

	protected void setTargetMode(URL[] urls) {
		fTargetMode = ICoreConstants.TARGET21;
		for (int i = 0; i < urls.length; i++) {
			if (urls[i].getFile().indexOf("org.eclipse.osgi") != -1) {//$NON-NLS-1$
				fTargetMode = null;
				break;
			}
		}
	}

	public String getTargetMode() {
		return fTargetMode;
	}
	
	public void setTargetMode(String mode) {
		fTargetMode = mode;
	}

	private void setJavaProfiles(File bundleLocation) {
		if (bundleLocation == null)
			return;
		if (bundleLocation.isDirectory())
			fJavaProfiles = getDirJavaProfiles(bundleLocation);
		else
			fJavaProfiles = getJarJavaProfiles(bundleLocation);
		if (fJavaProfiles != null)
			// sort the javaProfiles in descending order
			Arrays.sort(fJavaProfiles, new Comparator() {
				public int compare(Object profile1, Object profile2) {
					return -((String) profile1).compareTo(profile2);
				}
			});
		// if the selected java profile is set; make sure it is still available
		if (fJavaProfile != null) {
			if (fJavaProfiles == null)
				fJavaProfile = null;
			else if (Arrays.binarySearch(fJavaProfiles, fJavaProfile) < 0)
				fJavaProfile = null;
		}
		fJavaProfileChanged = true; // alway indicate the selected java profile
									// has changed
	}

	private String[] getDirJavaProfiles(File bundleLocation) {
		String[] profiles = bundleLocation.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.endsWith(".profile"); //$NON-NLS-1$
			}
		});
		return profiles;
	}

	private String[] getJarJavaProfiles(File bundleLocation) {
		ZipFile zipFile = null;
		ArrayList results = new ArrayList(6);
		try {
			zipFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
			Enumeration entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				String entryName = ((ZipEntry) entries.nextElement()).getName();
				if (entryName.indexOf('/') < 0 && entryName.endsWith(".profile")) //$NON-NLS-1$
					results.add(entryName);
			}
		} catch (IOException e) {
			// nothing to do
		} finally {
			if (zipFile != null)
				try {
					zipFile.close();
				} catch (IOException e) {
					// nothing to do
				}
		}
		return (String[]) results.toArray(new String[results.size()]);
	}

	public void addBundleDescription(BundleDescription toAdd) {
		fState.addBundle(toAdd);
	}

	private PluginConverter acquirePluginConverter() {
		if (fConverter == null) {
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			fConverter = (PluginConverter) tracker.getService();
			tracker.close();
		}
		return fConverter;
	}

	public long getNextId() {
		return ++fId;
	}

	private BundleDescription findActiveBundle(String symbolicName) {
		BundleDescription[] bundles = fState.getBundles(symbolicName);
		for (int i = 0; i < bundles.length; i++) {
			if (bundles[i].isResolved())
				return bundles[i];
		}
		return null;
	}

	protected void logResolutionErrors() {
		MultiStatus errors = new MultiStatus(PDECore.getPluginId(), 1,
				PDECoreMessages.ExternalModelManager_scanningProblems, 
				null);

		StateHelper helper = Platform.getPlatformAdmin().getStateHelper();
		BundleDescription[] all = fState.getBundles();
		for (int i = 0; i < all.length; i++) {
			if (!all[i].isResolved()) {
				VersionConstraint[] unsatisfiedConstraints = helper
						.getUnsatisfiedConstraints(all[i]);
				if (unsatisfiedConstraints.length == 0) {
					if (DEBUG) {
						BundleDescription activeBundle = findActiveBundle(all[i]
								.getSymbolicName());
						String message = "Plug-in located at \"" + all[i].getLocation() + "\" was disabled because plug-in located at \"" + activeBundle.getLocation() + "\" was selected."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						System.out.print(message);
					}
				} else {
					for (int j = 0; j < unsatisfiedConstraints.length; j++) {
						String message = getResolutionFailureMessage(unsatisfiedConstraints[j]);
						if (message != null)
							errors.add(new Status(IStatus.WARNING, all[i]
									.getSymbolicName(), IStatus.WARNING, message, null));
					}
				}
			}
		}
		if (errors.getChildren().length > 0)
			PDECore.log(errors);
	}

	private String getResolutionFailureMessage(VersionConstraint unsatisfied) {
		if (unsatisfied.isResolved())
			throw new IllegalArgumentException();
		if (unsatisfied instanceof ImportPackageSpecification)
			return "Missing imported package: " + toString(unsatisfied); //$NON-NLS-1$
		if (unsatisfied instanceof BundleSpecification && !((BundleSpecification)unsatisfied).isOptional())
			return "Missing required plug-in: " + toString(unsatisfied); //$NON-NLS-1$
		if (unsatisfied instanceof HostSpecification)
			return "Missing Fragment Host: " + toString(unsatisfied); //$NON-NLS-1$
		return null;
	}

	private String toString(VersionConstraint constraint) {
		VersionRange versionRange = constraint.getVersionRange();
		if (versionRange == null || versionRange.getMinimum() != null)
			return constraint.getName();
		return constraint.getName() + '_' + versionRange;
	}

}
