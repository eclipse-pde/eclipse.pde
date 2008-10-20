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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.DirectoryClassFileContainer;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Ant task to generate the .api_description file during the Eclipse build.
 */
public class ApiFileGeneratorTask extends Task {

	static class APIToolsNatureDefaultHandler extends DefaultHandler {
		private static final String NATURE_ELEMENT_NAME = "nature"; //$NON-NLS-1$
		boolean isAPIToolsNature = false;
		boolean insideNature = false;
		StringBuffer buffer;
		public void error(SAXParseException e) throws SAXException {
			e.printStackTrace();
		}
		public void startElement(String uri, String localName, String name, Attributes attributes)
				throws SAXException {
			if (this.isAPIToolsNature) return;
			this.insideNature = NATURE_ELEMENT_NAME.equals(name);
			if (this.insideNature) {
				this.buffer = new StringBuffer();
			}
		}
		public void characters(char[] ch, int start, int length)
				throws SAXException {
			if (this.insideNature) {
				this.buffer.append(ch, start, length);
			}
		}
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			if (this.insideNature) {
				// check the contents of the characters
				String natureName = String.valueOf(this.buffer).trim();
				this.isAPIToolsNature = ApiPlugin.NATURE_ID.equals(natureName);
			}
			this.insideNature = false;
		}
		public boolean isAPIToolsNature() {
			return this.isAPIToolsNature;
		}
	}

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
		// check if the project contains the api tools nature
		File dotProjectFile = new File(root, ".project"); //$NON-NLS-1$
		
		if (!isAPIToolsNature(dotProjectFile)) {
			return;
		}
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
		File manifestFile = null;
		Map manifestMap = null;
		if (targetProjectFolder.exists() && targetProjectFolder.isDirectory()) {
			File manifestDir = new File(targetProjectFolder, "META-INF"); //$NON-NLS-1$
			if (manifestDir.exists() && manifestDir.isDirectory()) {
				manifestFile = new File(manifestDir, "MANIFEST.MF"); //$NON-NLS-1$
			}
			if (manifestFile.exists()) {
				BufferedInputStream inputStream = null;
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
			}
		}
		ApiDescription apiDescription = new ApiDescription(this.projectName);
		TagScanner tagScanner = TagScanner.newScanner();
		if (allFiles != null) {
			for (int i = 0, max = allFiles.length; i < max; i++) {
				CompilationUnit unit = new CompilationUnit(allFiles[i].getAbsolutePath());
				if (DEBUG) {
					System.out.println("Unit name[" + i + "] : " + unit.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					//default to highest, since we have no EE context
					//TODO should use EE context to resolve a compliance level
					Map options = JavaCore.getOptions();
					options.put(JavaCore.COMPILER_COMPLIANCE, resolveCompliance(manifestMap));
					tagScanner.scan(unit, apiDescription, classFileContainer, options);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		// check the manifest file
		String componentName = this.projectName;
		String componentID = this.projectName;
		if (manifestMap != null && DEBUG) {
			for (Iterator iterator = manifestMap.keySet().iterator(); iterator.hasNext(); ) {
				Object key = iterator.next();
				System.out.print("key = " + key); //$NON-NLS-1$
				System.out.println(" value = " + manifestMap.get(key)); //$NON-NLS-1$
			}
			String localization = (String) manifestMap.get(org.osgi.framework.Constants.BUNDLE_LOCALIZATION);
			String name = (String) manifestMap.get(org.osgi.framework.Constants.BUNDLE_NAME);
			String nameKey = (name != null && name.startsWith("%")) ? name.substring(1) : null; //$NON-NLS-1$;
			if (nameKey != null) {
				Properties properties = new Properties();
				BufferedInputStream inputStream = null;
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

	/**
	 * Resolves the compiler compliance based on the BREE entry in the MANIFEST.MF file
	 * @param manifestmap
	 * @return The derived {@link JavaCore#COMPILER_COMPLIANCE} from the BREE in the manifest map, 
	 * or {@link JavaCore#VERSION_1_3} if there is no BREE entry in the map or if the BREE entry does not directly map 
	 * to one of {"1.3", "1.4", "1.5", "1.6"}.
	 */
	private String resolveCompliance(Map manifestmap) {
		if(manifestmap != null) {
			String eename = (String) manifestmap.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			if(eename != null) {
				if("J2SE-1.4".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_4;
				}
				if("J2SE-1.5".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_5;
				}
				if("JavaSE-1.6".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_6;
				}
			}
		}
		return JavaCore.VERSION_1_3;
	}
	
	/**
	 * Resolves if the '.project' file belongs to an API enabled project or not
	 * @param dotProjectFile
	 * @return true if the '.project' file is for an API enabled project, false otherwise
	 */
	private boolean isAPIToolsNature(File dotProjectFile) {
		if (!dotProjectFile.exists()) return false;
		BufferedInputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(dotProjectFile));
			String contents = new String(Util.getInputStreamAsCharArray(stream, -1, "UTF-8")); //$NON-NLS-1$
			return containsAPIToolsNature(contents);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return false;
	}
	/**
	 * Check if the given source contains an source extension point.
	 * 
	 * @param pluginXMLContents the given file contents
	 * @return true if it contains a source extension point, false otherwise
	 */
	private boolean containsAPIToolsNature(String pluginXMLContents) {
		SAXParserFactory factory = null;
		try {
			factory = SAXParserFactory.newInstance();
		} catch (FactoryConfigurationError e) {
			return false;
		}
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}

		if (saxParser == null) {
			return false;
		}

		// Parse
		InputSource inputSource = new InputSource(new BufferedReader(new StringReader(pluginXMLContents)));
		try {
			APIToolsNatureDefaultHandler defaultHandler = new APIToolsNatureDefaultHandler();
			saxParser.parse(inputSource, defaultHandler);
			return defaultHandler.isAPIToolsNature();
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
		return false;
	}
}
