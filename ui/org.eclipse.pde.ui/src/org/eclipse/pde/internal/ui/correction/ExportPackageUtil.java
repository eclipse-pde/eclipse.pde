package org.eclipse.pde.internal.ui.correction;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.core.ibundle.IBundle;
import org.eclipse.pde.internal.core.text.bundle.Bundle;
import org.eclipse.pde.internal.core.text.bundle.BundleModel;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageObject;
import org.eclipse.pde.internal.core.text.bundle.PackageObject;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class ExportPackageUtil {
	
	public static final boolean removeExportPackage(IBundle bundle, String pkgName) {
		if (! (bundle instanceof Bundle))
			return false;
		ExportPackageHeader header = (ExportPackageHeader)((Bundle)bundle).getManifestHeader(Constants.EXPORT_PACKAGE);
		return header != null && header.removePackage(pkgName) != null;
	}
	
	public static final boolean removeExportPackage(IBundle bundle, PackageObject pkg) {
		if (! (bundle instanceof Bundle))
			return false;
		ExportPackageHeader header = (ExportPackageHeader)((Bundle)bundle).getManifestHeader(Constants.EXPORT_PACKAGE);
		return header != null && header.removePackage(pkg) != null;
	}
	
	public static final boolean organizeExportPackages(IProject proj) throws CoreException, MalformedTreeException, BadLocationException {
		IFile manifest = proj.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
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
				ExportPackageUtil.organizeExportPackages(model.getBundle(), proj);
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
			return true;
		} finally {
			try {
				FileBuffers.getTextFileBufferManager().disconnect(manifest.getFullPath(), null);
			} catch (CoreException e) {
				PDEPlugin.log(e);
			}
		}
	}
	
	public static final boolean organizeExportPackages(IBundle bundle, IProject proj) {
		if (! (bundle instanceof Bundle))
			return false;
		ExportPackageHeader header = (ExportPackageHeader)((Bundle)bundle).getManifestHeader(Constants.EXPORT_PACKAGE);
		ExportPackageObject[] currentPkgs = header.getPackages();
		
		// Running list of packages in the project
		Set packages = new HashSet();
		
		IJavaProject jp = JavaCore.create(proj);
		try {
			IPackageFragmentRoot[] roots = jp.getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				if (isImmediateRoot(roots[i])) {
					IJavaElement[] elements = roots[i].getChildren();
					for (int j = 0; j < elements.length; j++){
						if (elements[j] instanceof IPackageFragment) {
							IPackageFragment fragment = (IPackageFragment)elements[j];
							String name = fragment.getElementName();
							if (name.length() == 0)
								name = "."; //$NON-NLS-1$
							if ((fragment.hasChildren() || fragment.getNonJavaResources().length > 0) && !header.hasPackage(name)) {
								header.addPackage(new ExportPackageObject(header, fragment, Constants.VERSION_ATTRIBUTE));
							}
							packages.add(name);
						}
					}
				}
			}
			
			// Remove packages that don't exist
			for (int i = 0; i < currentPkgs.length; i++) {
				if (!packages.contains(currentPkgs[i].getName()))
					header.removePackage(currentPkgs[i]);
			}
			return true;
		} catch (JavaModelException e) {
			return false;
		}
	}
	
	private static boolean isImmediateRoot(IPackageFragmentRoot root) throws JavaModelException {
		int kind = root.getKind();
		return kind == IPackageFragmentRoot.K_SOURCE
				|| (kind == IPackageFragmentRoot.K_BINARY && !root.isExternal());
	}
	
}
