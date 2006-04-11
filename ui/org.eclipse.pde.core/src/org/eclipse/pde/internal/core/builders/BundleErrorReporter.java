/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.builders;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
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
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.VersionConstraint;
import org.eclipse.osgi.service.resolver.VersionRange;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.ModelEntry;
import org.eclipse.pde.internal.core.NLResourceHelper;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDECoreMessages;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.search.PluginJavaSearchUtil;
import org.eclipse.pde.internal.core.util.IdUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;

public class BundleErrorReporter extends JarManifestErrorReporter {

	IPluginModelBase fModel;

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
			return new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.ERROR, 
					PDECoreMessages.BundleErrorReporter_invalidVersionRangeFormat, e); 
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

	private boolean fOsgiR4;
	
	private Set fFragmentsPackages = null;
	private Set fHostPackages = null;
	private Set fProjectPackages = null;	

	private String fPluginId = ""; //$NON-NLS-1$

	public BundleErrorReporter(IFile file) {
		super(file);
	}

	public void validateContent(IProgressMonitor monitor) {
		super.validateContent(monitor);
		if (fHeaders == null || getErrorCount() > 0)
			return;

		fModel = PDECore.getDefault().getModelManager().findModel(fProject);
		setOsgiR4();

		if (!validateBundleSymbolicName())
			return;
		validateFragmentHost();	
		validateRequiredHeader(Constants.BUNDLE_NAME);
		validateBundleVersion();
		
		validateEclipsePlatformFilter();
		validateBundleActivator();
		validateBundleClasspath();
		validateRequireBundle(monitor);
		validateImportPackage(monitor);
		//validateExportPackage(monitor);
		validateAutoStart();
		validateLazyStart();
		validateExtensibleAPI();
		
		validateTranslatableHeaders();
	}
	
	private void setOsgiR4() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_MANIFESTVERSION);
		if (header != null) {		
			String version = header.getValue();
			try {
				fOsgiR4 = version != null && Integer.parseInt(version) > 1 ;
			}  catch (NumberFormatException e) {			
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
		fPluginId = elements.length > 0 ? elements[0].getValue() : null;
		if (fPluginId == null || fPluginId.length() == 0) {
			report(PDECoreMessages.BundleErrorReporter_NoSymbolicName, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return false;
		}
		
		validatePluginId(header, fPluginId);
		validateSingleton(header, elements[0]);

		return true;
	}
	
	private boolean validatePluginId(IHeader header, String value) {
		if (!IdUtil.isValidCompositeID(value)) {
			String message = PDECoreMessages.BundleErrorReporter_InvalidSymbolicName; 
			report(message, getLine(header, value), CompilerFlags.WARNING);
			return false;
		}
		return true;
	}

	private void validateSingleton(IHeader header, ManifestElement element) {
		String singletonAttr = element.getAttribute(ICoreConstants.SINGLETON_ATTRIBUTE);
		String singletonDir = element.getDirective(Constants.SINGLETON_DIRECTIVE);
		boolean hasExtensions = fModel.getPluginBase().getExtensionPoints().length > 0
								|| fModel.getPluginBase().getExtensions().length > 0;

		if (hasExtensions) {
			if (TargetPlatform.getTargetVersion() >= 3.1) {
				if (!"true".equals(singletonDir)) { //$NON-NLS-1$
					if ("true".equals(singletonAttr)) { //$NON-NLS-1$
						if (isCheckDeprecated()) {
							String message = PDECoreMessages.BundleErrorReporter_deprecated_attribute_singleton;
							report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
									CompilerFlags.P_DEPRECATED,	PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET);
							return;
						}
					} else {
						String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonRequired, Constants.SINGLETON_DIRECTIVE); 
						report(message, header.getLineNumber() + 1,	CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET);						
						return;
					}
				}
			} else if (!"true".equals(singletonAttr)) { //$NON-NLS-1$
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_singletonAttrRequired,
						  ICoreConstants.SINGLETON_ATTRIBUTE);
				report(message, header.getLineNumber() + 1, CompilerFlags.ERROR, PDEMarkerFactory.M_SINGLETON_ATT_NOT_SET);				
				return;
			}
		}
		
		if (TargetPlatform.getTargetVersion() >= 3.1) {
			if (singletonAttr != null) {
				if (isCheckDeprecated()) {
					String message = PDECoreMessages.BundleErrorReporter_deprecated_attribute_singleton;
					report(message, getLine(header, ICoreConstants.SINGLETON_ATTRIBUTE + "="), //$NON-NLS-1$
							CompilerFlags.P_DEPRECATED,	PDEMarkerFactory.M_SINGLETON_DIR_NOT_SET);
				}			
			}
		} else if (singletonDir != null) {
			if (isCheckDeprecated()) {
				String message = PDECoreMessages.BundleErrorReporter_unsupportedSingletonDirective;
				report(message, getLine(header, Constants.SINGLETON_DIRECTIVE + ":="), //$NON-NLS-1$
						CompilerFlags.P_DEPRECATED,	PDEMarkerFactory.M_SINGLETON_DIR_NOT_SUPPORTED);
				
			}
		}
		validateBooleanAttributeValue(header, element, ICoreConstants.SINGLETON_ATTRIBUTE);
		validateBooleanDirectiveValue(header, element, Constants.SINGLETON_DIRECTIVE);	
	}

	private void validateFragmentHost() {
		IHeader header = (IHeader) fHeaders.get(Constants.FRAGMENT_HOST);
		if (header == null) {
			if (isCheckNoRequiredAttr() && fProject.getFile("fragment.xml").exists()) {  //$NON-NLS-1$
				report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT);
			}	
			return;
		}
		
		if (header.getElements().length == 0) {
			if (isCheckNoRequiredAttr())
				report(PDECoreMessages.BundleErrorReporter_HostNeeded, 1, CompilerFlags.P_NO_REQUIRED_ATT);
			return;
		}
		
		if (!isCheckUnresolvedImports())
			return;
		
		BundleDescription desc = fModel.getBundleDescription();	
		if (desc == null)
			return;
		
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
						VersionRange range = host.getVersionRange();
						if (!range.isIncluded(version)) {
							String versionRange = host.getVersionRange().toString();
							report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, versionRange), 
									getLine(header, versionRange),
									CompilerFlags.P_UNRESOLVED_IMPORTS);
							return;
						}
					} else {
						resolved = false;
					}
				}
				
				if (!resolved) {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedHost, name), 
							getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS);	
					return;
				}
			}
		} 
		
		ModelEntry entry = PDECore.getDefault().getModelManager().findEntry(name);
		IPluginModelBase model = entry == null ? null : entry.getActiveModel();
		if (model == null || model instanceof IFragmentModel || !model.isEnabled()) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_HostNotExistPDE, name), 
					getLine(header, name), CompilerFlags.P_UNRESOLVED_IMPORTS);				
		}		
	}

	private void validateBundleVersion() {
		IHeader header = validateRequiredHeader(Constants.BUNDLE_VERSION);
		if (header == null)
			return;
	
		IStatus status = validateVersionString(header.getValue());		
		if(!status.isOK()){
			int line = getLine(header, header.getValue());
			report(status.getMessage(), line, CompilerFlags.ERROR);
		}
	}

	private void validateEclipsePlatformFilter() {
		IHeader header = (IHeader) fHeaders.get(ICoreConstants.PLATFORM_FILTER);
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
						report(PDECoreMessages.BundleErrorReporter_badFilter, 
								header.getLineNumber() + 1, severity);
					}
				}
			}
		} catch (InvalidSyntaxException ise) {
			report(PDECoreMessages.BundleErrorReporter_invalidFilterSyntax, 
					header.getLineNumber() + 1, CompilerFlags.ERROR);
		}	
	}
	
	private void validateBundleActivator() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_ACTIVATOR);
		if (header == null)
			return;

		String activator = header.getValue();
		BundleDescription desc = fModel.getBundleDescription();
		if (desc != null && desc.getHost() != null) {
			report(PDECoreMessages.BundleErrorReporter_fragmentActivator, header.getLineNumber() + 1, CompilerFlags.ERROR);
			return;
		}
		
		if (isCheckUnknownClass()) {
			try {
				if (fProject.hasNature(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(fProject);
					if (activator.indexOf('$') != -1)
						activator = activator.replace('$', '.');
					
					// Look for this activator in the project's classpath
					IType type = javaProject.findType(activator);
					if (type == null || !type.exists()) {
						report(NLS.bind(PDECoreMessages.BundleErrorReporter_NoExist, activator), 
								getLine(header, activator),
								CompilerFlags.P_UNKNOWN_CLASS,
								PDEMarkerFactory.M_UNKNOWN_ACTIVATOR);
					}
				}
			} catch (CoreException ce) {
			}
		}
	}
	
	private void validateBundleClasspath() {
		IHeader header = (IHeader) fHeaders.get(Constants.BUNDLE_CLASSPATH);
		if (header != null && header.getElements().length == 0) {
			report(PDECoreMessages.BundleErrorReporter_ClasspathNotEmpty, header.getLineNumber() + 1, CompilerFlags.ERROR);
		}
	}
	
	private void validateRequireBundle(IProgressMonitor monitor) {
		if (!isCheckUnresolvedImports())
			return;
		
		IHeader header = (IHeader) fHeaders.get(Constants.REQUIRE_BUNDLE);
		if (header == null)
			return;
			
		ManifestElement[] required = header.getElements();
		for (int i = 0; i < required.length; i++) {
			checkCanceled(monitor);

			String bundleID = required[i].getValue();

			validateBundleVersionAttribute(header, required[i]);
			validateVisibilityDirective(header, required[i]);
			validateReprovideAttribute(header, required[i]);
			validateResolutionDirective(header, required[i]);
			validateOptionalAttribute(header, required[i]);
			
			boolean optional = isOptional(required[i]);
			int severity = getRequireBundleSeverity(required[i], optional);

			IPluginModel model = PDECore.getDefault().getModelManager().findPluginModel(bundleID);
			if (model == null || !model.isEnabled()) {
				IMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistPDE, bundleID), 
								getPackageLine(header, required[i]),
								severity, PDEMarkerFactory.M_REQ_BUNDLE_NOT_AVAILABLE);
				try {
					if (marker != null) {
						marker.setAttribute("bundleId", required[i].getValue()); //$NON-NLS-1$
						if (optional)
							marker.setAttribute("optional", true); //$NON-NLS-1$
					}
				} catch (CoreException e) {
				}
				continue;
			}
			
			String requiredRange = required[i].getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
			if (requiredRange != null && validateVersionRange(requiredRange).isOK()) {
				VersionRange versionRange = new VersionRange(requiredRange);
				String version = model.getPlugin().getVersion();
				if (version != null && !versionRange.isIncluded(new Version(version))) {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_BundleRangeInvalidInBundleVersion, bundleID + ": " + versionRange.toString()),  //$NON-NLS-1$
							getPackageLine(header, required[i]), severity);
				}
			}
		}
	}
	
	private void validateBundleVersionAttribute(IHeader header, ManifestElement element) {
		String versionRange = element.getAttribute(Constants.BUNDLE_VERSION_ATTRIBUTE);
		if (versionRange != null && !validateVersionRange(versionRange).isOK()) {
			report(NLS.bind(PDECoreMessages.BundleErrorReporter_InvalidFormatInBundleVersion, 
				  element.getValue()), getPackageLine(header, element),
					CompilerFlags.ERROR); 
		}
	}

	private void validateVisibilityDirective(IHeader header, ManifestElement element) {
		String visibility = element.getDirective(Constants.VISIBILITY_DIRECTIVE);
		if (visibility != null) {
			validateDirectiveValue(header, element,Constants.VISIBILITY_DIRECTIVE, 
					new String[] {Constants.VISIBILITY_PRIVATE, Constants.VISIBILITY_REEXPORT });
		}
	}

	private void validateReprovideAttribute(IHeader header, ManifestElement element) {
		String message;
		String rexport = element.getAttribute(ICoreConstants.REPROVIDE_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, element,ICoreConstants.REPROVIDE_ATTRIBUTE);
			if (fOsgiR4 && isCheckDeprecated()) {
				message = NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_reprovide,
								   ICoreConstants.REPROVIDE_ATTRIBUTE); 
				report(message, 
					   getLine(header, ICoreConstants.REPROVIDE_ATTRIBUTE + "="),  //$NON-NLS-1$
					   CompilerFlags.P_DEPRECATED); //$NON-NLS-1$
			}
		}
	}

	private boolean isOptional(ManifestElement element) {
		return Constants.RESOLUTION_OPTIONAL.equals(element.getDirective(Constants.RESOLUTION_DIRECTIVE))
					|| "true".equals(element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE)); //$NON-NLS-1$
	}

	private int getRequireBundleSeverity(ManifestElement requireBundleElement, boolean optional) {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNRESOLVED_IMPORTS);
		if (optional && severity != CompilerFlags.IGNORE) 
			severity += 1;
		return severity;
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

	private void validateOptionalAttribute(IHeader header, ManifestElement element) {
		String rexport = element.getAttribute(ICoreConstants.OPTIONAL_ATTRIBUTE);
		if (rexport != null) {
			validateBooleanAttributeValue(header, element, ICoreConstants.OPTIONAL_ATTRIBUTE);
			if (fOsgiR4 && isCheckDeprecated()) {
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_optional,
					   ICoreConstants.OPTIONAL_ATTRIBUTE),
					   getLine(header, ICoreConstants.OPTIONAL_ATTRIBUTE + "="),  //$NON-NLS-1$
					   CompilerFlags.P_DEPRECATED); 
			}
		}
	}

	private void validateImportPackage(IProgressMonitor monitor) {
		BundleDescription desc = fModel.getBundleDescription();
		if (desc == null)
			return;
		
		IHeader header = (IHeader) fHeaders.get(Constants.IMPORT_PACKAGE);
		if (header == null)
			return;
		
		boolean hasUnresolved = false;
		VersionConstraint[] constraints = desc.getContainingState().getStateHelper().getUnsatisfiedConstraints(desc);
		for (int i = 0; i < constraints.length; i++) {
			if (constraints[i] instanceof ImportPackageSpecification) {
				hasUnresolved = true;
				break;
			}
		}
	
		if (!hasUnresolved)
			return;
		
		HashMap exported = getAvailableExportedPackages(desc.getContainingState());
			
		ImportPackageSpecification[] imports = desc.getImportPackages();
		ManifestElement[] elements = header.getElements();		
		for (int i = 0; i < elements.length; i++) {
			checkCanceled(monitor);			
			
			validateSpecificationVersionAttribute(header, elements[i]);
			validateVersionAttribute(header, elements[i], true);
			validateResolutionDirective(header, elements[i]);

			String name = imports[i].getName();
			if (name.equals("java") || name.startsWith("java.")) { //$NON-NLS-1$ //$NON-NLS-2$
				IHeader jreHeader = (IHeader)fHeaders.get(ICoreConstants.ECLIPSE_JREBUNDLE);
				if (jreHeader == null || !"true".equals(jreHeader.getValue())) { //$NON-NLS-1$
					report(PDECoreMessages.BundleErrorReporter_importNoJRE, getPackageLine(header, elements[i]), CompilerFlags.ERROR, PDEMarkerFactory.M_JAVA_PACKAGE__PORTED);
					continue;
				}
			}
			
			if (imports[i].isResolved() || !isCheckUnresolvedImports())
				continue;

			boolean optional = isOptional(elements[i]);
			int severity = getRequireBundleSeverity(elements[i], optional);
			
			ExportPackageDescription export = (ExportPackageDescription)exported.get(name);
			if (export != null) {
				if (export.getSupplier().isResolved()) {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_unsatisfiedConstraint, imports[i].toString()), 
						   getPackageLine(header, elements[i]), severity);					
				} else {
					report(NLS.bind(PDECoreMessages.BundleErrorReporter_unresolvedExporter,
									new String[] {export.getSupplier().getSymbolicName(), name}), 
						   getPackageLine(header, elements[i]), severity);
				}
			} else {
				IMarker marker = report(NLS.bind(PDECoreMessages.BundleErrorReporter_PackageNotExported, name), 
										getPackageLine(header, elements[i]),
										severity, PDEMarkerFactory.M_IMPORT_PKG_NOT_AVAILABLE);
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
		IHeader header = (IHeader) fHeaders.get(Constants.EXPORT_PACKAGE);
		if (header == null)
			return;
	
		String message = null;
		ManifestElement[] elements = header.getElements();

		for (int i = 0; i < elements.length; i++) {
			checkCanceled(monitor);

			validateVersionAttribute(header, elements[i], false);
			validateSpecificationVersionAttribute(header,elements[i]);
			validateX_InternalDirective(header, elements[i]);		
			validateX_FriendsDirective(header, elements[i]);
			
			String name = elements[i].getValue();
			if (name.equals("java") || name.startsWith("java.")) { //$NON-NLS-1$ //$NON-NLS-2$
				IHeader jreHeader = (IHeader)fHeaders.get(ICoreConstants.ECLIPSE_JREBUNDLE);
				if (jreHeader == null || !"true".equals(jreHeader.getValue())) { //$NON-NLS-1$
					message = PDECoreMessages.BundleErrorReporter_exportNoJRE;
					report(message, getPackageLine(header, elements[i]), CompilerFlags.ERROR, PDEMarkerFactory.M_JAVA_PACKAGE__PORTED);
				}
			} else if (".".equals(name.trim())) { //$NON-NLS-1$
				// workaround for manifest converter generating "."
				continue;
			}

			if (!isCheckUnresolvedImports()) {
				continue;
			}

			/* The exported package does not exist in the bundle */
			if (!getProjectPackages().contains(name)) {
				if (!(getHostPackages().contains(name)
						&& getFragmentsPackages().contains(name))) {
					message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, name); 
					IMarker marker = report(message, getPackageLine(header, elements[i]),
							CompilerFlags.P_UNRESOLVED_IMPORTS, PDEMarkerFactory.M_EXPORT_PKG_NOT_EXIST);
					try {
						if (marker != null)
							marker.setAttribute("packageName", name); //$NON-NLS-1$
					} catch (CoreException e) {
					}
				}
			}
			
		}
	}
	
	/**
	 * @return Map of IPackageFragment from current project
	 */
	private Set getProjectPackages() {
		if (fProjectPackages == null) {
			Set set = new HashSet();
			addProjectPackages(set, fProject);
			fProjectPackages = set;
		}
		return fProjectPackages;
	}

	/**
	 * @return Map of IPackageFragment from current project
	 */
	private Set getHostPackages() {
		if (fHostPackages == null) {
			Set set = new HashSet();
			BundleDescription desc = fModel.getBundleDescription();
			if (desc == null)
				return set;
			HostSpecification host = desc.getHost();
			if (host != null) {
				IPluginModel model = PDECore.getDefault().getModelManager()
						.findPluginModel(host.getName());
				if (model == null) {
					return set;
				}
				IResource resource = model.getUnderlyingResource();
				if (resource != null) {
                    addProjectPackages(set, resource.getProject());
                } else {
            		try {
						if (fProject.hasNature(JavaCore.NATURE_ID)) {
							IPackageFragment[] packages = PluginJavaSearchUtil
									.collectPackageFragments(
											new IPluginBase[] { model
													.getPluginBase() },
											JavaCore.create(fProject), false);
							for (int i = 0; i < packages.length; i++)
								set.add(packages[i].getElementName());
						}
					} catch (JavaModelException jme) {
						PDECore.log(jme);
					} catch (CoreException ce) {
					}
                }
			}
			fHostPackages = set;
		}
		return fHostPackages;
	}
	
	/**
	 * @return Map of IPackageFragment from corresponding fragment projects
	 */
	private Set getFragmentsPackages() {
		if (fFragmentsPackages == null) {
			Set set = new HashSet();
			IFragmentModel[] models = PDECore.getDefault().getModelManager()
					.getFragments();
			for (int i = 0; i < models.length; i++) {
				String hostId = models[i].getFragment().getPluginId();
				if (!fPluginId.equals(hostId)) {
					continue;
				}
				IResource resource = models[i].getUnderlyingResource();
				if (resource != null) {
					addProjectPackages(set, resource.getProject());

				}
			}
			fFragmentsPackages = set;
		}
		return fFragmentsPackages;
	}

	/*
	 * Adds IPackageFragment from a project to a map
	 */
	private void addProjectPackages(Set set, IProject proj) {
		try {
			if (proj.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject jp = JavaCore.create(proj);
				IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
				for (int i = 0; i < roots.length; i++) {
					if (roots[i].getKind() == IPackageFragmentRoot.K_SOURCE
						|| (roots[i].getKind() == IPackageFragmentRoot.K_BINARY && !roots[i].isExternal())) {
						IJavaElement[] children = roots[i].getChildren();
						for (int j = 0; j < children.length; j++) {
							IPackageFragment f = (IPackageFragment) children[j];
							String name = f.getElementName();
							if (name.equals("")) //$NON-NLS-1$
								name = "."; //$NON-NLS-1$
							if (f.hasChildren() || f.getNonJavaResources().length > 0)
								set.add(name);
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
		return CompilerFlags.getFlag(fProject,
				CompilerFlags.P_UNRESOLVED_IMPORTS) != CompilerFlags.IGNORE;
	}

	private void validateTranslatableHeaders() {
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_NOT_EXTERNALIZED);
		if (severity == CompilerFlags.IGNORE)
			return;
		
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			IHeader header = (IHeader) fHeaders.get(ICoreConstants.TRANSLATABLE_HEADERS[i]);
			if (header != null) {
				String value = header.getValue();
				if (!value.startsWith("%")) { //$NON-NLS-1$
					report(NLS.bind(PDECoreMessages.Builders_Manifest_non_ext_attribute, header.getName()),
							getLine(header, value),
							severity,
							PDEMarkerFactory.P_UNTRANSLATED_NODE, header.getName()); 
				} else if (fModel instanceof AbstractModel) {
					NLResourceHelper helper = ((AbstractModel)fModel).getNLResourceHelper();
					if (helper == null || !helper.resourceExists(value))
						report(NLS.bind(PDECoreMessages.Builders_Manifest_key_not_found, value.substring(1)), getLine(header, value), severity);				
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
	}
	
	private void validateSpecificationVersionAttribute(IHeader header, ManifestElement element) {
		String version = element.getAttribute(ICoreConstants.PACKAGE_SPECIFICATION_VERSION);
		IStatus status = validateVersionString(version);
		if(!status.isOK()){
			report(status.getMessage(), getPackageLine(header, element), CompilerFlags.ERROR); 
		}
		if (isCheckDeprecated()) {
			if (fOsgiR4 && version != null) {
				report(NLS.bind(PDECoreMessages.BundleErrorReporter_deprecated_attribute_specification_version,
								ICoreConstants.PACKAGE_SPECIFICATION_VERSION),
						getPackageLine(header, element), CompilerFlags.P_DEPRECATED); 
			}
		}
	}

	private void validateVersionAttribute(IHeader header, ManifestElement element, boolean range) {
		String version = element.getAttribute(Constants.VERSION_ATTRIBUTE);
		if (version == null)
			return;
		IStatus status = range ? validateVersionRange(version) : validateVersionString(version);
		if(!status.isOK()) {
			report(status.getMessage(), getPackageLine(header, element), CompilerFlags.ERROR); 
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
		String message = NLS.bind(PDECoreMessages.BundleErrorReporter_dir_value,
				(new String[] { internal, ICoreConstants.INTERNAL_DIRECTIVE })); 
		report(message, getPackageLine(header, element), CompilerFlags.ERROR); 
	}

	private void validateX_FriendsDirective(IHeader header, ManifestElement element) {
		String friends = element.getDirective(ICoreConstants.FRIENDS_DIRECTIVE);
		String internal = element.getDirective(ICoreConstants.INTERNAL_DIRECTIVE);
		if (friends != null && internal != null) {
			String message = NLS.bind(
					PDECoreMessages.BundleErrorReporter_directive_hasNoEffectWith_,
					new String[] { ICoreConstants.FRIENDS_DIRECTIVE,
							ICoreConstants.INTERNAL_DIRECTIVE }); 
			report(message, getPackageLine(header, element), CompilerFlags.WARNING); 
		}
	}

	private void validateAutoStart() {
		IHeader header = (IHeader) fHeaders.get(ICoreConstants.ECLIPSE_AUTOSTART);
		if (!isValidStartHeader(header))
			return; // valid start header problems already reported
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (severity == CompilerFlags.IGNORE)
			return;
		if (TargetPlatform.getTargetVersion() >= 3.2) {
			int line = header.getLineNumber();
			header = (IHeader) fHeaders.get(Constants.BUNDLE_MANIFESTVERSION);
			if (header != null && header.getValue().equals("2")) //$NON-NLS-1$
				report(PDECoreMessages.BundleErrorReporter_startHeader_autoStartDeprecated, line + 1, severity, PDEMarkerFactory.M_DEPRECATED_AUTOSTART);
		}
	}
	
	private void validateLazyStart() {
		IHeader header = (IHeader) fHeaders.get(ICoreConstants.ECLIPSE_LAZYSTART);
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_DEPRECATED);
		if (header != null 
				&& TargetPlatform.getTargetVersion() < 3.2
				&& severity != CompilerFlags.IGNORE) {
			report(PDECoreMessages.BundleErrorReporter_lazyStart_unsupported,
					header.getLineNumber() + 1, severity,
					PDEMarkerFactory.NO_RESOLUTION);
		} else
			isValidStartHeader(header);
	}
	
	private boolean isValidStartHeader(IHeader header) {
		if (header == null)
			return false;
		ManifestElement[] elements = header.getElements();
		return (startHeaderElementsValid(header, elements) && 
				exceptionsAttributesValid(header, elements));
	}
	
	private boolean startHeaderElementsValid(IHeader header, ManifestElement[] elements) {
		if (elements == null || elements.length == 0) 
			return true;
		int severity = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ELEMENT);
		if (severity == CompilerFlags.IGNORE)
			return true;
		if (elements.length > 1) {
			report(header.getName() + PDECoreMessages.BundleErrorReporter_startHeader_tooManyElements, header.getLineNumber() + 1, severity);
			return false;
		}
		String[] values = elements[0].getValueComponents();
		if (values == null ||
				values.length > 1 ||
				(!values[0].equals(Boolean.toString(true)) &&
				 !values[0].equals(Boolean.toString(false)))) {
			report(header.getName() + PDECoreMessages.BundleErrorReporter_startHeader_illegalValue, header.getLineNumber() + 1, severity);
			return false;
		}
		return true;
	}
	
	private boolean exceptionsAttributesValid(IHeader header, ManifestElement[] elements) {
		if (elements == null || elements.length == 0) 
			return true;
		int unknwnAttSev = CompilerFlags.getFlag(fProject, CompilerFlags.P_UNKNOWN_ATTRIBUTE);
		if (unknwnAttSev == CompilerFlags.IGNORE)
			return true;
		Enumeration keys = elements[0].getKeys();
		if (keys != null && keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			if ("exceptions".equals(key)) { //$NON-NLS-1$
				String[] values = elements[0].getAttributes(key);
				for (int i = 0; i < values.length; i++) {
					StringTokenizer st = new StringTokenizer(values[i], ","); //$NON-NLS-1$
					while (st.hasMoreTokens())
						if (!packageExists(header, st.nextToken().trim()))
							return false;
				}
			}
		}
		return true;
	}
	
	private boolean packageExists(IHeader header, String exportPackageStmt) {
		/* The exported package does not exist in the bundle */
		if (!getProjectPackages().contains(exportPackageStmt)) {
			if (!(getHostPackages().contains(exportPackageStmt)
					&& getFragmentsPackages().contains(exportPackageStmt))) {
				String message = NLS.bind(PDECoreMessages.BundleErrorReporter_NotExistInProject, exportPackageStmt); 
				report(message, header.getLineNumber() + 1, CompilerFlags.P_UNRESOLVED_IMPORTS);
				return false;
			}
		}
		return true;
	}
	
	public void report(String message, int line, int severity, int problemID, String headerName) {
		try {
			IMarker marker = report(message, line, severity, problemID);
			if (marker != null)
				marker.setAttribute(PDEMarkerFactory.MPK_LOCATION_PATH, headerName);
		} catch (CoreException e) {
		}
	}
}
