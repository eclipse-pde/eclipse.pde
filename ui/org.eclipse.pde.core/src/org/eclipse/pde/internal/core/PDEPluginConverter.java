/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.service.pluginconversion.PluginConversionException;
import org.eclipse.osgi.service.pluginconversion.PluginConverter;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.ServiceTracker;

public class PDEPluginConverter {
	
	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().toOSString());
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			converter.convertManifest(inputFile, outputFile, false, target, true, dictionary);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		}  finally {
			monitor.done();
		}
	}
	
	public static void createBundleForFramework(IProject project, Set packages, IProgressMonitor monitor) throws CoreException {
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().toOSString());
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			double version = TargetPlatform.getTargetVersion();
			String versionString =  version <= 3.1 ? ICoreConstants.TARGET31 : TargetPlatform.getTargetVersionString();
			converter.convertManifest(inputFile, outputFile, false, versionString, true, null);
			
			InputStream manifestStream = new FileInputStream(outputFile);
			Manifest manifest = new Manifest(manifestStream);
			Properties prop = manifestToProperties(manifest.getMainAttributes());
			prop.remove(ICoreConstants.ECLIPSE_AUTOSTART); 
			prop.remove(ICoreConstants.ECLIPSE_LAZYSTART);
			StringBuffer buffer = new StringBuffer();
			Iterator iter = packages.iterator();
			while (iter.hasNext()) {
				if (buffer.length() > 0) {
					buffer.append(","); //$NON-NLS-1$
					buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
					buffer.append(" "); //$NON-NLS-1$
				}
				buffer.append(iter.next().toString());
			}
			if (buffer.length() > 0)
				prop.put(Constants.IMPORT_PACKAGE, buffer.toString());
			converter.writeManifest(outputFile, prop, false);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			monitor.done();
		}
	}
	
	public static void modifyBundleClasspathHeader(IProject project, IPluginModelBase model) {
		IFile file = project.getFile(JarFile.MANIFEST_NAME);
		if (file.exists()) {
			InputStream manifestStream = null;
			try {
				manifestStream = new FileInputStream(file.getLocation().toFile());
				Manifest manifest = new Manifest(manifestStream);
				Properties prop = manifestToProperties(manifest.getMainAttributes());
				String classpath = prop.getProperty(Constants.BUNDLE_CLASSPATH);
				if (classpath == null) {
					prop.put(Constants.BUNDLE_CLASSPATH, 
							ClasspathUtilCore.getFilename(model));
				} else {
					ManifestElement[] elements = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, classpath);
					StringBuffer buffer = new StringBuffer();
					for (int i = 0; i < elements.length; i++) {
						if (buffer.length() > 0) {
							buffer.append(","); //$NON-NLS-1$
							buffer.append(System.getProperty("line.separator")); //$NON-NLS-1$
							buffer.append(" "); //$NON-NLS-1$
						}
						if (elements[i].getValue().equals(".")) //$NON-NLS-1$
							buffer.append(ClasspathUtilCore.getFilename(model));
						else
							buffer.append(elements[i].getValue());
					}
					prop.put(Constants.BUNDLE_CLASSPATH, buffer.toString());
				}
				ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
						.getBundleContext(), PluginConverter.class.getName(), null);
				tracker.open();
				PluginConverter converter = (PluginConverter) tracker.getService();
				converter.writeManifest(new File(file.getLocation().toOSString()), prop, false);
				file.refreshLocal(1, null);
				tracker.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			} catch (BundleException e) {
			} catch (PluginConversionException e) {
			} catch (CoreException e) {
			} finally {
				try {
					if (manifestStream != null)
						manifestStream.close();
				} catch (IOException e) {
				}
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
	
}
