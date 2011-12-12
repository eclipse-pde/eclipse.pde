/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Code 9 Corporation - on going enhancements and maintenance
 *     Brock Janiczak <brockj@tpg.com.au> - bug 169373
 *     Gary Duprex <Gary.Duprex@aspectstools.com> - bug 150225
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 209432, 214156
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.util.*;
import org.osgi.framework.*;

public class BundleErrorReporter extends JarManifestErrorReporter {

	private boolean fOsgiR4;
	private IPluginModelBase fModel;
	private Set fProjectPackages;

	public BundleErrorReporter(IFile file) {
		super(file);
	}

	public void validateContent(IProgressMonitor monitor) {
		super.validateContent(monitor);
		if (fHeaders == null || getErrorCount() > 0)
			return;

		fModel = PluginRegistry.findModel(fProject);
		// be paranoid.  something could have gone wrong reading the file etc.
		if (fModel == null || !validateBundleSymbolicName())
			return;

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
	}

	private boolean validateBundleManifestVersion() {
		IHeader header = getHeader(Constants.BUNDLE_MANIFESTVERSION);
		if (header != null) {
			String version = header.getValue();
			if (!(fOsgiR4 = "2".equals(version)) && !"1".equals(version)) { //$NON-NLS-1$ //$NON-NLS-2$
				report(PDECoreMessages.BundleErrorReporter_illegalManifestVersion, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
				return false;
			}
		}
		return true;
	}

	private void validateExportPackages() {
		IHeader header = getHeader(Constants.EXPORT_PACKAGE);

		// check for missing exported packages
		if (fModel instanceof IBundlePluginModelBase) {
			IBundlePluginModelBase bundleModel = (IBundlePluginModelBase) fModel;
			IBundle bundle = bundleModel.getBundleModel().getBundle();
			IManifestHeader bundleClasspathheader = bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);

			IPackageFragmentRoot[] roots = ManifestUtils.findPackageFragmentRoots(bundleClasspathheader, fProject);
			// Running list of packages in the project
			//Set packages = new HashSet();
			StringBuffer packages = new StringBuffer();
			for (int i = 0; i < roots.length; i++) {
				try {
					if (ManifestUtils.isImmediateRoot(roots[i])) {
						IJavaElement[] javaElements = roots[i].getChildren();
						for (int j = 0; j < javaElements.length; j++)
							if (javaElements[j] instanceof IPackageFragment) {
								IPackageFragment fragment = (IPackageFragment) javaElements[j];
								String name = fragment.getElementName();
								if (name.length() == 0)
									name = "."; //$NON-NLS-1$
								if (fragment.containsJavaResources() || fragment.getNonJavaResources().length > 0) {
									if (!containsPackage(header, name)) {
										packages.append(name);
										if (j < javaElements.length - 1)
											packages.append(","); //$NON-NLS-1$

									}
								}
							}
					}
				} catch (JavaModelException e) {
				}
			}

			// if we actually have packages to add
			if (packages.toString().length() > 0) {
				IMarker marker = report(PDECoreMessages.BundleErrorReporter_missingPackagesInProject, header == null ? 1 : header.getLineNumber() + 1, CompilerFlags.P_MISSING_EXPORT_PKGS, PDEMarkerFactory.M_MISSING_EXPORT_PKGS, PDEMarkerFactory.CAT_OTHER);
				addMarkerAttribute(marker, "packages", packages.toString()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * @return boolean false if fatal
	 */
	private boolean validateBundleSymbolicName() {
		IHeader header = validateRequiredHeader(Constants.BUNDLE_SYMBOLICNAME);
		if (header == null)
			return false;

		ManifestElement[] elements = header.getElements();
		String id = elements.length > 0 ? elements[0].getValue() : null;
		if (id == null || id.length() == 0) {
			report(PDECoreMessages.BundleErrorReporter_NoSymbolicName, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			return false;
		}

		if (!validateBundleManifestVersion())
			return false;
		validatePluginId(header, id);
		validateSingleton(header, elements[0]);

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4)
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_SYMBOLICNAME), header.getLineNumber() + 1, CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);

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
							report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
									CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_DEPRECATION);
							return;
						}
					} else {
						String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonRequired, Constants.SINGLETON_DIRECTIVE);
						report(message, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_FATAL);
						return;
					}
				}
			} else if (!"true".equals(singletonAttr)) { //$NON-NLS-1$
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonAttrRequired, ICoreConstants.SINGLETON_ATTRIBUTE);
				report(message, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_ATT_NOT_SET, PDEMarkerFactory.CAT_OTHER);
				return;
			}
		}

		if (fOsgiR4) {
			if (singletonAttr != null) {
				if (isCheckDeprecated()) {
					String message = PDECoreMessages.BundleErrorReporter_deprecated_attribute_singleton;
					report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
							CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET, PDEMarkerFactory.CAT_OTHER);
				}
			}
		} else if (singletonDir != null) {
			if (isCheckDeprecated()) {
				String message = PDECoreMessages.BundleErrorReporter_unsupportedSingletonDirective;
				report(message, getLine(header, Constants.SINGLETON_DIRECTIVE + ":="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SUPPORTED, PDEMarkerFactory.CAT_OTHER);

			}
		}
		validateBooleanAttributeValue(header, element, ICoreConstants.SINGLETON_ATTRIBUTE);
		validateBooleanDirectiveValue(header, element, Constants.SINGLETON_DIRECTIVE);
	}

	private void validateFragmentHost() {
		IHeader header = getHeader(Constants.FRAGMENT_HOST);
		if (header == null) {
			if (isCheckNoRequiredAttr() && PDEProject.getFragmentXml(fProject).exists()) {
				report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT, PDEMarkerFactory.CAT_FATAL);
			}
			return;
		}

		if (header.getElements().length == 0) {
			if (isCheckNoRequiredAttr())
				report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT, PDEMarkerFactory.CAT_FATAL);
			return;
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4)
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.FRAGMENT_HOST), header.getLineNumber() + 1, CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);

		if (!isCheckUnresolvedImports())
			return;

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			ManifestElement[] elems = header.getElements();
			if (elems.length > 0) {
				if (!VersionUtil.validateVersionRange(elems[0].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE)).isOK()) {
					int line = getLine(header, header.getValue());
					report(PDECoreMessages.BundleErrorReporter_InvalidFormatInBundleVersion, line, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
				}
			}
			return;
		}

		HostSpecification host = desc.getHost();
		if (host == null)
			return;

		String name = host.getName();
		if (host.getSupplier() == null) {
			boolean missingHost = false;
			ResolverError[] errors = desc.getContainingState().getResolverErrors(desc);
			for (int i = 0; i < errors.length; i++) {
				if (errors[i].getType() == ResolverError.MISSING_FRAGMENT_HOST) {
					missingHost = true;
					break;
				}
			}

			if (missingHost) {
				BundleDescription[] suppliers = desc.getContainingState().getBundles(name);
				boolean resolved = true;
				for (int i = 0; i < suppliers.length; i++) {
					if (suppliers[i].getHost() != null)
						continue;
					if (suppliers[i].isResolved()) {
						Version version = suppliers[i].getVersion();
						// use fully qualified name to avoid conflict with other VersionRange class
						org.eclipse.osgi.service.resolver.VersionRange range = host.getVersionRange();
						if (!range.isIncluded(version)) {
							String versionRange = host.getVersionRange().toString();
							report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, versionRange), getLine(header, versionRange), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
							return;
						}
					} else {
						resolved = false;
					}
				}

				if (!resolved) {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedHost, name), getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
					return;
				}
			}
		}

		IPluginModelBase model = PluginRegistry.findModel(name);
		if (model == null || model instanceof IFragmentModel || !model.isEnabled()) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_HostNotExistPDE, name), getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateBundleVersion() {
		IHeader header = validateRequiredHeader(Constants.BUNDLE_VERSION);
		if (header == null)
			return;

		IStatus status = VersionUtil.validateVersion(header.getValue());
		if (!status.isOK()) {
			int line = getLine(header, header.getValue());
			report(status.getMessage(), line, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
		}
	}

	private void validateRequiredExecutionEnvironment() {
		int sev = CompilerFlags.getFlag(fProject, CompilerFlags.P_INCOMPATIBLE_ENV);
		if (sev == CompilerFlags.IGNORE)
			return;
		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null)
			return;

		// if we aren't a java project, let's not check for a BREE
		try {
			if (!fProject.hasNature(JavaCore.NATURE_ID))
				return;
		} catch (CoreException e) {
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

					for (int i = 0; i < entries.length; i++) {
						if (entries[i].getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
							IPath currentPath = entries[i].getPath();
							if (JavaRuntime.newDefaultJREContainerPath().matchingFirstSegments(currentPath) > 0) {
								String eeId = JavaRuntime.getExecutionEnvironmentId(currentPath);
								if (eeId != null) {
									IMarker marker = report(PDECoreMessages.BundleErrorReporter_noExecutionEnvironmentSet, 1, sev, PDEMarkerFactory.M_EXECUTION_ENVIRONMENT_NOT_SET, PDEMarkerFactory.CAT_EE);
									addMarkerAttribute(marker, "ee_id", eeId); //$NON-NLS-1$
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
				for (int i = 0; i < systemEnvs.length; i++) {
					// Get strictly compatible EE for the default VM
					if (systemEnvs[i].isStrictlyCompatible(vm)) {
						IMarker marker = report(PDECoreMessages.BundleErrorReporter_noExecutionEnvironmentSet, 1, sev, PDEMarkerFactory.M_EXECUTION_ENVIRONMENT_NOT_SET, PDEMarkerFactory.CAT_EE);
						addMarkerAttribute(marker, "ee_id", systemEnvs[i].getId()); //$NON-NLS-1$
						break;
					}
				}
			}
			return;
		}

		IHeader header = getHeader(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
		if (header == null)
			return;

		IExecutionEnvironment env = JavaRuntime.getExecutionEnvironmentsManager().getEnvironment(bundleEnvs[0]);
		if (env != null) {
			IJavaProject jproject = JavaCore.create(fProject);
			IClasspathEntry[] entries;
			try {
				entries = jproject.getRawClasspath();
				for (int i = 0; i < entries.length; i++) {
					if (entries[i].getEntryKind() != IClasspathEntry.CPE_CONTAINER)
						continue;
					IPath currentPath = entries[i].getPath();
					if (JavaRuntime.newDefaultJREContainerPath().matchingFirstSegments(currentPath) == 0)
						continue;

					IPath validPath = JavaRuntime.newJREContainerPath(env);
					if (!validPath.equals(currentPath)) {
						// Check if the user is using a perfect match JRE
						IVMInstall vm = JavaRuntime.getVMInstall(currentPath);
						if (vm == null || !env.isStrictlyCompatible(vm)) {
							report(NLS.bind(PDECoreMessages.BundleErrorReporter_reqExecEnv_conflict, bundleEnvs[0]), getLine(header, bundleEnvs[0]), sev, PDEMarkerFactory.M_MISMATCHED_EXEC_ENV, PDEMarkerFactory.CAT_EE);
						}
					}
				}
			} catch (JavaModelException e) {
			}
		}
		IExecutionEnvironment[] systemEnvs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		for (int i = 0; i < bundleEnvs.length; i++) {
			boolean found = false;
			for (int j = 0; j < systemEnvs.length; j++) {
				if (bundleEnvs[i].equals(systemEnvs[j].getId())) {
					found = true;
					break;
				}
			}
			if (!found) {
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_reqExecEnv_unknown, bundleEnvs[i]), getLine(header, bundleEnvs[i]), sev, PDEMarkerFactory.M_UNKNOW_EXEC_ENV, PDEMarkerFactory.CAT_EE);
				break;
			}
		}
	}

	/**
	 * Validates the Eclipse-BundleShape header
	 * 
	 * @since 3.5
	 */
	private void validateEclipseBundleShape() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_BUNDLE_SHAPE);
		if (header == null)
			return;

		String value = header.getValue();
		if (value != null) {
			validateHeaderValue(header, ICoreConstants.SHAPE_VALUES);
		}

	}

	private void validateEclipsePlatformFilter() {
		IHeader header = getHeader(ICoreConstants.PLATFORM_FILTER);
		if (header == null)
			return;

		try {
			PDECore.getDefault().getBundleContext().createFilter(header.getValue());
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_INCOMPATIBLE_ENV);
			if (severity == CompilerFlags.IGNORE)
				return;
			BundleDescription desc = fModel.getBundleDescription();
			if (desc != null && !desc.isResolved()) {
				ResolverError[] errors = desc.getContainingState().getResolverErrors(desc);
				for (int i = 0; i < errors.length; i++) {
					if (errors[i].getType() == ResolverError.PLATFORM_FILTER) {
						report(PDECoreMessages.BundleErrorReporter_badFilter, header.getLineNumber() + 1, severity, PDEMarkerFactory.CAT_OTHER);
					}
				}
			}
		} catch (InvalidSyntaxException ise) {
			report(PDECoreMessages.BundleErrorReporter_invalidFilterSyntax, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
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
			report(message, header.getLineNumber() + 1, CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
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
			report(message, header.getLineNumber() + 1, CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
		}

	}

	private void validateBundleActivator() {
		IHeader header = getHeader(Constants.BUNDLE_ACTIVATOR);
		if (header == null)
			return;

		String activator = header.getValue();
		BundleDescription desc = fModel.getBundleDescription();
		if (desc != null && desc.getHost() != null) {
			report(PDECoreMessages.BundleErrorReporter_fragmentActivator, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			return;
		}

		if (isCheckUnknownClass()) {
			try {
				if (fProject.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(fProject);

					// Look for this activator in the project's classpath
					if (!PDEJavaHelper.isOnClasspath(activator, javaProject)) {
						report(NLS.bind(PDECoreMessages.BundleErrorReporter_NoExist, activator), getLine(header, activator), CompilerFlags.P_UNKNOWN_CLASS, PDEMarkerFactory.M_UNKNOWN_ACTIVATOR, PDEMarkerFactory.CAT_FATAL);
					}
				}
			} catch (CoreException ce) {
			}
		}
	}

	private void validateBundleClasspath() {
		IHeader header = getHeader(Constants.BUNDLE_CLASSPATH);
		if (header != null) {
			if (header.getElements().length == 0) {
				report(PDECoreMessages.BundleErrorReporter_ClasspathNotEmpty, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
			}
		}
	}

	private void validateRequireBundle(IProgressMonitor monitor) {
		if (!isCheckUnresolvedImports())
			return;

		IHeader header = getHeader(Constants.REQUIRE_BUNDLE);
		if (header == null)
			return;

		ManifestElement[] required = header.getElements();

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			for (int i = 0; i < required.length; i++)
				validateBundleVersionAttribute(header, required[i]);
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
					IMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistPDE, bundleID), getPackageLine(header, required[i]), severity, PDEMarkerFactory.M_REQ_BUNDLE_NOT_AVAILABLE, PDEMarkerFactory.CAT_FATAL);
					try {
						if (marker != null) {
							marker.setAttribute("bundleId", required[i].getValue()); //$NON-NLS-1$
							if (optional)
								marker.setAttribute("optional", true); //$NON-NLS-1$
						}
					} catch (CoreException e) {
					}
				} else {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, bundleID + ": " + specs[i].getVersionRange()), //$NON-NLS-1$
							getPackageLine(header, required[i]), severity, PDEMarkerFactory.CAT_FATAL);
				}
			}
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4)
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.REQUIRE_BUNDLE), header.getLineNumber() + 1, CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);

	}

	private void validateBundleVersionAttribute(IHeader header, ManifestElement element) {
		String versionRange = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_REQ_BUNDLE);
		if (severity != CompilerFlags.IGNORE && versionRange == null) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()), getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
		}

		if (versionRange != null && !VersionUtil.validateVersionRange(versionRange).isOK()) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_InvalidFormatInBundleVersion, element.getValue()), getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
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
				report(message, getLine(header, ICoreConstants.REPROVIDE_ATTRIBUTE + "="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
			}
		}
	}

	private boolean isOptional(ManifestElement element) {
		return Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE)) || "true".equals(element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
	}

	private int getRequireBundleSeverity(ManifestElement requireBundleElement, boolean optional) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNRESOLVED_IMPORTS);
		if (optional && severity != CompilerFlags.IGNORE)
			severity += 1;
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
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_optional, ICoreConstants.OPTIONAL_ATTRIBUTE), getLine(header, ICoreConstants.OPTIONAL_ATTRIBUTE + "="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
			}
		}
	}

	private void validateImportPackage(IProgressMonitor monitor) {
		IHeader header = getHeader(Constants.IMPORT_PACKAGE);
		if (header == null)
			return;

		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null) {
			ManifestElement[] elements = header.getElements();
			for (int i = 0; i < elements.length; i++) {
				validateSpecificationVersionAttribute(header, elements[i]);
				validateImportPackageVersion(header, elements[i]);
			}
			return;
		}

		boolean hasUnresolved = false;
		VersionConstraint[] constraints = desc.getContainingState().getStateHelper().getUnsatisfiedConstraints(desc);
		for (int i = 0; i < constraints.length; i++) {
			if (constraints[i] instanceof ImportPackageSpecification) {
				hasUnresolved = true;
				break;
			}
		}

		HashMap exported = getAvailableExportedPackages(desc.getContainingState());

		ImportPackageSpecification[] imports = desc.getImportPackages();
		if (desc.hasDynamicImports()) {
			List staticImportsList = new ArrayList();
			for (int i = 0; i < imports.length; ++i) {
				if (!imports[i].getDirective(Constants.RESOLUTION_DIRECTIVE).equals(ImportPackageSpecification.RESOLUTION_DYNAMIC))
					staticImportsList.add(imports[i]);
			}
			imports = (ImportPackageSpecification[]) staticImportsList.toArray(new ImportPackageSpecification[staticImportsList.size()]);
		}

		ManifestElement[] elements = header.getElements();
		int index = 0;
		for (int i = 0; i < elements.length; i++) {
			checkCanceled(monitor);

			validateSpecificationVersionAttribute(header, elements[i]);
			validateResolutionDirective(header, elements[i]);

			// TODO we should only validate versions that we have a match
			validateImportPackageVersion(header, elements[i]);

			if (!hasUnresolved)
				continue;

			int length = elements[i].getValueComponents().length;
			for (int j = 0; j < length; j++) {
				ImportPackageSpecification importSpec = imports[index++];
				String name = importSpec.getName();
				if (name.equals("java") || name.startsWith("java.")) { //$NON-NLS-1$ //$NON-NLS-2$
					IHeader jreHeader = getHeader(ICoreConstants.ECLIPSE_JREBUNDLE);
					if (jreHeader == null || !"true".equals(jreHeader.getValue())) { //$NON-NLS-1$
						report(PDECoreMessages.BundleErrorReporter_importNoJRE, getPackageLine(header, elements[i]), CompilerFlags.ERROR, PDEMarkerFactory.M_JAVA_PACKAGE__PORTED, PDEMarkerFactory.CAT_FATAL);
						continue;
					}
				}

				if (importSpec.isResolved() || !isCheckUnresolvedImports())
					continue;

				boolean optional = isOptional(elements[i]);
				int severity = getRequireBundleSeverity(elements[i], optional);

				ExportPackageDescription export = (ExportPackageDescription) exported.get(name);
				if (export != null) {
					if (export.getSupplier().isResolved()) {
						Version version = export.getVersion();
						org.eclipse.osgi.service.resolver.VersionRange range = importSpec.getVersionRange();
						if (range != null && !range.isIncluded(version)) {
							report(NLS.bind(PDECoreMessages.BundleErrorReporter_unsatisfiedConstraint, importSpec.toString()), getPackageLine(header, elements[i]), severity, PDEMarkerFactory.CAT_FATAL);
						}
					} else {
						report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedExporter, new String[] {export.getSupplier().getSymbolicName(), name}), getPackageLine(header, elements[i]), severity, PDEMarkerFactory.CAT_OTHER);
					}
				} else {
					IMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_PackageNotExported, name), getPackageLine(header, elements[i]), severity, PDEMarkerFactory.M_IMPORT_PKG_NOT_AVAILABLE, PDEMarkerFactory.CAT_FATAL);
					try {
						if (marker != null) {
							marker.setAttribute("packageName", name); //$NON-NLS-1$
							if (optional)
								marker.setAttribute("optional", true); //$NON-NLS-1$
						}
					} catch (CoreException e) {
					}
				}
			}
		}
	}

	private HashMap getAvailableExportedPackages(State state) {
		BundleDescription[] bundles = state.getBundles();

		HashMap exported = new HashMap();
		for (int i = 0; i < bundles.length; i++) {
			ExportPackageDescription[] exports = bundles[i].getExportPackages();
			for (int j = 0; j < exports.length; j++) {
				String name = exports[j].getName();
				if (exported.containsKey(name)) {
					if (exports[j].getSupplier().isResolved()) {
						exported.put(name, exports[j]);
					}
				} else {
					exported.put(name, exports[j]);
				}
			}
		}
		return exported;
	}

	protected void validateExportPackage(IProgressMonitor monitor) {
		IHeader header = getHeader(Constants.EXPORT_PACKAGE);
		if (header == null)
			return;

		String message = null;
		ManifestElement[] elements = header.getElements();

		for (int i = 0; i < elements.length; i++) {
			checkCanceled(monitor);

			validateExportPackageVersion(header, elements[i]);
			validateSpecificationVersionAttribute(header, elements[i]);
			validateX_InternalDirective(header, elements[i]);
			validateX_FriendsDirective(header, elements[i]);

			String[] valueComps = elements[i].getValueComponents();
			for (int j = 0; j < valueComps.length; j++) {
				String name = valueComps[j];
				if (name.equals("java") || name.startsWith("java.")) { //$NON-NLS-1$ //$NON-NLS-2$
					IHeader jreHeader = getHeader(ICoreConstants.ECLIPSE_JREBUNDLE);
					if (jreHeader == null || !"true".equals(jreHeader.getValue())) { //$NON-NLS-1$
						message = PDECoreMessages.BundleErrorReporter_exportNoJRE;
						report(message, getPackageLine(header, elements[i]), CompilerFlags.ERROR, PDEMarkerFactory.M_JAVA_PACKAGE__PORTED, PDEMarkerFactory.CAT_FATAL);
					}
				} else if (".".equals(name.trim())) { //$NON-NLS-1$
					// workaround for manifest converter generating "."
					continue;
				}

				if (!isCheckUnresolvedImports()) {
					continue;
				}

				/* The exported package does not exist in the bundle.  Allow project folders to be packages (see bug 166680 comment 17)*/
				if (!getExportedPackages().contains(name) && !(fProject.getFolder(name.replace('.', '/')).exists())) {
					message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, name);
					IMarker marker = report(message, getPackageLine(header, elements[i]), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.M_EXPORT_PKG_NOT_EXIST, PDEMarkerFactory.CAT_OTHER);
					addMarkerAttribute(marker, "packageName", name); //$NON-NLS-1$
				}
			}
		}

	}

	private boolean containsPackage(IHeader header, String name) {
		if (header != null) {
			ManifestElement[] elements = header.getElements();
			for (int i = 0; i < elements.length; i++) {
				if (elements[i].getValue().equals(name))
					return true;
			}
		}
		return false;
	}

	private Set getExportedPackages() {
		if (fProjectPackages == null) {
			fProjectPackages = new HashSet();
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
			} else {
				try {
					if (fProject.hasNature(JavaCore.NATURE_ID)) {
						IPackageFragment[] packages = PluginJavaSearchUtil.collectPackageFragments(new IPluginModelBase[] {model}, JavaCore.create(fProject), false);
						for (int i = 0; i < packages.length; i++)
							fProjectPackages.add(packages[i].getElementName());
					}
				} catch (CoreException ce) {
				}
			}
		}
	}

	private void addFragmentPackages(BundleDescription[] fragments) {
		for (int i = 0; i < fragments.length; i++) {
			String id = fragments[i].getSymbolicName();
			IPluginModelBase model = PluginRegistry.findModel(id);
			IResource resource = model instanceof IFragmentModel ? model.getUnderlyingResource() : null;
			if (resource != null) {
				addProjectPackages(resource.getProject());
			}
		}
	}

	private void addProjectPackages(IProject proj) {
		try {
			if (proj.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(proj);
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE || (roots[i].getKind() == IPackageFragmentRoot.K_BINARY && !roots[i].isExternal())) {
						IJavaElement[] children = roots[i].getChildren();
						for (int j = 0; j < children.length; j++) {
							IPackageFragment f = (IPackageFragment) children[j];
							String name = f.getElementName();
							if (name.equals("")) //$NON-NLS-1$
								name = "."; //$NON-NLS-1$
							if (f.hasChildren() || f.getNonJavaResources().length > 0)
								fProjectPackages.add(name);
						}
					}
				}
			}
		} catch (CoreException ce) {
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
		if (severity == CompilerFlags.IGNORE)
			return;

		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			IHeader header = getHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]);
			if (header != null) {
				String value = header.getValue();
				if (!value.startsWith("%")) { //$NON-NLS-1$
					report(NLS.bind(PDECoreMessages.Builders_Manifest_non_ext_attribute, header.getName()), getLine(header, value), severity, PDEMarkerFactory.P_UNTRANSLATED_NODE, header.getName(), PDEMarkerFactory.CAT_NLS);
				} else if (fModel instanceof AbstractNLModel) {
					NLResourceHelper helper = ((AbstractNLModel) fModel).getNLResourceHelper();
					if (helper == null || !helper.resourceExists(value))
						report(NLS.bind(PDECoreMessages.Builders_Manifest_key_not_found, value.substring(1)), getLine(header, value), severity, PDEMarkerFactory.CAT_NLS);
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
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_specification_version, ICoreConstants.PACKAGE_SPECIFICATION_VERSION), getPackageLine(header, element), CompilerFlags.P_DEPRECATED, PDEMarkerFactory.CAT_DEPRECATION);
			}
		}
	}

	private void validateImportPackageVersion(IHeader header, ManifestElement element) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_IMP_PKG);
		if (severity != CompilerFlags.IGNORE && version == null) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()), getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
		}
		validateVersionAttribute(header, element, true);
	}

	private void validateExportPackageVersion(IHeader header, ManifestElement element) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_MISSING_VERSION_EXP_PKG);
		if (severity != CompilerFlags.IGNORE && version == null) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_MissingVersion, element.getValue()), getPackageLine(header, element), severity, PDEMarkerFactory.CAT_OTHER);
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
		if (internal == null)
			return;

		for (int i = 0; i < BOOLEAN_VALUES.length; i++) {
			if (BOOLEAN_VALUES[i].equals(internal))
				return;
		}
		String message = NLS.bind(PDECoreMessages.BundleErrorReporter_dir_value, (new String[] {internal, ICoreConstants.INTERNAL_DIRECTIVE}));
		report(message, getPackageLine(header, element), CompilerFlags.ERROR, PDEMarkerFactory.CAT_FATAL);
	}

	private void validateX_FriendsDirective(IHeader header, ManifestElement element) {
		String friends = element.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		String internal = element.getDirective(ICoreConstants.INTERNAL_DIRECTIVE);
		if (friends != null && internal != null) {
			String message = NLS.bind(PDECoreMessages.BundleErrorReporter_directive_hasNoEffectWith_, new String[] {ICoreConstants.FRIENDS_DIRECTIVE, ICoreConstants.INTERNAL_DIRECTIVE});
			IMarker marker = report(message, getPackageLine(header, element), CompilerFlags.WARNING, PDEMarkerFactory.M_DIRECTIVE_HAS_NO_EFFECT, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, "packageName", element.getValue()); //$NON-NLS-1$
		}
	}

	private void validateBundleActivatorPolicy() {
		IHeader header = getHeader(Constants.BUNDLE_ACTIVATIONPOLICY);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (header == null)
			return;

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4)
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_ACTIVATIONPOLICY), header.getLineNumber() + 1, CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);

		if (TargetPlatformHelper.getTargetVersion() >= 3.3) {
			validateHeaderValue(header, new String[] {Constants.ACTIVATION_LAZY});
		} else if (severity != CompilerFlags.IGNORE && !containsValidActivationHeader()) {
			report(PDECoreMessages.BundleErrorReporter_bundleActivationPolicy_unsupported, header.getLineNumber() + 1, severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private void validateAutoStart() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_AUTOSTART);
		if (!validateStartHeader(header))
			return; // valid start header problems already reported

		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (severity != CompilerFlags.IGNORE && TargetPlatformHelper.getTargetVersion() >= 3.2 && !containsValidActivationHeader()) {
			int line = header.getLineNumber();
			String message = NLS.bind(PDECoreMessages.BundleErrorReporter_startHeader_autoStartDeprecated, new Object[] {ICoreConstants.ECLIPSE_AUTOSTART, getCurrentActivationHeader()});
			IMarker marker = report(message, line + 1, severity, PDEMarkerFactory.M_DEPRECATED_AUTOSTART, PDEMarkerFactory.CAT_DEPRECATION);
			if (marker != null) {
				try {
					marker.setAttribute(PDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_AUTOSTART);
					ManifestElement elem = header.getElements()[0];
					boolean unnecessary = elem.getValue().equals("false") && elem.getAttribute("excludes") == null; //$NON-NLS-1$ //$NON-NLS-2$
					marker.setAttribute(PDEMarkerFactory.ATTR_CAN_ADD, !unnecessary);
				} catch (CoreException e) {
				}
			}
		}
	}

	private void validateLazyStart() {
		IHeader header = getHeader(ICoreConstants.ECLIPSE_LAZYSTART);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		validateStartHeader(header);
		if (header != null) {
			if (severity == CompilerFlags.IGNORE || containsValidActivationHeader())
				return;
			double targetVersion = TargetPlatformHelper.getTargetVersion();
			if (targetVersion < 3.2) {
				report(PDECoreMessages.BundleErrorReporter_lazyStart_unsupported, header.getLineNumber() + 1, severity, PDEMarkerFactory.NO_RESOLUTION, PDEMarkerFactory.CAT_OTHER);
			} else if (targetVersion > 3.3) {
				int line = header.getLineNumber();
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_startHeader_autoStartDeprecated, new Object[] {ICoreConstants.ECLIPSE_LAZYSTART, getCurrentActivationHeader()});
				IMarker marker = report(message, line + 1, severity, PDEMarkerFactory.M_DEPRECATED_AUTOSTART, PDEMarkerFactory.CAT_DEPRECATION);
				if (marker != null) {
					try {
						marker.setAttribute(PDEMarkerFactory.ATTR_HEADER, ICoreConstants.ECLIPSE_LAZYSTART);
						ManifestElement elem = header.getElements()[0];

						boolean unnecessary = elem.getValue().equals("false") && elem.getAttribute("excludes") == null; //$NON-NLS-1$ //$NON-NLS-2$
						marker.setAttribute(PDEMarkerFactory.ATTR_CAN_ADD, !unnecessary);
					} catch (CoreException e) {
					}
				}
			}
		}
	}

	private boolean containsValidActivationHeader() {
		String header;
		double targetVersion = TargetPlatformHelper.getTargetVersion();
		if (targetVersion < 3.2)
			header = ICoreConstants.ECLIPSE_AUTOSTART;
		else if (targetVersion < 3.4)
			header = ICoreConstants.ECLIPSE_LAZYSTART;
		else
			header = Constants.BUNDLE_ACTIVATIONPOLICY;

		return getHeader(header) != null;
	}

	private String getCurrentActivationHeader() {
		double targetVersion = TargetPlatformHelper.getTargetVersion();
		if (targetVersion < 3.2)
			return ICoreConstants.ECLIPSE_AUTOSTART;
		else if (targetVersion < 3.4)
			return ICoreConstants.ECLIPSE_LAZYSTART;
		return Constants.BUNDLE_ACTIVATIONPOLICY;
	}

	private boolean validateStartHeader(IHeader header) {
		if (header == null)
			return false;
		validateBooleanValue(header);
		return exceptionsAttributesValid(header, header.getElements());
	}

	private boolean exceptionsAttributesValid(IHeader header, ManifestElement[] elements) {
		if (elements == null || elements.length == 0)
			return true;
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
		if (severity == CompilerFlags.IGNORE)
			return true;
		Enumeration keys = elements[0].getKeys();
		if (keys != null && keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if ("exceptions".equals(key)) { //$NON-NLS-1$
				String[] values = elements[0].getAttributes(key);
				for (int i = 0; i < values.length; i++) {
					StringTokenizer st = new StringTokenizer(values[i], ","); //$NON-NLS-1$
					while (st.hasMoreTokens()) {
						String name = st.nextToken().trim();
						if (!getExportedPackages().contains(name)) {
							String message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, name);
							report(message, getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.CAT_OTHER);
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
		if (header != null)
			validateBooleanValue(header);
	}

	public void report(String message, int line, int severity, int problemID, String headerName, String category) {
		try {
			IMarker marker = report(message, line, severity, problemID, category);
			if (marker != null)
				marker.setAttribute(PDEMarkerFactory.MPK_LOCATION_PATH, headerName);
		} catch (CoreException e) {
		}
	}

	private void validateImportExportServices() {
		if (fOsgiR4) {
			IHeader importHeader = getHeader(ICoreConstants.IMPORT_SERVICE);
			IHeader exportHeader = getHeader(ICoreConstants.EXPORT_SERVICE);
			int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);

			if (severity == CompilerFlags.IGNORE)
				return;

			if (importHeader != null) {
				int line = importHeader.getLineNumber();
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_importexport_servicesDeprecated, ICoreConstants.IMPORT_SERVICE), line + 1, severity, PDEMarkerFactory.M_DEPRECATED_IMPORT_SERVICE, PDEMarkerFactory.CAT_DEPRECATION);
			}

			if (exportHeader != null) {
				int line = exportHeader.getLineNumber();
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_importexport_servicesDeprecated, ICoreConstants.EXPORT_SERVICE), line + 1, severity, PDEMarkerFactory.M_DEPRECATED_EXPORT_SERVICE, PDEMarkerFactory.CAT_DEPRECATION);
			}
		}
	}

	private void validateBundleLocalization() {
		IHeader header = getHeader(Constants.BUNDLE_LOCALIZATION);
		if (header == null)
			return;
		String location = header.getValue();
		String fileName = null;
		int index = location.lastIndexOf('/');
		if (index > 0) {
			fileName = location.substring(index + 1);
			location = location.substring(0, index);
		} else {
			fileName = location;
			location = new String();
		}

		// Header introduced in OSGi R4 - warn if R3 manifest
		if (!fOsgiR4)
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_R4SyntaxInR3Bundle, Constants.BUNDLE_LOCALIZATION), header.getLineNumber() + 1, CompilerFlags.WARNING, PDEMarkerFactory.M_R4_SYNTAX_IN_R3_BUNDLE, PDEMarkerFactory.CAT_OTHER);

		IResource res = PDEProject.getBundleRoot(fProject).findMember(location);
		if (res == null || !(res instanceof IContainer)) {
			report(PDECoreMessages.BundleErrorReporter_localization_folder_not_exist, header.getLineNumber() + 1, CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE), PDEMarkerFactory.CAT_OTHER);
			return;
		}
		IContainer folder = (IContainer) res;
		try {
			IResource[] children = folder.members();
			for (int i = 0; i < children.length; i++) {
				if (!(children[i] instanceof IFile))
					continue;
				String childName = children[i].getName();
				if (childName.endsWith(".properties") && childName.startsWith(fileName)) //$NON-NLS-1$
					return;
			}
		} catch (CoreException e) {
		}
		report(PDECoreMessages.BundleErrorReporter_localization_properties_file_not_exist, header.getLineNumber() + 1, CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_RESOURCE), PDEMarkerFactory.CAT_OTHER);
	}

	private void validateFragmentHost(IHeader requireBundleHeader, ManifestElement element) {
		IHeader header = getHeader(Constants.FRAGMENT_HOST);
		if (header == null)
			return;

		ManifestElement[] elements = header.getElements();

		if (header != null && elements[0] != null && elements[0].getValue().equals(element.getValue())) {
			IMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_unecessaryDependencyDueToFragmentHost, element.getValue()), getPackageLine(requireBundleHeader, element), CompilerFlags.WARNING, PDEMarkerFactory.M_UNECESSARY_DEP, PDEMarkerFactory.CAT_OTHER);
			addMarkerAttribute(marker, "bundleId", element.getValue()); //$NON-NLS-1$
		}
	}

	private void validateProvidePackage() {
		IHeader header = getHeader(ICoreConstants.PROVIDE_PACKAGE);
		if (header == null)
			return;

		if (fOsgiR4 && isCheckDeprecated()) {
			report(PDECoreMessages.BundleErrorReporter_providePackageHeaderDeprecated, header.getLineNumber() + 1, CompilerFlags.P_DEPRECATED, PDEMarkerFactory.M_DEPRECATED_PROVIDE_PACKAGE, PDEMarkerFactory.CAT_OTHER);
		}
	}

	private void addMarkerAttribute(IMarker marker, String attr, String value) {
		if (marker != null)
			try {
				marker.setAttribute(attr, value);
			} catch (CoreException e) {
			}
	}
}