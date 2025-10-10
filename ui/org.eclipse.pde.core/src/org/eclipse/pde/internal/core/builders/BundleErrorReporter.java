/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     Code 9 Corporation - on going enhancements and maintenance
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 209432, 214156
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IModuleDescription;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.eclipse.osgi.service.resolver.BaseDescription;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.core.AbstractNLModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.PDEState;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.TargetWeaver;
import org.eclipse.pde.internal.core.builders.IncrementalErrorReporter.VirtualMarker;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.natures.PluginProject;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.core.util.UtilMessages;
import org.eclipse.pde.internal.core.util.VersionUtil;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.framework.wiring.BundleCapability;

public class BundleErrorReporter extends JarManifestErrorReporter {

	private boolean fOsgiR4;
	private IPluginModelBase fModel;
	private Set<String> fProjectPackages;

	public BundleErrorReporter(IFile file) {
		super(file);
	}

	@Override
	protected void validate(IProgressMonitor monitor) {
		super.validate(monitor);
		if (fHeaders == null || getErrorCount() > 0) {
			return;
		}

		fModel = PluginRegistry.findModel(fProject);
		// be paranoid.  something could have gone wrong reading the file etc.
		if (fModel == null || !validateBundleSymbolicName()) {
			return;
		}
		if (PluginProject.isJavaProject(fProject)) {
			validateAutomaticModuleName();
		}
		if (!validateVersionOfRequireBundle()) {
			return;
		}
		if (!validateVersionOfImportPackage()) {
			return;
		}

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null && fModel.getInstallLocation() != null) {
			// There was a problem creating the OSGi bundle description, possibly a bad header
			try {
				StateObjectFactory stateObjectFactory = BundleHelper.getPlatformAdmin().getFactory();
				File bundleLocation = new File(fModel.getInstallLocation());
				Map<String, String> manifest = ManifestUtils.loadManifest(bundleLocation);
				TargetWeaver.weaveManifest(manifest, bundleLocation);
				Hashtable<String, String> dictionaryManifest = new Hashtable<>(manifest);
				stateObjectFactory.createBundleDescription(null, dictionaryManifest, null, 1);
			} catch (BundleException e) {
				if (e.getType() == BundleException.MANIFEST_ERROR) {
					// Extract header from the error message and obtain the line
					// number of the header
					String msg = e.getMessage();
					String[] splitArray = msg.split(":"); //$NON-NLS-1$
					String firstString = splitArray[0];
					String[] splitToken = firstString.split(" "); //$NON-NLS-1$
					String lastString = splitToken[splitToken.length - 1];
					IHeader header = null;
					header = getHeader(lastString);
					// Reporting falls back on line 1, in case of parse error
					int line = header == null ? 1 : header.getLineNumber();
					report(e.getMessage(), line, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					return;
				}
			} catch (CoreException e) {
				// Ignore problems loading the manifest for now as the editor shouldn't have opened
			}
		}

		validateFragmentHost();
		validateBundleVersion();
		validateRequiredExecutionEnvironment();

		validateEclipsePlatformFilter();
		validateBundleActivator();
		validateBundleClasspath();
		validateRequireBundle(monitor);
		validateImportPackage(monitor);
		validateExportPackage(monitor);
		validateExportPackages();
		validateAutoStart();
		validateLazyStart();
		validateBundleActivatorPolicy();
		validateExtensibleAPI();
		validateTranslatableHeaders();
		validateImportExportServices();
		validateBundleLocalization();
		validateProvidePackage();
		validateEclipseBundleShape();
		validateEclipseGenericCapability();
		validateEclipseGenericRequire();
		validateServiceComponent();
		if (isCheckDeprecated()) {
			validateDeprecated();
		}
	}

	private void validateDeprecated() {
		IHeader header = getHeader(Constants.REQUIRE_BUNDLE);
		if (header == null) {
			return;
		}
		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			return;
		}
		BundleSpecification[] requiredBundles = desc.getRequiredBundles();
		for (BundleSpecification bundleSpecification : requiredBundles) {
			BaseDescription bundle = bundleSpecification.getSupplier();
			if (bundle != null) {
				BundleCapability capability = bundle.getCapability();
				if (capability != null) {
					String deprecatedDirective = capability.getDirectives().get("deprecated"); //$NON-NLS-1$
					if (deprecatedDirective != null) {
						report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecatedBundle, bundle.getName(),
								deprecatedDirective), header.getLineNumber(), CompilerFlags.WARNING,
								PDEMarkerFactory.CAT_DEPRECATION);
					}
				}
			}
		}

	}


	private void validateAutomaticModuleName() {
		int compilerFlag = CompilerFlags.getFlag(fProject, CompilerFlags.P_NO_AUTOMATIC_MODULE);
		if( compilerFlag == CompilerFlags.IGNORE) {
			return;
		}

		IJavaProject jp = JavaCore.create(fProject);
		IModuleDescription moduleDescription = null;
		if (jp != null) {
			try {
				moduleDescription = jp.getModuleDescription();
			} catch (JavaModelException e) {

			}
		}
		if (moduleDescription == null) {
			IHeader header = fHeaders.get(ICoreConstants.AUTOMATIC_MODULE_NAME.toLowerCase());
			if (header == null) {
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_headerMissingAutoModule, ICoreConstants.AUTOMATIC_MODULE_NAME),  1, CompilerFlags.P_NO_AUTOMATIC_MODULE, PDEMarkerFactory.M_NO_AUTOMATIC_MODULE, PDEMarkerFactory.CAT_OTHER);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_NO_AUTOMATIC_MODULE);
			}
		}
		if (moduleDescription != null) {
			IHeader header = fHeaders.get(ICoreConstants.AUTOMATIC_MODULE_NAME.toLowerCase());
			if( header != null) {
				report(PDECoreMessages.BundleErrorReporter_ConflictingAutoModule, header.getLineNumber(),
						CompilerFlags.WARNING, PDEMarkerFactory.M_CONFLICTING_AUTOMATIC_MODULE, PDEMarkerFactory.CAT_OTHER);
			}

		}
	}

	private boolean validateBundleManifestVersion() {
		IHeader header = getHeader(Constants.BUNDLE_MANIFESTVERSION);
		if (header != null) {
			String version = header.getValue();
			if (!(fOsgiR4 = "2".equals(version)) && !"1".equals(version)) { //$NON-NLS-1$ //$NON-NLS-2$
				report(PDECoreMessages.BundleErrorReporter_illegalManifestVersion, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
				return false;
			}
		}
		return true;
	}

	private void validateExportPackages() {
		IHeader header = getHeader(Constants.EXPORT_PACKAGE);

		// check for missing exported packages
		if (fModel instanceof IBundlePluginModelBase bundleModel) {
			IBundle bundle = bundleModel.getBundleModel().getBundle();
			IManifestHeader bundleClasspathheader = bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);

			IPackageFragmentRoot[] roots = ManifestUtils.findPackageFragmentRoots(bundleClasspathheader, fProject);
			// Running list of packages in the project
			//Set packages = new HashSet();
			StringBuilder packages = new StringBuilder();
			for (IPackageFragmentRoot root : roots) {
				try {
					if (ManifestUtils.isImmediateRoot(root)) {
						IJavaElement[] javaElements = root.getChildren();
						for (int j = 0; j < javaElements.length; j++) {
							if (javaElements[j] instanceof IPackageFragment fragment) {
								String name = fragment.getElementName();
								if (name.length() == 0) {
									continue;
								}
								if (fragment.containsJavaResources() || fragment.getNonJavaResources().length > 0) {
									if (isInternalPackage(name)) {
										continue;
									}
									if (!containsPackage(header, name)) {
										packages.append(name);
										packages.append(","); //$NON-NLS-1$
										byte[] bytes = packages.toString().getBytes(StandardCharsets.UTF_8);
										// See MarkerInfo::checkValidAttribute
										if (bytes.length > 65535) {
											packages.delete(packages.lastIndexOf(name), packages.length());
											break;
										}
									}
								}
							}
						}
					}
				} catch (JavaModelException e) {
				}
			}
			if (packages.toString().length() > 0) {
				if (packages.substring(packages.length() - 1).equals(",")) { //$NON-NLS-1$
					packages.setLength(packages.length() - 1);
				}
			}

			// if we actually have packages to add
			if (packages.toString().length() > 0) {
				VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_missingPackagesInProject, header == null ? 1 : header.getLineNumber(), CompilerFlags.P_MISSING_EXPORT_PKGS, PDEMarkerFactory.M_MISSING_EXPORT_PKGS, PDEMarkerFactory.CAT_OTHER);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_MISSING_EXPORT_PKGS);
				addMarkerAttribute(marker, "packages", packages.toString()); //$NON-NLS-1$
			}
		}
	}

	private boolean isInternalPackage(String name) {
		String[] split = name.split("\\."); //$NON-NLS-1$
		for (String section : split) {
			// dont consider packages with "internal" or "impl" section in it
			// for exporting
			if (section.equals("internal") || section.equals("impl")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}

		}
		return false;
	}

	/**
	 * @return boolean false if fatal
	 */
	private boolean validateBundleSymbolicName() {
		IHeader header = validateRequiredHeader(Constants.BUNDLE_SYMBOLICNAME);
		if (header == null) {
			return false;
		}

		ManifestElement[] elements = header.getElements();
		String id = elements.length > 0 ? elements[0].getValue() : null;
		if (id == null || id.length() == 0) {
			report(PDECoreMessages.BundleErrorReporter_NoSymbolicName, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			return false;
		}

		if (!validateBundleManifestVersion()) {
			return false;
		}
		validatePluginId(header, id);
		validateSingleton(header, elements[0]);

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_SYMBOLICNAME), header.getLineNumber(), CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);
		}

		return true;
	}

	/**
	 * @return boolean false if fatal
	 */
	private boolean validateVersionOfRequireBundle() {
		// check the version range of require bundle are ok
		IHeader header = getHeader(Constants.REQUIRE_BUNDLE);
		if (header == null) {
			return true;
		}
		ManifestElement[] required = header.getElements();
		for (ManifestElement element : required) {
			String versionRange = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (versionRange != null) {
				try {
					new VersionRange(versionRange);
				} catch (IllegalArgumentException e) {
					report(e.getMessage(), getLine(header, element.getValue()), CompilerFlags.ERROR,
							PDEMarkerFactory.CAT_FATAL);
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * @return boolean false if fatal
	 */
	private boolean validateVersionOfImportPackage() {
		// check the version range of import package are ok
		IHeader header = getHeader(Constants.IMPORT_PACKAGE);
		if (header == null) {
			return true;
		}
		ManifestElement[] importPackages = header.getElements();
		for (ManifestElement element : importPackages) {
			String versionRange = element.getAttribute(Constants.VERSION_ATTRIBUTE);
			if (versionRange != null) {
				try {
					new VersionRange(versionRange);
				} catch (IllegalArgumentException e) {
					report(e.getMessage(), getLine(header, element.getValue()), CompilerFlags.ERROR,
							PDEMarkerFactory.CAT_FATAL);
					return false;
				}
			}
		}
		return true;
	}

	private boolean validatePluginId(IHeader header, String value) {
		if (!IdUtil.isValidCompositeID3_0(value)) {
			String message = PDECoreMessages.BundleErrorReporter_InvalidSymbolicName;
			report(message, getLine(header, value), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			return false;
		}
		return true;
	}

	// validateBundleManifestVersion must be called before this function.  Relies on fOSGiR4 being set correctly
	private void validateSingleton(IHeader header, ManifestElement element) {
		String singletonAttr = element.getAttribute(ICoreConstants.SINGLETON_ATTRIBUTE);
		String singletonDir = element.getDirective(Constants.SINGLETON_DIRECTIVE);
		IPluginBase base = fModel.getPluginBase();
		// must check the existence of plugin.xml file instead of using IPluginBase because if the bundle is not a singleton,
		// it won't be registered with the extension registry and will always return 0 when querying extensions/extension points
		boolean hasExtensions = base != null && PDEProject.getPluginXml(fProject).exists();
		if (hasExtensions) {
			if (TargetPlatformHelper.getTargetVersion() >= 3.1) {
				if (!"true".equals(singletonDir)) { //$NON-NLS-1$
					if ("true".equals(singletonAttr)) { //$NON-NLS-1$
						if (isCheckDeprecated() && fOsgiR4) {
							String message = PDECoreMessages.BundleErrorReporter_deprecated_attribute_singleton;
							VirtualMarker marker = report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
									CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_DEPRECATION);
							addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
							return;
						}
					} else {
						Enumeration<String> attrKeys = element.getDirectiveKeys();
						int length = 0;
						String key = null;
						if (attrKeys != null) {
							while (attrKeys.hasMoreElements()) {
								key = attrKeys.nextElement();
								length++;
							}
						}
						if (length == 1) {
							String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonRequired,
									Constants.SINGLETON_DIRECTIVE);
							VirtualMarker marker = report(message, header.getLineNumber(), CompilerFlags.ERROR,
									PDEMarkerFactory.M_SINGLETON_DIR_CHANGE, PDEMarkerFactory.CAT_FATAL);
							addMarkerAttribute(marker, "userDirective", key); //$NON-NLS-1$
							return;
						}
						String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonRequired, Constants.SINGLETON_DIRECTIVE);
						report(message, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_FATAL);
						return;
					}
				}
			} else if (!"true".equals(singletonAttr)) { //$NON-NLS-1$
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonAttrRequired, ICoreConstants.SINGLETON_ATTRIBUTE);
				report(message, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_ATT_NOT_SET, PDEMarkerFactory.CAT_OTHER);
				return;
			}
		}

		if (fOsgiR4) {
			if (singletonAttr != null) {
				if (isCheckDeprecated()) {
					String message = PDECoreMessages.BundleErrorReporter_deprecated_attribute_singleton;
					VirtualMarker marker = report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
							CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_OTHER);
					addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_DEPRECATED);
				}
			}
		} else if (singletonDir != null) {
			if (isCheckDeprecated()) {
				String message = PDECoreMessages.BundleErrorReporter_unsupportedSingletonDirective;
				VirtualMarker marker = report(message, getLine(header, Constants.SINGLETON_DIRECTIVE + ":="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SUPPORTED, PDEMarkerFactory.CAT_OTHER);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_DEPRECATED);

			}
		}
		validateBooleanAttributeValue(header, element, ICoreConstants.SINGLETON_ATTRIBUTE);
		validateBooleanDirectiveValue(header, element, Constants.SINGLETON_DIRECTIVE);
	}

	private void validateFragmentHost() {
		IHeader header = getHeader(Constants.FRAGMENT_HOST);
		if (header == null) {
			if (isCheckNoRequiredAttr() && PDEProject.getFragmentXml(fProject).exists()) {
				VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT, PDEMarkerFactory.CAT_FATAL);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_NO_REQUIRED_ATT);
			}
			return;
		}

		if (header.getElements().length == 0) {
			if (isCheckNoRequiredAttr()) {
				VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT, PDEMarkerFactory.CAT_FATAL);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_NO_REQUIRED_ATT);
			}
			return;
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.FRAGMENT_HOST), header.getLineNumber(), CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);
		}

		if (!isCheckUnresolvedImports()) {
			return;
		}

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			ManifestElement[] elems = header.getElements();
			if (elems.length > 0) {
				if (!VersionUtil.validateVersionRange(elems[0].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)).isOK()) {
					int line = getLine(header, header.getValue());
					VirtualMarker marker = report(UtilMessages.BundleErrorReporter_InvalidFormatInBundleVersion, line,CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
					addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_UNRESOLVED_IMPORTS);
				}
			}
			return;
		}

		HostSpecification host = desc.getHost();
		if (host == null) {
			return;
		}

		String name = host.getName();
		if (host.getSupplier() == null) {
			boolean missingHost = false;
			ResolverError[] errors = desc.getContainingState().getResolverErrors(desc);
			for (ResolverError error : errors) {
				if (error.getType() == ResolverError.MISSING_FRAGMENT_HOST) {
					missingHost = true;
					break;
				}
			}

			if (missingHost) {
				BundleDescription[] suppliers = desc.getContainingState().getBundles(name);
				boolean resolved = true;
				for (BundleDescription supplier : suppliers) {
					if (supplier.getHost() != null) {
						continue;
					}
					if (supplier.isResolved()) {
						Version version = supplier.getVersion();
						if (!host.getVersionRange().includes(version)) {
							String versionRange = host.getVersionRange().toString();
							report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, versionRange), getLine(header, versionRange), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
							return;
						}
					} else {
						resolved = false;
					}
				}

				if (!resolved) {
					VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedHost, name), getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
					addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_UNRESOLVED_IMPORTS);
					return;
				}
			}
		}

		IPluginModelBase model = PluginRegistry.findModel(name);
		if (model == null || model instanceof IFragmentModel || !model.isEnabled()) {
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_HostNotExistPDE, name), getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_UNRESOLVED_IMPORTS);
		}
	}

	private void validateBundleVersion() {
		IHeader header = validateRequiredHeader(Constants.BUNDLE_VERSION);
		if (header == null) {
			return;
		}

		IStatus status = VersionUtil.validateVersion(header.getValue());
		if (!status.isOK()) {
			int line = getLine(header, header.getValue());
			report(status.getMessage(), line, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateRequiredExecutionEnvironment() {
		int sev = CompilerFlags.getFlag(fProject, CompilerFlags.P_INCOMPATIBLE_ENV);
		if (sev == CompilerFlags.IGNORE) {
			return;
		}
		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			return;
		}

		// if we aren't a java project, let's not check for a BREE
		if (!PluginProject.isJavaProject(fProject)) {
			return;
		}
		String[] bundleEnvs = desc.getExecutionEnvironments();
		if (bundleEnvs == null || bundleEnvs.length == 0) {
			// No EE specified
			IJavaProject javaProject = JavaCore.create(fProject);

			// See if the project has an EE classpath entry
			if (javaProject.exists()) {
				try {
					IClasspathEntry[] entries = javaProject.getRawClasspath();

					for (IClasspathEntry entry : entries) {
						if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							IPath currentPath = entry.getPath();
							if (JavaRuntime.newDefaultJREContainerPath().matchingFirstSegments(currentPath) > 0) {
								String eeId = JavaRuntime.getExecutionEnvironmentId(currentPath);
								if (eeId != null) {
									VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_noExecutionEnvironmentSet, 1, sev, PDEMarkerFactory.M_EXECUTION_ENVIRONMENT_NOT_SET, PDEMarkerFactory.CAT_EE);
									addMarkerAttribute(marker, "ee_id", eeId); //$NON-NLS-1$
									addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,	CompilerFlags.P_INCOMPATIBLE_ENV);
									return;
								}
							}
						}
					}
				} catch (JavaModelException e) {
					PDECore.log(e);
				}
			}

			// If no EE classpath entry, get a matching EE for the project JRE (or the default JRE)
			IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
			IVMInstall vm = JavaRuntime.getDefaultVMInstall();
			if (javaProject.exists()) {
				try {
					vm = JavaRuntime.getVMInstall(javaProject);
				} catch (CoreException e) {
					PDECore.log(e);
				}
			}

			if (vm != null) {
				for (IExecutionEnvironment systemEnv : systemEnvs) {
					// Get strictly compatible EE for the default VM
					if (systemEnv.isStrictlyCompatible(vm)) {
						VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_noExecutionEnvironmentSet, 1, sev, PDEMarkerFactory.M_EXECUTION_ENVIRONMENT_NOT_SET, PDEMarkerFactory.CAT_EE);
						addMarkerAttribute(marker, "ee_id", systemEnv.getId()); //$NON-NLS-1$
						addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_INCOMPATIBLE_ENV);
						break;
					}
				}
			}
			return;
		}

		@SuppressWarnings("deprecation")
		IHeader header = getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header == null) {
			return;
		}

		IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(bundleEnvs[0]);
		if (env != null) {
			IJavaProject jproject = JavaCore.create(fProject);
			IClasspathEntry[] entries;
			try {
				entries = jproject.getRawClasspath();
				for (IClasspathEntry entry : entries) {
					if (entry.getEntryKind() != IClasspathEntry.CPE_CONTAINER) {
						continue;
					}
					IPath currentPath = entry.getPath();
					if (JavaRuntime.newDefaultJREContainerPath().matchingFirstSegments(currentPath) == 0) {
						continue;
					}

					IPath validPath = JavaRuntime.newJREContainerPath(env);
					if (!validPath.equals(currentPath)) {
						// Check if the user is using a perfect match JRE
						IVMInstall vm = JavaRuntime.getVMInstall(currentPath);
						if (vm == null || !env.isStrictlyCompatible(vm)) {
							IExecutionEnvironmentsManager manager = JavaRuntime.getExecutionEnvironmentsManager();
							IExecutionEnvironment[] environments = manager.getExecutionEnvironments();
							String systemEE =null;
							for (IExecutionEnvironment environment : environments) {
								if (environment.isStrictlyCompatible(vm)) {
									systemEE = environment.getId();
									break;
								}
							}
							VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_reqExecEnv_conflict, bundleEnvs[0]),getLine(header, bundleEnvs[0]), sev, PDEMarkerFactory.M_MISMATCHED_EXEC_ENV,PDEMarkerFactory.CAT_EE);
							addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,CompilerFlags.P_INCOMPATIBLE_ENV);
							if (systemEE != null) {
								addMarkerAttribute(marker, "BREE", systemEE); //$NON-NLS-1$
							}
						}
					}
				}
			} catch (JavaModelException e) {
			}
		}
		IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		int numInvalidExecEnv = 0;
		for (String bundleEnv : bundleEnvs) {
			boolean found = false;
			for (IExecutionEnvironment systemEnv : systemEnvs) {
				if (bundleEnv.equals(systemEnv.getId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				numInvalidExecEnv++;
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_reqExecEnv_unknown, bundleEnv),
						getLine(header, bundleEnv), sev, PDEMarkerFactory.M_UNKNOW_EXEC_ENV, PDEMarkerFactory.CAT_EE);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_INCOMPATIBLE_ENV);
				break;
			}
		}

		if (numInvalidExecEnv == bundleEnvs.length) {
			return;
		}
		// Check for highest BREE of bundle dependencies
		int compilerFlag = CompilerFlags.getFlag(fProject, CompilerFlags.P_EXEC_ENV_TOO_LOW);
		if (compilerFlag != CompilerFlags.IGNORE) {
			ArrayList<Object> checkBREE = checkBREE(desc);
			String highestDependencyEE = checkBREE.size() > 0 ? (String) checkBREE.get(0) : ""; //$NON-NLS-1$
			String highestBundleEE = getHighestBREE(bundleEnvs);
			try {
				if (highestBundleEE != getHighestEE(highestDependencyEE, highestBundleEE)) {
					BundleDescription object = null;
					if (checkBREE.size() == 2) {
						object = (BundleDescription) checkBREE.get(1);
					}
					VirtualMarker marker = report(
							NLS.bind(PDECoreMessages.BundleErrorReporter_ExecEnv_tooLow, highestDependencyEE,
									object != null ? object.getName() : ""), //$NON-NLS-1$
							getLine(header, highestBundleEE), compilerFlag, PDEMarkerFactory.M_EXEC_ENV_TOO_LOW,
							PDEMarkerFactory.CAT_EE);
					addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_EXEC_ENV_TOO_LOW);
					addMarkerAttribute(marker, PDEMarkerFactory.REQUIRED_EXEC_ENV, highestDependencyEE);
				}
			} catch (Exception e) {
				PDECore.log(e);
			}
			checkBREE.clear();
		}
	}

	private static final List<String> EXECUTION_ENVIRONMENT_NAMES = List.of("OSGi/Minimum", //$NON-NLS-1$
			"CDC-1.0/Foundation", //$NON-NLS-1$
			"CDC-1.1/Foundation", "JRE", "J2SE", "JavaSE"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

	private static final Pattern EE_PATTERN = Pattern.compile("(.*)-(\\d+)\\.?(\\d+)?(.*)?"); //$NON-NLS-1$

	/**
	 * <p>
	 * Returns the highest Execution Environment between two given Execution
	 * Environments. An Execution Environment is <b>higher</b> than another
	 * Execution Environment if it's name occurs at a later index in
	 * {@link #EXECUTION_ENVIRONMENT_NAMES}, or if the names are equal and it's
	 * major version is greater, or if the names and major version are equal and
	 * it's minor version is greater.
	 * </p>
	 * <p>
	 * For example, the name component of JavaSE-1.8 is 'JavaSE', it's major
	 * version is 1 and it's minor version is 8. Thus JavaSE-1.8 is a higher
	 * Execution Environment than JRE-1.1 since it's name occurs at later index
	 * than 'JRE' in {@link #EXECUTION_ENVIRONMENT_NAMES}.
	 * </p>
	 *
	 * @param execEnv1
	 *            String representation of the first Execution Environment to
	 *            compare
	 * @param execEnv2
	 *            String representation of the second Execution Environment to
	 *            compare
	 * @return The string representation of the highest Execution Environment
	 *         between the two Execution Environments
	 */
	private static String getHighestEE(String execEnv1, String execEnv2) throws IllegalArgumentException {
		if (execEnv1 == null) {
			return execEnv2;
		} else if (execEnv2 == null) {
			return execEnv1;
		}

		Matcher eeMatcher1 = EE_PATTERN.matcher(execEnv1);
		Matcher eeMatcher2 = EE_PATTERN.matcher(execEnv2);

		if (!eeMatcher1.matches()) {
			throw new IllegalArgumentException(String.format("%s is not a valid Execution Environment", execEnv1)); //$NON-NLS-1$
		}
		if (!eeMatcher2.matches()) {
			throw new IllegalArgumentException(String.format("%s is not a valid Execution Environment", execEnv2)); //$NON-NLS-1$
		}

		String eeName1 = eeMatcher1.group(1);
		String eeName2 = eeMatcher2.group(1);
		int eeNameIndex1 = EXECUTION_ENVIRONMENT_NAMES.indexOf(eeName1);
		int eeNameIndex2 = EXECUTION_ENVIRONMENT_NAMES.indexOf(eeName2);
		int eeMajorVersion1 = Integer.parseInt(eeMatcher1.group(2));
		int eeMajorVersion2 = Integer.parseInt(eeMatcher2.group(2));
		Integer eeMinorVersion1 = null;
		Integer eeMinorVersion2 = null;

		if (eeMatcher1.groupCount() > 2 && eeMatcher1.group(3) != null) {
			eeMinorVersion1 = Integer.valueOf(eeMatcher1.group(3));
		}
		if (eeMatcher2.groupCount() > 2 && eeMatcher2.group(3) != null) {
			eeMinorVersion2 = Integer.valueOf(eeMatcher2.group(3));
		}

		if (eeNameIndex1 > eeNameIndex2) {
			return execEnv1;
		} else if (eeNameIndex1 < eeNameIndex2) {
			return execEnv2;
		}

		// EE1 and EE2 have the same EE name
		if (eeMajorVersion1 > eeMajorVersion2) {
			return execEnv1;
		} else if (eeMajorVersion1 < eeMajorVersion2) {
			return execEnv2;
		}

		// EE1 and EE2 have the same major version
		if (eeMinorVersion1 != null && eeMinorVersion2 != null) {
			if (eeMinorVersion1 > eeMinorVersion2) {
				return execEnv1;
			} else if (eeMinorVersion1 < eeMinorVersion2) {
				return execEnv2;
			}
		}

		// EE1 == EE2
		return execEnv1;
	}

	/**
	 * Compares all the Execution Environments in an array of Execution
	 * Environments strings and returns the highest one.
	 *
	 * @param executionEnvironments
	 *            Array of Execution Environment strings to compare
	 * @return The highest Execution Environment in the array of Execution
	 *         Environments, null if an error occurred or if an empty array is
	 *         given
	 */
	private String getHighestBREE(String[] executionEnvironments) {
		if (executionEnvironments.length == 0) {
			return null;
		}
		String highestExecEnv = executionEnvironments[0];
		if (executionEnvironments.length > 1) {
			for (String execEnv : executionEnvironments) {
				try {
				highestExecEnv = getHighestEE(highestExecEnv, execEnv);
				} catch (Exception e) {
					PDECore.log(e);
					return null;
				}

			}
		}

		return highestExecEnv;
	}

	/**
	 * Gets the highest Execution Environment required by a bundle or any of
	 * it's transitive dependencies.
	 *
	 * @param desc
	 *            The bundle description of the bundle which we wish to check
	 *            for it's highest required Execution Environment
	 * @return List containing the highest Execution Environment &
	 *         BundleDescription with the highest BREE required by the bundle or
	 *         any of it's dependencies
	 */
	private ArrayList<Object> checkBREE(BundleDescription desc) {
		ArrayList<Object> ret = new ArrayList<>();
		String highestBREE = getHighestBREE(desc.getExecutionEnvironments());
		ret.add(highestBREE);
		HashSet<BundleDescription> visitedBundles = new HashSet<>();
		Deque<BundleDescription> bundleDescriptions = new ArrayDeque<>();
		bundleDescriptions.push(desc);
		while (!bundleDescriptions.isEmpty()) {
			BundleDescription dependencyDesc = bundleDescriptions.pop();
			visitedBundles.add(dependencyDesc);
			for (BundleSpecification transitiveDependencyDesc : dependencyDesc.getRequiredBundles()) {
				if (transitiveDependencyDesc.isOptional()) {
					continue;
				}
				if (!visitedBundles.contains(transitiveDependencyDesc.getSupplier())) {
					if (transitiveDependencyDesc.getSupplier() instanceof BundleDescription) {
						bundleDescriptions.push((BundleDescription) transitiveDependencyDesc.getSupplier());
					}
				}
			}
			try {
				String high = getHighestEE(highestBREE, getHighestBREE(dependencyDesc.getExecutionEnvironments()));
				if (!high.equals(highestBREE)) {
					highestBREE = high;
					ret.clear();
					ret.add(highestBREE);
					ret.add(dependencyDesc);
				}
			} catch (Exception e) {
				PDECore.log(e);
			}
		}
		return ret;
	}
	/**
	 * Validates the Eclipse-BundleShape header
	 *
	 * @since 3.5
	 */
	private void validateEclipseBundleShape() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_BUNDLE_SHAPE);
		if (header == null) {
			return;
		}

		String value = header.getValue();
		if (value != null) {
			validateHeaderValue(header, ICoreConstants.SHAPE_VALUES);
		}

	}

	private void validateEclipsePlatformFilter() {
		IHeader header = getHeader(ICoreConstants.PLATFORM_FILTER);
		if (header == null) {
			return;
		}

		try {
			PDECore.getDefault().getBundleContext().createFilter(header.getValue());
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_INCOMPATIBLE_ENV);
			if (severity == CompilerFlags.IGNORE) {
				return;
			}
			BundleDescription desc = fModel.getBundleDescription();
			if (desc != null && !desc.isResolved()) {
				ResolverError[] errors = desc.getContainingState().getResolverErrors(desc);
				for (ResolverError error : errors) {
					if (error.getType() == ResolverError.PLATFORM_FILTER) {
						VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_badFilter,
								header.getLineNumber(), severity, PDEMarkerFactory.CAT_OTHER);
						addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_INCOMPATIBLE_ENV);
					}
				}
			}
		} catch (InvalidSyntaxException ise) {
			VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_invalidFilterSyntax, header.getLineNumber(),
					CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_INCOMPATIBLE_ENV);
		}
	}

	private void validateEclipseGenericCapability() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_GENERIC_CAPABILITY);
		if (header == null) {
			return;
		}
		String message;
		if ((TargetPlatformHelper.getTargetVersion() >= 3.7) && isCheckDeprecated()) {
			message = NLS.bind(PDECoreMessages.BundleErrorReporter_eclipse_genericCapabilityDeprecated, ICoreConstants.ECLIPSE_GENERIC_CAPABILITY, Constants.PROVIDE_CAPABILITY);
			VirtualMarker marker = report(message, header.getLineNumber(), CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
		}

	}

	private void validateEclipseGenericRequire() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_GENERIC_REQUIRED);
		if (header == null) {
			return;
		}
		String message;
		if ((TargetPlatformHelper.getTargetVersion() >= 3.7) && isCheckDeprecated()) {
			message = NLS.bind(PDECoreMessages.BundleErrorReporter_eclipse_genericRequireDeprecated, ICoreConstants.ECLIPSE_GENERIC_REQUIRED, Constants.REQUIRE_CAPABILITY);
			VirtualMarker marker = report(message, header.getLineNumber(), CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
		}

	}

	private void validateBundleActivator() {
		IHeader header = getHeader(Constants.BUNDLE_ACTIVATOR);
		if (header == null) {
			return;
		}

		String activator = header.getValue();
		BundleDescription desc = fModel.getBundleDescription();
		if (desc != null && desc.getHost() != null) {
			report(PDECoreMessages.BundleErrorReporter_fragmentActivator, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			return;
		}

		if (isCheckUnknownClass() && PluginProject.isJavaProject(fProject)
				&& !PDEJavaHelper.isOnClasspath(activator, JavaCore.create(fProject))) {
			// Look for this activator in the project's classpath
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_NoExist, activator),
					getLine(header, activator), CompilerFlags.P_UNKNOWN_CLASS, PDEMarkerFactory.M_UNKNOWN_ACTIVATOR,
					PDEMarkerFactory.CAT_FATAL);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_UNKNOWN_CLASS);
		}
	}

	private void validateBundleClasspath() {
		IHeader header = getHeader(Constants.BUNDLE_CLASSPATH);
		if (header != null) {
			if (header.getElements().length == 0) {
				report(PDECoreMessages.BundleErrorReporter_ClasspathNotEmpty, header.getLineNumber(), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			}
		}
	}

	private void validateRequireBundle(IProgressMonitor monitor) {
		if (!isCheckUnresolvedImports()) {
			return;
		}

		IHeader header = getHeader(Constants.REQUIRE_BUNDLE);
		if (header == null) {
			return;
		}

		ManifestElement[] required = header.getElements();

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			for (ManifestElement element : required) {
				validateBundleVersionAttribute(header, element);
			}
			return;
		}

		BundleSpecification[] specs = desc.getRequiredBundles();
		if (specs.length != required.length) {
			// If the bundle description has stale data in it, don't compare it to the header data, see bug 308741
			specs = null;
		}

		for (int i = 0; i < required.length; i++) {
			checkCanceled(monitor);

			String bundleID = required[i].getValue();

			validateBundleVersionAttribute(header, required[i]);
			validateVisibilityDirective(header, required[i]);
			validateReprovideAttribute(header, required[i]);
			validateResolutionDirective(header, required[i]);
			validateOptionalAttribute(header, required[i]);
			validateFragmentHost(header, required[i]);

			boolean optional = isOptional(required[i]);
			int severity = getRequireBundleSeverity(required[i], optional);

			// It is possible for the bundle description to not match the headers
			if (specs != null && specs[i].getSupplier() == null) {
				if (desc.getContainingState().getBundle(specs[i].getName(), null) == null) {
					PDEState pdeState = TargetPlatformHelper.getPDEState();
					if (pdeState != null) {
						IPluginModelBase[] targetModels = pdeState.getTargetModels();
						if (targetModels != null && targetModels.length == 0) {
							VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_EmptyTargetPlatform, 1,
									severity, PDEMarkerFactory.M_REQ_BUNDLE_NOT_AVAILABLE, PDEMarkerFactory.CAT_FATAL);
							addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,
									CompilerFlags.P_UNRESOLVED_IMPORTS);
							return;
						}
					}
					VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistPDE, bundleID), getPackageLine(header, required[i]), severity, PDEMarkerFactory.M_REQ_BUNDLE_NOT_AVAILABLE, PDEMarkerFactory.CAT_FATAL);
					addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_UNRESOLVED_IMPORTS);
					if (marker != null) {
						marker.setAttribute("bundleId", required[i].getValue()); //$NON-NLS-1$
						if (optional) {
							marker.setAttribute("optional", true); //$NON-NLS-1$
						}
					}
				} else {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, bundleID + ": " + specs[i].getVersionRange()), //$NON-NLS-1$
							getPackageLine(header, required[i]), severity, PDEMarkerFactory.CAT_FATAL);
				}
			}
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.REQUIRE_BUNDLE), header.getLineNumber(), CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);
		}

	}

	private void validateBundleVersionAttribute(IHeader header, ManifestElement element) {
		String versionRange = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE);
		if (severity != CompilerFlags.IGNORE && versionRange == null) {
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()),
					getPackageLine(header, element), severity, PDEMarkerFactory.M_MISSINGVERSION_REQ_BUNDLE,
					PDEMarkerFactory.CAT_OTHER);
			marker.setAttribute("bundleId", element.getValue()); //$NON-NLS-1$
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE);
		}

		if (versionRange != null && !VersionUtil.validateVersionRange(versionRange).isOK()) {
			VirtualMarker marker = report(
					NLS.bind(UtilMessages.BundleErrorReporter_InvalidFormatInBundleVersion, element.getValue()),
					getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE);
		}
	}

	private void validateVisibilityDirective(IHeader header, ManifestElement element) {
		String visibility = element.getDirective(Constants.VISIBILITY_DIRECTIVE);
		if (visibility != null) {
			validateDirectiveValue(header, element, Constants.VISIBILITY_DIRECTIVE, new String[] {Constants.VISIBILITY_PRIVATE, Constants.VISIBILITY_REEXPORT});
		}
	}

	private void validateReprovideAttribute(IHeader header, ManifestElement element) {
		String message;
		String rexport = element.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, element, ICoreConstants.REPROVIDE_ATTRIBUTE);
			if (fOsgiR4 && isCheckDeprecated()) {
				message = NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_reprovide, ICoreConstants.REPROVIDE_ATTRIBUTE);
				VirtualMarker marker = report(message, getLine(header, ICoreConstants.REPROVIDE_ATTRIBUTE + "="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
			}
		}
	}

	private boolean isOptional(ManifestElement element) {
		return Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE)) || "true".equals(element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
	}

	private int getRequireBundleSeverity(ManifestElement requireBundleElement, boolean optional) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNRESOLVED_IMPORTS);
		// only for error, optional dependency should be shown as warning
		if (optional && severity == CompilerFlags.ERROR) {
			severity = CompilerFlags.WARNING;
		}
		return severity;
	}

	private void validateResolutionDirective(IHeader header, ManifestElement requireBundleElement) {
		String resolution = requireBundleElement.getDirective(Constants.RESOLUTION_DIRECTIVE);
		if (resolution != null) {
			validateDirectiveValue(header, requireBundleElement, Constants.RESOLUTION_DIRECTIVE, new String[] {Constants.RESOLUTION_MANDATORY, Constants.RESOLUTION_OPTIONAL});
		}
	}

	private void validateOptionalAttribute(IHeader header, ManifestElement element) {
		String rexport = element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, element, ICoreConstants.OPTIONAL_ATTRIBUTE);
			if (fOsgiR4 && isCheckDeprecated()) {
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_optional, ICoreConstants.OPTIONAL_ATTRIBUTE), getLine(header, ICoreConstants.OPTIONAL_ATTRIBUTE + "="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
			}
		}
	}

	private void validateImportPackage(IProgressMonitor monitor) {
		IHeader header = getHeader(Constants.IMPORT_PACKAGE);
		if (header == null) {
			return;
		}

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			ManifestElement[] elements = header.getElements();
			for (ManifestElement element : elements) {
				validateSpecificationVersionAttribute(header, element);
				validateImportPackageVersion(header, element);
			}
			return;
		}

		boolean hasUnresolved = false;
		VersionConstraint[] constraints = desc.getContainingState().getStateHelper().getUnsatisfiedConstraints(desc);
		for (VersionConstraint constraint : constraints) {
			if (constraint instanceof ImportPackageSpecification) {
				hasUnresolved = true;
				break;
			}
		}

		HashMap<String, ExportPackageDescription> exported = getAvailableExportedPackages(desc.getContainingState());

		ImportPackageSpecification[] imports = desc.getImportPackages();
		if (desc.hasDynamicImports()) {
			List<ImportPackageSpecification> staticImportsList = new ArrayList<>();
			for (int i = 0; i < imports.length; ++i) {
				if (!imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC)) {
					staticImportsList.add(imports[i]);
				}
			}
			imports = staticImportsList.toArray(new ImportPackageSpecification[staticImportsList.size()]);
		}

		ManifestElement[] elements = header.getElements();
		int index = 0;
		for (ManifestElement element : elements) {
			checkCanceled(monitor);

			validateSpecificationVersionAttribute(header, element);
			validateResolutionDirective(header, element);

			// TODO we should only validate versions that we have a match
			validateImportPackageVersion(header, element);

			if (!hasUnresolved) {
				continue;
			}

			int length = element.getValueComponents().length;
			for (int j = 0; j < length; j++) {
				ImportPackageSpecification importSpec = imports[index++];
				if (importSpec.isResolved() || !isCheckUnresolvedImports()) {
					continue;
				}
				String name = importSpec.getName();
				boolean optional = isOptional(element);
				int severity = getRequireBundleSeverity(element, optional);

				ExportPackageDescription export = exported.get(name);
				if (export != null) {
					if (export.getSupplier().isResolved()) {
						Version version = export.getVersion();
						VersionRange range = importSpec.getVersionRange();
						if (range != null && !range.includes(version)) {
							VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_unsatisfiedConstraint,importSpec.toString()),getPackageLine(header, element), severity, PDEMarkerFactory.CAT_FATAL);
							addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,CompilerFlags.P_UNRESOLVED_IMPORTS);
							return;
						}
					} else {
						VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedExporter,new String[] { export.getSupplier().getSymbolicName(), name }),getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
						addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_UNRESOLVED_IMPORTS);
						return;
					}
				}

				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_PackageNotExported, name),
						getPackageLine(header, element), severity, PDEMarkerFactory.M_IMPORT_PKG_NOT_AVAILABLE,
						PDEMarkerFactory.CAT_FATAL);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_UNRESOLVED_IMPORTS);
				if (marker != null) {
					marker.setAttribute("packageName", name); //$NON-NLS-1$
					if (optional) {
						marker.setAttribute("optional", true); //$NON-NLS-1$
					}
				}
			}
		}
	}

	private HashMap<String, ExportPackageDescription> getAvailableExportedPackages(State state) {
		BundleDescription[] bundles = state.getBundles();

		HashMap<String, ExportPackageDescription> exported = new HashMap<>();
		for (BundleDescription bundle : bundles) {
			ExportPackageDescription[] exports = bundle.getExportPackages();
			for (ExportPackageDescription export : exports) {
				String name = export.getName();
				if (exported.containsKey(name)) {
					if (export.getSupplier().isResolved()) {
						exported.put(name, export);
					}
				} else {
					exported.put(name, export);
				}
			}
		}
		return exported;
	}

	protected void validateExportPackage(IProgressMonitor monitor) {
		IHeader header = getHeader(Constants.EXPORT_PACKAGE);
		if (header == null) {
			return;
		}

		String message = null;
		ManifestElement[] elements = header.getElements();

		for (ManifestElement element : elements) {
			checkCanceled(monitor);

			validateExportPackageVersion(header, element);
			validateSpecificationVersionAttribute(header, element);
			validateX_InternalDirective(header, element);
			validateX_FriendsDirective(header, element);

			String[] valueComps = element.getValueComponents();
			for (String valueComp : valueComps) {
				String name = valueComp;
				if (name.equals("java") || name.startsWith("java.")) { //$NON-NLS-1$ //$NON-NLS-2$
					IHeader jreHeader = getHeader(ICoreConstants.ECLIPSE_JREBUNDLE);
					if (jreHeader == null || !"true".equals(jreHeader.getValue())) { //$NON-NLS-1$
						message = PDECoreMessages.BundleErrorReporter_exportNoJRE;
						report(message, getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.M_JAVA_PACKAGE__PORTED, PDEMarkerFactory.CAT_FATAL);
					}
				} else if (".".equals(name.trim())) { //$NON-NLS-1$
					// workaround for manifest converter generating "."
					continue;
				}

				if (!isCheckUnresolvedImports()) {
					continue;
				}

				/* The exported package does not exist in the bundle.  Allow project folders to be packages (see bug 166680 comment 17 and bug 575419)*/
				if (!getExportedPackages().contains(name) && !isFolderOnClasspath(name)) {
					message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, name);
					VirtualMarker marker = report(message, getPackageLine(header, element), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.M_EXPORT_PKG_NOT_EXIST, PDEMarkerFactory.CAT_OTHER);
					addMarkerAttribute(marker, "packageName", name); //$NON-NLS-1$
					addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_UNRESOLVED_IMPORTS);

				}
			}
		}

	}

	private boolean isFolderOnClasspath(String name) {
		if (name.contains("/") || name.contains("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
			// According to the OSGi specification package name elements may
			// only be separated by a dot. But the subsequent code would find
			// corresponding folders even if the names where separated by
			// slashes (forward or backward). This premature checks prevents it.
			return false;
		}
		String folderName = name.replace('.', '/');
		if (fProject.getFolder(folderName).exists()) {
			return true; // package is a folder in the project
		}
		IHeader bundleClasspath = getHeader(Constants.BUNDLE_CLASSPATH);
		// Search jars on the bundle-classpath for the given package
		return bundleClasspath != null && Arrays.stream(bundleClasspath.getElements()).map(e -> e.getValue().strip())
				.filter(e -> e.endsWith(".jar")).map(fProject::getFile).filter(IFile::exists).anyMatch(f -> { //$NON-NLS-1$
					try (JarFile jar = new JarFile(new File(f.getLocationURI()))) {
						return jar.getJarEntry(folderName) != null;
					} catch (Exception e) {
						return false;
					}
				});
	}

	private boolean containsPackage(IHeader header, String name) {
		if (header != null) {
			ManifestElement[] elements = header.getElements();
			for (ManifestElement element : elements) {
				if (element.getValue().equals(name)) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<String> getExportedPackages() {
		if (fProjectPackages == null) {
			fProjectPackages = new HashSet<>();
			addProjectPackages(fProject);
			BundleDescription desc = fModel.getBundleDescription();
			if (desc != null) {
				HostSpecification host = desc.getHost();
				if (host != null) {
					addHostPackages(host.getName());
				} else {
					addFragmentPackages(desc.getFragments());
				}
			}
		}
		return fProjectPackages;
	}

	private void addHostPackages(String hostID) {
		IPluginModelBase model = PluginRegistry.findModel(hostID);
		if (model != null) {
			IResource resource = model.getUnderlyingResource();
			if (resource != null) {
				addProjectPackages(resource.getProject());
			} else if (PluginProject.isJavaProject(fProject)) {
				try {
					IPackageFragment[] fragments = PluginJavaSearchUtil.collectPackageFragments(new IPluginModelBase[] { model }, JavaCore.create(fProject), false);
					for (IPackageFragment fragment : fragments) {
						fProjectPackages.add(fragment.getElementName());
					}
				} catch (CoreException ce) {
				}
			}
		}
	}

	private void addFragmentPackages(BundleDescription[] fragments) {
		for (BundleDescription fragment : fragments) {
			String id = fragment.getSymbolicName();
			IPluginModelBase model = PluginRegistry.findModel(id);
			IResource resource = model instanceof IFragmentModel ? model.getUnderlyingResource() : null;
			if (resource != null) {
				addProjectPackages(resource.getProject());
			}
		}
	}

	private void addProjectPackages(IProject proj) {
		if (PluginProject.isJavaProject(proj)) {
			try {
				IPackageFragmentRoot[] roots = JavaCore.create(proj).getPackageFragmentRoots();
				for (IPackageFragmentRoot root : roots) {
					if (root.getKind() == IPackageFragmentRoot.K_SOURCE
							|| (root.getKind() == IPackageFragmentRoot.K_BINARY && !root.isExternal())) {
						IJavaElement[] children = root.getChildren();
						for (IJavaElement element : children) {
							IPackageFragment f = (IPackageFragment) element;
							String name = f.getElementName();
							if (name.equals("")) { //$NON-NLS-1$
								name = "."; //$NON-NLS-1$
							}
							if (f.hasChildren() || f.getNonJavaResources().length > 0) {
								fProjectPackages.add(name);
							}
						}
					}
				}
			} catch (CoreException ce) {
			}
		}
	}

	protected boolean isCheckDeprecated() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckNoRequiredAttr() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_NO_REQUIRED_ATT) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckUnknownClass() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_CLASS) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckUnresolvedImports() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_UNRESOLVED_IMPORTS) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckMissingExportPackageVersion() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_EXP_PKG) != CompilerFlags.IGNORE;
	}

	protected boolean isCheckMissingImportPackageVersion() {
		return CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_IMP_PKG) != CompilerFlags.IGNORE;
	}

	private void validateTranslatableHeaders() {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_NOT_EXTERNALIZED);
		if (severity == CompilerFlags.IGNORE) {
			return;
		}

		for (String element : ICoreConstants.TRANSLATABLE_HEADERS) {
			IHeader header = getHeader(element);
			if (header != null) {
				String value = header.getValue();
				if (!value.startsWith("%")) { //$NON-NLS-1$
					VirtualMarker marker = report(NLS.bind(PDECoreMessages.Builders_Manifest_non_ext_attribute, header.getName()), getLine(header, value), severity, PDEMarkerFactory.P_UNTRANSLATED_NODE, header.getName(), PDEMarkerFactory.CAT_NLS);
					addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_NOT_EXTERNALIZED);
				} else if (fModel instanceof AbstractNLModel) {
					NLResourceHelper helper = ((AbstractNLModel) fModel).getNLResourceHelper();
					if (helper == null || !helper.resourceExists(value)) {
						VirtualMarker marker = report(NLS.bind(PDECoreMessages.Builders_Manifest_key_not_found, value.substring(1), PDEManager.getBundleLocalization(fModel).concat(".properties")), getLine(header, value), severity, PDEMarkerFactory.CAT_NLS); //$NON-NLS-1$
						addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_NOT_EXTERNALIZED);
					}
				}
			}
		}
	}

	private void validateSpecificationVersionAttribute(IHeader header, ManifestElement element) {
		String version = element.getAttribute(ICoreConstants.PACKAGE_SPECIFICATION_VERSION);
		IStatus status = VersionUtil.validateVersion(version);
		if (!status.isOK()) {
			report(status.getMessage(), getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
		}
		if (isCheckDeprecated()) {
			if (fOsgiR4 && version != null) {
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_specification_version, ICoreConstants.PACKAGE_SPECIFICATION_VERSION), getPackageLine(header, element), CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
			}
		}
	}

	private void validateImportPackageVersion(IHeader header, ManifestElement element) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_IMP_PKG);
		if (severity != CompilerFlags.IGNORE && version == null) {
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()), getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_MISSING_VERSION_IMP_PKG);
		}
		validateVersionAttribute(header, element, true);
	}

	private void validateExportPackageVersion(IHeader header, ManifestElement element) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_EXP_PKG);
		if (severity != CompilerFlags.IGNORE && version == null) {
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()), getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_MISSING_VERSION_EXP_PKG);
		}
		validateVersionAttribute(header, element, false);
	}

	private void validateVersionAttribute(IHeader header, ManifestElement element, boolean range) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		if (version != null) {
			IStatus status = range ? VersionUtil.validateVersionRange(version) : VersionUtil.validateVersion(version);
			if (!status.isOK()) {
				report(status.getMessage(), getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			}
		}
	}

	private void validateX_InternalDirective(IHeader header, ManifestElement element) {
		String internal = element.getDirective(ICoreConstants.INTERNAL_DIRECTIVE);
		if (internal == null) {
			return;
		}

		for (String value : BOOLEAN_VALUES) {
			if (value.equals(internal)) {
				return;
			}
		}
		String message = NLS.bind(PDECoreMessages.BundleErrorReporter_dir_value, (new String[] {internal, ICoreConstants.INTERNAL_DIRECTIVE}));
		report(message, getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
	}

	private void validateX_FriendsDirective(IHeader header, ManifestElement element) {
		String friends = element.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		String internal = element.getDirective(ICoreConstants.INTERNAL_DIRECTIVE);
		if (friends != null && internal != null) {
			String message = NLS.bind(PDECoreMessages.BundleErrorReporter_directive_hasNoEffectWith_, new String[] {ICoreConstants.FRIENDS_DIRECTIVE, ICoreConstants.INTERNAL_DIRECTIVE});
			VirtualMarker marker = report(message, getPackageLine(header, element), CompilerFlags.WARNING, PDEMarkerFactory.M_DIRECTIVE_HAS_NO_EFFECT, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, "packageName", element.getValue()); //$NON-NLS-1$
		}
	}

	private void validateBundleActivatorPolicy() {
		IHeader header = getHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (header == null) {
			return;
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_ACTIVATIONPOLICY), header.getLineNumber(), CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);
		}
		if (TargetPlatformHelper.getTargetVersion() >= 3.3) {
			validateHeaderValue(header, new String[] {Constants.ACTIVATION_LAZY});
		} else if (severity != CompilerFlags.IGNORE && !containsValidActivationHeader()) {
			VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_bundleActivationPolicy_unsupported, header.getLineNumber(), severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_DEPRECATED);
		}
	}

	private void validateAutoStart() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_AUTOSTART);
		if (!validateStartHeader(header)) {
			return; // valid start header problems already reported
		}

		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (severity != CompilerFlags.IGNORE && TargetPlatformHelper.getTargetVersion() >= 3.2 && !containsValidActivationHeader()) {
			String message = NLS.bind(PDECoreMessages.BundleErrorReporter_startHeader_autoStartDeprecated, new Object[] {ICoreConstants.ECLIPSE_AUTOSTART, getCurrentActivationHeader()});
			VirtualMarker marker = report(message, header.getLineNumber(), severity, PDEMarkerFactory.M_DEPRECATED_AUTOSTART, PDEMarkerFactory.CAT_DEPRECATION);
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_DEPRECATED);
			if (marker != null) {
				marker.setAttribute(PDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_AUTOSTART);
				ManifestElement elem = header.getElements()[0];
				boolean unnecessary = elem.getValue().equals("false") && elem.getAttribute("excludes") == null; //$NON-NLS-1$ //$NON-NLS-2$
				marker.setAttribute(PDEMarkerFactory.ATTR_CAN_ADD, !unnecessary);
			}
		}
	}

	private void validateLazyStart() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_LAZYSTART);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		validateStartHeader(header);
		if (header != null) {
			if (severity == CompilerFlags.IGNORE || containsValidActivationHeader()) {
				return;
			}
			double targetVersion = TargetPlatformHelper.getTargetVersion();
			if (targetVersion < 3.2) {
				VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_lazyStart_unsupported, header.getLineNumber(), severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_DEPRECATED);
			} else if (targetVersion > 3.3) {
				int line = header.getLineNumber();
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_startHeader_autoStartDeprecated, new Object[] {ICoreConstants.ECLIPSE_LAZYSTART, getCurrentActivationHeader()});
				VirtualMarker marker = report(message, line, severity, PDEMarkerFactory.M_DEPRECATED_AUTOSTART, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_DEPRECATED);
				if (marker != null) {
					marker.setAttribute(PDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_LAZYSTART);
					ManifestElement elem = header.getElements()[0];

					boolean unnecessary = elem.getValue().equals("false") && elem.getAttribute("excludes") == null; //$NON-NLS-1$ //$NON-NLS-2$
					marker.setAttribute(PDEMarkerFactory.ATTR_CAN_ADD, !unnecessary);
				}
			}
		}
	}

	private boolean containsValidActivationHeader() {
		String header;
		double targetVersion = TargetPlatformHelper.getTargetVersion();
		if (targetVersion < 3.2) {
			header = ICoreConstants.ECLIPSE_AUTOSTART;
		} else if (targetVersion < 3.4) {
			header = ICoreConstants.ECLIPSE_LAZYSTART;
		} else {
			header = Constants.BUNDLE_ACTIVATIONPOLICY;
		}

		return getHeader(header) != null;
	}

	private String getCurrentActivationHeader() {
		double targetVersion = TargetPlatformHelper.getTargetVersion();
		if (targetVersion < 3.2) {
			return ICoreConstants.ECLIPSE_AUTOSTART;
		} else if (targetVersion < 3.4) {
			return ICoreConstants.ECLIPSE_LAZYSTART;
		}
		return Constants.BUNDLE_ACTIVATIONPOLICY;
	}

	private boolean validateStartHeader(IHeader header) {
		if (header == null) {
			return false;
		}
		validateBooleanValue(header);
		return exceptionsAttributesValid(header, header.getElements());
	}

	private boolean exceptionsAttributesValid(IHeader header, ManifestElement[] elements) {
		if (elements == null || elements.length == 0) {
			return true;
		}
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
		if (severity == CompilerFlags.IGNORE) {
			return true;
		}
		Enumeration<String> keys = elements[0].getKeys();
		if (keys != null && keys.hasMoreElements()) {
			String key = keys.nextElement();
			if ("exceptions".equals(key)) { //$NON-NLS-1$
				String[] values = elements[0].getAttributes(key);
				for (String value : values) {
					StringTokenizer st = new StringTokenizer(value, ","); //$NON-NLS-1$
					while (st.hasMoreTokens()) {
						String name = st.nextToken().trim();
						if (!getExportedPackages().contains(name)) {
							String message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, name);
							VirtualMarker marker = report(message, getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_OTHER);
							addMarkerAttribute(marker,PDEMarkerFactory.compilerKey, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	private void validateExtensibleAPI() {
		IHeader header = getHeader(ICoreConstants.EXTENSIBLE_API);
		if (header != null) {
			validateBooleanValue(header);
		}
	}

	public VirtualMarker report(String message, int line, int severity, int problemID, String headerName, String category) {
		VirtualMarker marker = report(message, line, severity, problemID, category);
		if (marker != null) {
			marker.setAttribute(PDEMarkerFactory.MPK_LOCATION_PATH, headerName);
		}
		return marker;
	}

	private void validateImportExportServices() {
		if (fOsgiR4) {
			IHeader importHeader = getHeader(ICoreConstants.IMPORT_SERVICE);
			IHeader exportHeader = getHeader(ICoreConstants.EXPORT_SERVICE);
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);

			if (severity == CompilerFlags.IGNORE) {
				return;
			}

			if (importHeader != null) {
				int line = importHeader.getLineNumber();
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_importexport_servicesDeprecated, ICoreConstants.IMPORT_SERVICE), line, severity, PDEMarkerFactory.M_DEPRECATED_IMPORT_SERVICE, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_DEPRECATED);
			}

			if (exportHeader != null) {
				int line = exportHeader.getLineNumber();
				VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_importexport_servicesDeprecated, ICoreConstants.EXPORT_SERVICE), line, severity, PDEMarkerFactory.M_DEPRECATED_EXPORT_SERVICE, PDEMarkerFactory.CAT_DEPRECATION);
				addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_DEPRECATED);
			}
		}
	}

	private void validateBundleLocalization() {
		IHeader header = getHeader(Constants.BUNDLE_LOCALIZATION);
		if (header == null) {
			return;
		}
		String location = header.getValue();
		String fileName = null;
		int index = location.lastIndexOf('/');
		if (index > 0) {
			fileName = location.substring(index + 1);
			location = location.substring(0, index);
		} else {
			fileName = location;
			location = ""; //$NON-NLS-1$
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_LOCALIZATION), header.getLineNumber(), CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);
		}

		IResource res = PDEProject.getBundleRoot(fProject).findMember(location);
		if (res == null || !(res instanceof IContainer folder)) {
			VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_localization_folder_not_exist, header.getLineNumber(), CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE), PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_UNKNOWN_RESOURCE);
			return;
		}
		try {
			IResource[] children = folder.members();
			for (int i = 0; i < children.length; i++) {
				if (!(children[i] instanceof IFile)) {
					continue;
				}
				String childName = children[i].getName();
				if (childName.endsWith(".properties") && childName.startsWith(fileName)) { //$NON-NLS-1$
					return;
				}
			}
		} catch (CoreException e) {
		}
		VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_localization_properties_file_not_exist, header.getLineNumber(), CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE), PDEMarkerFactory.CAT_OTHER);
		addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_UNKNOWN_RESOURCE);
	}

	private void validateFragmentHost(IHeader requireBundleHeader, ManifestElement element) {
		IHeader header = getHeader(Constants.FRAGMENT_HOST);
		if (header == null) {
			return;
		}

		ManifestElement[] elements = header.getElements();

		if (elements[0] != null && elements[0].getValue().equals(element.getValue())) {
			VirtualMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_unecessaryDependencyDueToFragmentHost, element.getValue()), getPackageLine(requireBundleHeader, element), CompilerFlags.WARNING, PDEMarkerFactory.M_UNECESSARY_DEP, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, "bundleId", element.getValue()); //$NON-NLS-1$
			addMarkerAttribute(marker,PDEMarkerFactory.compilerKey,  CompilerFlags.P_UNRESOLVED_IMPORTS);

		}
	}

	private void validateProvidePackage() {
		IHeader header = getHeader(ICoreConstants.PROVIDE_PACKAGE);
		if (header == null) {
			return;
		}

		if (fOsgiR4 && isCheckDeprecated()) {
			VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_providePackageHeaderDeprecated, header.getLineNumber(), CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_DEPRECATED_PROVIDE_PACKAGE, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey,CompilerFlags.P_DEPRECATED );
		}
	}

	private void addMarkerAttribute(VirtualMarker marker, String attr, String value) {
		if (marker != null) {
			marker.setAttribute(attr, value);
		}
	}

	/**
	 * Verifies that if a Service-Component exists then a corresponding Bundle-ActivationPolicy is present.
	 */
	private void validateServiceComponent() {
		IHeader header = getHeader(ICoreConstants.SERVICE_COMPONENT);
		if (header == null) {
			return;
		}

		if (getHeader(Constants.BUNDLE_ACTIVATIONPOLICY) != null) {
			return;
		}

		int compilerFlag = CompilerFlags.getFlag(fProject, CompilerFlags.P_SERVICE_COMP_WITHOUT_LAZY_ACT);
		if (compilerFlag != CompilerFlags.IGNORE) {
			VirtualMarker marker = report(PDECoreMessages.BundleErrorReporter_serviceComponentLazyStart, header.getLineNumber(), CompilerFlags.P_SERVICE_COMP_WITHOUT_LAZY_ACT, PDEMarkerFactory.M_SERVICECOMPONENT_MISSING_LAZY, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, PDEMarkerFactory.compilerKey, CompilerFlags.P_SERVICE_COMP_WITHOUT_LAZY_ACT);
		}
	}
}
