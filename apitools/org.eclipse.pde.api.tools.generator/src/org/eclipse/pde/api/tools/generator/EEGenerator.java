/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.generator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.util.IAttributeNamesConstants;
import org.eclipse.jdt.core.util.IClassFileAttribute;
import org.eclipse.jdt.core.util.IClassFileReader;
import org.eclipse.jdt.core.util.IFieldInfo;
import org.eclipse.jdt.core.util.IInnerClassesAttribute;
import org.eclipse.jdt.core.util.IInnerClassesAttributeEntry;
import org.eclipse.jdt.core.util.IMethodInfo;
import org.eclipse.jdt.core.util.ISignatureAttribute;
import org.eclipse.pde.api.tools.generator.util.Util;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is a java application to generate the EE descriptions for specified EE
 * names:
 * <p>
 * Accepted names are:
 * </p>
 * <ol>
 * <li>JRE-1.1,</li>
 * <li>J2SE-1.2,</li>
 * <li>J2SE-1.3,</li>
 * <li>J2SE-1.4,</li>
 * <li>J2SE-1.5,</li>
 * <li>JavaSE-1.6,</li>
 * <li>JavaSE-1.7,</li>
 * <li>JavaSE-1.8,</li>
 * <li>CDC-1.0_Foundation-1.0,</li>
 * <li>CDC-1.1_Foundation-1.1,</li>
 * <li>OSGi_Minimum-1.0</li>,
 * <li>OSGi_Minimum-1.1,</li>
 * <li>OSGi_Minimum-1.2.</li>
 * </ol>
 * This can be called using: -output c:/EE_descriptions -config
 * C:\OSGi_profiles\configuration.properties -EEs
 * JRE-1.1,J2SE-1.2,J2SE-1.3,J2SE-
 * 1.4,J2SE-1.5,JavaSE-1.6,JavaSE-1.7,JavaSE-1.8,JavaSE-9,CDC-
 * 1.0_Foundation-1.0,CDC-1.1_Foundation-1.1,OSGi_Minimum-1.0,OSGi_Minimum-1.1,OSGi_Minimum-1.
 * 2
 */
public class EEGenerator {
	static class AbstractNode {
		protected int addedProfileValue = -1;
		protected int removedProfileValue = -1;

		public void persistAnnotations(Element element) {
			if (this.addedProfileValue != -1) {
				element.setAttribute(IApiXmlConstants.ATTR_ADDED_PROFILE, Integer.toString(this.addedProfileValue));
			}
			if (this.removedProfileValue != -1) {
				element.setAttribute(IApiXmlConstants.ATTR_REMOVED_PROFILE, Integer.toString(this.removedProfileValue));
			}
		}

		public void persistAnnotations(Element element, String OSGiProfileName) {
			int value = ProfileModifiers.getValue(OSGiProfileName);
			if (value != -1) {
				element.setAttribute(IApiXmlConstants.ATTR_PROFILE, Integer.toString(value));
			}
		}

		public void setAddedProfileValue(int value) {
			if (this.addedProfileValue != -1) {
				System.err.println("Remove profile value is already set"); //$NON-NLS-1$
			}
			this.addedProfileValue = value;
		}
		public void setRemovedProfileValue(int value) {
			if (this.removedProfileValue != -1) {
				System.err.println("Remove profile value is already set"); //$NON-NLS-1$
			}
			this.removedProfileValue = value;
		}
	}
	static class Field extends AbstractNode implements Comparable<Field> {
		char[] name;
		char[] type;

		Field(char[] fname, char[] ftype) {
			this.name = fname;
			if (ftype != null) {
				this.type = CharOperation.replaceOnCopy(ftype, '/', '.');
			}
		}
		@Override
		public int compareTo(Field field) {
			return CharOperation.compareTo(this.name, field.name);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Field)) {
				return false;
			}
			Field other = (Field) obj;
			return Arrays.equals(name, other.name);
		}

		public int getStatus() {
			return -1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(name);
			result = prime * result + Arrays.hashCode(type);
			return result;
		}

		public void persistXML(Document document, Element parent) {
			Element field = document.createElement(IApiXmlConstants.ELEMENT_FIELD);
			parent.appendChild(field);
			field.setAttribute(IApiXmlConstants.ATTR_NAME, new String(this.name));
			field.setAttribute(IApiXmlConstants.ATTR_STATUS, Integer.toString(getStatus()));
		}

		public void persistXML(Document document, Element parent, String OSGiProfileName) {
			Element field = document.createElement(IApiXmlConstants.ELEMENT_FIELD);
			parent.appendChild(field);
			field.setAttribute(IApiXmlConstants.ATTR_NAME, new String(this.name));
			persistAnnotations(field, OSGiProfileName);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Field : ") //$NON-NLS-1$
			.append(this.name).append(' ').append(Signature.toCharArray(this.type));
			return String.valueOf(builder);
		}
	}
	static class Method extends AbstractNode implements Comparable<Method> {
		public static final char[] NO_GENERIC_SIGNATURE = new char[0];
		char[] genericSignature;
		int modifiers;
		char[] selector;
		char[] signature;

		Method(int mods, char[] select, char[] sig, char[] genericsig) {
			this.selector = select;
			this.signature = sig;
			this.modifiers = mods;
			if (genericsig == null) {
				this.genericSignature = NO_GENERIC_SIGNATURE;
			} else {
				this.genericSignature = genericsig;
			}
		}
		@Override
		public int compareTo(Method method) {
			int compare = CharOperation.compareTo(this.selector, method.selector);
			if (compare == 0) {
				int compareTo = CharOperation.compareTo(this.signature, method.signature);
				if (compareTo == 0) {
					return this.getStatus() - method.getStatus();
				}
				return compareTo;
			}
			return compare;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Method)) {
				return false;
			}
			Method other = (Method) obj;
			if (!Arrays.equals(selector, other.selector)) {
				return false;
			}
			return Arrays.equals(signature, other.signature);
		}

		public int getStatus() {
			return -1;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(selector);
			result = prime * result + Arrays.hashCode(signature);
			return result;
		}

		public void persistXML(Document document, Element parent) {
			Element method = document.createElement(IApiXmlConstants.ELEMENT_METHOD);
			parent.appendChild(method);
			method.setAttribute(IApiXmlConstants.ATTR_NAME, new String(this.selector));
			method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, new String(this.signature));
			method.setAttribute(IApiXmlConstants.ATTR_STATUS, Integer.toString(getStatus()));
		}

		public void persistXML(Document document, Element parent, String OSGiProfileName) {
			Element method = document.createElement(IApiXmlConstants.ELEMENT_METHOD);
			parent.appendChild(method);
			method.setAttribute(IApiXmlConstants.ATTR_NAME, new String(this.selector));
			method.setAttribute(IApiXmlConstants.ATTR_SIGNATURE, new String(this.signature));
			persistAnnotations(method, OSGiProfileName);
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Method : "); //$NON-NLS-1$
			if (this.genericSignature != null && this.genericSignature.length != 0) {
				builder.append(Signature.toCharArray(this.genericSignature, this.selector, null, true, true));
			} else {
				builder.append(Signature.toCharArray(this.signature, this.selector, null, true, true));
			}
			return String.valueOf(builder);
		}
	}

	static class Package extends AbstractNode implements Comparable<Package> {
		String name;
		List<Type> types;

		public Package(String pname) {
			this.name = pname;
		}
		public void addType(Type type) {
			if (this.types == null) {
				this.types = new ArrayList<>();
			}
			this.types.add(type);
		}
		public void collectTypes(Map<String, Type> result) {
			Collections.sort(this.types);
			for (Type type : this.types) {
				String typeName = new String(type.name);
				result.put(typeName, type);
			}
		}
		@Override
		public int compareTo(Package package1) {
			return this.name.compareTo(package1.name);
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Package)) {
				return false;
			}
			Package other = (Package) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}

		public Type getType(Type typeToFind) {
			if (this.types == null) {
				return null;
			}
			int index = this.types.indexOf(typeToFind);
			if (index != -1) {
				return this.types.get(index);
			}
			return null;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}
		public void persistAsClassStubsForZip(ZipOutputStream zipOutputStream, ProfileInfo info) throws IOException {
			if (this.types == null) {
				return;
			}
			Collections.sort(this.types);
			for (Type type : this.types) {
				StringBuilder buffer = new StringBuilder(this.name.replace('.', '/'));
				String simpleName = type.getSimpleName();
				buffer.append('/').append(simpleName.replace('.', '$'));
				byte[] classFileBytes = info.getClassFileBytes(type);
				if (classFileBytes != null) {
					Util.writeZipFileEntry(zipOutputStream, String.valueOf(buffer), classFileBytes);
				}
			}
		}
		public void persistXML(Document document, Element element, String OSGiProfileName) {
			Element pkg = document.createElement(IApiXmlConstants.ELEMENT_PACKAGE);
			pkg.setAttribute(IApiXmlConstants.ATTR_NAME, this.name);
			element.appendChild(pkg);
			if (this.types == null) {
				return;
			}
			Collections.sort(this.types);
			for (Iterator<Type> iterator2 = this.types.iterator(); iterator2.hasNext();) {
				iterator2.next().persistXML(document, pkg, OSGiProfileName);
			}
		}
		public int size() {
			return this.types == null ? 0 : this.types.size();
		}
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("Package : "); //$NON-NLS-1$
			builder.append(this.name).append(Util.LINE_SEPARATOR);
			return String.valueOf(builder);
		}
	}

	static class ProfileInfo {
		private static final String BLACK_LIST_NAME = "_blackList_.txt"; //$NON-NLS-1$
		private static final String CDC_SUBDIR = "cdc"; //$NON-NLS-1$
		private static final String JRE_SUBDIR = "jre"; //$NON-NLS-1$
		private static final String OSGI_SUBDIR = "osgi"; //$NON-NLS-1$
		private static final String OTHER_PACKAGES = "org.osgi.framework.system.packages"; //$NON-NLS-1$

		private static boolean checkDocStatus(ProfileInfo info, Type type, ZipFile docZip, String docURL, String docRoot) {
			if (docZip == null && docURL == null) {
				// if no doc to validate we accept it if on white list
				if (DEBUG) {
					System.out.println("No javadoc zip or url for " + info.profileName); //$NON-NLS-1$
				}
				return info.isOnWhiteList(type);
			}
			String typeName = getDocTypeName(docRoot, type);
			if (DEBUG) {
				System.out.println("Retrieving javadoc for type: " + typeName); //$NON-NLS-1$
			}
			if (docZip == null) {
				char[] contents = info.getOnlineDocContents(docURL, typeName);
				if (contents == null) {
					if (DEBUG) {
						System.out.println("Found no doc for " + typeName + " - check whitelist"); //$NON-NLS-1$ //$NON-NLS-2$
					}
					return info.isOnWhiteList(type);
				}
				return true;
			}
			return docZip.getEntry(typeName) != null || info.isOnWhiteList(type);
		}

		public static String getDocTypeName(String docRoot, Type type) {
			StringBuilder buffer = new StringBuilder(docRoot);
			char[] typeNameForDoc = CharOperation.replaceOnCopy(type.name, '.', '/');
			typeNameForDoc = CharOperation.replaceOnCopy(typeNameForDoc, '$', '.');
			buffer.append(typeNameForDoc);
			buffer.append(".html"); //$NON-NLS-1$
			return String.valueOf(buffer);
		}

		public static ProfileInfo getProfileInfo(String profileName, String jreLib, String osgiProfile, String jreDoc, String jreURL, String docRoot, String cacheLocation, String whiteList) {
			ProfileInfo info = new ProfileInfo(profileName, jreLib, osgiProfile, jreDoc, jreURL, docRoot, cacheLocation, whiteList);
			if (info.getProfileName() == null) {
				return null;
			}
			return info;
		}

		private static Set<String> initializePackages(String fileName) {
			Set<String> knownPackages = null;
			try {
				Properties allProperties = new Properties();
				BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(fileName));
				allProperties.load(inputStream);
				inputStream.close();
				String property = allProperties.getProperty(OTHER_PACKAGES);
				if (property != null && property.length() != 0) {
					String[] packages = property.split(","); //$NON-NLS-1$
					for (String package1 : packages) {
						if (knownPackages == null) {
							knownPackages = new LinkedHashSet<>();
						}
						knownPackages.add(package1);
					}
				} else {
					knownPackages = Collections.emptySet();
				}
			} catch (IOException e) {
				// ignore
			}
			return knownPackages;
		}
		private static Set<String> initializeWhiteList(String fileName) {
			if (fileName == null) {
				return Collections.emptySet();
			}
			Set<String> values = new HashSet<>();
			try (LineNumberReader reader = new LineNumberReader(new BufferedReader(new FileReader(fileName)))) {
				String line;
				while ((line = reader.readLine()) != null) {
					String trimmedLine = line.trim();
					if (!trimmedLine.isEmpty()) {
						// only non-empty lines are added trimmed on both ends
						values.add(trimmedLine);
					}
				}
			} catch (IOException e) {
				// ignore
			}
			return values;
		}

		File[] allFiles;
		// set of type names that don't have javadoc
		Set<String> blackList;
		String cacheLocation;
		Map<String, Package> data;
		String docRoot;
		long generatedSize;
		String JREdoc;
		String JRElib;
		String JREURL;

		String OSGiProfile;
		String profileName;
		long totalSize;

		Set<String> whiteList;

		private ProfileInfo(String profilename, String jreLib, String osgiProfile, String jreDoc, String jreURL, String docroot, String cacheloc, String whitelist) {
			this.JREdoc = jreDoc;
			this.JREURL = jreURL;
			this.JRElib = jreLib;
			this.OSGiProfile = osgiProfile;
			this.docRoot = docroot;
			this.profileName = profilename;
			this.cacheLocation = cacheloc;
			this.whiteList = initializeWhiteList(whitelist);
		}

		private void addToBlackList(String typeName) {
			if (this.blackList != null) {
				this.blackList.add(typeName);
				return;
			}
			this.blackList = new TreeSet<>();
			this.blackList.add(typeName);
		}

		public void dumpToCache(String typeName, char[] contents) {
			if (!CACHE_ENABLED || this.cacheLocation == null) {
				return;
			}
			File cacheDir = new File(this.cacheLocation);
			if (!cacheDir.exists()) {
				if (!cacheDir.mkdirs()) {
					System.err.println("Cache creation failed for " + typeName + " for profile " + this.getProfileName()); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			}
			File profileCache = new File(cacheDir, getProfileFileName());
			if (!profileCache.exists()) {
				if (!profileCache.mkdirs()) {
					System.err.println("Cache creation failed for " + typeName + " for profile " + this.getProfileName()); //$NON-NLS-1$ //$NON-NLS-2$
					return;
				}
			}
			File docType = new File(profileCache, typeName);
			if (docType.exists()) {
				// already in the cache
				return;
			} else {
				File parentFile = docType.getParentFile();
				if (!parentFile.exists()) {
					if (!parentFile.mkdirs()) {
						System.err.println("Cache creation failed for " + typeName + " for profile " + this.getProfileName()); //$NON-NLS-1$ //$NON-NLS-2$
						return;
					}
				}
				try (BufferedWriter writer = new BufferedWriter(new FileWriter(docType))) {
					writer.write(contents);
					writer.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		void generateEEDescription(String outputDir) {
			if (this.data == null) {
				System.err.println("No data to persist for " + this.getProfileName()); //$NON-NLS-1$
				return;
			}
			String subDir = getSubDir();
			persistData(outputDir, subDir);
			persistDataAsClassFilesInZipFormat(outputDir, subDir);
		}

		public Map<String, Type> getAllTypes() {
			Map<String, Type> result = new LinkedHashMap<>();
			Set<String> keySet = this.data.keySet();
			String[] sortedKeys = new String[keySet.size()];
			keySet.toArray(sortedKeys);
			Arrays.sort(sortedKeys);
			for (String key : sortedKeys) {
				Package package1 = this.data.get(key);
				if (package1 != null) {
					package1.collectTypes(result);
				} else {
					System.err.println("Missing package for profile info XML serialization: " + key); //$NON-NLS-1$
				}
			}
			return result;
		}

		private File getCacheRoot() {
			File cacheDir = new File(this.cacheLocation);
			if (cacheDir.exists() || cacheDir.mkdirs()) {
				File profileCache = new File(cacheDir, getProfileFileName());
				if (profileCache.exists() || profileCache.mkdirs()) {
					return profileCache;
				}
			}
			return null;
		}

		public byte[] getClassFileBytes(Type type) {
			if (this.allFiles == null) {
				throw new IllegalStateException("No jar files to open"); //$NON-NLS-1$
			}
			String typeName = new String(type.name);
			byte[] classFileBytes = null;
			try {
				String zipFileEntryName = typeName.replace('.', '/') + ".class"; //$NON-NLS-1$
				loop: for (int i = 0, max = this.allFiles.length; i < max; i++) {
					if (allFiles[i].toString().endsWith(".jmod") && !zipFileEntryName.startsWith("classes/")) { //$NON-NLS-1$ //$NON-NLS-2$
						zipFileEntryName = "classes/" + zipFileEntryName; //$NON-NLS-1$
					}
					try (ZipFile zipFile = new ZipFile(allFiles[i])) {
						ZipEntry zipEntry = zipFile.getEntry(zipFileEntryName);
						if (zipEntry == null) {
							continue loop;
						}
						try (InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry))) {
							classFileBytes = Util.getInputStreamAsByteArray(inputStream, -1);
							break loop;
						}
					}
				}
			} catch (ZipException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (classFileBytes == null) {
				throw new IllegalStateException("Could not retrieve byte[] for " + typeName); //$NON-NLS-1$
			}
			ClassReader classReader = new ClassReader(classFileBytes);
			StubClassAdapter visitor = new StubClassAdapter(type);
			classReader.accept(visitor, ClassReader.SKIP_DEBUG);
			if (visitor.shouldIgnore()) {
				return null;
			}

			return visitor.getStub().getBytes();
		}

		public char[] getFromCache(String typeName) {
			if (!CACHE_ENABLED || this.cacheLocation == null) {
				return null;
			}
			File profileCache = getCacheRoot();
			if (profileCache != null) {
				File docType = new File(profileCache, typeName);
				if (docType.exists()) {
					try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(docType))) {
						return Util.getInputStreamAsCharArray(stream, -1, null);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		char[] getOnlineDocContents(String docURL, String typeName) {
			// check the doc online
			String typeDocURL = docURL + typeName;
			char[] contents = this.getFromCache(typeName);
			if (contents == null && !ONLY_USE_CACHE) {
				// check the black list
				if (this.isOnBlackList(typeName)) {
					return null;
				}
				contents = Util.getURLContents(typeDocURL);
				if (contents == null) {
					this.addToBlackList(typeName);
					return null;
				} else {
					this.dumpToCache(typeName, contents);
				}
			}
			return contents;
		}

		public String getProfileFileName() {
			return Util.getProfileFileName(getProfileName());
		}

		public String getProfileName() {
			return this.profileName;
		}

		private String getSubDir() {
			if (Util.getProfileFileName(ProfileModifiers.CDC_1_0_FOUNDATION_1_0_NAME).equals(this.profileName) || Util.getProfileFileName(ProfileModifiers.CDC_1_1_FOUNDATION_1_1_NAME).equals(this.profileName)) {
				return CDC_SUBDIR;
			} else if (Util.getProfileFileName(ProfileModifiers.OSGI_MINIMUM_1_0_NAME).equals(this.profileName) || Util.getProfileFileName(ProfileModifiers.OSGI_MINIMUM_1_1_NAME).equals(this.profileName) || Util.getProfileFileName(ProfileModifiers.OSGI_MINIMUM_1_2_NAME).equals(this.profileName)) {
				return OSGI_SUBDIR;
			} else {
				return JRE_SUBDIR;
			}
		}

		private Set<String> initializeBlackList() {
			File cacheRoot = getCacheRoot();
			File blackListFile = new File(cacheRoot, BLACK_LIST_NAME);
			Set<String> values = new TreeSet<>();
			if (blackListFile.exists()) {
				try (LineNumberReader reader = new LineNumberReader(
						new BufferedReader(new FileReader(blackListFile)))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String trimmedLine = line.trim();
						if (!trimmedLine.isEmpty()) {
							// only non-empty lines are added trimmed on both
							// ends
							values.add(trimmedLine);
						}
					}
				} catch (IOException e) {
					// ignore
				}
			}
			return values;
		}

		public void initializeData() throws IOException {
			String pname = this.getProfileName();
			if (pname == null) {
				// invalid profile info
				System.err.println("Info are invalid"); //$NON-NLS-1$
				return;
			}

			if (DEBUG) {
				System.out.println("Profile : " + pname); //$NON-NLS-1$
			}
			long time = System.currentTimeMillis();
			this.allFiles = Util.getAllFiles(new File(this.JRElib),
					pathname -> pathname.isDirectory() || pathname.getName().toLowerCase().endsWith(".jar") //$NON-NLS-1$
							|| pathname.getName().toLowerCase().endsWith(".jmod")); //$NON-NLS-1$
			if (allFiles == null) {
				System.err.println("No jar files to proceed"); //$NON-NLS-1$
				return;
			}
			// initialize known packages
			String osgiProfileName = this.OSGiProfile;
			Set<String> knownPackages = initializePackages(osgiProfileName);
			// known packages should be part of the white list by default
			if (this.whiteList != null && !this.whiteList.isEmpty()) {
				this.whiteList.addAll(knownPackages);
			} else {
				this.whiteList = Collections.unmodifiableSet(knownPackages);
			}
			Map<String, Type> allVisibleTypes = new LinkedHashMap<>();
			Map<String, Type> allTypes = new LinkedHashMap<>();
			this.totalSize = 0;
			for (File currentFile : allFiles) {
				this.totalSize += currentFile.length();

				try (ZipFile zipFile = new ZipFile(currentFile)) {
					for (Enumeration<? extends ZipEntry> enumeration = zipFile.entries(); enumeration.hasMoreElements();) {
						ZipEntry zipEntry = enumeration.nextElement();
						if (!zipEntry.getName().endsWith(".class")) //$NON-NLS-1$
						 {
							continue;
						}
						InputStream inputStream = new BufferedInputStream(zipFile.getInputStream(zipEntry));
						IClassFileReader classFileReader = null;
						try {
							classFileReader = ToolFactory.createDefaultClassFileReader(inputStream, IClassFileReader.ALL_BUT_METHOD_BODIES);
						} finally {
							inputStream.close();
						}
						if (classFileReader == null) {
							continue;
						}
						char[] className = classFileReader.getClassName();
						char[] packageName = null;
						int lastIndexOf = CharOperation.lastIndexOf('/', className);
						if (lastIndexOf == -1) {
							packageName = new char[0];
						} else {
							packageName = CharOperation.subarray(className, 0, lastIndexOf);
						}
						if (this.isMatching(knownPackages, packageName)) {
							Type type = Type.newType(this, classFileReader, this.JREdoc, this.JREURL, this.docRoot);
							if (type != null) {
								if (type.isProtected() || type.isPublic()) {
									allVisibleTypes.put(type.getFullQualifiedName(), type);
								}
								allTypes.put(type.getFullQualifiedName(), type);
							}
						}
					}
				}
			}
			// list all results
			List<Type> visibleTypes = new ArrayList<>();
			visibleTypes.addAll(allVisibleTypes.values());
			// check transitive closure to make sure all methods/fields from
			// superclass are visible when resolving fields/methods
			while (!visibleTypes.isEmpty()) {
				Type type = visibleTypes.remove(0);
				String superclassName = type.getSuperclassName();
				while (superclassName != null) {
					if (allVisibleTypes.get(superclassName) == null) {
						// look for required type
						Type currentSuperclass = allTypes.get(superclassName);
						if (currentSuperclass == null) {
							if (DEBUG) {
								System.out.println("Missing type: " + superclassName); //$NON-NLS-1$
							}
							break;
						} else {
							allVisibleTypes.put(currentSuperclass.getFullQualifiedName(), currentSuperclass);
							visibleTypes.add(currentSuperclass);
							if (DEBUG) {
								System.out.println("Reinject type: " + currentSuperclass.getFullQualifiedName()); //$NON-NLS-1$
							}
						}
					}
					Type superclass = allVisibleTypes.get(superclassName);
					superclassName = superclass.getSuperclassName();
				}
			}
			List<Type> isInDoc = new ArrayList<>();
			ZipFile docZip = null;
			if (this.JREdoc != null) {
				try {
					docZip = new ZipFile(this.JREdoc);
				} catch (FileNotFoundException e) {
					// no zip file
				}
			}
			try {
				for (Type type : allVisibleTypes.values()) {
					if (checkDocStatus(this, type, docZip, this.JREURL, this.docRoot)) {
						isInDoc.add(type);
					}
				}
			} finally {
				if (docZip != null) {
					docZip.close();
				}
			}
			HashMap<String, Package> typesPerPackage = new LinkedHashMap<>();
			for (Type type : isInDoc) {
				String packageName = type.getPackage();
				Package package1 = typesPerPackage.get(packageName);
				if (package1 == null) {
					package1 = new Package(packageName);
					typesPerPackage.put(packageName, package1);
				}
				package1.addType(type);
			}
			this.data = typesPerPackage;
			if (DEBUG) {
				System.out.println("Time spent for gathering datas for " + pname + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			if (this.blackList != null) {
				// persist black list
				this.persistBlackList();
			}
		}

		private boolean isMatching(Set<String> knownPackages, char[] packageName) {
			if (CharOperation.indexOf("java/".toCharArray(), packageName, true) == 0) { //$NON-NLS-1$
				return true;
			}
			if (knownPackages.isEmpty()) {
				return false;
			}
			String currentPackage = new String(CharOperation.replaceOnCopy(packageName, '/', '.'));
			return knownPackages.contains(currentPackage) || this.isOnWhiteList(currentPackage);
		}

		private boolean isOnBlackList(String typeName) {
			if (this.blackList != null) {
				return this.blackList.contains(typeName);
			}
			// retrieve black list if it exists
			this.blackList = initializeBlackList();
			return this.blackList.contains(typeName);
		}
		private boolean isOnWhiteList(String packageName) {
			return packageName.startsWith("java.") || packageName.startsWith("javax.") || this.whiteList.contains(packageName); //$NON-NLS-1$ //$NON-NLS-2$
		}
		private boolean isOnWhiteList(Type type) {
			return isOnWhiteList(type.getPackage());
		}
		private void persistBlackList() {
			if (this.blackList == null || this.blackList.isEmpty()) {
				return;
			}
			File cacheRoot = getCacheRoot();
			File blackListFile = new File(cacheRoot, BLACK_LIST_NAME);
			if (!blackListFile.exists()) {
				try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(blackListFile)));) {
					for (Iterator<String> iterator = this.blackList.iterator(); iterator.hasNext();) {
						writer.println(iterator.next());
					}
					writer.flush();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		private void persistChildren(Document document, Element xmlElement, Map<String, Package> packageMap, String OSGiProfileName) {
			Set<String> keySet = packageMap.keySet();
			String[] sortedKeys = new String[keySet.size()];
			keySet.toArray(sortedKeys);
			Arrays.sort(sortedKeys);
			for (String key : sortedKeys) {
				Package package1 = packageMap.get(key);
				if (package1 != null) {
					package1.persistXML(document, xmlElement, OSGiProfileName);
				} else {
					System.err.println("Missing package for profile info XML serialization: " + key); //$NON-NLS-1$
				}
			}
		}

		private void persistChildrenAsClassFile(ZipOutputStream zipOutputStream, Map<String, Package> packageMap, ProfileInfo info) throws IOException {
			Set<String> keySet = packageMap.keySet();
			String[] sortedKeys = new String[keySet.size()];
			keySet.toArray(sortedKeys);
			Arrays.sort(sortedKeys);
			for (String key : sortedKeys) {
				Package package1 = packageMap.get(key);
				if (package1 != null) {
					package1.persistAsClassStubsForZip(zipOutputStream, info);
				} else {
					System.err.println("Missing package for profile info zip serialization: " + key); //$NON-NLS-1$
				}
			}
		}
		private void persistData(String rootName, String subDirName) {
			try {
				Document document = org.eclipse.pde.api.tools.internal.util.Util.newDocument();
				Element component = document.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
				String profileName2 = this.getProfileName();
				component.setAttribute(IApiXmlConstants.ATTR_ID, profileName2);
				component.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION);
				document.appendChild(component);
				persistChildren(document, component, this.data, profileName2);
				String contents = org.eclipse.pde.api.tools.internal.util.Util.serializeDocument(document);
				String fileName = profileName2 + ".xml"; //$NON-NLS-1$
				Util.write(rootName, subDirName, fileName, contents);
			} catch (DOMException e) {
				e.printStackTrace();
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}

		private void persistDataAsClassFilesInZipFormat(String rootName, String subDirName) {
			ZipOutputStream zipOutputStream = null;
			String profileName2 = this.getProfileFileName();
			File root = new File(rootName);
			if (!root.exists()) {
				root.mkdirs();
			}
			File subDir = new File(root, subDirName);
			if (!subDir.exists()) {
				subDir.mkdir();
			}
			if (profileName2.indexOf('/') != 0) {
				profileName2 = profileName2.replace('/', '_');
			}
			File file = new File(subDir, profileName2 + ".zip"); //$NON-NLS-1$
			try {
				zipOutputStream = Util.getOutputStream(file);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			if (zipOutputStream == null) {
				System.err.println("Could not create the output file : " + file.getAbsolutePath()); //$NON-NLS-1$
				return;
			}
			try {
				persistChildrenAsClassFile(zipOutputStream, this.data, this);
				zipOutputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					zipOutputStream.close();
				} catch (IOException e) {
					// ignore
				}
			}
			this.generatedSize = file.length();
			System.out.println("The stub for the profile " + this.profileName + " was generated from " + this.totalSize + " bytes."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			System.out.println("Its generated size is " + this.generatedSize + " bytes."); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println("Ratio : " + (((double) this.generatedSize / (double) this.totalSize) * 100.0) + "%"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		@Override
		public String toString() {
			return this.getProfileName();
		}
	}

	static class StubClass {
		private static final int CURRENT_VERSION = 3;
		int access;
		int classNameIndex;
		List<StubField> fields;

		int[] interfacesIndexes;
		List<StubMethod> methods;
		Map<String, Integer> pool;
		int poolIndex;
		int superNameIndex;

		public StubClass(int acc, String className2, String superName2, String[] interfaces2) {
			this.access = acc;
			this.pool = new LinkedHashMap<>();
			this.classNameIndex = getIndex(className2);
			this.superNameIndex = superName2 != null ? getIndex(superName2) : -1;
			if (interfaces2 != null) {
				this.interfacesIndexes = new int[interfaces2.length];
				for (int i = 0; i < interfaces2.length; i++) {
					this.interfacesIndexes[i] = getIndex(interfaces2[i]);
				}
			}
		}

		public void addField(String fieldName) {
			if (this.fields == null) {
				this.fields = new ArrayList<>();
			}
			this.fields.add(new StubField(getIndex(fieldName)));
		}

		public StubMethod addMethod(String methodName, String desc) {
			if (this.methods == null) {
				this.methods = new ArrayList<>();
			}
			StubMethod method = new StubMethod(getIndex(methodName), getIndex(desc));
			this.methods.add(method);
			return method;
		}

		public byte[] getBytes() {
			ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

			try (DataOutputStream outputStream = new DataOutputStream(byteArrayOutputStream)) {
				outputStream.writeShort(CURRENT_VERSION);
				outputStream.writeShort(this.poolIndex);
				for (Entry<String, Integer> next : this.pool.entrySet()) {
					outputStream.writeUTF(next.getKey());
					outputStream.writeShort(next.getValue().intValue());
				}
				outputStream.writeChar(this.access);
				outputStream.writeShort(this.classNameIndex);
				outputStream.writeShort(this.superNameIndex);
				int length = this.interfacesIndexes != null ? this.interfacesIndexes.length : 0;
				outputStream.writeShort(length);
				for (int i = 0; i < length; i++) {
					outputStream.writeShort(this.interfacesIndexes[i]);
				}
				int fieldsLength = this.fields == null ? 0 : this.fields.size();
				outputStream.writeShort(fieldsLength);
				for (int i = 0; i < fieldsLength; i++) {
					outputStream.writeShort(this.fields.get(i).nameIndex);
				}
				int methodsLength = this.methods == null ? 0 : this.methods.size();
				outputStream.writeShort(methodsLength);
				for (int i = 0; i < methodsLength; i++) {
					StubMethod stubMethod = this.methods.get(i);
					outputStream.writeShort(stubMethod.selectorIndex);
					outputStream.writeShort(stubMethod.signatureIndex);
					outputStream.writeByte(stubMethod.isPolymorphic ? 1 : 0);
				}
				outputStream.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.pool = null;
			this.fields = null;
			this.methods = null;
			return byteArrayOutputStream.toByteArray();
		}

		private int getIndex(String name) {
			Integer integer = this.pool.get(name);
			if (integer != null) {
				return integer.intValue();
			}
			int value = this.poolIndex++;
			this.pool.put(name, Integer.valueOf(value));
			return value;
		}
	}

	/**
	 * Class adapter
	 */
	static class StubClassAdapter extends ClassVisitor {
		static final int IGNORE_CLASS_FILE = 0x100;

		int flags;
		String name;
		StubClass stub;
		Type type;

		/**
		 * Constructor
		 * @param stubtype
		 */
		public StubClassAdapter(Type stubtype) {
			super(Opcodes.ASM6, new ClassWriter(0));
			this.type = stubtype;
		}

		public StubClass getStub() {
			return this.stub;
		}

		/**
		 * @return if this class file should be ignored or not
		 */
		public boolean shouldIgnore() {
			return (this.flags & IGNORE_CLASS_FILE) != 0;
		}

		@Override
		public void visit(int version, int access, String className, String signature, String superName, String[] interfaces) {
			this.name = className;
			this.stub = new StubClass(access, className, superName, interfaces);
		}

		@Override
		public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
			return null;
		}

		@Override
		public void visitAttribute(Attribute attr) {
			if ("Synthetic".equals(attr.type)) { //$NON-NLS-1$
				this.flags |= IGNORE_CLASS_FILE;
			}
		}

		@Override
		public void visitEnd() {
		}

		@Override
		public FieldVisitor visitField(int access, String fieldName, String desc, String signature, Object value) {
			if (type.getField(fieldName) == null) {
				return null;
			}
			this.stub.addField(fieldName);
			return null;
		}

		@Override
		public void visitInnerClass(String innerClassName, String outerName, String innerName, int access) {
			if (this.name.equals(innerClassName) && (outerName == null)) {
				// local class
				this.flags |= IGNORE_CLASS_FILE;
			}
		}

		@Override
		public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
			if ("<clinit>".equals(methodName)) { //$NON-NLS-1$
				return null;
			}
			if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
				return null;
			}
			if (((access & Opcodes.ACC_BRIDGE) != 0) || ((access & Opcodes.ACC_SYNTHETIC) != 0)) {
				return null;
			}
			if (this.type.getMethod(methodName, desc) == null) {
				return null;
			}
			final StubMethod method = this.stub.addMethod(methodName, desc);
			return new MethodVisitor(Opcodes.ASM6, super.visitMethod(access, methodName, desc, signature, exceptions)) {
				@Override
				public AnnotationVisitor visitAnnotation(String sig, boolean visible) {
					if (visible && "Ljava/lang/invoke/MethodHandle$PolymorphicSignature;".equals(sig)) { //$NON-NLS-1$
						method.isPolymorphic();
					}
					return super.visitAnnotation(sig, visible);
				}
			};
		}
		@Override
		public void visitOuterClass(String arg0, String arg1, String arg2) {
			// ignore
		}
		@Override
		public void visitSource(String arg0, String arg1) {
			// ignore
		}
	}

	static class StubField {
		int nameIndex;

		public StubField(int index) {
			this.nameIndex = index;
		}
	}

	static class StubMethod {
		boolean isPolymorphic;
		int selectorIndex;
		int signatureIndex;

		public StubMethod(int selectorindex, int sigindex) {
			this.selectorIndex = selectorindex;
			this.signatureIndex = sigindex;
		}

		public void isPolymorphic() {
			this.isPolymorphic = true;
		}
	}

	static class Type extends AbstractNode implements Comparable<Type> {

		static boolean isFinal(int accessFlags) {
			return (accessFlags & Flags.AccFinal) != 0;
		}
		static boolean isPrivate(int accessFlags) {
			return (accessFlags & Flags.AccPrivate) != 0;
		}
		static boolean isProtected(int accessFlags) {
			return (accessFlags & Flags.AccProtected) != 0;
		}
		static boolean isPublic(int accessFlags) {
			return (accessFlags & Flags.AccPublic) != 0;
		}
		static boolean isStatic(int accessFlags) {
			return (accessFlags & Flags.AccStatic) != 0;
		}

		private static boolean isVisibleField(int typeAccessFlags, int fieldAccessFlags) {
			if (isPublic(fieldAccessFlags)) {
				return true;
			}
			if (isProtected(fieldAccessFlags)) {
				return !isFinal(typeAccessFlags);
			}
			return false;
		}

		private static boolean isVisibleMethod(int typeAccessFlags, int methodAccessFlags) {
			if (isPublic(methodAccessFlags)) {
				return true;
			}
			if (isProtected(methodAccessFlags)) {
				return !isFinal(typeAccessFlags);
			}
			return false;
		}

		public static Type newType(ProfileInfo info, IClassFileReader reader, String docZipFileName, String docURL, String docRoot) {
			IInnerClassesAttribute innerClassesAttribute = reader.getInnerClassesAttribute();
			if (innerClassesAttribute != null) {
				// search the right entry
				IInnerClassesAttributeEntry[] entries = innerClassesAttribute.getInnerClassAttributesEntries();
				for (IInnerClassesAttributeEntry entry : entries) {
					char[] innerClassName = entry.getInnerClassName();
					if (innerClassName != null) {
						if (CharOperation.equals(reader.getClassName(), innerClassName)) {
							int accessFlags2 = entry.getAccessFlags();
							if (isPrivate(accessFlags2)) {
								return null;
							}
						}
					}
				}
			}
			return new Type(reader, docZipFileName);
		}

		Set<Field> fields;
		Set<Method> methods;
		int modifiers;

		char[] name;
		char[] superclassName;
		char[][] superinterfacesNames;

		private Type(IClassFileReader reader, String docZipFileName) {
			ZipFile docZip = null;
			try {
				if (docZipFileName != null) {
					try {
						docZip = new ZipFile(docZipFileName);
					} catch (FileNotFoundException e) {
						// no zip file
					}
				}
				char[] className = reader.getClassName();
				className = CharOperation.replaceOnCopy(className, '/', '.');
				this.name = className;
				if (DEBUG) {
					System.out.println("Adding type: " + String.valueOf(className)); //$NON-NLS-1$
				}
				char[] scname = reader.getSuperclassName();
				if (scname != null) {
					scname = CharOperation.replaceOnCopy(scname, '/', '.');
					this.superclassName = scname;
				}
				char[][] interfaceNames = CharOperation.deepCopy(reader.getInterfaceNames());
				for (char[] interfaceName : interfaceNames) {
					CharOperation.replace(interfaceName, '/', '.');
				}
				this.superinterfacesNames = interfaceNames;
				this.modifiers = reader.getAccessFlags();
				IFieldInfo[] fieldInfos = reader.getFieldInfos();
				int length = fieldInfos.length;
				for (int i = 0; i < length; i++) {
					IFieldInfo fieldInfo = fieldInfos[i];
					if (isVisibleField(this.modifiers, fieldInfo.getAccessFlags())) {
						if (fields == null) {
							this.fields = new HashSet<>();
						}
						Field field = new Field(fieldInfo.getName(), fieldInfo.getDescriptor());
						fields.add(field);
						if (DEBUG) {
							System.out.println("Adding field: " + field); //$NON-NLS-1$
						}
					}
				}
				IMethodInfo[] methodInfos = reader.getMethodInfos();
				length = methodInfos.length;
				for (IMethodInfo methodInfo : methodInfos) {
					IClassFileAttribute[] attributes = methodInfo.getAttributes();
					ISignatureAttribute signatureAttribute = null;
					for (IClassFileAttribute currentAttribute : attributes) {
						if (CharOperation.equals(currentAttribute.getAttributeName(), IAttributeNamesConstants.SIGNATURE)) {
							signatureAttribute = (ISignatureAttribute) currentAttribute;
							break;
						}
					}
					char[] signature = null;
					if (signatureAttribute != null) {
						signature = signatureAttribute.getSignature();
					} else {
						signature = methodInfo.getDescriptor();
					}
					int accessFlags = methodInfo.getAccessFlags();
					if (isVisibleMethod(this.modifiers, accessFlags)) {
						if (methods == null) {
							this.methods = new HashSet<>();
						}
						Method method = new Method(accessFlags, methodInfo.getName(), methodInfo.getDescriptor(), signatureAttribute == null ? null : signature);
						methods.add(method);
						if (DEBUG) {
							System.out.println("Adding method: " + method); //$NON-NLS-1$
						}
					}
				}
			} catch (IOException e) {
				// no zip file
				System.err.println("Missing doc zip at " + docZipFileName); //$NON-NLS-1$
			} finally {
				if (docZip != null) {
					try {
						docZip.close();
					} catch (IOException e) {
						// ignore
					}
				}
			}
		}

		public void addField(Field f) {
			if (this.fields == null) {
				this.fields = new HashSet<>();
			}
			this.fields.add(f);
		}
		public void addMethod(Method m) {
			if (this.methods == null) {
				this.methods = new HashSet<>();
			}
			this.methods.add(m);
		}
		@Override
		public int compareTo(Type type) {
			return this.getSimpleName().compareTo(type.getSimpleName());
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof Type)) {
				return false;
			}
			Type other = (Type) obj;
			return Arrays.equals(name, other.name);
		}
		public Field getField(String fname) {
			if (this.fields == null) {
				return null;
			}
			Field fieldToFind = new Field(fname.toCharArray(), null);
			for (Field currentField : this.fields) {
				if (fieldToFind.equals(currentField)) {
					return currentField;
				}
			}
			return null;
		}

		public String getFullQualifiedName() {
			return String.valueOf(this.name);
		}
		public String getSuperclassName() {
			if (this.superclassName == null) {
				return null;
			}
			return String.valueOf(this.superclassName);
		}
		public Method getMethod(String selector, String signature) {
			if (this.methods == null) {
				return null;
			}
			Method methodToFind = new Method(0, selector.toCharArray(), signature.toCharArray(), null);
			for (Method currentMethod : this.methods) {
				if (methodToFind.equals(currentMethod)) {
					return currentMethod;
				}
			}
			return null;
		}
		public String getPackage() {
			int index = CharOperation.lastIndexOf('.', this.name);
			return new String(CharOperation.subarray(this.name, 0, index));
		}
		public String getSimpleName() {
			return Util.getSimpleName(this.name);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(name);
			return result;
		}

		public boolean isProtected() {
			return isProtected(this.modifiers);
		}

		public boolean isPublic() {
			return isPublic(this.modifiers);
		}

		public void persistXML(Document document, Element parent, String OSGiProfileName) {
			Element type = document.createElement(IApiXmlConstants.ELEMENT_TYPE);
			parent.appendChild(type);
			type.setAttribute(IApiXmlConstants.ATTR_NAME, getSimpleName());
			if (this.superclassName != null) {
				type.setAttribute(IApiXmlConstants.ATTR_SUPER_CLASS, new String(this.superclassName));
			}
			if (this.superinterfacesNames != null && this.superinterfacesNames.length != 0) {
				type.setAttribute(IApiXmlConstants.ATTR_SUPER_INTERFACES, Util.getInterfaces(this.superinterfacesNames));
			}
			type.setAttribute(IApiXmlConstants.ATTR_INTERFACE, Boolean.toString((this.modifiers & Flags.AccInterface) != 0));
			persistAnnotations(type, OSGiProfileName);
			if (this.fields != null) {
				Field[] allFields = new Field[this.fields.size()];
				this.fields.toArray(allFields);
				Arrays.sort(allFields);
				for (Field allField : allFields) {
					allField.persistXML(document, type, OSGiProfileName);
				}
			}
			if (this.methods != null) {
				Method[] allMethods = new Method[this.methods.size()];
				this.methods.toArray(allMethods);
				Arrays.sort(allMethods);
				for (Method allMethod : allMethods) {
					allMethod.persistXML(document, type, OSGiProfileName);
				}
			}
		}
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append(this.getPackage() + "." + this.getSimpleName()).append(Util.LINE_SEPARATOR); //$NON-NLS-1$
			// list all fields

			if (this.fields != null) {
				Field[] allFields = new Field[this.fields.size()];
				this.fields.toArray(allFields);
				Arrays.sort(allFields);
				for (Field field : allFields) {
					buffer.append("\t") //$NON-NLS-1$
					.append(field).append(Util.LINE_SEPARATOR);
				}
			}
			if (this.methods != null) {
				Method[] allMethods = new Method[this.methods.size()];
				this.methods.toArray(allMethods);
				Arrays.sort(allMethods);
				for (Method method : allMethods) {
					buffer.append("\t").append(method).append(Util.LINE_SEPARATOR); //$NON-NLS-1$
				}
			}
			return String.valueOf(buffer);
		}
	}
	static SortedSet<String> ACCEPTED_EEs;
	static boolean CACHE_ENABLED = true;
	static boolean DEBUG = false;
	static boolean ONLY_USE_CACHE = false;
	static final String PROPERTY_CACHE_LOCATION = ".cacheLocation"; //$NON-NLS-1$
	static final String PROPERTY_DOC_ROOT = ".docRoot"; //$NON-NLS-1$
	static final String PROPERTY_JRE_DOC = ".jreDoc"; //$NON-NLS-1$
	static final String PROPERTY_JRE_LIB = ".jreLib"; //$NON-NLS-1$
	static final String PROPERTY_JRE_URL = ".jreURL"; //$NON-NLS-1$
	static final String PROPERTY_OSGI_PROFILE = ".osgiProfile"; //$NON-NLS-1$
	static final String PROPERTY_WHITE_LIST = ".whiteList"; //$NON-NLS-1$

	static {
		String[] ees = new String[] { "JRE-1.1", //$NON-NLS-1$
				"J2SE-1.2", //$NON-NLS-1$
				"J2SE-1.3", //$NON-NLS-1$
				"J2SE-1.4", //$NON-NLS-1$
				"J2SE-1.5", //$NON-NLS-1$
				"JavaSE-1.6", //$NON-NLS-1$
				"JavaSE-1.7", //$NON-NLS-1$
				"JavaSE-1.8", //$NON-NLS-1$
				"JavaSE-9", //$NON-NLS-1$ ,
				"CDC-1.0_Foundation-1.0", //$NON-NLS-1$
				"CDC-1.1_Foundation-1.1", //$NON-NLS-1$
				"OSGi_Minimum-1.0", //$NON-NLS-1$
				"OSGi_Minimum-1.1", //$NON-NLS-1$
				"OSGi_Minimum-1.2" //$NON-NLS-1$
		};
		ACCEPTED_EEs = new TreeSet<>();
		for (String ee : ees) {
			ACCEPTED_EEs.add(ee);
		}
	}
	private static String getAllEEValues() {
		StringBuilder buffer = new StringBuilder();
		for (String ee : ACCEPTED_EEs) {
			if (buffer.length() != 0) {
				buffer.append(", "); //$NON-NLS-1$
			}
			buffer.append(ee);
		}
		return String.valueOf(buffer);
	}

	public static void main(String[] args) {
		EEGenerator generator = new EEGenerator();
		generator.configure(args);
		if (!generator.isInitialized()) {
			System.err.println("Usage: -output <path to root to output files> -config <path to configuration file> -EEs <list of EE to generate separated with commas>"); //$NON-NLS-1$
			return;
		}
		String property = System.getProperty("DEBUG"); //$NON-NLS-1$
		DEBUG = (property != null) && "true".equalsIgnoreCase(property); //$NON-NLS-1$
		generator.run();
	}
	private ProfileInfo[] allProfiles;
	String configurationFile;

	String[] EEToGenerate;

	String output;

	private boolean checkFileProperty(String property) {
		if (property == null) {
			return false;
		}
		File jreDocFile = new File(property);
		return jreDocFile.exists() && jreDocFile.isFile();
	}

	private boolean checkJREProperty(String property) {
		if (property == null) {
			return false;
		}
		File jreLibFolder = new File(property);
		return jreLibFolder.exists() && jreLibFolder.isDirectory();
	}

	private void configure(String[] args) {
		String currentArg = null;
		int argCount = args.length;
		int index = -1;
		final int DEFAULT = 0;
		final int OUTPUT = 1;
		final int CONFIG = 2;
		final int EEs = 3;
		int mode = DEFAULT;
		while (++index < argCount) {
			currentArg = args[index];
			switch (mode) {
				case DEFAULT:
					if ("-output".equals(currentArg)) { //$NON-NLS-1$
						if (this.output != null)
						 {
							throw new IllegalArgumentException("output value is already set"); //$NON-NLS-1$
						}
						mode = OUTPUT;
						continue;
					}
					if ("-config".equals(currentArg)) { //$NON-NLS-1$
						if (this.configurationFile != null)
						 {
							throw new IllegalArgumentException("configuration value is already set"); //$NON-NLS-1$
						}
						mode = CONFIG;
						continue;
					}
					if ("-EEs".equals(currentArg)) { //$NON-NLS-1$
						if (this.EEToGenerate != null)
						 {
							throw new IllegalArgumentException("EEs value is already set"); //$NON-NLS-1$
						}
						mode = EEs;
						continue;
					}
					// ignore unknown arguments - might be passed in by the
					// Eclipse application
					continue;
				case OUTPUT:
					this.output = currentArg;
					mode = DEFAULT;
					continue;
				case CONFIG:
					this.configurationFile = currentArg;
					mode = DEFAULT;
					continue;
				case EEs:
					String listOfEEs = currentArg;
					StringTokenizer tokenizer = new StringTokenizer(listOfEEs, ","); //$NON-NLS-1$
				List<String> list = new ArrayList<>();
					while (tokenizer.hasMoreTokens()) {
						String currentEE = tokenizer.nextToken().trim();
						if (ACCEPTED_EEs.contains(currentEE)) {
							list.add(currentEE);
						} else {
							throw new IllegalArgumentException("Wrong EE value: " + currentEE + " accepted values are: " + getAllEEValues()); //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					if (!list.isEmpty()) {
						list.toArray(this.EEToGenerate = new String[list.size()]);
					}
					mode = DEFAULT;
					continue;
				default:
					break;
			}
		}
		if (this.output == null) {
			throw new IllegalArgumentException("output value is missing"); //$NON-NLS-1$
		}
		// check output
		File file = new File(this.output);
		if (!file.exists()) {
			if (!file.mkdirs()) {
				this.output = null;
				throw new IllegalArgumentException("Could not create the output dir"); //$NON-NLS-1$
			}
		}
		// check configuration file
		File configuration = new File(this.configurationFile);
		if (!configuration.exists()) {
			this.configurationFile = null;
			throw new IllegalArgumentException("Configuration file doesn't exist"); //$NON-NLS-1$
		}
		Properties properties = new Properties();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(configuration));
			properties.load(reader);
		} catch (IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Could not properly initialize the properties"); //$NON-NLS-1$
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		List<ProfileInfo> infos = new ArrayList<>();
		for (String EE : EEToGenerate) {
			// Retrieve all properties for each EE
			// JRELIB, OSGI_PROFILE, JRE_DOC, JRE_URL, CACHE, WHITE_LIST
			String key = EE + PROPERTY_JRE_LIB;
			String jreLibProperty = properties.getProperty(key, null);
			if (!checkJREProperty(jreLibProperty)) {
				throw new IllegalArgumentException("Wrong property value : " + key); //$NON-NLS-1$
			}
			key = EE + PROPERTY_CACHE_LOCATION;
			String cacheLocationProperty = properties.getProperty(key, null);
			if (cacheLocationProperty != null) {
				if (cacheLocationProperty.isEmpty()) {
					cacheLocationProperty = null;
				}
			}
			key = EE + PROPERTY_DOC_ROOT;
			String docRootProperty = properties.getProperty(key, ""); //$NON-NLS-1$
			key = EE + PROPERTY_JRE_DOC;
			String jreDocProperty = properties.getProperty(key, null);
			if (jreDocProperty != null && !jreDocProperty.isEmpty()) {
				if (!checkFileProperty(jreDocProperty)) {
					throw new IllegalArgumentException("Wrong property value : " + key); //$NON-NLS-1$
				}
			} else {
				jreDocProperty = null;
			}
			key = EE + PROPERTY_JRE_URL;
			String jreUrlProperty = properties.getProperty(key, null);
			if (jreUrlProperty != null && !jreUrlProperty.isEmpty()) {
				if (Util.getURLContents(jreUrlProperty + docRootProperty + "java/lang/Object.html") == null) { //$NON-NLS-1$
					throw new IllegalArgumentException("Wrong property value : " + key); //$NON-NLS-1$
				}
			} else {
				jreUrlProperty = null;
			}
			key = EE + PROPERTY_OSGI_PROFILE;
			String osgiProfileProperty = properties.getProperty(key, null);
			if (osgiProfileProperty != null && !osgiProfileProperty.isEmpty()) {
				if (!checkFileProperty(osgiProfileProperty)) {
					throw new IllegalArgumentException("Wrong property value : " + key); //$NON-NLS-1$
				}
			} else {
				osgiProfileProperty = null;
			}
			key = EE + PROPERTY_WHITE_LIST;
			String whiteListProperty = properties.getProperty(key, null);
			if (whiteListProperty != null && !whiteListProperty.isEmpty()) {
				if (!checkFileProperty(whiteListProperty)) {
					throw new IllegalArgumentException("Wrong property value : " + key); //$NON-NLS-1$
				}
			} else {
				whiteListProperty = null;
			}
			infos.add(ProfileInfo.getProfileInfo(EE, jreLibProperty, osgiProfileProperty, jreDocProperty, jreUrlProperty, docRootProperty, cacheLocationProperty, whiteListProperty));
		}
		if (infos.isEmpty()) {
			throw new IllegalArgumentException("Profile infos cannot be empty"); //$NON-NLS-1$
		}
		infos.toArray(allProfiles = new ProfileInfo[infos.size()]);
	}

	private boolean isInitialized() {
		return this.configurationFile != null && this.EEToGenerate != null && this.output != null;
	}

	private void run() {
		if (allProfiles == null) {
			System.err.println("No descriptions to generate"); //$NON-NLS-1$
			return;
		}
		int numberOfProfiles = allProfiles.length;
		for (int i = 0; i < numberOfProfiles; i++) {
			ProfileInfo profileInfo = allProfiles[i];
			if (profileInfo != null) {
				try {
					profileInfo.initializeData();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// persist the EE description
				profileInfo.generateEEDescription(this.output);
			}
		}
	}
}
