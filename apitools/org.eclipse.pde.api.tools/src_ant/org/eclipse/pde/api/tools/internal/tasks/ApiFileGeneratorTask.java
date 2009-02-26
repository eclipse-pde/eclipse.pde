/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
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
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.ApiDescription;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.CompilationUnit;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
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
 * @deprecated This task is deprecated and will be removed once PDE/Build and the Eclipse builder will use the new task
 * ApiFileGenerationTask.
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

	boolean debug;

	String projectName;
	String projectLocation;
	String targetFolder;
	Set apiPackages = new HashSet(0);

	/**
	 * Set the project name.
	 * 
	 * @param projectName the given project name
	 */
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	/**
	 * Set the project location.
	 * 
	 * @param projectName the given project location
	 */
	public void setProjectLocation(String projectLocation) {
		this.projectLocation = projectLocation;
	}
	/**
	 * Set the target folder.
	 *
	 * @param targetFolder the given target folder
	 */
	public void setTargetFolder(String targetFolder) {
		this.targetFolder = targetFolder;
	}

	/**
	 * Set the debug value.
	 * <p>The possible values are: <code>true</code>, <code>false</code></p>
	 * <p>Default is <code>false</code>.</p>
	 *
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue); 
	}
	
	/**
	 * Execute the ant task
	 */
	public void execute() throws BuildException {
		if (this.projectName == null
				|| this.projectLocation == null
				|| this.targetFolder == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(
				Messages.bind(Messages.api_generation_printArguments2,
					new String[] {
						this.projectName,
						this.projectLocation,
						this.targetFolder
					})
			);
			writer.flush();
			writer.close();
			throw new BuildException(String.valueOf(out.getBuffer()));
		}
		if (this.debug) {
			System.out.println(this.targetFolder);
			System.out.println(this.projectLocation);
			System.out.println(this.projectName);
		}
		// create the directory class file container
		StringBuffer classFileContainerRootBuffer = new StringBuffer(this.targetFolder);
		classFileContainerRootBuffer.append(File.separatorChar).append(this.projectName);
		DirectoryApiTypeContainer classFileContainer = new DirectoryApiTypeContainer(null,
				String.valueOf(classFileContainerRootBuffer));
		String[] packageNames = null;
		try {
			packageNames = classFileContainer.getPackageNames();
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		if (this.debug) {
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
			if (this.debug) {
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
		File manifestFile = null;
		Map manifestMap = null;
		if (targetProjectFolder.exists() && targetProjectFolder.isDirectory()) {
			File manifestDir = new File(targetProjectFolder, "META-INF"); //$NON-NLS-1$
			if (manifestDir.exists() && manifestDir.isDirectory()) {
				manifestFile = new File(manifestDir, "MANIFEST.MF"); //$NON-NLS-1$
			}
			if (manifestFile != null && manifestFile.exists()) {
				BufferedInputStream inputStream = null;
				try {
					inputStream = new BufferedInputStream(new FileInputStream(manifestFile));
					manifestMap = ManifestElement.parseBundleManifest(inputStream, null);
					this.apiPackages = collectApiPackageNames(manifestMap);
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
		File[] allFiles = Util.getAllFiles(root, new FileFilter() {
			public boolean accept(File path) {
				return (path.isFile() && Util.isJavaFileName(path.getName()) && isApi(path.getParent())) || path.isDirectory();
			}
		});
		ApiDescription apiDescription = new ApiDescription(this.projectName);
		TagScanner tagScanner = TagScanner.newScanner();
		if (allFiles != null) {
			Map options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_COMPLIANCE, resolveCompliance(manifestMap));
			CompilationUnit unit = null;
			for (int i = 0, max = allFiles.length; i < max; i++) {
				unit = new CompilationUnit(allFiles[i].getAbsolutePath());
				if (this.debug) {
					System.out.println("Unit name[" + i + "] : " + unit.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					tagScanner.scan(unit, apiDescription, classFileContainer, options);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		// check the manifest file
		String componentName = this.projectName;
		String componentID = this.projectName;
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
	 * Returns if the given path ends with one of the collected API path names
	 * @param path
	 * @return true if the given path name ends with one of the collected API package names 
	 */
	private boolean isApi(String path) {
		String pkg = null;
		for(Iterator iter = this.apiPackages.iterator(); iter.hasNext();) {
			pkg = (String) iter.next();
			if(path.endsWith(pkg.replace('.', File.separatorChar))) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Collects the names of the packages that are API for the bundle the api description is being created for
	 * @param manifestmap
	 * @return the names of the packages that are API for the bundle the api description is being created for
	 * @throws BundleException if parsing the manifest map to get API package names fail for some reason
	 */
	private Set/*<String>*/ collectApiPackageNames(Map manifestmap) throws BundleException {
		HashSet set = new HashSet();
		ManifestElement[] packages = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, (String) manifestmap.get(Constants.EXPORT_PACKAGE));
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				ManifestElement packageName = packages[i];
				Enumeration directiveKeys = packageName.getDirectiveKeys();
				if(directiveKeys == null) {
					set.add(packageName.getValue());
				} else {
					boolean include = true;
					loop: for (; directiveKeys.hasMoreElements();) {
						Object directive = directiveKeys.nextElement();
						if ("x-internal".equals(directive)) { //$NON-NLS-1$
							String value = packageName.getDirective((String) directive);
							if (Boolean.valueOf(value).booleanValue()) {
								include = false;
								break loop;
							}
						}
						if ("x-friends".equals(directive)) { //$NON-NLS-1$
							include = false;
							break loop;
						}
					}
					if (include) {
						set.add(packageName.getValue());
					}
				}
			}
		}
		return set;
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
