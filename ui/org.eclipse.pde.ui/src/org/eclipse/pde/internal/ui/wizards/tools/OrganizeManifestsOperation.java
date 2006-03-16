package org.eclipse.pde.internal.ui.wizards.tools;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.ISharedExtensionsModel;
import org.eclipse.pde.internal.core.bundle.BundlePluginModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.ibundle.IBundlePluginModel;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.BundleTextChangeListener;
import org.eclipse.pde.internal.core.text.plugin.FragmentModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.core.text.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormEditor;
import org.eclipse.pde.internal.ui.search.dependencies.GatherUnusedDependenciesOperation;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.ISaveablePart;

public class OrganizeManifestsOperation implements IRunnableWithProgress, IOrganizeManifestsSettings {
	
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
	private MultiTextEdit fXMLTextEdit;
	
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
		BundleTextChangeListener bundleTextChangeListener = null;
		ITextFileBuffer xmlModelBuffer = null;
		IDocument xmlDoc = null;
		boolean loadedFromEditor = false;
		try {
			
			loadedFromEditor = loadFromEditor();
			
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
			}
			
			if (connectBundle() && !loadedFromEditor) {
				manifestBuffer = connectBuffer(manifest, manager);
				manifestDoc = manifestBuffer.getDocument();
				
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
			
			runCleanup(monitor);
			
		} catch (CoreException e) {
			PDEPlugin.log(e);
		} finally {
			try {
				writeChanges(manifestBuffer, manifestDoc, OrganizeManifest.getTextEdit(bundleTextChangeListener));
				writeChanges(xmlModelBuffer, xmlDoc, fXMLTextEdit);
				
				if (fCurrentOpenEditor != null && !monitor.isCanceled())
					fCurrentOpenEditor.doSave(monitor);
				
				if (connectExtensions(underlyingXML))
					manager.disconnect(underlyingXML.getFullPath(), null);

				if (connectBundle() && !loadedFromEditor)
					manager.disconnect(manifest.getFullPath(), null);
				
			} catch (CoreException e) {
				PDEPlugin.log(e);
			} finally {
				monitor.done();
			}
		}
	}
	
	private ITextFileBuffer connectBuffer(IFile file, ITextFileBufferManager manager) throws CoreException {
		manager.connect(file.getFullPath(), null);
		return manager.getTextFileBuffer(file.getFullPath());
	}
	
	private boolean connectExtensions(IFile underlyingXML) {
		return underlyingXML.exists() && (fPrefixIconNL || fUnusedKeys || fUnusedDependencies);
	}
	
	private boolean connectBundle() {
		return fAddMissing || fModifyDep || fUnusedDependencies
				|| fRemoveLazy || fRemoveUnresolved || fUnusedKeys;
	}
	
	private boolean loadFromEditor() {
		fCurrentOpenEditor = PDEPlugin.getOpenManifestEditor(fCurrentProject);
		if (fCurrentOpenEditor != null) {
			IBaseModel model = ((PDEFormEditor)fCurrentOpenEditor).getAggregateModel();
			if (model instanceof IBundlePluginModel) {
				fCurrentBundleModel = (IBundlePluginModel)model;
				ISharedExtensionsModel sharedExtensions = fCurrentBundleModel.getExtensionsModel();
				if (sharedExtensions instanceof PluginModelBase)
					fCurrentPluginModelBase = (PluginModelBase)sharedExtensions;
				return true;
			}
		}
		return false;
	}
	
	private void resetModels() {
		fCurrentProject = null;
		fCurrentBundleModel = null;
		fCurrentPluginModelBase = null;
		fCurrentOpenEditor = null;
		fXMLTextEdit = null;
	}
	
	private void writeChanges(ITextFileBuffer buffer, IDocument document, MultiTextEdit multiEdit) {
		if (multiEdit == null || buffer == null || document == null || multiEdit.getChildrenSize() == 0)
			return;
		
		try {
			multiEdit.apply(document);
			buffer.commit(null, true);
		} catch (MalformedTreeException e1) {
		} catch (BadLocationException e1) {
		} catch (CoreException e) {
		}

	}
	
	private void runCleanup(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		IBundle bundle = null;
		if (fCurrentBundleModel != null)
			bundle = fCurrentBundleModel.getBundleModel().getBundle();
		String projectName = fCurrentProject.getName();
		
		if (fAddMissing || fRemoveUnresolved) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_export, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.organizeExportPackages(bundle, fCurrentProject, fAddMissing, fRemoveUnresolved);
			if (fAddMissing)
				monitor.worked(1);
			if (fRemoveUnresolved)
				monitor.worked(1);
		}
		
		if (fMarkInternal) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_filterInternal, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.markPackagesInternal(bundle, fPackageFilter);
			monitor.worked(1);
		}
		
		if (fModifyDep) {
			String message = fRemoveDependencies ?
					NLS.bind(PDEUIMessages.OrganizeManifestsOperation_removeUnresolved, projectName) :
						NLS.bind(PDEUIMessages.OrganizeManifestsOperation_markOptionalUnresolved, projectName);
			monitor.subTask(message);
			if (!monitor.isCanceled())
				OrganizeManifest.organizeImportPackages(bundle, fRemoveDependencies);
			monitor.worked(1);
			
			if (!monitor.isCanceled())
				OrganizeManifest.organizeRequireBundles(bundle, fRemoveDependencies);
			monitor.worked(1);
		}
		
		if (fUnusedDependencies) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedDeps, projectName));
			if (!monitor.isCanceled()) {
				SubProgressMonitor submon = new SubProgressMonitor(monitor, 4);
				GatherUnusedDependenciesOperation udo = new GatherUnusedDependenciesOperation(fCurrentBundleModel);
				udo.run(submon);
				GatherUnusedDependenciesOperation.removeDependencies(fCurrentBundleModel, udo.getList().toArray());
				submon.done();
			}
		}
		
		if (fRemoveLazy) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_lazyStart, fCurrentProject.getName()));
			if (!monitor.isCanceled())
				OrganizeManifest.removeUnneededLazyStart(bundle);
			monitor.worked(1);
		}
		
		if (fPrefixIconNL) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_nlIconPath, projectName));
			if (!monitor.isCanceled())
				fXMLTextEdit = OrganizeManifest.prefixIconPaths(fCurrentPluginModelBase);
			monitor.worked(1);
		}
		
		if (fUnusedKeys) {
			monitor.subTask(NLS.bind(PDEUIMessages.OrganizeManifestsOperation_unusedKeys, projectName));
			if (!monitor.isCanceled())
				OrganizeManifest.removeUnusedKeys(fCurrentProject, bundle, fCurrentPluginModelBase);
			monitor.worked(1);
		}
	}

	private int getTotalTicksPerProject() {
		int ticks = 0;
		if (fAddMissing)		ticks += 1;
		if (fMarkInternal)		ticks += 1;
		if (fRemoveUnresolved)	ticks += 1;
		if (fModifyDep)			ticks += 2;
		if (fUnusedDependencies)ticks += 4;
		if (fRemoveLazy)		ticks += 1;
		if (fPrefixIconNL)		ticks += 1;
		if (fUnusedKeys)		ticks += 1;
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
