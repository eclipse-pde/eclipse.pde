/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.builders;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class BundleErrorReporter extends JarManifestErrorReporter {
	
	/**
	 * @param versionString
	 *            the version to be checked, null is allowed and will be treated
	 *            as 0.0.0
	 * @return IStatus
	 */
	protected static IStatus validateVersionString(String versionString) {
		if (versionString == null)
			return Status.OK_STATUS;
		return PluginVersionIdentifier.validateVersion(versionString);
	}

	protected static IStatus validateVersionRange(String versionRangeString) {
		try {
			new VersionRange(versionRangeString);
		} catch (IllegalArgumentException e) {
			return new Status(IStatus.ERROR, PDE.PLUGIN_ID, IStatus.ERROR, 
					PDE
					.getResourceString("BundleErrorReporter.invalidVersionRangeFormat"), e); //$NON-NLS-1$
		}

		// need to do our extra checks for each piece of the versionRange
		int comma = versionRangeString.indexOf(',');
		if (comma < 0) {
			return validateVersionString(versionRangeString);
		}

		IStatus status = validateVersionString(versionRangeString.substring(1, comma));
		if(!status.isOK()){
			return status;
		}
		return validateVersionString(versionRangeString
				.substring(comma + 1, versionRangeString.length() - 1));
	}

	private boolean fEclipse3_1;
	
	private boolean fHasExtensibleApi = false;

	private boolean fFragment;

	private Map fFragmentsPackagesMap = null;

	private Map fHostPackagesMap = null;

	private boolean fHasFragment_Xml;

	private boolean fHasExtensions;

	private String fHostBundleId;

	// private Vector fIimportPkgs;
	//
	private String fPluginId = ""; //$NON-NLS-1$

	private Map fProjectPackagesMap = null;

	public BundleErrorReporter(IFile file) {
		super(file);
	}

	/**
	 * Adds IPackageFragment from a project to a map
	 */
	private void addProjectPackages(Map map, IProject proj) {
		IJavaProject jp = JavaCore.create(proj);
		try {
			IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].isArchive() && !roots[i].isExternal())) {
					IJavaElement[] children = roots[i].getChildren();
					for (int j = 0; j < children.length; j++) {
						IPackageFragment f = (IPackageFragment) children[j];
						if (f.hasChildren())
							map.put(f.getElementName(), f);
					}
				}
			}
		} catch (JavaModelException e) {
		}
	}

	/**
	 * @return Map of IPluginModelBase not including current plug-in
	 */
	private HashMap getAvailableBundles() {
		HashMap map = new HashMap();
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager()
				.getPlugins();

		for (int i = 0; i < plugins.length; i++) {
			IPluginBase element = plugins[i].getPluginBase();
			if ((element.getId() != null) && !element.getId().equals(fPluginId)) {
				// filter out the current project
				map.put(element.getId(), plugins[i]);
			}
		}
		return map;
	}

	/**
	 * @return Map of ExportPackageDescription by String
	 */
	private HashMap getAvailableExportedPackages() {
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager()
				.getPlugins();

		HashMap map = new HashMap();
		for (int i = 0; i < plugins.length; i++) {
			if ((plugins[i].getPluginBase().getId() != null)
					&& !(plugins[i].getPluginBase().getId()).equals(fPluginId)) {
				BundleDescription bd = plugins[i].getBundleDescription();
				if (bd != null) {
					ExportPackageDescription[] elements = bd
							.getExportPackages();
					if (elements != null) {
						for (int j = 0; j < elements.length; j++) {
							map.put(elements[j].getName(), elements[j]);
						}
					}
				}
			}
		}
		return map;
	}

	/**
	 * @return Map of IPackageFragment from corresponding fragment projects
	 */
	private Map getFragmentsPackages() {
		if (fFragmentsPackagesMap == null) {
			Map map = new HashMap();
			IFragmentModel[] models = PDECore.getDefault().getModelManager()
					.getFragments();
			for (int i = 0; i < models.length; i++) {
				String hostId = models[i].getFragment().getPluginId();
				if (!fPluginId.equals(hostId)) {
					continue;
				}
				IResource resource = models[i].getUnderlyingResource();
				if (resource != null) {
					addProjectPackages(map, resource.getProject());

				}
			}
			fFragmentsPackagesMap = map;
		}
		return fFragmentsPackagesMap;
	}

	/**
	 * @return Map of IPackageFragment from current project
	 */
	private Map getHostPackages() {
		if (fHostPackagesMap == null) {
			Map map = new HashMap();
			if (fHostBundleId != null) {
				IPluginModel model = PDECore.getDefault().getModelManager()
						.findPluginModel(fHostBundleId);
				if (model == null) {
					return map;
				}
				IResource resource = model.getUnderlyingResource();
				if (resource != null) {
                    addProjectPackages(map, resource.getProject());
                } else {
                    try {
                        IPackageFragment[] packages = PluginJavaSearchUtil
                                .collectPackageFragments(
                                        new IPluginBase[] { model
                                                .getPluginBase() }, JavaCore
                                                .create(fProject));
                        for (int i = 0; i < packages.length; i++)
                            fHostPackagesMap.put(packages[i].getElementName(),
                                    packages[i]);
                    } catch (JavaModelException jme) {
                        PDE.log(jme);
                    }
                }
			}
			fHostPackagesMap = map;
		}
		return fHostPackagesMap;
	}

	private int getPackageLine(IHeader header, String packageName) {
		// first check for this exact package on the last line
		try {
			IRegion lineRegion = fTextDocument.getLineInformation(header
					.getLineNumber()
					+ header.getLinesSpan() - 1);
			String lineStr = fTextDocument.get(lineRegion.getOffset(),
					lineRegion.getLength());
			if (lineStr.endsWith(packageName)) {
				return header.getLineNumber() + header.getLinesSpan();
			}
		} catch (BadLocationException ble) {
			PDECore.logException(ble);
		}

		// search all except last entries
		return Math
				.max(
						getLine(header, packageName + ","), getLine(header, packageName + ";")); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * @return Map of IPackageFragment from current project
	 */
	private Map getProjectPackages() {
		if (fProjectPackagesMap == null) {
			Map map = new HashMap();
			addProjectPackages(map, fProject);
			fProjectPackagesMap = map;
		}
		return fProjectPackagesMap;
	}

	protected boolean isCheckDeprecated() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckNoRequiredAttr() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_NO_REQUIRED_ATT) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckUnknownAttr() {
		return CompilerFlags.getFlag(fProject,
				CompilerFlags.P_UNKNOWN_ATTRIBUTE) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckUnknownClass() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_CLASS) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckUnresolvedImports() {
		return CompilerFlags.getFlag(fProject,
				CompilerFlags.P_UNRESOLVED_IMPORTS) != CompilerFlags.IGNORE;
	}

//	 /**
//	 * @return true if the given file exists in the project
//	 */
//	 private boolean isFileExist(String fileName) {
//	 IResource member = fProject.findMember(fileName);
//	 if (member != null) {
//	 if ((member instanceof IFile) && (member.exists()))
//	 return true;
//	 }
//	
//	 return false;
//	 }

	private void readBundleManifestVersion() {
		IHeader header = (IHeader) fHeaders
				.get(Constants.BUNDLE_MANIFESTVERSION);
		if (header == null) {
			return;
		}
		Version v = new Version(header.getValue());
		if (v.getMajor() >= 2) {
			fEclipse3_1 = true;
		}
	}

	private void validateBundleActivator() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_ACTIVATOR);
		if (header == null) {
			return;
		}
		String activator = header.getValue();
		String message;
		if (fFragment) {
			/* Fragment bundles must not specify a Bundle Activator */
			message = PDE
					.getResourceString("BundleErrorReporter.fragmentActivator"); //$NON-NLS-1$
			report(message, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return;
		}
		if (isCheckUnknownClass()) {
			IJavaProject javaProject = JavaCore.create(fProject);
			try {
				// Look for this activator in the project's classpath
				IType type = javaProject.findType(activator);

				/* Activator type does not exist */
				if (type == null || !type.exists()) {
					message = PDE.getFormattedMessage(
							"BundleErrorReporter.NoExist", activator); //$NON-NLS-1$
					report(message, getLine(header, activator),
							CompilerFlags.P_UNKNOWN_CLASS);
					return;
				}
			} catch (JavaModelException e) {
				PDECore.logException(e);
			}
		}
	}

	private void validateBundleClasspath() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_CLASSPATH);
		if (header == null) {
			return;
		}
		String classpath = header.getValue();

		String message = null;
		if (classpath.trim().length() == 0) {
			/* It is defined but it is an empty string */
			message = PDE
					.getResourceString("BundleErrorReporter.ClasspathNotEmpty"); //$NON-NLS-1$
			report(message, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return;
		}

		ManifestElement[] elements = header.getElements();
		if (elements.length == 0) {
			return;
		}
	}

	/**
	 * @return boolean false if fatal
	 */
	private boolean validateBundleSymbolicName() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_SYMBOLICNAME);
		String message;
		if (header == null) {
			report(
					PDE
							.getFormattedMessage(
									"BundleErrorReporter.headerMissing", Constants.BUNDLE_SYMBOLICNAME), 1, //$NON-NLS-1$
					CompilerFlags.ERROR);
			return false;
		}
		String symbolicName = header.getValue();
		if ((symbolicName.trim()).length() == 0) {
			message = PDE
					.getResourceString("BundleErrorReporter.NoSymbolicName"); //$NON-NLS-1$
			report(message, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return false;
		}
		ManifestElement[] elements = header.getElements();
		if (elements.length == 0) {
			return false;
		}
		fPluginId = elements[0].getValue();

		validatePluginId(header, fPluginId);

		validateSingletonAttribute(header, elements[0]);
		validateSingletonDirective(header, elements[0]);

		return true;
	}

	private void validateBundleVersion() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_VERSION);
		if (header == null) {
			report(
					PDE
							.getFormattedMessage(
									"BundleErrorReporter.headerMissing", Constants.BUNDLE_VERSION), 1, //$NON-NLS-1$
					CompilerFlags.ERROR);
			return;
		}
		String version = header.getValue();
		IStatus status = validateVersionString(version);
		if(!status.isOK()){
			int line = getLine(header, version);
			report(status.getMessage(), line, CompilerFlags.ERROR);
		}
	}

	private void validateBundleVersionAttribute(IHeader header,
			ManifestElement element) {
		String versionRange = element
				.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (versionRange != null && !validateVersionRange(versionRange).isOK()) {
			String message = PDE
					.getFormattedMessage(
							"BundleErrorReporter.InvalidFormatInBundleVersion", element.getValue()); //$NON-NLS-1$
			report(message, getPackageLine(header, element.getValue()),
					CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}

	public void validateContent(IProgressMonitor monitor) {
		super.validateContent(monitor);
		if (fHeaders == null || getErrorCount() > 0) {
			return;
		}

		readBundleManifestVersion();
		fHasFragment_Xml = fProject.getFile("fragment.xml").exists(); //$NON-NLS-1$

		// sets fPluginId
		if (!validateBundleSymbolicName()) {
			return;
		}
		IPluginModelBase modelBase = PDECore.getDefault().getModelManager()
				.findModel(fProject);
		if (modelBase != null) {
			fHasExtensions = modelBase.getPluginBase().getExtensionPoints().length > 0
					|| modelBase.getPluginBase().getExtensions().length > 0;
		}

		validateBundleVersion();
		// sets fExtensibleApi
		validateExtensibleAPI();
		// sets fHostBundleId
		validateFragmentHost();
		validateBundleClasspath();
		validateBundleActivator();
		validateRequireBundle();
		validateExportPackage();
		validateProvidePackage();
		validateImportPackage();
		// validateNativeCode();
	}

	private void validateExportPackage() {
		IHeader header = (IHeader) fHeaders.get(Constants.EXPORT_PACKAGE);
		if (header == null) {
			return;
		}
		String message = null;
		ManifestElement[] exportPackageElements = header.getElements();

		for (int i = 0; i < exportPackageElements.length; i++) {
			String exportPackageStmt = exportPackageElements[i].getValue();

			validateVersionAttribute(header, exportPackageElements[i]);

			validateSpecificationVersionAttribute(header,
					exportPackageElements[i]);

			if (!isCheckUnresolvedImports()) {
				continue;
			}
			IPackageFragment f = (IPackageFragment) getProjectPackages().get(
					exportPackageStmt);
			/* The exported package cannot be default package. */
			if (f != null && f.isDefaultPackage()) {
				message = PDE
						.getResourceString("BundleErrorReporter.CannotExportDefaultPackage"); //$NON-NLS-1$
				report(message, getPackageLine(header, exportPackageStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
				continue;
			}

			/* The exported package does not exist in the bundle */
			if (!getProjectPackages().containsKey(exportPackageStmt)) {
				if (!(getHostPackages().containsKey(exportPackageStmt) || fHasExtensibleApi
						&& getFragmentsPackages()
								.containsKey(exportPackageStmt))) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.NotExistInProject", exportPackageStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, exportPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
					continue;
				}
			}

		}
	}

	private void validateExtensibleAPI(){
		IHeader header = (IHeader) fHeaders.get(ICoreConstants.EXTENSIBLE_API);
		if(header==null){
			return;
		}
		validateBooleanValue(header);
		
		fHasExtensibleApi = "true".equals(header.getValue()); //$NON-NLS-1$
	}
	
	private void validateFragmentHost() {
		IHeader header = (IHeader) fHeaders.get(Constants.FRAGMENT_HOST);
		String message;
		if (header == null) {
			if (isCheckNoRequiredAttr() && fHasFragment_Xml) { //$NON-NLS-1$
				message = PDE
						.getResourceString("BundleErrorReporter.HostNeeded"); //$NON-NLS-1$
				report(message, 1, CompilerFlags.P_NO_REQUIRED_ATT);
			}
			return;
		}

		fFragment = true;
		ManifestElement[] fragmentHostElements = header.getElements();
		if (isCheckNoRequiredAttr() && fragmentHostElements.length == 0) {
			message = PDE.getResourceString("BundleErrorReporter.HostNeeded"); //$NON-NLS-1$
			report(message, 1, CompilerFlags.P_NO_REQUIRED_ATT);
			return;
		}

		String fragmentHostStmt = fragmentHostElements[0].getValue();
		if (!validatePluginId(header, fragmentHostStmt)) {
			return;
		}

		validateBundleVersionAttribute(header, fragmentHostElements[0]);

		if (isCheckUnresolvedImports()) {
			HashMap availableBundlesMap = getAvailableBundles();
			IPluginModelBase availableModel = (IPluginModelBase) availableBundlesMap
					.get(fragmentHostStmt);
			if (availableModel == null || !availableModel.isEnabled()) {
				/*
				 * Host bundle does not exist in the PDE target platform.
				 */
				message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.HostNotExistPDE", fragmentHostStmt); //$NON-NLS-1$
				report(message, getLine(header, fragmentHostStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS);
				return;
			}
			if (availableModel instanceof IFragmentModel) {
				/* The host is a fragment */
				message = PDE.getFormattedMessage(
						"BundleErrorReporter.HostIsFragment", fragmentHostStmt); //$NON-NLS-1$
				report(message, getLine(header, fragmentHostStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS);
				return;
			}
			String availableVersion = availableModel.getPluginBase()
					.getVersion();
			String requiredVersionRange = fragmentHostElements[0]
					.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (requiredVersionRange != null
					&& validateVersionRange(requiredVersionRange).isOK()) {
				VersionRange versionRange = new VersionRange(
						requiredVersionRange);
				if (!versionRange.isIncluded(new Version(availableVersion))) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.BundleRangeInvalidInBundleVersion", fragmentHostStmt); //$NON-NLS-1$
					report(message, getLine(header, requiredVersionRange),
							CompilerFlags.P_UNRESOLVED_IMPORTS);
				}
			}
			// save for fragment host id
			fHostBundleId = fragmentHostStmt;

		}
	}

	private void validateImportPackage() {
		IHeader header = (IHeader) fHeaders.get(Constants.IMPORT_PACKAGE);
		if (header == null) {
			return;
		}
		String message = null;
		HashMap availableExportedPackagesMap = getAvailableExportedPackages();

		ManifestElement[] importPackageElements = header.getElements();
		for (int i = 0; i < importPackageElements.length; i++) {

			validateSpecificationVersionAttribute(header,
					importPackageElements[i]);
			validateVersionAttribute(header, importPackageElements[i]);

			validateResolutionDirective(header, importPackageElements[i]);

			String importPackageStmt = importPackageElements[i].getValue();

			if (!isCheckUnresolvedImports()) {
				continue;
			}
			if (!availableExportedPackagesMap.containsKey(importPackageStmt)) {
				/* No bundle exports this package */
				message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.PackageNotExported", importPackageStmt); //$NON-NLS-1$
				report(message, getPackageLine(header, importPackageStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS);
				continue;
			}

			String requiredVersion = importPackageElements[i]
					.getAttribute(Constants.VERSION_ATTRIBUTE);
			if (requiredVersion != null && validateVersionRange(requiredVersion).isOK()) {
				VersionRange range = new VersionRange(requiredVersion);
				ExportPackageDescription epd = (ExportPackageDescription) availableExportedPackagesMap
						.get(importPackageStmt);
				if (epd.getVersion() != null
						&& !range.isIncluded(epd.getVersion())) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.VersionNotInRange", new String[] { importPackageStmt, requiredVersion }); //$NON-NLS-1$
					report(message, getPackageLine(header, importPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS);
					continue;
				}
			}
		}
	}

//	 private void validateNativeCode() {
//		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_NATIVECODE);
//		if (header == null) {
//			return;
//		}
//		String nativeCode = header.getValue();
//		if (nativeCode == null) {
//			return;
//		}
//		String message = null;
//
//		ManifestElement[] nativeCodeElements = header.getElements();
//		for (int i = 0; i < nativeCodeElements.length; i++) {
//			String fileNames = nativeCodeElements[i].getValue();
//			// Parse the file names
//			StringTokenizer st = new StringTokenizer(fileNames, ";"); //$NON-NLS-1$
//			String filesErrorMsg = ""; //$NON-NLS-1$
//			while (st.hasMoreTokens()) {
//				String name = st.nextToken();
//				if (!filesErrorMsg.equals("")) //$NON-NLS-1$
//					filesErrorMsg += ","; //$NON-NLS-1$
//				filesErrorMsg += name;
//
//				if (!isFileExist(name)) {
//					// File does not exist.
//					message = PDE.getFormattedMessage(
//							"BundleErrorReporter.FileNotExist", name); //$NON-NLS-1$
//					report(message, getLine(header, name),
//							CompilerFlags.P_UNKNOWN_RESOURCE);
//				}
//			}
//
//			String[] processors = nativeCodeElements[i]
//					.getAttributes(Constants.BUNDLE_NATIVECODE_PROCESSOR);
//			if ((processors == null) || (processors.length == 0)) {
//				// No processor settings
//				message = PDE.getFormattedMessage(
//						"BundleErrorReporter.NativeNoProcessor", filesErrorMsg); //$NON-NLS-1$
//				report(message, header.getLineNumber() + 1,
//						CompilerFlags.P_NO_REQUIRED_ATT);
//			} else {
//				HashSet set = new HashSet(Arrays
//						.asList(NativeCodeAttributeValues.PROCESSOR_TYPES));
//				set
//						.addAll(Arrays
//								.asList(NativeCodeAttributeValues.ADDITIONAL_PROCESSOR_ALIASES));
//				for (int j = 0; j < processors.length; j++) {
//					if (!set.contains(processors[j])) {
//						// Processor is unrecognized
//						message = PDE
//								.getFormattedMessage(
//										"BundleErrorReporter.NativeInvalidProcessor", processors[j]); //$NON-NLS-1$
//						report(message, getLine(header, processors[j]),
//								CompilerFlags.P_UNKNOWN_ATTRIBUTE);
//					}
//				}
//			}
//
//			String[] osNames = nativeCodeElements[i]
//					.getAttributes(Constants.BUNDLE_NATIVECODE_OSNAME);
//			if ((osNames == null) || (osNames.length == 0)) {
//				// No OS settings
//				message = PDE.getFormattedMessage(
//						"BundleErrorReporter.NativeNoOSName", filesErrorMsg); //$NON-NLS-1$
//				report(message, header.getLineNumber() + 1,
//						CompilerFlags.P_NO_REQUIRED_ATT);
//			} else {
//				HashSet set = new HashSet(Arrays
//						.asList(NativeCodeAttributeValues.OS_TYPES));
//				set
//						.addAll(Arrays
//								.asList(NativeCodeAttributeValues.ADDITIONAL_OS_ALIASES));
//				for (int j = 0; j < osNames.length; j++) {
//					if (!set.contains(osNames[j])) {
//						// OS name is unrecognized
//						message = PDE
//								.getFormattedMessage(
//										"BundleErrorReporter.NativeInvalidOSName", osNames[j]); //$NON-NLS-1$
//						report(message, getLine(header, osNames[j]),
//								CompilerFlags.P_UNKNOWN_ATTRIBUTE);
//
//					}
//				}
//			}
//
//			String osVersion = nativeCodeElements[i]
//					.getAttribute(Constants.BUNDLE_NATIVECODE_OSVERSION);
//			if (osVersion != null) {
//				// version is in wrong format
//				if (!isValidVersionRange(osVersion)) {
//					message = PDE
//							.getFormattedMessage(
//									"BundleErrorReporter.NativeInvalidOSVersion", osVersion); //$NON-NLS-1$
//					report(message, getLine(header, osVersion),
//							CompilerFlags.P_UNKNOWN_ATTRIBUTE);
//				}
//			}
//
//			String filter = nativeCodeElements[i]
//					.getAttribute(Constants.SELECTION_FILTER_ATTRIBUTE);
//			if (filter != null) {
//				BundleContext context = PDE.getDefault().getBundleContext();
//				try {
//					context.createFilter(filter);
//				} catch (InvalidSyntaxException e) {
//					// selection filter is in a wrong format
//					String[] msg = new String[2];
//					msg[0] = filter;
//					msg[1] = e.getMessage();
//					message = PDE.getFormattedMessage(
//							"BundleErrorReporter.NativeInvalidFilter", msg); //$NON-NLS-1$
//					report(message, getLine(header, filter),
//							CompilerFlags.P_UNKNOWN_ATTRIBUTE);
//				}
//			}
//
//			String[] lang = nativeCodeElements[i]
//					.getAttributes(Constants.BUNDLE_NATIVECODE_LANGUAGE);
//			if ((lang != null) && (lang.length > 0)) {
//				HashSet set = new HashSet();
//				for (int k = 0; k < NativeCodeAttributeValues.LANGUAGES.length; k++) {
//					set.add(NativeCodeAttributeValues.LANGUAGES[k][1]);
//				}
//				for (int j = 0; j < lang.length; j++) {
//					if (!set.contains(lang[j])) {
//						// Language is unrecognized
//						message = PDE
//								.getFormattedMessage(
//										"BundleErrorReporter.NativeInvalidLanguage", lang[j]); //$NON-NLS-1$
//						report(message, getLine(header, lang[i]),
//								CompilerFlags.P_UNKNOWN_ATTRIBUTE);
//					}
//				}
//			}
//		}
//	}

	private void validateOptionalAttribute(IHeader header,
			ManifestElement requireBundleElements) {
		String message;
		String rexport = requireBundleElements
				.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, requireBundleElements,
					ICoreConstants.OPTIONAL_ATTRIBUTE);
			if (fEclipse3_1 && isCheckDeprecated()) {
				message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.deprecated-attribute", ICoreConstants.SINGLETON_ATTRIBUTE); //$NON-NLS-1$
				report(
						message,
						getLine(header, ICoreConstants.OPTIONAL_ATTRIBUTE + "="), CompilerFlags.P_DEPRECATED); //$NON-NLS-1$
			}
		}
	}

	private boolean validatePluginId(IHeader header, String value) {
		String message;
		if (!IdUtil.isValidPluginId(value)) {
			message = PDE
					.getResourceString("BundleErrorReporter.InvalidSymbolicName"); //$NON-NLS-1$
			report(message, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return false;
		}
		return true;
	}

	private void validateProvidePackage() {
		IHeader header = (IHeader) fHeaders.get(ICoreConstants.PROVIDE_PACKAGE);
		if (header == null) {
			return;
		}
		String message = null;
		if (fEclipse3_1 && isCheckDeprecated()) {
			message = PDE
					.getFormattedMessage(
							"BundleErrorReporter.deprecated-header", ICoreConstants.PROVIDE_PACKAGE); //$NON-NLS-1$
			report(message, header.getLineNumber() + 1,
					CompilerFlags.P_DEPRECATED);
		}
		ManifestElement[] exportPackageElements = header.getElements();

		for (int i = 0; i < exportPackageElements.length; i++) {
			String exportPackageStmt = exportPackageElements[i].getValue();

			validateSpecificationVersionAttribute(header,
					exportPackageElements[i]);

			if (!isCheckUnresolvedImports()) {
				continue;
			}
			IPackageFragment f = (IPackageFragment) getProjectPackages().get(
					exportPackageStmt);
			/* The exported package cannot be default package. */
			if (f != null && f.isDefaultPackage()) {
				message = PDE
						.getResourceString("BundleErrorReporter.CannotExportDefaultPackage"); //$NON-NLS-1$
				report(message, getPackageLine(header, exportPackageStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
				continue;
			}

			/* The exported package does not exist in the bundle */
			if (!getProjectPackages().containsKey(exportPackageStmt)) {
				if (!(getHostPackages().containsKey(exportPackageStmt) || fHasExtensibleApi
						&& getFragmentsPackages()
								.containsKey(exportPackageStmt))) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.NotExistInProject", exportPackageStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, exportPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
					continue;
				}
			}

		}
	}

	private void validateReprovideAttribute(IHeader header,
			ManifestElement requireBundleElements) {
		String message;
		String rexport = requireBundleElements
				.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, requireBundleElements,
					ICoreConstants.REPROVIDE_ATTRIBUTE);
			if (fEclipse3_1 && isCheckDeprecated()) {
				message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.deprecated-attribute", ICoreConstants.REPROVIDE_ATTRIBUTE); //$NON-NLS-1$
				report(message,
						getLine(header, ICoreConstants.REPROVIDE_ATTRIBUTE
								+ "="), CompilerFlags.P_DEPRECATED); //$NON-NLS-1$
			}
		}
	}

	private void validateRequireBundle() {
		IHeader header = (IHeader) fHeaders.get(Constants.REQUIRE_BUNDLE);
		if (header == null) {
			return;
		}
		String message = null;
		HashMap availableBundlesMap = getAvailableBundles();

		ManifestElement[] requireBundleElements = header.getElements();
		for (int i = 0; i < requireBundleElements.length; i++) {
			String requireBundleStmt = requireBundleElements[i].getValue();

			validateBundleVersionAttribute(header, requireBundleElements[i]);

			validateVisibilityDirective(header, requireBundleElements[i]);

			validateReprovideAttribute(header, requireBundleElements[i]);

			validateResolutionDirective(header, requireBundleElements[i]);

			validateOptionalAttribute(header, requireBundleElements[i]);

			if (!isCheckUnresolvedImports()) {
				return;
			}
			/* This id does not exist in the PDE target platform */
			if (!availableBundlesMap.containsKey(requireBundleStmt)) {
				message = PDE.getFormattedMessage(
						"BundleErrorReporter.NotExistPDE", requireBundleStmt); //$NON-NLS-1$
				report(message, getPackageLine(header, requireBundleStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS);
				continue;
			}
			IPluginModelBase availableModel = (IPluginModelBase) availableBundlesMap
					.get(requireBundleStmt);
			if (!(availableModel instanceof IPluginModel)) {
				/* This is a fragment */
				message = PDE.getFormattedMessage(
						"BundleErrorReporter.IsFragment", requireBundleStmt); //$NON-NLS-1$
				report(message, getPackageLine(header, requireBundleStmt),
						CompilerFlags.P_UNRESOLVED_IMPORTS);
				continue;
			}
			String requiredVersionRange = requireBundleElements[i]
					.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (requiredVersionRange != null
					&& validateVersionRange(requiredVersionRange).isOK()) {
				VersionRange versionRange = new VersionRange(
						requiredVersionRange);
				String availableVersion = availableModel.getPluginBase()
						.getVersion();
				if (!versionRange.isIncluded(new Version(availableVersion))) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.BundleRangeInvalidInBundleVersion", requireBundleStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, requireBundleStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS);
				}

			}
		}
	}

	private void validateResolutionDirective(IHeader header,
			ManifestElement requireBundleElement) {
		String resolution = requireBundleElement
				.getDirective(Constants.RESOLUTION_DIRECTIVE);
		if (resolution != null) {
			validateDirectiveValue(header, requireBundleElement,
					Constants.RESOLUTION_DIRECTIVE, new String[] {
							Constants.RESOLUTION_MANDATORY,
							Constants.RESOLUTION_OPTIONAL });
		}
	}

	private void validateSingletonAttribute(IHeader header,
			ManifestElement element) {
		String message;
		String singletonAttr = element
				.getAttribute(ICoreConstants.SINGLETON_ATTRIBUTE);
		if (fHasExtensions) {
			if (!fEclipse3_1) {
				if (!"true".equals(singletonAttr)) { //$NON-NLS-1$
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.singletonAttrRequired", ICoreConstants.SINGLETON_ATTRIBUTE); //$NON-NLS-1$
					report(message, header.getLineNumber() + 1,
							CompilerFlags.ERROR);
				}
			}
		}
		if (isCheckDeprecated()) {
			if (fEclipse3_1 && singletonAttr != null) {
				message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.deprecated-attribute", ICoreConstants.SINGLETON_ATTRIBUTE); //$NON-NLS-1$
				report(message,
						getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE
								+ "="), CompilerFlags.P_DEPRECATED); //$NON-NLS-1$
			}
		}
		validateBooleanAttributeValue(header, element,
				ICoreConstants.SINGLETON_ATTRIBUTE);
	}

	private void validateSingletonDirective(IHeader header,
			ManifestElement element) {
		String singletonDir = element
				.getDirective(Constants.SINGLETON_DIRECTIVE);
		if (fHasExtensions) {
			if (fEclipse3_1) {
				if (!"true".equals(singletonDir)) { //$NON-NLS-1$
					String message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.singletonRequired", Constants.SINGLETON_DIRECTIVE); //$NON-NLS-1$
					report(message, header.getLineNumber() + 1,
							CompilerFlags.ERROR);
				}

			}
		}
		if (isCheckUnknownAttr()) {
			if (!fEclipse3_1 && singletonDir != null) {
				String message = PDE
						.getFormattedMessage(
								"BundleErrorReporter.UnknownDirective", Constants.SINGLETON_DIRECTIVE); //$NON-NLS-1$
				report(message, getLine(header, Constants.SINGLETON_DIRECTIVE
						+ ":="), //$NON-NLS-1$
						CompilerFlags.P_UNKNOWN_ATTRIBUTE);
			}
		}
		validateBooleanDirectiveValue(header, element,
				Constants.SINGLETON_DIRECTIVE);
	}

	private void validateSpecificationVersionAttribute(IHeader header,
			ManifestElement element) {
		String version = element
				.getAttribute(ICoreConstants.PACKAGE_SPECIFICATION_VERSION);
		IStatus status = validateVersionString(version);
		if(!status.isOK()){
			report(status.getMessage(), getPackageLine(header, element.getValue()),
					CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}

	private void validateVersionAttribute(IHeader header,
			ManifestElement element) {
		String versionRange = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		if (versionRange == null)
			return;
		IStatus status =validateVersionRange(versionRange);
		if(!status.isOK()) {
			report(status.getMessage(), getPackageLine(header, element.getValue()),
					CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}

	private void validateVisibilityDirective(IHeader header,
			ManifestElement requireBundleElement) {
		String visibility = requireBundleElement
				.getDirective(Constants.VISIBILITY_DIRECTIVE);
		if (visibility != null) {
			validateDirectiveValue(header, requireBundleElement,
					Constants.VISIBILITY_DIRECTIVE, new String[] {
							Constants.VISIBILITY_PRIVATE,
							Constants.VISIBILITY_REEXPORT });
		}
	}

}
