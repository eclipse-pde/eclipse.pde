/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     EclipseSource Corporation - ongoing enhancements
 *     Benjamin Cabe <benjamin.cabe@anyware-tech.com> - bug 219513
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.resource.DeleteResourceChange;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.plugin.IExtensions;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.TargetPlatformHelper;
import org.eclipse.pde.internal.core.bnd.FileResource;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleFragmentModel;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModelBase;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IDocumentElementNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.core.text.bundle.SingleManifestHeader;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.ManifestUtils;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;
import org.osgi.service.prefs.BackingStoreException;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Jar;

public class OrganizeManifest implements IOrganizeManifestsSettings {

	private static String F_NL_PREFIX = "$nl$"; //$NON-NLS-1$
	private static String[] F_ICON_EXTENSIONS = new String[] {"BMP", "ICO", "JPEG", "JPG", "GIF", "PNG", "TIFF" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	};

	public static void organizeRequireBundles(IBundle bundle, boolean removeImports) {
		if (!(bundle instanceof Bundle))
			return;

		RequireBundleHeader header = (RequireBundleHeader) ((Bundle) bundle).getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			RequireBundleObject[] bundles = header.getRequiredBundles();
			for (RequireBundleObject requiredBundle : bundles) {
				String pluginId = requiredBundle.getId();
				if (PluginRegistry.findModel(pluginId) == null) {
					if (removeImports)
						header.removeBundle(requiredBundle);
					else {
						requiredBundle.setOptional(true);
					}
				}
			}
		}
	}

	public static void organizeExportPackages(IBundle bundle, IProject project, boolean addMissing, boolean removeUnresolved) {
		if (!addMissing && !removeUnresolved)
			return;

		if (!(bundle instanceof Bundle))
			return;

		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] currentPkgs;
		if (header == null) {
			bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
			header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			currentPkgs = new ExportPackageObject[0];
		} else
			currentPkgs = header.getPackages();

		IManifestHeader bundleClasspathheader = bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);

		IPackageFragmentRoot[] roots = ManifestUtils.findPackageFragmentRoots(bundleClasspathheader, project);
		// Running list of packages in the project
		Set<String> packages = new HashSet<>();
		for (IPackageFragmentRoot root : roots) {
			try {
				if (ManifestUtils.isImmediateRoot(root)) {
					IJavaElement[] elements = root.getChildren();
					for (IJavaElement element : elements)
						if (element instanceof IPackageFragment fragment) {
							String name = fragment.getElementName();
							if (name.length() == 0)
								name = "."; //$NON-NLS-1$
							if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0)) {
								if (addMissing && !header.hasPackage(name))
									header.addPackage(new ExportPackageObject(header, fragment, Constants.VERSION_ATTRIBUTE));
								else
									packages.add(name);
							}
						}
				}
			} catch (JavaModelException e) {
			}
		}

		// Remove packages that don't exist
		if (removeUnresolved)
			for (int i = 0; i < currentPkgs.length; i++)
				if (!packages.contains(currentPkgs[i].getName()))
					header.removePackage(currentPkgs[i]);
	}

	public static void markPackagesInternal(IBundle bundle, String packageFilter) {
		if (packageFilter == null || bundle == null || !(bundle instanceof Bundle))
			return;

		ExportPackageHeader header = (ExportPackageHeader) bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header == null)
			return;

		ExportPackageObject[] currentPkgs = header.getPackages();
		Pattern pat = PatternConstructor.createPattern(packageFilter, false);
		for (int i = 0; i < currentPkgs.length; i++) {
			String values = currentPkgs[i].getValueComponents()[0];
			if (!currentPkgs[i].isInternal() && currentPkgs[i].getFriends().length == 0 && pat.matcher(values).matches()) {
				currentPkgs[i].setInternal(true);
			}
		}
	}

	public static void organizeImportPackages(IBundle bundle, boolean removeImports) {
		if (!(bundle instanceof Bundle))
			return;
		ImportPackageHeader header = (ImportPackageHeader) ((Bundle) bundle).getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header == null)
			return;
		ImportPackageObject[] importedPackages = header.getPackages();
		Set<String> availablePackages = getAvailableExportedPackages();
		// get Preference
		for (ImportPackageObject importedPackage : importedPackages) {
			String pkgName = importedPackage.getName();
			if (!availablePackages.contains(pkgName)) {
				if (removeImports)
					header.removePackage(importedPackage);
				else {
					importedPackage.setOptional(true);
				}
			}
		}
	}

	public static void computeImportPackages(IBundlePluginModelBase pluginModel, IProject project,
			IProgressMonitor monitor) throws CoreException {
		IBundle bundle = pluginModel.getBundleModel().getBundle();
		// Step 1) Enable using import for automatic managed dependencies
		IEclipsePreferences pref = new ProjectScope(project).getNode(PDECore.PLUGIN_ID);
		pref.putBoolean(ICoreConstants.RESOLVE_WITH_REQUIRE_BUNDLE, false);
		try {
			pref.flush();
		} catch (BackingStoreException e) {
		}
		// Step 2) convert all required bundles
		IBuildModel buildModel = PluginRegistry.createBuildModel(pluginModel);
		if (buildModel != null) {
			RequireBundleHeader header = (RequireBundleHeader) (bundle.getManifestHeader(Constants.REQUIRE_BUNDLE));
			if (header != null) {
				RequireBundleObject[] bundles = header.getRequiredBundles();
				if (bundles.length > 0) {
					IBuild build = buildModel.getBuild();
					IBuildEntry entry = build.getEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
					if (entry == null) {
						entry = buildModel.getFactory().createEntry(IBuildEntry.SECONDARY_DEPENDENCIES);
						build.add(entry);
					}
					for (RequireBundleObject requiredBundle : bundles) {
						String pluginId = requiredBundle.getId();
						header.removeBundle(requiredBundle);
						if (!entry.contains(pluginId)) {
							entry.addToken(pluginId);
						}
					}
					IFile buildProperties = PDEProject.getBuildProperties(project);
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					try (PrintWriter writer = new PrintWriter(stream)) {
						build.write("", writer); //$NON-NLS-1$
					}
					if (buildProperties.exists()) {
						buildProperties.setContents(new ByteArrayInputStream(stream.toByteArray()),
								IResource.FORCE | IResource.KEEP_HISTORY, monitor);
					} else {
						buildProperties.create(new ByteArrayInputStream(stream.toByteArray()), true, monitor);
					}
				}
			}
		}
		// Step 3) compute the current required import packages
		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] classpath = javaProject.getResolvedClasspath(true);
		IPath outputLocation = javaProject.getOutputLocation();
		IWorkspaceRoot workspaceRoot = project.getWorkspace().getRoot();
		try (Jar jar = new Jar(project.getProject().getName()); Analyzer analyzer = new Analyzer(jar)) {
			analyzer.setImportPackage("*"); //$NON-NLS-1$
			IFolder folder = workspaceRoot.getFolder(outputLocation);
			FileResource.addResources(jar, folder, null);
			for (IClasspathEntry cp : classpath) {
				if (cp.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
					File file = cp.getPath().toFile();
					if (file != null && file.exists()) {
						analyzer.addClasspath(file);
					}
				}
				if (cp.getEntryKind() == IClasspathEntry.CPE_SOURCE && !cp.isTest()) {
					IPath location = cp.getOutputLocation();
					if (location != null) {
						IFolder folder2 = workspaceRoot.getFolder(location);
						FileResource.addResources(jar, folder2, null);
					}

				}
			}
			Manifest manifest = analyzer.calcManifest();
			String calculatedImportPackage = manifest.getMainAttributes().getValue(Constants.IMPORT_PACKAGE);
			if (calculatedImportPackage == null || calculatedImportPackage.isBlank()) {
				bundle.setHeader(Constants.IMPORT_PACKAGE, null);
			} else {
				Parameters header = OSGiHeader.parseHeader(calculatedImportPackage);
				StringBuilder buffer = new StringBuilder();
				for (Entry<String, Attrs> entry : header.entrySet()) {
					if (buffer.length() > 0) {
						buffer.append(ManifestUtils.MANIFEST_LIST_SEPARATOR);
					}
					buffer.append(entry.getKey());
					Attrs value = entry.getValue();
					String attrs = value.toString();
					if (!attrs.isEmpty()) {
						buffer.append(";"); //$NON-NLS-1$
						buffer.append(attrs);
					}
				}
				bundle.setHeader(Constants.IMPORT_PACKAGE, buffer.toString());
			}

		} catch (Exception e) {
			throw new CoreException(Status.error("Error generating manifest!", e)); //$NON-NLS-1$
		}
	}

	private static final Set<String> getAvailableExportedPackages() {
		State state = TargetPlatformHelper.getState();
		ExportPackageDescription[] packages = state.getExportedPackages();
		Set<String> set = new HashSet<>();
		for (ExportPackageDescription exportedPackage : packages) {
			set.add(exportedPackage.getName());
		}
		return set;
	}

	public static void removeUnneededLazyStart(IBundle bundle) {
		if (!(bundle instanceof Bundle))
			return;
		if (bundle.getHeader(Constants.BUNDLE_ACTIVATOR) == null && bundle.getHeader(ICoreConstants.SERVICE_COMPONENT) == null) {
			String[] remove = new String[] {ICoreConstants.ECLIPSE_LAZYSTART, ICoreConstants.ECLIPSE_AUTOSTART, Constants.BUNDLE_ACTIVATIONPOLICY};
			for (String element : remove) {
				IManifestHeader lazy = ((Bundle) bundle).getManifestHeader(element);
				if (lazy instanceof SingleManifestHeader)
					((SingleManifestHeader) lazy).setMainComponent(null);
			}
		}

	}

	public static Change deleteUselessPluginFile(IProject project, IPluginModelBase modelBase) {
		if (modelBase == null)
			return null;

		IExtensions ext = modelBase.getExtensions();
		if (ext.getExtensionPoints().length > 0 || ext.getExtensions().length > 0)
			return null;
		IFile pluginFile = (modelBase instanceof IBundleFragmentModel) ? PDEProject.getFragmentXml(project) : PDEProject.getPluginXml(project);
		return new DeleteResourceChange(pluginFile.getFullPath(), true);
	}

	public static TextFileChange[] removeUnusedKeys(final IProject project, final IBundle bundle, final IPluginModelBase modelBase) {
		String localization = bundle.getLocalization();
		if (localization == null)
			localization = "plugin"; //$NON-NLS-1$
		IFile propertiesFile = project.getFile(localization + ".properties"); //$NON-NLS-1$
		if (!propertiesFile.exists())
			return new TextFileChange[0];

		return PDEModelUtility.changesForModelModication(new ModelModification(propertiesFile) {
			@Override
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (!(model instanceof IBuildModel))
					return;

				IBuild build = ((IBuildModel) model).getBuild();
				IBuildEntry[] entries = build.getBuildEntries();
				ArrayList<String> allKeys = new ArrayList<>(entries.length);
				for (int i = 0; i < entries.length; i++)
					if (!allKeys.contains(entries[i].getName()))
						allKeys.add(entries[i].getName());

				ArrayList<String> usedkeys = new ArrayList<>();
				findTranslatedStrings(project, modelBase, bundle, usedkeys);

				allKeys.removeAll(usedkeys);

				if (allKeys.isEmpty())
					return;

				// scan properties file for keys referencing other keys
				for (IBuildEntry buildEntry : entries) {
					String[] tokens = buildEntry.getTokens();
					if (tokens == null || tokens.length == 0)
						continue;
					String entry = tokens[0];
					for (int k = 1; k < tokens.length; k++)
						entry += ',' + tokens[k];
					if (entry.indexOf('%') == entry.lastIndexOf('%'))
						continue;

					// allKeys must NOT have any duplicates
					for (int j = 0; j < allKeys.size(); j++) {
						String akey = '%' + allKeys.get(j) + '%';
						if (entry.contains(akey))
							allKeys.remove(allKeys.get(j--));
						if (allKeys.isEmpty())
							return;
					}
				}

				for (int i = 0; i < allKeys.size(); i++) {
					IBuildEntry entry = build.getEntry(allKeys.get(i));
					build.remove(entry);
				}
			}
		}, null);
	}

	private static void findTranslatedStrings(IProject project, IPluginModelBase pluginModel, IBundle bundle, ArrayList<String> list) {

		findTranslatedXMLStrings(pluginModel, list);
		findTranslatedMFStrings(bundle, list);

		IPluginModelBase model = PluginRegistry.findModel(project);

		BundleDescription bundleDesc = model.getBundleDescription();
		HostSpecification hostSpec = bundleDesc.getHost();
		if (hostSpec != null) {
			BundleDescription[] hosts = hostSpec.getHosts();
			for (BundleDescription host : hosts) {
				IPluginModelBase hostModel = PluginRegistry.findModel(host);
				if (hostModel != null) {
					findTranslatedXMLStrings(getTextModel(hostModel, false), list);
					findTranslatedMFStrings(getTextBundle(hostModel), list);
				}
			}
		} else {
			IFragmentModel[] fragmentModels = PDEManager.findFragmentsFor(model);
			for (IFragmentModel fragmentModel : fragmentModels) {
				findTranslatedXMLStrings(getTextModel(fragmentModel, true), list);
				findTranslatedMFStrings(getTextBundle(fragmentModel), list);
			}
		}
	}

	private static IPluginModelBase getTextModel(IPluginModelBase model, boolean fragment) {
		if (model instanceof PluginModel || model instanceof FragmentModel)
			return model;

		if (model != null) {
			if (!fileExists(model.getInstallLocation(), fragment ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR))
				return null;
			IDocument doc = CoreUtility.getTextDocument(new File(model.getInstallLocation()), fragment ? ICoreConstants.FRAGMENT_FILENAME_DESCRIPTOR : ICoreConstants.PLUGIN_FILENAME_DESCRIPTOR);
			IPluginModelBase returnModel;
			if (fragment)
				returnModel = new FragmentModel(doc, false);
			else
				returnModel = new PluginModel(doc, false);
			try {
				returnModel.load();
			} catch (CoreException e) {
			}

			if (returnModel.isLoaded())
				return returnModel;
		}
		return null;
	}

	private static IBundle getTextBundle(IPluginModelBase model) {
		if (model != null) {
			if (!fileExists(model.getInstallLocation(), ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR))
				return null;
			IDocument doc = CoreUtility.getTextDocument(new File(model.getInstallLocation()), ICoreConstants.BUNDLE_FILENAME_DESCRIPTOR);
			IBundleModel bundleModel = new BundleModel(doc, false);
			try {
				bundleModel.load();
			} catch (CoreException e) {
			}

			if (bundleModel.isLoaded())
				return bundleModel.getBundle();
		}
		return null;
	}

	private static void findTranslatedXMLStrings(IPluginModelBase model, ArrayList<String> list) {
		if (model == null)
			return;

		IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
		for (IPluginExtensionPoint point : points) {
			String value = getTranslatedKey(point.getName());
			if (value != null && !list.contains(value))
				list.add(value);
		}
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (IPluginExtension extension : extensions)
			if (extension instanceof IDocumentElementNode)
				inspectElementForTranslation((IDocumentElementNode) extension, list);
	}

	private static void inspectElementForTranslation(IDocumentElementNode parent, ArrayList<String> list) {
		IDocumentTextNode text = parent.getTextNode();
		String textValue = getTranslatedKey(text != null ? text.getText() : null);
		if (textValue != null && !list.contains(textValue))
			list.add(textValue);

		IDocumentAttributeNode[] attributes = parent.getNodeAttributes();
		for (IDocumentAttributeNode attribute : attributes) {
			String attrValue = getTranslatedKey(attribute.getAttributeValue());
			if (attrValue != null && !list.contains(attrValue))
				list.add(attrValue);
		}

		if (!(parent instanceof IPluginParent))
			return;

		IPluginObject[] children = ((IPluginParent) parent).getChildren();
		for (IPluginObject element : children)
			if (element instanceof IDocumentElementNode)
				inspectElementForTranslation((IDocumentElementNode) element, list);
	}

	private static void findTranslatedMFStrings(IBundle bundle, ArrayList<String> list) {
		if (bundle == null)
			return;
		for (String element : ICoreConstants.TRANSLATABLE_HEADERS) {
			String key = getTranslatedKey(bundle.getHeader(element));
			if (key != null && !list.contains(key))
				list.add(key);
		}
	}

	private static String getTranslatedKey(String value) {
		if (value != null && value.length() > 1 && value.charAt(0) == '%' && value.charAt(1) != '%')
			return value.substring(1);
		return null;
	}

	private static boolean fileExists(String container, String filename) {
		return new File(container + filename).exists();
	}

	/**
	 * Finds all resource paths ending with a valid icon file extension and creates
	 * a text edit operation in <code>multiEdit</code> for each one that is not prefixed by an
	 * $nl$ segment.
	 *
	 * @param model -
	 */
	public static void prefixIconPaths(IPluginModelBase model) {
		if (model == null)
			return;

		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (IPluginExtension extension : extensions) {
			ISchema schema = registry.getSchema(extension.getPoint());
			if (schema != null)
				inspectElementsIconPaths(schema, extension);
		}
	}

	private static void inspectElementsIconPaths(ISchema schema, IPluginParent parent) {
		IPluginObject[] children = parent.getChildren();
		for (IPluginObject childObject : children) {
			IPluginElement child = (IPluginElement) childObject;
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (IPluginAttribute attribute : attributes) {
					ISchemaAttribute attInfo = schemaElement.getAttribute(attribute.getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.RESOURCE) {
						String value = attribute.getValue();
						if (value.startsWith(F_NL_PREFIX))
							continue;
						int fileExtIndex = value.lastIndexOf('.');
						if (fileExtIndex == -1)
							continue;
						value = value.substring(fileExtIndex + 1);
						for (String iconExtension : F_ICON_EXTENSIONS) {
							if (value.equalsIgnoreCase(iconExtension)) {
								IPath path = IPath.fromOSString(F_NL_PREFIX);
								String newValue = attribute.getValue();
								if (newValue.charAt(0) != IPath.SEPARATOR)
									path = path.addTrailingSeparator();
								newValue = path.toString() + newValue;
								try {
									child.setAttribute(attribute.getName(), newValue);
								} catch (CoreException e1) {
								}
								break;
							}
						}
					}
				}
			}
			inspectElementsIconPaths(schema, child);
		}
	}

	protected static MultiTextEdit getTextEdit(IModelTextChangeListener listener) {
		if (listener == null)
			return null;
		TextEdit[] edits = listener.getTextOperations();
		if (edits.length == 0)
			return null;
		MultiTextEdit multiEdit = new MultiTextEdit();
		multiEdit.addChildren(edits);
		return multiEdit;
	}

}
