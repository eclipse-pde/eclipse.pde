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
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
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

//	private static final String ACTIVATOR_INTERFACE = "org.osgi.framework.BundleActivator"; //$NON-NLS-1$

	/**
	 * @param versionString
	 *            the version to be checked, null is allowed and will be treated
	 *            as 0.0.0
	 * @return true if in valid format with valid characters, false otherwise
	 */
	protected static boolean isValidVersion(String versionString) {
		if (versionString == null)
			return true;
		return PluginVersionIdentifier.validateVersion(versionString) == Status.OK_STATUS;
	}

	protected static boolean isValidVersionRange(String versionRangeString) {
		try {
			new VersionRange(versionRangeString);
		} catch (IllegalArgumentException e) {
			return false;
		}

		// need to do our extra checks for each piece of the versionRange
		int comma = versionRangeString.indexOf(',');
		if (comma < 0) {
			return isValidVersion(versionRangeString);
		}

		return (isValidVersion(versionRangeString.substring(1, comma)) && isValidVersion(versionRangeString
				.substring(comma + 1, versionRangeString.length() - 1)));
	}

	private boolean fEclipse3_1;

	private boolean fFragment;

	private Map fFragmentsPackagesMap = null;

	private boolean fHasFragment_Xml;

	private boolean fHasExtensions;

	private String fHostBundleId;

//	private Vector fIimportPkgs;
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
	 * check if the fragment package implement the same class as host package
	 */
	private void checkDuplicateClass(IPackageFragment hostPackage,
			IPackageFragment pkg, int line) {
		try {
			ICompilationUnit[] units = pkg.getCompilationUnits();

			for (int i = 0; i < units.length; i++) {
				String className = units[i].getElementName();
				boolean error = false;

				ICompilationUnit u = hostPackage.getCompilationUnit(className);
				if (u.exists()) {
					error = true; // hostPackage has this class
				} else {
					// Host package could have only class files, check .class
					// file name
					String className2 = className.substring(0, className
							.lastIndexOf(".")) + ".class"; //$NON-NLS-1$ //$NON-NLS-2$
					IClassFile cFile = hostPackage.getClassFile(className2);
					if (cFile.exists())
						error = true;
				}

				if (error) {
					// Found the same class
					String fullClass = hostPackage.getElementName()
							+ "." + className.substring(0, className.lastIndexOf(".")); //$NON-NLS-1$ //$NON-NLS-2$
					//$NON-NLS-2$
					String message = PDE.getFormattedMessage(
							"BundleErrorReporter.HostConflictClass", fullClass); //$NON-NLS-1$
					report(message, line, CompilerFlags.P_UNKNOWN_CLASS);
				}
			}
		} catch (JavaModelException e) {
			PDECore.logException(e);
		}
	}

//	/**
//	 * Finds the build model of the bundle
//	 */
//	private IBuild findBuild(IPluginModelBase model) {
//		IBuildModel buildModel = model.getBuildModel();
//		if (buildModel == null) {
//			IProject project = model.getUnderlyingResource().getProject();
//			IFile buildFile = project.getFile("build.properties"); //$NON-NLS-1$
//			if (buildFile.exists()) {
//				buildModel = new WorkspaceBuildModel(buildFile);
//				try {
//					buildModel.load();
//				} catch (CoreException e) {
//					return null;
//				}
//			}
//		}
//		return (buildModel != null) ? buildModel.getBuild() : null;
//	}
//
//	/**
//	 * Gets all activators for this project
//	 */
//	private IType[] getAllActivators() {
//		IType activatorInterface = JavaModelInterface.getJavaModelInterface()
//				.findTypeOnClasspath(ACTIVATOR_INTERFACE, fProject,
//						JavaModelInterface.SEARCHPATH_FULL);
//
//		List list = new ArrayList();
//		if (activatorInterface != null) {
//			// Scope the search to the project's classpath.
//			// Even if the activator is from another bundle,
//			// the package must be imported and imported
//			// packages must be on the classpath.
//			IType[] activators = JavaModelInterface.getJavaModelInterface()
//					.getImplementorsOf(activatorInterface, fProject,
//							JavaModelInterface.SEARCHPATH_FULL);
//
//			// Remove abstract classes from the set of detected activators.
//			boolean isAbstract;
//			for (int i = 0; i < activators.length; i++) {
//				try {
//					isAbstract = ((activators[i].getFlags() & Flags.AccAbstract) == Flags.AccAbstract);
//				} catch (JavaModelException ex) {
//					isAbstract = true;
//				}
//				if (!isAbstract)
//					list.add(activators[i]);
//			}
//		}
//
//		IType[] activators = new IType[list.size()];
//		list.toArray(activators);
//
//		return activators;
//	}

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

//	/**
//	 * Finds the services exported in the target platform and workspace
//	 */
//	private Vector getAvailableExportedServices() {
//		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager()
//				.getPlugins();
//		Vector results = new Vector();
//
//		for (int i = 0; i < plugins.length; i++) {
//			if ((plugins[i].getPluginBase().getId() != null)
//					&& !(plugins[i].getPluginBase().getId()).equals(fPluginId)) {
//				BundleDescription bd = plugins[i].getBundleDescription();
//				if (bd != null) {
//					String location = bd.getLocation();
//					getExportedServicesFromPlugin(location, results);
//				}
//			}
//		}
//
//		return results;
//	}
//
//	/**
//	 * Services exported from the given plugin
//	 */
//	private void getExportedServicesFromPlugin(String pluginLocation,
//			Vector services) {
//		InputStream manifestStream = null;
//		String value = null;
//		try {
//			File file = new File(pluginLocation, JarFile.MANIFEST_NAME);
//			if (file != null) {
//				manifestStream = new FileInputStream(file);
//
//				if (manifestStream != null) {
//					Manifest m = new Manifest(manifestStream);
//					Attributes d = m.getMainAttributes();
//					value = d.getValue(Constants.EXPORT_SERVICE);
//				}
//			}
//
//			if (value != null && !(value.trim()).equals("")) { //$NON-NLS-1$
//				ManifestElement[] elements = ManifestElement.parseHeader(
//						Constants.EXPORT_SERVICE, value);
//				for (int i = 0; i < elements.length; i++) {
//					services.add(elements[i].getValue());
//				}
//			}
//		} catch (FileNotFoundException e) {
//			// ignore this error
//		} catch (IOException e) {
//			PDECore.logException(e);
//		} catch (BundleException e) {
//			// PDECore.logException(e);
//		}
//	}

	/**
	 * @return Map of IPackageFragment from current project
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

//	/**
//	 * @return the import packages from this bundle
//	 */
//	private Vector getImportPackages(IBundle bundle) {
//		if (fIimportPkgs == null) {
//			fIimportPkgs = new Vector();
//			if (bundle != null) {
//				String importPackage = bundle
//						.getHeader(Constants.IMPORT_PACKAGE);
//				if (importPackage != null) {
//					try {
//						ManifestElement[] importPackageElements;
//						importPackageElements = ManifestElement.parseHeader(
//								Constants.IMPORT_PACKAGE, importPackage);
//
//						for (int i = 0; i < importPackageElements.length; i++) {
//							fIimportPkgs.add(importPackageElements[i]
//									.getValue());
//						}
//					} catch (BundleException e) {
//						// PDECore.logException(e);
//					}
//				}
//			}
//		}
//		return fIimportPkgs;
//	}

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

//	/**
//	 * @return true if the given file exists in the project
//	 */
//	private boolean isFileExist(String fileName) {
//		IResource member = fProject.findMember(fileName);
//		if (member != null) {
//			if ((member instanceof IFile) && (member.exists()))
//				return true;
//		}
//
//		return false;
//	}
//
//	/**
//	 * A class is considered in scope when it is in the bundle, or in another
//	 * bundle whose package is being imported, or in require bundle's provide
//	 * package
//	 */
//	private boolean isInScope(IType[] classes, IBundle bundle) {
//		Map projectPackages = getProjectPackages();
//		Vector imports = getImportPackages(bundle);
//
//		for (int i = 0; i < classes.length; i++) {
//			IType cls = classes[i];
//			// Ensure class is a valid service implementation class.
//			if (isValidServiceClass(cls)) {
//				String pkgName = cls.getPackageFragment().getElementName();
//				// Check if the class is part of this bundle.
//				if (projectPackages.containsKey(pkgName))
//					return true;
//
//				// Check if the class is part of another bundle
//				// whose package is imported by this bundle.
//				if (imports.contains(pkgName))
//					return true;
//			}
//		}
//		return false;
//	}

//	private boolean isValidExportedServiceInterface(IType serviceInterface,
//			IBundle bundle) {
//		IType[] classes = JavaModelInterface.getJavaModelInterface()
//				.getImplementorsOf(serviceInterface, fProject,
//						JavaModelInterface.SEARCHPATH_FULL);
//
//		return isInScope(classes, bundle);
//	}

//	private boolean isValidServiceClass(IType serviceClass) {
//		if (serviceClass.getPackageFragment().isDefaultPackage())
//			return false;
//
//		try {
//			int flags = serviceClass.getFlags();
//			return !Flags.isAbstract(flags);
//		} catch (JavaModelException ex) {
//		}
//
//		return false;
//	}

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

				/* Activator type exists but it is not a class */
				if (!type.isClass()) {
					message = PDE.getFormattedMessage(
							"BundleErrorReporter.NotAClass", activator); //$NON-NLS-1$
					report(message, getLine(header, activator),
							CompilerFlags.P_UNKNOWN_CLASS);
					return;
				}

				/* Activator type is abstract */
				if ((type.getFlags() & Flags.AccAbstract) == Flags.AccAbstract) {
					message = PDE.getFormattedMessage(
							"BundleErrorReporter.IsAbstract", activator); //$NON-NLS-1$
					report(message, getLine(header, activator),
							CompilerFlags.P_UNKNOWN_CLASS);
					return;
				}

				// /*
				// * Activator type does not implement BundleActivator interface
				// */
				// // TODO find a faster way because the next call is slow
				// ITypeHierarchy th = type.newSupertypeHierarchy(
				// new ICompilationUnit[0], new NullProgressMonitor());
				// IType[] interfaces = th.getAllSupertypes(type);
				// boolean implement = false;
				// for (int i = 0; i < interfaces.length; i++) {
				// if (ACTIVATOR_INTERFACE.equals(interfaces[i]
				// .getFullyQualifiedName())) {
				// implement = true;
				// break;
				// }
				// }
				// if (!implement) {
				// message = PDE.getFormattedMessage(
				// "BundleErrorReporter.NotImplementActivator",
				// activator);
				// //$NON-NLS-1$
				// report(message, getLine(header, activator),
				// CompilerFlags.P_UNKNOWN_CLASS);
				// return;
				// }

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

		// // TODO . is flagged as error
		// // find the files in build.properties bin.includes entry
		// List paths = new ArrayList();
		// HashSet buildJarsSet = new HashSet();
		// IPluginModelBase pluginModel = PDECore.getDefault().getModelManager()
		// .findModel(fProject);
		// IBuild buildModel = findBuild(pluginModel);
		// if (buildModel != null) {
		// IBuildEntry entry = buildModel.getEntry(IBuildEntry.BIN_INCLUDES);
		// if (entry != null) {
		// String[] buildJars = entry.getTokens();
		// if (buildJars != null) {
		// buildJarsSet.addAll(Arrays.asList(buildJars));
		// }
		// }
		// }
		//		
		// for (int i = 0; i < elements.length; i++) {
		// String classpathEntry = elements[i].getValue();
		// paths.add(classpathEntry);
		// // TODO does not work well, must do regular expression matching on
		// // bin entries
		// boolean checkBinIncludes = false;
		//
		// if (isFileExist(classpathEntry)) {
		// checkBinIncludes = true;
		// } else {
		// if (buildModel != null) {
		// IBuildEntry entry = buildModel
		// .getEntry(IBuildEntry.JAR_PREFIX + classpathEntry);
		// if ((entry != null) && (entry.getTokens().length > 0)) {
		// /*
		// * This classpath entry exists, and src.jar_file_name
		// * exists in build.properties file
		// */
		// checkBinIncludes = true;
		// } else {
		// // This jar file does not exist, it will not be
		// // built either.
		// message = PDE.getFormattedMessage(
		// "BundleErrorReporter.FileNotExist", //$NON-NLS-1$
		// classpathEntry);
		// //$NON-NLS-1$
		// report(message, getLine(header, classpathEntry),
		// CompilerFlags.P_UNKNOWN_RESOURCE);
		//
		// }
		// }
		// }
		//
		// if (checkBinIncludes) {
		// if (!buildJarsSet.contains(classpathEntry)) {
		// // This jar will not built correctly. It does not exist
		// // in bin.includes in build.properties file */
		// message = PDE.getFormattedMessage(
		// "BundleErrorReporter.JarNotInBuild", //$NON-NLS-1$
		// classpathEntry);
		// //$NON-NLS-1$
		// report(message, getLine(header, classpathEntry),
		// CompilerFlags.P_UNKNOWN_RESOURCE);
		// }
		// }
		// }
		//
		// /*
		// * When a non-external jar exists on the project's classpath but is
		// not
		// * specified in the manifest's Bundle-Classpath
		// */
		// IJavaProject jp = JavaCore.create(fProject);
		// IClasspathEntry[] entries = getJavaModelInterface()
		// .getClasspathEntries(fProject,
		// JavaModelInterface.SEARCHPATH_EXTERNAL);
		// for (int i = 0; i < entries.length; i++) {
		// IPackageFragmentRoot[] roots = jp
		// .findPackageFragmentRoots(entries[i]);
		// for (int j = 0; j < roots.length; j++) {
		// if (!roots[j].isExternal()) {
		// String jarPath = roots[j].getPath().removeFirstSegments(1)
		// .toString();
		// if (!paths.remove(jarPath)) {
		// message = PDE
		// .getFormattedMessage(
		// "BundleErrorReporter.ClasspathMissing", jarPath); //$NON-NLS-1$
		// report(message, header.getLineNumber() + 1,
		// CompilerFlags.P_NO_REQUIRED_ATT);
		// }
		// }
		// }
		// }
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
		if (!isValidVersion(version)) {
			String message = PDE
					.getResourceString("BundleErrorReporter.VersionIncorrectFormat"); //$NON-NLS-1$
			int line = getLine(header, version);
			report(message, line, CompilerFlags.ERROR);
		}
	}

	private void validateBundleVersionAttribute(IHeader header,
			ManifestElement element) {
		String versionRange = element
				.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (versionRange != null && !isValidVersionRange(versionRange)) {
			String message = PDE
					.getFormattedMessage(
							"BundleErrorReporter.InvalidFormatInBundleVersion", element.getValue()); //$NON-NLS-1$
			report(message, getPackageLine(header, element.getValue()),
					CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}

	public void validateContent(IProgressMonitor monitor) {
		// long time = System.currentTimeMillis();
		// System.out.print("Validating " + fFile + " ... "); //$NON-NLS-1$
		// //$NON-NLS-2$
		// try {

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
		// sets fHostBundleId
		validateFragmentHost();
		validateBundleClasspath();
		validateBundleActivator();
		validateRequireBundle();
		validateExportPackage();
		validateProvidePackage();
		validateImportPackage();
		// validateExportService();
		// validateImportService();
		// validateNativeCode();

		// } finally {
		// long took = System.currentTimeMillis() - time;
		// System.out.println(+took + " ms"); //$NON-NLS-1$
		// }

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
				if (fFragment
						|| !getFragmentsPackages().containsKey(
								exportPackageStmt)) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.NotExistInProject", exportPackageStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, exportPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
					continue;
				}
			}

			// /*
			// * If this is a fragment bundle, it conflicts with a host bundle’s
			// * Export-Package entry only if it has the same package name.
			// * (Search from target platform)
			// */
			// // TODO flags org.eclipse.core.resources.compatibility as bad
			// // Error message not very helpful.
			// if (fFragment && (fHostBundleId != null)) {
			// IPluginModelBase hostModel = PDECore.getDefault()
			// .getModelManager().findModel(fHostBundleId);
			// BundleDescription bd = hostModel.getBundleDescription();
			// if (bd != null) {
			// ExportPackageDescription[] pkgs = bd.getExportPackages();
			// if (pkgs != null) {
			// for (int k = 0; k < pkgs.length; k++) {
			// if (exportPackageStmt.equals(pkgs[k].getName())) {
			// message = PDE
			// .getFormattedMessage(
			// "BundleErrorReporter.HostConflictExport", //$NON-NLS-1$
			// exportPackageStmt);
			// //$NON-NLS-1$
			// report(message, getPackageLine(header,
			// exportPackageStmt),
			// CompilerFlags.P_UNRESOLVED_IMPORTS);
			// break;
			// }
			// }
			// }
			// }
			// }
		}
	}

//	private void validateExportService() {
//		IHeader header = (IHeader) fHeaders.get(Constants.EXPORT_SERVICE);
//		if (header == null) {
//			return;
//		}
//		String message = null;
//		ManifestElement[] exportServiceElements = header.getElements();
//		for (int i = 0; i < exportServiceElements.length; i++) {
//			String exportServiceStmt = exportServiceElements[i].getValue();
//			IType type = JavaModelInterface.getJavaModelInterface()
//					.findTypeOnClasspath(exportServiceStmt, fProject,
//							JavaModelInterface.SEARCHPATH_FULL);
//			if (type == null || !type.exists()) {
//				/* The exported service does not exist */
//				message = PDE
//						.getFormattedMessage(
//								"BundleErrorReporter.ServiceNotExist", exportServiceStmt); //$NON-NLS-1$
//				report(message, getLine(header, exportServiceStmt),
//						CompilerFlags.P_UNKNOWN_CLASS);
//
//				continue;
//			}
//			try {
//				if (!type.isInterface()) {
//					/* The exported service is not interface */
//					message = message = PDE
//							.getFormattedMessage(
//									"BundleErrorReporter.ServiceNotInterface", exportServiceStmt); //$NON-NLS-1$ 
//					report(message, getLine(header, exportServiceStmt),
//							CompilerFlags.P_UNKNOWN_CLASS);
//
//					continue;
//				}
//			} catch (JavaModelException e1) {
//
//			}
//
//			if (type.getPackageFragment().isDefaultPackage()) {
//				/* The exported service cannot be in default package */
//				message = PDE
//						.getFormattedMessage(
//								"BundleErrorReporter.ServiceInDefaultPkg", exportServiceStmt); //$NON-NLS-1$
//				report(message, getLine(header, exportServiceStmt),
//						CompilerFlags.P_UNKNOWN_CLASS);
//				continue;
//			}
//			// if (!isValidExportedServiceInterface(type, bundle)) {
//			// /*
//			// * No public or abstract implementation of service within the
//			// * bundle
//			// */
//			// message = PDE
//			// .getFormattedMessage(
//			// "BundleErrorReporter.ServiceNoImplementation",
//			// exportServiceStmt); //$NON-NLS-1$
//			// report(message, getLine(header, exportServiceStmt),
//			// CompilerFlags.P_UNKNOWN_CLASS);
//			//
//			// }
//		}
//	}

	/**
	 * Find all package fragments from host bundle, and verify if fragment
	 * implements any of the same class as the host. If so, mark the error.
	 */
	private void validateFragmentClasses(int line) {
		if (fHostBundleId == null || fHostBundleId.length() <= 0) {
			return;
		}
		IPluginModelBase hostModel = PDECore.getDefault().getModelManager()
				.findModel(fHostBundleId);
		IPluginBase[] list = new IPluginBase[1];
		list[0] = hostModel.getPluginBase();
		HashMap hostMap = new HashMap();

		try {
			// Get host bundle packages
			IJavaProject jp = JavaCore.create(fProject);
			IPackageFragment[] pf = PluginJavaSearchUtil
					.collectPackageFragments(list, jp);
			for (int i = 0; i < pf.length; i++) {
				hostMap.put(pf[i].getElementName(), pf[i]);
			}
			// Get packages of the fragment bundle
			Map packageMap = getProjectPackages();
			Iterator it = packageMap.keySet().iterator();
			while (it.hasNext()) {
				String key = (String) it.next();
				if (hostMap.containsKey(key)) {
					// fragment has the same package name as host, therefore
					// check the classes
					IPackageFragment hostPackage = (IPackageFragment) hostMap
							.get(key);
					IPackageFragment pkg = (IPackageFragment) packageMap
							.get(key);
					checkDuplicateClass(hostPackage, pkg, line);
				}
			}
		} catch (JavaModelException e) {
			PDECore.logException(e);
		}
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
					&& isValidVersionRange(requiredVersionRange)) {
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
		if (isCheckUnknownClass()) {
			/*
			 * Fragment replaces the class(es) of the host bundle
			 */
			validateFragmentClasses(getLine(header, fragmentHostStmt));
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
			if (requiredVersion != null && isValidVersion(requiredVersion)) {
				ExportPackageDescription epd = (ExportPackageDescription) availableExportedPackagesMap
						.get(importPackageStmt);
				if (epd.getVersion() != null
						&& epd.getVersion().compareTo(
								new Version(requiredVersion)) < 0) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.VersionNotInRange", importPackageStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, importPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS);
					continue;
				}
			}
			// /*
			// * If this is a fragment bundle, it conflicts with the host
			// bundle’s
			// * Import-Package entry because it has the same package name. This
			// * can be extended to flag the problem only if all the attributes
			// * and directives do not match.
			// */
			// if (fFragment && (fHostBundleId != null)) {
			// IPluginModelBase hostModel = PDECore.getDefault()
			// .getModelManager().findModel(fHostBundleId);
			// BundleDescription bd = hostModel.getBundleDescription();
			// if (bd != null) {
			// ImportPackageSpecification[] pkgs = bd.getImportPackages();
			// if (pkgs != null) {
			// for (int k = 0; k < pkgs.length; k++) {
			// if (importPackageStmt.equals(pkgs[k].getName())) {
			// message = PDE
			// .getFormattedMessage(
			// "BundleErrorReporter.HostConflictImport",
			// importPackageStmt);
			// report(message, getPackageLine(header,
			// importPackageStmt),
			// CompilerFlags.P_UNRESOLVED_IMPORTS);
			// break;
			// }
			// }
			// }
			// }
			// }
		}
	}

//	private void validateImportService() {
//		IHeader header = (IHeader) fHeaders.get(Constants.IMPORT_SERVICE);
//		if (header == null) {
//			return;
//		}
//		String importService = header.getValue();
//		if (importService == null) {
//			return;
//		}
//		String message = null;
//		Vector availableExportedServices = getAvailableExportedServices();
//
//		ManifestElement[] importServiceElements = header.getElements();
//		for (int i = 0; i < importServiceElements.length; i++) {
//			String importServiceStmt = importServiceElements[i].getValue();
//
//			if (!availableExportedServices.contains(importServiceStmt)) {
//				/* It is not an exported service from target platform */
//				message = PDE
//						.getFormattedMessage(
//								"BundleErrorReporter.ServiceNotExported", importServiceStmt); //$NON-NLS-1$
//				report(message, getLine(header, importServiceStmt),
//						CompilerFlags.P_UNRESOLVED_IMPORTS);
//			}
//		}
//
//	}
//
//	private void validateNativeCode() {
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
				if (!getFragmentsPackages().containsKey(exportPackageStmt)) {
					message = PDE
							.getFormattedMessage(
									"BundleErrorReporter.NotExistInProject", exportPackageStmt); //$NON-NLS-1$
					report(message, getPackageLine(header, exportPackageStmt),
							CompilerFlags.P_UNRESOLVED_IMPORTS); //$NON-NLS-1$
					continue;
				}
			}

			// /*
			// * If this is a fragment bundle, it conflicts with a host bundle’s
			// * Export-Package entry only if it has the same package name.
			// * (Search from target platform)
			// */
			// // TODO flags org.eclipse.core.resources.compatibility as bad
			// // Error message not very helpful.
			// if (fFragment && (fHostBundleId != null)) {
			// IPluginModelBase hostModel = PDECore.getDefault()
			// .getModelManager().findModel(fHostBundleId);
			// BundleDescription bd = hostModel.getBundleDescription();
			// if (bd != null) {
			// ExportPackageDescription[] pkgs = bd.getExportPackages();
			// if (pkgs != null) {
			// for (int k = 0; k < pkgs.length; k++) {
			// if (exportPackageStmt.equals(pkgs[k].getName())) {
			// message = PDE
			// .getFormattedMessage(
			// "BundleErrorReporter.HostConflictExport", //$NON-NLS-1$
			// exportPackageStmt);
			// //$NON-NLS-1$
			// report(message, getPackageLine(header,
			// exportPackageStmt),
			// CompilerFlags.P_UNRESOLVED_IMPORTS);
			// break;
			// }
			// }
			// }
			// }
			// }
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
					&& isValidVersionRange(requiredVersionRange)) {
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

			// /*
			// * If this is a fragment bundle and it conflicts with the
			// * host bundle’s Require-Bundle entry if it has the same
			// * bundle symbolic name. This can be extended to flag the
			// * problem only if all the attributes and directives do
			// not
			// * match.
			// */
			// if (fFragment && (fHostBundleId != null)) {
			// IPluginModelBase hostModel = PDECore.getDefault()
			// .getModelManager().findModel(fHostBundleId);
			// BundleDescription bd = hostModel.getBundleDescription();
			// if (bd != null) {
			// BundleSpecification[] rbs = bd.getRequiredBundles();
			// if (rbs != null) {
			// for (int k = 0; k < rbs.length; k++) {
			// if (requireBundleStmt.equals(rbs[k]
			// .getName())) {
			// message = PDE
			// .getFormattedMessage(
			// "BundleErrorReporter.HostConflictRequireBundle",
			// requireBundleStmt); //$NON-NLS-1$
			// report(
			// message,
			// getPackageLine(header,
			// requireBundleStmt),
			// CompilerFlags.P_UNRESOLVED_IMPORTS);
			// break;
			// }
			// }
			// }
			// }
			// }

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
		if (version != null && !isValidVersion(version)) {
			String message = PDE
					.getFormattedMessage(
							"BundleErrorReporter.SpecificationVersionInvalidFormat", element.getValue()); //$NON-NLS-1$
			report(message, getPackageLine(header, element.getValue()),
					CompilerFlags.ERROR); //$NON-NLS-1$
		}
	}

	private void validateVersionAttribute(IHeader header,
			ManifestElement element) {
		String versionRange = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		if (versionRange != null && !isValidVersionRange(versionRange)) {
			String message = PDE
					.getFormattedMessage(
							"BundleErrorReporter.InvalidFormatVersionAttr", element.getValue()); //$NON-NLS-1$
			report(message, getPackageLine(header, element.getValue()),
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
