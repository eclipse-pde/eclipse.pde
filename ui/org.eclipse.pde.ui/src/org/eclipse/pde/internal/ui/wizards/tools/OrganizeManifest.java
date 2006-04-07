/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttribute;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentTextNode;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
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
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class OrganizeManifest implements IOrganizeManifestsSettings {

	private static String F_NL_PREFIX = "$nl$"; //$NON-NLS-1$
	private static String[] F_ICON_EXTENSIONS = new String[] {
		"BMP", "ICO", "JPEG", "JPG", "GIF", "PNG", "TIFF" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	};
	
	public static void organizeRequireBundles(IBundle bundle, boolean removeImports) {
		if (!(bundle instanceof Bundle))
			return;
		
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		RequireBundleHeader header = (RequireBundleHeader)((Bundle)bundle).getManifestHeader(Constants.REQUIRE_BUNDLE);
		if (header != null) {
			RequireBundleObject[] bundles = header.getRequiredBundles();
			for (int i = 0; i < bundles.length; i++) {
				String pluginId = bundles[i].getId();
				if (manager.findEntry(pluginId) == null) {
					if (removeImports)
						header.removeBundle(bundles[i]);
					else {
						bundles[i].setOptional(true);
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
		
		ExportPackageHeader header = (ExportPackageHeader)bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] currentPkgs;
		if (header == null) {
			bundle.setHeader(Constants.EXPORT_PACKAGE, ""); //$NON-NLS-1$
			header = (ExportPackageHeader)bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
			currentPkgs = new ExportPackageObject[0];
		} else  
			currentPkgs = header.getPackages();
		
		IPackageFragmentRoot[] roots = findPackageFragmentRoots(bundle, project);
		// Running list of packages in the project
		Set packages = new HashSet();
		for (int i = 0; i < roots.length; i++) {
			try {
				if (isImmediateRoot(roots[i])) {
					IJavaElement[] elements = roots[i].getChildren();
					for (int j = 0; j < elements.length; j++)
						if (elements[j] instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment)elements[j];
							String name = fragment.getElementName();
							if (name.length() == 0)
								name = "."; //$NON-NLS-1$
							if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0)){
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
	
	private static IPackageFragmentRoot[] findPackageFragmentRoots(IBundle bundle, IProject proj) {
		IJavaProject jproj = JavaCore.create(proj);
		BundleClasspathHeader cpHeader = (BundleClasspathHeader)bundle.getManifestHeader(Constants.BUNDLE_CLASSPATH);
		Vector libs;
		if (cpHeader == null) 
			libs = new Vector();
		else 
		    libs = cpHeader.getElementNames();
		if (libs.size() == 0) 
			libs.add("."); //$NON-NLS-1$
		
		List pkgFragRoots = new LinkedList();
		IBuild build = null;
		
		Iterator it = libs.iterator();
		while (it.hasNext()) {
			String lib = (String)it.next();
			IPackageFragmentRoot root = null;
			if (!lib.equals(".")) //$NON-NLS-1$
				root = jproj.getPackageFragmentRoot(proj.getFile(lib));
			if (root != null && root.exists()) {
				pkgFragRoots.add(root);
			} else {
				// Parse build.properties only once
				if (build == null) 
					build = getBuild(proj);
				// if valid build.properties exists.  Do NOT use else statement!  getBuild() could return null.
				if (build != null) {  
					IBuildEntry entry = build.getEntry("source." + lib); //$NON-NLS-1$
					if (entry == null)
						continue;
					String[] tokens = entry.getTokens();
					for (int i = 0; i < tokens.length; i++) {
						root = jproj.getPackageFragmentRoot(proj.getFolder(tokens[i]));
						if (root != null && root.exists())
							pkgFragRoots.add(root);
					}
				}
			}
		}
		return (IPackageFragmentRoot[]) pkgFragRoots.toArray(new IPackageFragmentRoot[pkgFragRoots.size()]);
	}
	
	private final static IBuild getBuild(IProject proj){
		IFile buildProps = proj.getFile("build.properties"); //$NON-NLS-1$
		if (buildProps != null) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(buildProps);
			if (model != null) 
				return model.getBuild();
		}
		return null;
	}

	private static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE
				|| (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}

	public static void markPackagesInternal(IBundle bundle, String packageFilter) {
		if (packageFilter == null || bundle == null || !(bundle instanceof Bundle))
			return;
		
		ExportPackageHeader header = (ExportPackageHeader)bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		if (header == null)
			return;
		
		ExportPackageObject[] currentPkgs = header.getPackages();
		Pattern pat = PatternConstructor.createPattern(packageFilter, false);
		for (int i = 0; i < currentPkgs.length; i++) {
			String values = currentPkgs[i].getValueComponents()[0];
			if (!currentPkgs[i].isInternal() 
					&& currentPkgs[i].getFriends().length == 0
					&& pat.matcher(values).matches()) {
				currentPkgs[i].setInternal(true);
			}
		}
	}
	
	public static void organizeImportPackages(IBundle bundle, boolean removeImports) {
		if (!(bundle instanceof Bundle))
			return;
		ImportPackageHeader header = (ImportPackageHeader)((Bundle)bundle).getManifestHeader(Constants.IMPORT_PACKAGE);
		if (header == null)
			return;
		ImportPackageObject[] importedPackages = header.getPackages();
		Set availablePackages = getAvailableExportedPackages();
		// get Preference
		for (int i = 0; i < importedPackages.length; i++) {
			String pkgName = importedPackages[i].getName();
			if (!availablePackages.contains(pkgName)){
				if (removeImports)
					header.removePackage(importedPackages[i]);
				else {
					importedPackages[i].setOptional(true);
				}
			}
		}
	}
	
	private static final Set getAvailableExportedPackages() {
		State state = TargetPlatform.getState();
		ExportPackageDescription[] packages = state.getExportedPackages();
		Set set = new HashSet();
		for (int i = 0; i < packages.length; i++) {
			set.add(packages[i].getName());
		}
		return set;
	}


	
	public static void removeUnneededLazyStart(IBundle bundle) {
		if (!(bundle instanceof Bundle))
			return;
		if (bundle.getHeader(Constants.BUNDLE_ACTIVATOR) == null) {
			String[] remove = new String[] {
					ICoreConstants.ECLIPSE_LAZYSTART, ICoreConstants.ECLIPSE_AUTOSTART};
			for (int i = 0; i < remove.length; i++) {
				IManifestHeader lazy = ((Bundle)bundle).getManifestHeader(remove[i]);
				if (lazy instanceof SingleManifestHeader)
					((SingleManifestHeader)lazy).setMainComponent(null);
			}
		}
		
	}
	
	public static void removeUnusedKeys(IProject project, IBundle bundle, IPluginModelBase modelBase) {
		String localization = bundle.getHeader(Constants.BUNDLE_LOCALIZATION);
		if (localization == null)
			localization = "plugin"; //$NON-NLS-1$
		IFile propertiesFile = project.getFile(localization + ".properties"); //$NON-NLS-1$
		if (!propertiesFile.exists())
			return;
		
		IPath propertiesPath = propertiesFile.getFullPath();
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(propertiesPath, null);
			ITextFileBuffer buffer = manager.getTextFileBuffer(propertiesPath);
			IDocument document = buffer.getDocument();
			// reuse BuildModel - basic properties file model
			BuildModel properties = new BuildModel(document, false);
			properties.load();
			if (properties.isLoaded()) {
				
				IModelTextChangeListener listener = new PropertiesTextChangeListener(document);
				properties.addModelChangedListener(listener);

				IBuild build = properties.getBuild();
				IBuildEntry[] entries = build.getBuildEntries();
				ArrayList allKeys = new ArrayList(entries.length);
				for (int i = 0; i < entries.length; i++)
					if (!allKeys.contains(entries[i].getName()))
						allKeys.add(entries[i].getName());
				
				ArrayList usedkeys = new ArrayList();
				findTranslatedStrings(project, modelBase, bundle, usedkeys);
				
				for (int i = 0; i < usedkeys.size(); i++)
					allKeys.remove(usedkeys.get(i));
				
				if (allKeys.size() == 0)
					return;
				
				// scan properties file for keys referencing other keys
				for (int i = 0; i < entries.length; i++) {
					String[] tokens = entries[i].getTokens();
					if (tokens == null || tokens.length == 0)
						continue;
					String entry = tokens[0];
					for (int k = 1; k < tokens.length; k++)
						entry += ',' + tokens[k];
					if (entry.indexOf('%') == entry.lastIndexOf('%'))
						continue;
					
					// allKeys must NOT have any duplicates
					for (int j = 0; j < allKeys.size(); j++) {
						String akey = '%' + (String)allKeys.get(j) + '%';
						if (entry.indexOf(akey) != -1)
							allKeys.remove(allKeys.get(j--));
						if (allKeys.size() == 0)
							return;
					}
				}
				
				for (int i = 0; i < allKeys.size(); i++) {
					IBuildEntry entry = build.getEntry((String)allKeys.get(i));
					build.remove(entry);
				}
				
				MultiTextEdit multi = getTextEdit(listener);
				if (multi != null && multi.getChildrenSize() > 0) {
					multi.apply(document);
					buffer.commit(null, true);
				}

			}
		} catch (CoreException e) {
			PDEPlugin.log(e);
		} catch (MalformedTreeException e) {
			PDEPlugin.log(e);
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
		} finally {
			try {
				FileBuffers.getTextFileBufferManager().disconnect(propertiesPath, null);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
	
	private static void findTranslatedStrings(IProject project, IPluginModelBase pluginModel, IBundle bundle, ArrayList list) {
		
		findTranslatedXMLStrings(pluginModel, list);
		findTranslatedMFStrings(bundle, list);
		
		PluginModelManager manager = PDECore.getDefault().getModelManager();
		IPluginModelBase model = manager.findModel(project);
		
		BundleDescription bundleDesc = model.getBundleDescription();
		HostSpecification hostSpec = bundleDesc.getHost();
		if (hostSpec != null) {
			BundleDescription[] hosts = hostSpec.getHosts();
			for (int i = 0; i < hosts.length; i++) {
				IPluginModelBase hostModel = manager.findModel(hosts[i].getName());
				if (hostModel != null) {
					findTranslatedXMLStrings(getTextModel(hostModel, false), list);
					findTranslatedMFStrings(getTextBundle(hostModel), list);
				}
			}
		} else {
			IFragmentModel[] fragmentModels = PDEManager.findFragmentsFor(model);
			for (int i = 0; i < fragmentModels.length; i++) {
				findTranslatedXMLStrings(getTextModel(fragmentModels[i], true), list);
				findTranslatedMFStrings(getTextBundle(fragmentModels[i]), list);
			}
		}
	}
	
	private static IPluginModelBase getTextModel(IPluginModelBase model, boolean fragment) {
		if (model instanceof PluginModel || model instanceof FragmentModel)
			return model;

		if (model != null) {
			if (!fileExists(model.getInstallLocation(),
					fragment ? F_FRAGMENT_FILE : F_PLUGIN_FILE))
				return null;
			IDocument doc = CoreUtility.getTextDocument(
					new File(model.getInstallLocation()),
					fragment ? F_FRAGMENT_FILE : F_PLUGIN_FILE);
			IPluginModelBase returnModel;
			if (fragment)
				returnModel = new FragmentModel(doc, false);
			else
				returnModel = new PluginModel(doc, false);
			try {
				returnModel.load();
			} catch (CoreException e) {}
			
			if (returnModel.isLoaded())
				return returnModel;
		}
		return null;
	}
	
	private static IBundle getTextBundle(IPluginModelBase model) {
		if (model != null) {
			if (!fileExists(model.getInstallLocation(), F_MANIFEST_FILE))
				return null;
			IDocument doc = CoreUtility.getTextDocument(
					new File(model.getInstallLocation()), F_MANIFEST_FILE);
			IBundleModel bundleModel = new BundleModel(doc, false);
			try {
				bundleModel.load();
			} catch (CoreException e) {}
			
			if (bundleModel.isLoaded())
				return bundleModel.getBundle();
		}
		return null;
	}
	
	private static void findTranslatedXMLStrings(IPluginModelBase model, ArrayList list) {
		if (model == null)
			return;
		
		IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			String value = getTranslatedKey(points[i].getName());
			if (value != null && !list.contains(value))
				list.add(value);
		}
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++)
			if (extensions[i] instanceof IDocumentNode)
				inspectElementForTranslation((IDocumentNode)extensions[i], list);
	}
	
	private static void inspectElementForTranslation(IDocumentNode parent, ArrayList list) {
		IDocumentTextNode text = parent.getTextNode();
		String textValue = getTranslatedKey(text != null ? text.getText() : null);
		if (textValue != null && !list.contains(textValue))
			list.add(textValue);
		
		IDocumentAttribute[] attributes = parent.getNodeAttributes();
		for (int j = 0; j < attributes.length; j++) {
			String attrValue = getTranslatedKey(attributes[j].getAttributeValue());
			if (attrValue != null && !list.contains(attrValue))
				list.add(attrValue);
		}
		
		if (!(parent instanceof IPluginParent))
			return;
		
		IPluginObject[] children = ((IPluginParent)parent).getChildren();
		for (int i = 0; i < children.length; i++)
			if (children[i] instanceof IDocumentNode)
				inspectElementForTranslation((IDocumentNode)children[i], list);	
	}
	
	private static void findTranslatedMFStrings(IBundle bundle, ArrayList list) {
		if (bundle == null)
			return;
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			String key = getTranslatedKey(bundle.getHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]));
			if (key != null && !list.contains(key))
				list.add(key);
		}
	}
	
	private static String getTranslatedKey(String value) {
		if (value != null && value.length() > 1 
				&& value.charAt(0) == '%' && value.charAt(1) != '%')
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
		for (int i = 0; i < extensions.length; i++) {
			ISchema schema = registry.getSchema(extensions[i].getPoint());
			if (schema != null)
				inspectElementsIconPaths(schema, extensions[i]);
		}
	}
	
	private static void inspectElementsIconPaths(ISchema schema, IPluginParent parent) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					ISchemaAttribute attInfo = schemaElement.getAttribute(attributes[j].getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.RESOURCE) {
						String value = attributes[j].getValue();
						if (value.startsWith(F_NL_PREFIX))
							continue;
						int fileExtIndex = value.lastIndexOf('.');
						if (fileExtIndex == -1)
							continue;
						value = value.substring(fileExtIndex + 1);
						for (int e = 0; e < F_ICON_EXTENSIONS.length; e++) {
							if (value.equalsIgnoreCase(F_ICON_EXTENSIONS[e])) {
								IPath path = new Path(F_NL_PREFIX);
								String newValue = attributes[j].getValue();
								if (newValue.charAt(0) != IPath.SEPARATOR)
									path = path.addTrailingSeparator();
								newValue = path.toString() + newValue;
								try {
									child.setAttribute(attributes[j].getName(), newValue);
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
