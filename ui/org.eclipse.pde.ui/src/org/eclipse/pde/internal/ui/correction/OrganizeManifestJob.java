package org.eclipse.pde.internal.ui.correction;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PluginModelManager;
import org.eclipse.pde.internal.core.TargetPlatform;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.IModelTextChangeListener;
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
import org.eclipse.pde.internal.ui.IPreferenceConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class OrganizeManifestJob extends WorkspaceJob {
	
	private IProject fProject;

	public OrganizeManifestJob(String name, IProject proj) {
		super(name);
		fProject = proj;
	}

	public IStatus runInWorkspace(IProgressMonitor monitor)
			throws CoreException {
		IFile manifest = fProject.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
		try {
			ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
			manager.connect(manifest.getFullPath(), null);
			final ITextFileBuffer buffer = manager.getTextFileBuffer(manifest.getFullPath());
			final IDocument document = buffer.getDocument();		
			BundleModel model = new BundleModel(document, false);
			model.load();
			if (model.isLoaded()) {
				IModelTextChangeListener listener = new BundleTextChangeListener(document);
				model.addModelChangedListener(listener);
				
				organizeHeaders(model.getBundle(), monitor);
				
				TextEdit[] edits = listener.getTextOperations();
				if (edits.length > 0) {
					MultiTextEdit multi = new MultiTextEdit();
					multi.addChildren(edits);
					// if buffer is shared, could be open in editor, therefore run in UI Thread
					if (buffer.isShared()) {
						final MultiTextEdit finalEdit = multi;
						Display.getDefault().asyncExec(new Runnable() {
					           public void run() {
					        	  try {
					        		  finalEdit.apply(document);
					        		  buffer.commit(null, true);
					        	  } catch (Exception e) {
					        		  PDEPlugin.log(e);
					        	  }
					           }
					        });
					} else {
						multi.apply(document);
						buffer.commit(null, true);
					}
				}
			}
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.OK, PDEUIMessages.OrganizeManifestJob_ok, null);
		} catch (CoreException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		} catch (MalformedTreeException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		} catch (BadLocationException e) {
			PDEPlugin.log(e);
			return new Status(IStatus.OK, PDEPlugin.PLUGIN_ID, IStatus.ERROR, e.getLocalizedMessage(), null);
		} finally {
			try {
				FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), null);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
	
	private void organizeHeaders(IBundle bundle, IProgressMonitor monitor) {
		IPreferenceStore store = PDEPlugin.getDefault().getPreferenceStore();
		boolean removeImports = store.getString(IPreferenceConstants.PROP_RESOLVE_IMPORTS).equals(IPreferenceConstants.VALUE_REMOVE_IMPORT);
		
		monitor.beginTask(PDEUIMessages.OrganizeManifestJob_taskName, 3);
		
		if (!monitor.isCanceled()) {
			organizeExportPackages(bundle, fProject);
		}
		monitor.worked(1);
			
		if (!monitor.isCanceled()) {
			organizeImportPackages(bundle, removeImports);
		}
		monitor.worked(1);
		
		if (!monitor.isCanceled()) {
			organizeRequireBundles(bundle, removeImports);
		}
		monitor.worked(1);
	}
	
	public static void organizeExportPackages(IBundle bundle, IProject project) {
		if (!(bundle instanceof Bundle))
			return;
		
		ExportPackageHeader header = (ExportPackageHeader)bundle.getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] currentPkgs;
		if (header == null) {
			String newLine = TextUtilities.getDefaultLineDelimiter(((BundleModel)bundle.getModel()).getDocument());
			header = new ExportPackageHeader(Constants.EXPORT_PACKAGE, "", bundle, newLine); //$NON-NLS-1$
			currentPkgs = new ExportPackageObject[0];
		} else  
			currentPkgs = header.getPackages();
		// Running list of packages in the project
		Set packages = new HashSet();
		
		IPackageFragmentRoot[] roots = findPackageFragmentRoots(bundle, project);
		try {
			for (int i = 0; i < roots.length; i++) {
				if (isImmediateRoot(roots[i])) {
					IJavaElement[] elements = roots[i].getChildren();
					for (int j = 0; j < elements.length; j++)
						if (elements[j] instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment)elements[j];
							String name = fragment.getElementName();
							if (name.length() == 0)
								name = "."; //$NON-NLS-1$
							if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0)){
								if (!header.hasPackage(name)) 
									header.addPackage(new ExportPackageObject(header, fragment, Constants.VERSION_ATTRIBUTE));
								else
									packages.add(name);
							}
						}
				}
			}
			// Remove packages that don't exist
			for (int i = 0; i < currentPkgs.length; i++) 
				if (!packages.contains(currentPkgs[i].getName()))
					header.removePackage(currentPkgs[i]);
		} catch (JavaModelException e) {}
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

	protected static void organizeImportPackages(IBundle bundle, boolean removeImports) {
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

	protected static void organizeRequireBundles(IBundle bundle, boolean removeImports) {
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

}
