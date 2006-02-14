package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
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
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEManager;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
import org.eclipse.pde.internal.core.text.build.BuildModel;
import org.eclipse.pde.internal.core.text.build.PropertiesTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleClasspathHeader;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ImportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleHeader;
import org.eclipse.pde.internal.core.text.bundle.RequireBundleObject;
import org.eclipse.pde.internal.core.text.bundle.SingleManifestHeader;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Constants;

public class OrganizeManifestsOperation implements IRunnableWithProgress, IOrganizeManifestsSettings {
	
	private static final String F_MANIFEST_FILE = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	private static final String F_PLUGIN_FILE = "plugin.xml"; //$NON-NLS-1$
	private static final String F_FRAGMENT_FILE = "fragment.xml"; //$NON-NLS-1$
	private static String F_NL_PREFIX = "$nl$"; //$NON-NLS-1$
	private static String[] F_ICON_EXTENSIONS = new String[] {
		"BMP", "ICO", "JPEG", "JPG", "GIF", "PNG", "TIFF" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
	};
	
	
	// if operation is executed without setting operations, these defaults will be used
	protected boolean fAddMissing = true; // add all packages to export-package
	protected boolean fMarkInternal = true; // mark export-package as internal
	protected String fPackageFilter = IOrganizeManifestsSettings.VALUE_DEFAULT_FILTER;
	protected boolean fRemoveUnresolved = true; // remove unresolved export-package
	protected boolean fModifyDep = true; // modify import-package / require-bundle
	protected boolean fRemoveDependencies = true; // if true: remove, else mark optional
	protected boolean fUnusedDependencies; // find/remove unused dependencies - long running op
	protected boolean fRemoveLazy = true; // remove lazy/auto start if no activator
	protected boolean fPrefixIconNL; // prefix icon paths with $nl$
	protected boolean fUnusedKeys; // remove unused <bundle-localization>.properties keys
	
	
	private ArrayList fProjectList;
	private IProject fCurrentProject;
	private IBundlePluginModel fCurrentBundleModel;
	private PluginModelBase fCurrentPluginModelBase;
	private ISaveablePart fCurrentOpenEditor;
	
	public OrganizeManifestsOperation(ArrayList projectList) {
		fProjectList = projectList;
	}

	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		monitor.beginTask(PDEUIMessages.OrganizeManifestJob_taskName, fProjectList.size());
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		for (int i = 0; i < fProjectList.size(); i++) {
			if (monitor.isCanceled())
				break;
			resetModels();
			cleanProject((IProject)fProjectList.get(i), manager, new SubProgressMonitor(monitor, 1));
		}
	}
	
	private void cleanProject(IProject project, ITextFileBufferManager manager, IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		
		fCurrentProject = project;
		monitor.beginTask(fCurrentProject.getName(), getTotalTicksPerProject());
		IFile manifest = fCurrentProject.getFile(F_MANIFEST_FILE);
		IFile underlyingXML = fCurrentProject.getFile(F_PLUGIN_FILE);
		if (!underlyingXML.exists())
			underlyingXML = fCurrentProject.getFile(F_FRAGMENT_FILE);
		
		ITextFileBuffer manifestBuffer = null;
		IDocument manifestDoc = null;
		IDocument xmlDoc = null;
		BundleTextChangeListener bundleTextChangeListener = null;
		ITextFileBuffer xmlModelBuffer = null;
		MultiTextEdit textEdit = null;
		try {
			
			boolean loadedFromEditor = loadFromEditor();
			
			if (connectExtensions(underlyingXML)) {
				xmlModelBuffer = connectBuffer(underlyingXML, manager);
				xmlDoc = xmlModelBuffer.getDocument();
				
				if (!loadedFromEditor) {
					if (F_FRAGMENT_FILE.equals(underlyingXML.getName()))
						fCurrentPluginModelBase = new FragmentModel(xmlDoc, false);
					else
						fCurrentPluginModelBase = new PluginModel(xmlDoc, false);
					fCurrentPluginModelBase.load();
				}
				textEdit = new MultiTextEdit();
			}
			
			if (connectBundle()) {
				manifestBuffer = connectBuffer(manifest, manager);
				manifestDoc = manifestBuffer.getDocument();
				
				if (!loadedFromEditor) {
					fCurrentBundleModel = new BundlePluginModel();
					BundleModel bundleModel = new BundleModel(manifestDoc, false);
					bundleModel.load();
					bundleTextChangeListener = new BundleTextChangeListener(manifestDoc);
					bundleModel.addModelChangedListener(bundleTextChangeListener);
					bundleModel.setUnderlyingResource(manifest);
					fCurrentBundleModel.setBundleModel(bundleModel);
					if (fCurrentPluginModelBase != null)
						fCurrentBundleModel.setExtensionsModel(fCurrentPluginModelBase);
				}
			}
			
			runCleanup(monitor, textEdit);
			
		} catch (CoreException e) {
			PDEPlugin.log(e);
		} finally {
			try {
				writeChanges(manifestBuffer, manifestDoc, monitor, getTextEdit(bundleTextChangeListener));
				writeChanges(xmlModelBuffer, xmlDoc, monitor, textEdit);
				
				if (fCurrentOpenEditor != null && !monitor.isCanceled())
					fCurrentOpenEditor.doSave(monitor);
				
				if (connectBundle())
					manager.disconnect(manifest.getFullPath(), null);

				if (connectExtensions(underlyingXML))
					manager.disconnect(underlyingXML.getFullPath(), null);

			} catch (CoreException e) {
				PDEPlugin.log(e);
			} finally {
				monitor.done();
			}
		}
	}
	
	private ITextFileBuffer connectBuffer(IFile file, ITextFileBufferManager manager) throws CoreException {
		manager.connect(file.getFullPath(), null);
		ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
		if (buffer.isDirty())
			buffer.commit(null, true);
		return buffer;
	}
	
	private boolean connectExtensions(IFile underlyingXML) {
		return underlyingXML.exists() && (fPrefixIconNL || fUnusedKeys || fUnusedDependencies);
	}
	
	private boolean connectBundle() {
		return fAddMissing || fModifyDep || fUnusedDependencies
				|| fRemoveLazy || fRemoveUnresolved || fUnusedKeys;
	}
	
	private boolean loadFromEditor() throws PartInitException {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorReference[] editors = page.getEditorReferences();
		for (int i = 0; i < editors.length; i++) {
			if (editors[i].getId().equals(IPDEUIConstants.MANIFEST_EDITOR_ID)) {
				IPersistableElement persistable = editors[i].getEditorInput().getPersistable();
				if (!(persistable instanceof IFileEditorInput))
					continue;
				if (!((IFileEditorInput)persistable).getFile().getProject().equals(fCurrentProject))
					continue;
				fCurrentOpenEditor = page.findEditor(editors[i].getEditorInput());
				IBaseModel model = ((PDEFormEditor)fCurrentOpenEditor).getAggregateModel();
				if (model instanceof IBundlePluginModel) {
					fCurrentBundleModel = (IBundlePluginModel)model;
					ISharedExtensionsModel sharedExtensions = fCurrentBundleModel.getExtensionsModel();
					if (sharedExtensions instanceof PluginModelBase)
						fCurrentPluginModelBase = (PluginModelBase)sharedExtensions;
					return true;
				}
			}
		}
		return false;
	}
	
	private void resetModels() {
		fCurrentProject = null;
		fCurrentBundleModel = null;
		fCurrentPluginModelBase = null;
		fCurrentOpenEditor = null;
	}
	
	private static MultiTextEdit getTextEdit(IModelTextChangeListener listener) {
		if (listener == null)
			return null;
		MultiTextEdit multiEdit = new MultiTextEdit();
		TextEdit[] edits = listener.getTextOperations();
		if (edits.length > 0)
			multiEdit.addChildren(edits);
		return multiEdit;
	}
	
	private void writeChanges(final ITextFileBuffer buffer, final IDocument document, IProgressMonitor monitor, MultiTextEdit multiEdit) {
		if (multiEdit == null || buffer == null || document == null || multiEdit.getChildrenSize() == 0)
			return;
		
		try {
			if (buffer.isShared())
				buffer.commit(null, true);
			
			multiEdit.apply(document);
			buffer.commit(null, true);
		} catch (MalformedTreeException e1) {
		} catch (BadLocationException e1) {
		} catch (CoreException e) {
		}

	}
	
	private void runCleanup(IProgressMonitor monitor, MultiTextEdit edit) throws InvocationTargetException, InterruptedException {
		IBundle bundle = null;
		if (fCurrentBundleModel != null)
			bundle = fCurrentBundleModel.getBundleModel().getBundle();
		String projectName = fCurrentProject.getName();
		
		if (fAddMissing || fRemoveUnresolved) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_export, projectName));
			if (!monitor.isCanceled())
				organizeExportPackages(bundle, fCurrentProject, fAddMissing, fRemoveUnresolved);
			if (fAddMissing)
				monitor.worked(1);
			if (fRemoveUnresolved)
				monitor.worked(1);
		}
		
		if (fMarkInternal) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_filterInternal, projectName));
			if (!monitor.isCanceled())
				markPackagesInternal(bundle, fPackageFilter);
			monitor.worked(1);
		}
		
		if (fModifyDep) {
			String message = fRemoveDependencies ?
					NLS.bind(PDEUIMessages.OrganizeManifestsOperation_removeUnresolved, projectName) :
						NLS.bind(PDEUIMessages.OrganizeManifestsOperation_markOptionalUnresolved, projectName);
			monitor.subTask(message);
			if (!monitor.isCanceled())
				organizeImportPackages(bundle, fRemoveDependencies);
			monitor.worked(1);
			
			if (!monitor.isCanceled())
				organizeRequireBundles(bundle, fRemoveDependencies);
			monitor.worked(1);
		}
		
		
		if (fUnusedDependencies) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedDeps, projectName));
			if (!monitor.isCanceled()) {
				GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(fCurrentBundleModel);
				udo.run(new SubProgressMonitor(monitor, 4));
				GatherUnusedDependenciesOperation.removeDependencies(fCurrentBundleModel, udo.getList().toArray());
			}
		}
		
		
		if (fRemoveLazy) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_lazyStart, fCurrentProject.getName()));
			if (!monitor.isCanceled())
				removeUnneededLazyStart(bundle);
			monitor.worked(1);
		}
		
		if (fPrefixIconNL) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_nlIconPath, projectName));
			if (!monitor.isCanceled())
				prefixIconPaths(fCurrentPluginModelBase, edit);
			monitor.worked(1);
		}
		
		if (fUnusedKeys) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedKeys, projectName));
			if (!monitor.isCanceled())
				removeUnusedKeys(fCurrentProject, bundle, fCurrentPluginModelBase);
			monitor.worked(1);
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
			if (buffer.isDirty())
				buffer.commit(null, true);
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
					allKeys.add(entries[i].getName());
				
				ArrayList usedkeys = new ArrayList();
				findTranslatedStrings(project, modelBase, bundle, usedkeys);
				
				for (int i = 0; i < usedkeys.size(); i++)
					if (allKeys.contains(usedkeys.get(i)))
						allKeys.remove(usedkeys.get(i));
				
				for (int i = 0; i < allKeys.size(); i++) {
					IBuildEntry entry = build.getEntry((String)allKeys.get(i));
					build.remove(entry);
				}
				
				MultiTextEdit multi = getTextEdit(listener);
				if (multi.getChildrenSize() > 0) {
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
		
		if (!model.isLoaded()) {
			try { model.load(); } catch (CoreException e) {}
			if (!model.isLoaded())
				return;
		}
		IPluginExtensionPoint[] points = model.getPluginBase().getExtensionPoints();
		for (int i = 0; i < points.length; i++) {
			String value = getTranslatedKey(points[i].getName());
			if (value != null && !list.contains(value))
				list.add(value);
		}
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			inspectExtensionForTranslation(extensions[i], list);
		}
	}
	
	private static void inspectExtensionForTranslation(IPluginParent parent, ArrayList list) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			String textValue = getTranslatedKey(child.getText());
			if (textValue != null && !list.contains(textValue))
				list.add(textValue);
			
			IPluginAttribute[] attributes = child.getAttributes();
			for (int j = 0; j < attributes.length; j++) {
				String attrValue = getTranslatedKey(attributes[j].getValue());
				if (attrValue != null && !list.contains(attrValue))
					list.add(attrValue);
			}
			
			inspectExtensionForTranslation(child, list);
		}
	}
	
	private static void findTranslatedMFStrings(IBundle bundle, ArrayList list) {
		if (bundle == null)
			return;
		for (int i = 0; i < ICoreConstants.TRANSLATABLE_HEADERS.length; i++) {
			String key = getTranslatedKey(bundle.getHeader(ICoreConstants.TRANSLATABLE_HEADERS[i]));
			if (key != null)
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
	 * @param multiEdit - this MultiTextEdit object must be handled
	 * 		  (applied to a document) by the user after this operation is complete.
	 */
	public static void prefixIconPaths(PluginModelBase model, MultiTextEdit multiEdit) {
		if (model == null)
			return;
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IPluginExtension[] extensions = model.getPluginBase().getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			ISchema schema = registry.getSchema(extensions[i].getPoint());
			if (schema != null)
				inspectElementsIconPaths(schema, extensions[i], multiEdit);
		}
	}
	
	private static void inspectElementsIconPaths(ISchema schema, IPluginParent parent, MultiTextEdit multiEdit) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					if (!(attributes[j] instanceof PluginAttribute))
						continue;
					PluginAttribute attr = (PluginAttribute)attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.RESOURCE) {
						String value = attr.getValue();
						if (value.startsWith(F_NL_PREFIX))
							continue;
						int fileExtIndex = value.lastIndexOf('.');
						if (fileExtIndex == -1)
							continue;
						value = value.substring(fileExtIndex + 1);
						for (int e = 0; e < F_ICON_EXTENSIONS.length; e++) {
							if (value.equalsIgnoreCase(F_ICON_EXTENSIONS[e])) {
								IPath path = new Path(F_NL_PREFIX);
								if (attr.getValue().charAt(0) != Path.SEPARATOR)
									path = path.addTrailingSeparator();
								multiEdit.addChild(new InsertEdit(attr.getValueOffset(), path.toString()));
								break;
							}
						}
					}
				}
			}
			inspectElementsIconPaths(schema, child, multiEdit);
		}
	}
	
	
	private int getTotalTicksPerProject() {
		int ticks = 0;
		if (fAddMissing)
			ticks++;
		if (fMarkInternal)
			ticks++;
		if (fRemoveUnresolved)
			ticks++;
		if (fModifyDep)
			ticks += 2;
		if (fUnusedDependencies)
			ticks += 4;
		if (fRemoveLazy)
			ticks++;
		if (fPrefixIconNL)
			ticks++;
		if (fUnusedKeys)
			ticks++;
		return ticks;
	}
	
	
	public void setOperations(IDialogSettings settings) {
		fAddMissing = !settings.getBoolean(PROP_ADD_MISSING);
		fMarkInternal = !settings.getBoolean(PROP_MARK_INTERNAL);
		fPackageFilter = settings.get(PROP_INTERAL_PACKAGE_FILTER);
		fRemoveUnresolved = !settings.getBoolean(PROP_REMOVE_UNRESOLVED_EX);
		fModifyDep = !settings.getBoolean(PROP_MODIFY_DEP);
		fRemoveDependencies = !settings.getBoolean(PROP_RESOLVE_IMP_MARK_OPT);
		fUnusedDependencies = settings.getBoolean(PROP_UNUSED_DEPENDENCIES);
		fRemoveLazy = !settings.getBoolean(PROP_REMOVE_LAZY);
		fPrefixIconNL = settings.getBoolean(PROP_NLS_PATH);
		fUnusedKeys = settings.getBoolean(PROP_UNUSED_KEYS);
	}
}
