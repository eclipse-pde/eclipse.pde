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

import java.io.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.service.pluginconversion.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.util.tracker.*;

public class PDEPluginConverter {
	
	public static void convertToOSGIFormat(IProject project, String target, Dictionary dictionary, IProgressMonitor monitor) throws CoreException {
		InputStream manifestStream = null;
		try {
			File outputFile = new File(project.getLocation().append(
					"META-INF/MANIFEST.MF").toOSString()); //$NON-NLS-1$
			File inputFile = new File(project.getLocation().toOSString());
			ServiceTracker tracker = new ServiceTracker(PDECore.getDefault()
					.getBundleContext(), PluginConverter.class.getName(), null);
			tracker.open();
			PluginConverter converter = (PluginConverter) tracker.getService();
			converter.convertManifest(inputFile, outputFile, false, target, true, dictionary);

			String compliance = getComplianceLevel(project, target);
			if (compliance != null) {
				String execution = getExecutionEnvironment(compliance);
				if (execution != null) {
					manifestStream = new FileInputStream(outputFile);
					Manifest manifest = new Manifest(manifestStream);
					Properties prop = manifestToProperties(manifest.getMainAttributes());
					prop.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, execution);	
					// pretty-up the Require-Bundle header for the converter
					String required = prop.getProperty(Constants.REQUIRE_BUNDLE);
					if (required != null)
						prop.put(Constants.REQUIRE_BUNDLE, 
								required.replaceAll(",", "," + System.getProperty("line.separator") + " ")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
					converter.writeManifest(outputFile, prop, false);
				}
			}					
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}  finally {
			try {
				if (manifestStream != null)
					manifestStream.close();
			} catch (IOException e) {
			}
			monitor.done();
		}
	}
	
	public static void createBundleForFramework(IProject project, Set packages, boolean standard, IProgressMonitor monitor) throws CoreException {
		InputStream manifestStream = null;
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
			
			manifestStream = new FileInputStream(outputFile);
			Manifest manifest = new Manifest(manifestStream);
			Properties prop = manifestToProperties(manifest.getMainAttributes());
			if (standard) {
				prop.remove(ICoreConstants.ECLIPSE_AUTOSTART); 
				prop.remove(ICoreConstants.ECLIPSE_LAZYSTART);
			}
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
			String compliance = getComplianceLevel(project, TargetPlatform.getTargetVersionString());
			if (compliance != null) {
				String execution = getExecutionEnvironment(compliance);
				if (execution != null)
					prop.put(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, execution);
			}
			converter.writeManifest(outputFile, prop, false);
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
			tracker.close();
		} catch (PluginConversionException e) {
		} catch (CoreException e) {
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				if (manifestStream != null)
					manifestStream.close();
			} catch (IOException e) {
			}
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
							ClasspathComputer.getFilename(model));
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
							buffer.append(ClasspathComputer.getFilename(model));
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
	
	public static String getComplianceLevel(IProject project, String target) throws CoreException {
		double version = Double.parseDouble(target);
		if (version >= 3.2 && project.hasNature(JavaCore.NATURE_ID)) {
			IJavaProject jProject = JavaCore.create(project);
			return jProject.getOption(JavaCore.COMPILER_COMPLIANCE, true);
		}
		return null;
	}
	
	public static String getExecutionEnvironment(String compliance) {
		if (JavaCore.VERSION_1_5.equals(compliance))
			return "J2SE-5"; //$NON-NLS-1$
		if (JavaCore.VERSION_1_4.equals(compliance))
			return "J2SE-4"; //$NON-NLS-1$
		return JavaCore.VERSION_1_3.equals(compliance) ? "J2SE-3" : null; //$NON-NLS-1$
	}

}
