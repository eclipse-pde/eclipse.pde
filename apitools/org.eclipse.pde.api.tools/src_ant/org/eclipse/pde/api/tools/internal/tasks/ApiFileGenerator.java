/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.DirectoryClassFileContainer;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.BundleException;

public class ApiFileGenerator extends Task {
	
	private static final boolean DEBUG = false;

	String projectName;
	String projectLocation;
	String targetFolder;

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public void setProjectLocation(String projectLocation) {
		this.projectLocation = projectLocation;
	}
	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}
	
	public void execute() throws BuildException {
		if (DEBUG) {
			System.out.println(this.targetFolder);
			System.out.println(this.projectLocation);
			System.out.println(this.projectName);
		}
		// create the directory class file container
		StringBuffer classFileContainerRootBuffer = new StringBuffer(this.targetFolder);
		classFileContainerRootBuffer.append(File.separatorChar).append(this.projectName);
		DirectoryClassFileContainer classFileContainer = new DirectoryClassFileContainer(
				String.valueOf(classFileContainerRootBuffer), projectName);
		String[] packageNames = null;
		try {
			packageNames = classFileContainer.getPackageNames();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		if (DEBUG) {
			if (packageNames != null) {
				System.out.println("List all package names"); //$NON-NLS-1$
				for (int i = 0, max = packageNames.length; i < max; i++) {
					System.out.println("Package name : " + packageNames[i]); //$NON-NLS-1$
				}
			} else {
				System.out.println("No packages"); //$NON-NLS-1$
			}
		}

		// collect all compilation units
		File root = new File(this.projectLocation);
		if (!root.exists() || !root.isDirectory()) {
			if (DEBUG) {
				System.err.println("Should be a directory : " + this.projectLocation); //$NON-NLS-1$
			}
			return;
		}
		// check if the .api_description file exists in source
		File apiDescriptionFile = new File(root, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		File targetProjectFolder = new File(this.targetFolder, this.projectName); 
		if (apiDescriptionFile.exists()) {
			// copy to the target folder + project name
			Util.copy(apiDescriptionFile, new File(targetProjectFolder, IApiCoreConstants.API_DESCRIPTION_XML_NAME));
			return;
		}
		File[] allFiles = Util.getAllFiles(root, new FileFilter() {
			public boolean accept(File path) {
				return (path.isFile() && Util.isJavaFileName(path.getName())) || path.isDirectory();
			}
		});
		ApiDescription apiDescription = new ApiDescription(this.projectName);
		TagScanner tagScanner = TagScanner.newScanner();
		if (allFiles != null) {
			for (int i = 0, max = allFiles.length; i < max; i++) {
				CompilationUnit unit = new CompilationUnit(allFiles[i].getAbsolutePath());
				if (DEBUG) {
					System.out.println("Unit name[" + i + "] : " + unit.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					tagScanner.scan(unit, apiDescription, classFileContainer);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		// check the manifest file
		String componentName = this.projectName;
		String componentID = this.projectName;
		if (targetProjectFolder.exists() && targetProjectFolder.isDirectory()) {
			File manifestDir = new File(targetProjectFolder, "META-INF"); //$NON-NLS-1$
			if (manifestDir.exists() && manifestDir.isDirectory()) {
				File manifestFile = new File(manifestDir, "MANIFEST.MF"); //$NON-NLS-1$
				if (manifestFile.exists()) {
					BufferedInputStream inputStream = null;
					Map manifestMap = null;
					try {
						inputStream = new BufferedInputStream(new FileInputStream(manifestFile));
						manifestMap = ManifestElement.parseBundleManifest(inputStream, null);
					} catch (FileNotFoundException e) {
						ApiPlugin.log(e);
					} catch (IOException e) {
						ApiPlugin.log(e);
					} catch (BundleException e) {
						ApiPlugin.log(e);
					} finally {
						if (inputStream != null) {
							try {
								inputStream.close();
							} catch(IOException e) {
								// ignore
							}
						}
					}
					if (manifestMap != null && DEBUG) {
						for (Iterator iterator = manifestMap.keySet().iterator(); iterator.hasNext(); ) {
							Object key = iterator.next();
							System.out.print("key = " + key); //$NON-NLS-1$
							System.out.println(" value = " + manifestMap.get(key)); //$NON-NLS-1$
						}
					}
					String localization = (String) manifestMap.get(org.osgi.framework.Constants.BUNDLE_LOCALIZATION);
					String name = (String) manifestMap.get(org.osgi.framework.Constants.BUNDLE_NAME);
					String nameKey = (name != null && name.startsWith("%")) ? name.substring(1) : null; //$NON-NLS-1$;
					if (nameKey != null) {
						Properties properties = new Properties();
						inputStream = null;
						try {
							inputStream = new BufferedInputStream(new FileInputStream(new File(targetProjectFolder, localization + ".properties"))); //$NON-NLS-1$
							properties.load(inputStream);
						} catch(IOException e) {
							ApiPlugin.log(e);
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch(IOException e) {
									// ignore
								}
							}
						}
						String property = properties.getProperty(nameKey);
						if (property != null) {
							componentName = property.trim();
						}
					} else {
						componentName = name;
					}
					String symbolicName = (String) manifestMap.get(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME);
					if (symbolicName != null) {
						int indexOf = symbolicName.indexOf(';');
						if (indexOf == -1) {
							componentID = symbolicName.trim();
						} else {
							componentID = symbolicName.substring(0, indexOf).trim();
						}
					}
				}
			}
		}
		try {
			ApiSettingsXmlVisitor xmlVisitor = new ApiSettingsXmlVisitor(componentName, componentID);
			apiDescription.accept(xmlVisitor);
			String xml = xmlVisitor.getXML();
			Util.saveFile(apiDescriptionFile, xml);
			Util.copy(apiDescriptionFile, new File(targetProjectFolder, IApiCoreConstants.API_DESCRIPTION_XML_NAME));
		} catch (CoreException e) {
			ApiPlugin.log(e);
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
}
