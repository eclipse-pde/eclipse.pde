/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.ArchiveApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.CompositeApiTypeContainer;
import org.eclipse.pde.api.tools.internal.model.DirectoryApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeContainer;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class APIFileGenerator {

	static class APIToolsNatureDefaultHandler extends DefaultHandler {
		private static final String NATURE_ELEMENT_NAME = "nature"; //$NON-NLS-1$
		boolean isAPIToolsNature = false;
		boolean insideNature = false;
		StringBuilder buffer;

		@Override
		public void error(SAXParseException e) throws SAXException {
			e.printStackTrace();
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (this.isAPIToolsNature) {
				return;
			}
			this.insideNature = NATURE_ELEMENT_NAME.equals(name);
			if (this.insideNature) {
				this.buffer = new StringBuilder();
			}
		}

		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if (this.insideNature) {
				this.buffer.append(ch, start, length);
			}
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
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

	public boolean debug;
	public String projectName;
	public String projectLocation;
	public String targetFolder;
	public String binaryLocations;
	public Set<String> apiPackages = new HashSet<>(0);
	public String manifests;
	public String sourceLocations;
	public boolean allowNonApiProject = false;
	public String encoding;

	private static boolean isZipJarFile(String fileName) {
		String normalizedFileName = fileName.toLowerCase();
		return normalizedFileName.endsWith(".zip") //$NON-NLS-1$
				|| normalizedFileName.endsWith(".jar"); //$NON-NLS-1$
	}

	public void generateAPIFile() {
		if (this.binaryLocations == null || this.projectName == null || this.projectLocation == null || this.targetFolder == null) {
			StringWriter out = new StringWriter();
			PrintWriter writer = new PrintWriter(out);
			writer.println(NLS.bind(CoreMessages.api_generation_printArguments,
					new String[] {
					this.projectName, this.projectLocation,
					this.binaryLocations, this.targetFolder }));
			throw new IllegalArgumentException(String.valueOf(out.getBuffer()));
		}
		if (this.debug) {
			System.out.println("Project name : " + this.projectName); //$NON-NLS-1$
			System.out.println("Encoding: " + this.encoding); //$NON-NLS-1$
			System.out.println("Project location : " + this.projectLocation); //$NON-NLS-1$
			System.out.println("Binary locations : " + this.binaryLocations); //$NON-NLS-1$
			System.out.println("Target folder : " + this.targetFolder); //$NON-NLS-1$
			if (this.manifests != null) {
				System.out.println("Extra manifest entries : " + this.manifests); //$NON-NLS-1$
			}
			if (this.sourceLocations != null) {
				System.out.println("Extra source locations entries : " + this.sourceLocations); //$NON-NLS-1$
			}
		}
		// collect all compilation units
		File root = new File(this.projectLocation);
		if (!root.exists() || !root.isDirectory()) {
			if (this.debug) {
				System.err.println("Must be a directory : " + this.projectLocation); //$NON-NLS-1$
			}
			throw new IllegalArgumentException(
					NLS.bind(CoreMessages.api_generation_projectLocationNotADirectory, this.projectLocation));
		}
		// check if the project contains the API tools nature
		File dotProjectFile = new File(root, ".project"); //$NON-NLS-1$

		if (!this.allowNonApiProject && !isAPIToolsNature(dotProjectFile)) {
			System.err.println("The project does not have an API Tools nature so a api_description file will not be generated"); //$NON-NLS-1$
			return;
		}
		// check if the .api_description file exists
		File targetProjectFolder = new File(this.targetFolder);
		if (!targetProjectFolder.exists()) {
			targetProjectFolder.mkdirs();
		} else if (!targetProjectFolder.isDirectory()) {
			if (this.debug) {
				System.err.println("Must be a directory : " + this.targetFolder); //$NON-NLS-1$
			}
			throw new IllegalArgumentException(
					NLS.bind(CoreMessages.api_generation_targetFolderNotADirectory, this.targetFolder));
		}
		File apiDescriptionFile = new File(targetProjectFolder, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		if (apiDescriptionFile.exists()) {
			// get rid of the existing one
			// see https://bugs.eclipse.org/bugs/show_bug.cgi?id=414053
			if (this.debug) {
				System.out.println("Existing api description file deleted"); //$NON-NLS-1$
			}
			apiDescriptionFile.delete();
		}
		File[] allFiles = null;
		Map<String, String> manifestMap = null;
		IApiTypeContainer classFileContainer = null;
		if (!this.projectLocation.endsWith(Util.ORG_ECLIPSE_SWT)) {
			// create the directory class file container used to resolve
			// signatures during tag scanning
			String[] allBinaryLocations = this.binaryLocations.split(File.pathSeparator);
			List<IApiTypeContainer> allContainers = new ArrayList<>();
			IApiTypeContainer container = null;
			for (String allBinaryLocation : allBinaryLocations) {
				container = getContainer(allBinaryLocation);
				if (container == null) {
					throw new IllegalArgumentException(
							NLS.bind(CoreMessages.api_generation_invalidBinaryLocation, allBinaryLocation));
				}
				allContainers.add(container);
			}
			classFileContainer = new CompositeApiTypeContainer(null, allContainers);
			File manifestFile = null;
			File manifestDir = new File(root, "META-INF"); //$NON-NLS-1$
			if (manifestDir.exists() && manifestDir.isDirectory()) {
				manifestFile = new File(manifestDir, "MANIFEST.MF"); //$NON-NLS-1$
			}
			if (manifestFile != null && manifestFile.exists()) {
				try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(manifestFile));) {
					manifestMap = ManifestElement.parseBundleManifest(inputStream, null);
					this.apiPackages = collectApiPackageNames(manifestMap);
				} catch (IOException | BundleException e) {
					ApiPlugin.log(e);
				}
			}
			if (this.manifests != null) {
				String[] allManifestFiles = this.manifests.split(File.pathSeparator);
				for (String allManifestFile : allManifestFiles) {
					File currentManifest = new File(allManifestFile);
					Set<String> currentApiPackages = null;
					if (currentManifest.exists()) {
						try {
							if (isZipJarFile(currentManifest.getName())) {
								try (ZipFile zipFile = new ZipFile(currentManifest)) {
									final ZipEntry entry = zipFile.getEntry("META-INF/MANIFEST.MF"); //$NON-NLS-1$
									if (entry != null) {
										InputStream inputStream = zipFile.getInputStream(entry);
										manifestMap = ManifestElement.parseBundleManifest(inputStream, null);
										currentApiPackages = collectApiPackageNames(manifestMap);

									}
								}
							} else {
								try (InputStream inputStream = new FileInputStream(currentManifest)) {
									manifestMap = ManifestElement.parseBundleManifest(inputStream, null);
									currentApiPackages = collectApiPackageNames(manifestMap);
								}
							}
						} catch (IOException | BundleException e) {
							ApiPlugin.log(e);
						}
					}
					if (currentApiPackages != null) {
						if (this.apiPackages == null) {
							this.apiPackages = currentApiPackages;
						} else {
							this.apiPackages.addAll(currentApiPackages);
						}
					}
				}
			}
			FileFilter fileFilter = path -> (path.isFile() && Util.isJavaFileName(path.getName()) && isApi(path.getParent())) || path.isDirectory();
			allFiles = Util.getAllFiles(root, fileFilter);
			if (this.sourceLocations != null) {
				String[] allSourceLocations = this.sourceLocations.split(File.pathSeparator);
				for (String currentSourceLocation : allSourceLocations) {
					File[] allFiles2 = Util.getAllFiles(new File(currentSourceLocation), fileFilter);
					if (allFiles2 != null) {
						if (allFiles == null) {
							allFiles = allFiles2;
						} else {
							int length = allFiles.length;
							int length2 = allFiles2.length;
							System.arraycopy(allFiles, 0, (allFiles = new File[length + length2]), 0, length);
							System.arraycopy(allFiles2, 0, allFiles, length, length2);
						}
					}
				}
			}
		}
		ApiDescription apiDescription = new ApiDescription(this.projectName);
		TagScanner tagScanner = TagScanner.newScanner();
		if (allFiles != null && allFiles.length != 0) {
			Map<String, String> options = JavaCore.getOptions();
			options.put(JavaCore.COMPILER_COMPLIANCE, resolveCompliance(manifestMap));
			CompilationUnit unit = null;
			for (int i = 0, max = allFiles.length; i < max; i++) {
				unit = new CompilationUnit(allFiles[i].getAbsolutePath(), this.encoding);
				if (this.debug) {
					System.out.println("Unit name[" + i + "] : " + unit.getName()); //$NON-NLS-1$ //$NON-NLS-2$
				}
				try {
					tagScanner.scan(unit, apiDescription, classFileContainer, options, null);
				} catch (CoreException e) {
					ApiPlugin.log(e);
				} finally {
					try {
						if (classFileContainer != null) {
							classFileContainer.close();
						}
					} catch (CoreException e) {
						// ignore
					}
				}
			}
		}
		try {
			ApiDescriptionXmlCreator xmlVisitor = new ApiDescriptionXmlCreator(this.projectName, this.projectName);
			apiDescription.accept(xmlVisitor, null);
			String xml = xmlVisitor.getXML();
			Util.saveFile(apiDescriptionFile, xml);
		} catch (CoreException | IOException e) {
			ApiPlugin.log(e);
		}
	}

	/**
	 * Returns if the given path ends with one of the collected API path names
	 *
	 * @return true if the given path name ends with one of the collected API
	 *         package names
	 */
	boolean isApi(String path) {
		for (String pkg : apiPackages) {
			if (path.endsWith(pkg.replace('.', File.separatorChar))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Collects the names of the packages that are API for the bundle the API
	 * description is being created for
	 *
	 * @return the names of the packages that are API for the bundle the API
	 *         description is being created for
	 * @throws BundleException if parsing the manifest map to get API package
	 *             names fail for some reason
	 */
	private Set<String> collectApiPackageNames(Map<String, String> manifestmap) throws BundleException {
		HashSet<String> set = new HashSet<>();
		ManifestElement[] packages = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE, manifestmap.get(Constants.EXPORT_PACKAGE));
		if (packages != null) {
			for (int i = 0; i < packages.length; i++) {
				ManifestElement packageName = packages[i];
				Enumeration<String> directiveKeys = packageName.getDirectiveKeys();
				if (directiveKeys == null) {
					set.add(packageName.getValue());
				} else {
					boolean include = true;
					loop: for (; directiveKeys.hasMoreElements();) {
						Object directive = directiveKeys.nextElement();
						if ("x-internal".equals(directive)) { //$NON-NLS-1$
							String value = packageName.getDirective((String) directive);
							if (Boolean.parseBoolean(value)) {
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

	private IApiTypeContainer getContainer(String location) {
		File f = new File(location);
		if (!f.exists()) {
			return null;
		}
		if (isZipJarFile(location)) {
			return new ArchiveApiTypeContainer(null, location);
		} else {
			return new DirectoryApiTypeContainer(null, location);
		}
	}

	/**
	 * Resolves the compiler compliance based on the BREE entry in the
	 * MANIFEST.MF file
	 *
	 * @return The derived {@link JavaCore#COMPILER_COMPLIANCE} from the BREE in
	 *         the manifest map, or {@link JavaCore#VERSION_1_3} if there is no
	 *         BREE entry in the map or if the BREE entry does not directly map
	 *         to one of {"1.3", "1.4", "1.5", "1.6", "1.7","1.8"}.
	 */
	private String resolveCompliance(Map<String, String> manifestmap) {
		if (manifestmap != null) {
			String eename = manifestmap.get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
			if (eename != null) {
				if ("J2SE-1.4".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_4;
				}
				if ("J2SE-1.5".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_5;
				}
				if ("JavaSE-1.6".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_6;
				}
				if ("JavaSE-1.7".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_7;
				}
				if ("JavaSE-1.8".equals(eename)) { //$NON-NLS-1$
					return JavaCore.VERSION_1_8;
				}
			}
		}
		return JavaCore.VERSION_1_3;
	}

	/**
	 * Resolves if the '.project' file belongs to an API enabled project or not
	 *
	 * @return true if the '.project' file is for an API enabled project, false
	 *         otherwise
	 */
	private boolean isAPIToolsNature(File dotProjectFile) {
		if (!dotProjectFile.exists()) {
			return false;
		}
		try {
			String contents = Files.readString(dotProjectFile.toPath());
			return containsAPIToolsNature(contents);
		} catch (IOException e) {
			e.printStackTrace();
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
		try {
			@SuppressWarnings("restriction")
			SAXParser saxParser = org.eclipse.core.internal.runtime.XmlProcessorFactory
					.createSAXParserIgnoringDOCTYPE();
			APIToolsNatureDefaultHandler defaultHandler = new APIToolsNatureDefaultHandler();
			saxParser.parse(new InputSource(new StringReader(pluginXMLContents)), defaultHandler);
			return defaultHandler.isAPIToolsNature();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			return false;
		}
	}

}