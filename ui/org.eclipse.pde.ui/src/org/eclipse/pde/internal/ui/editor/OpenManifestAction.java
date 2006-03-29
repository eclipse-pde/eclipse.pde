package org.eclipse.pde.internal.ui.editor;
import java.util.ArrayList;
import java.util.Iterator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.WorkspaceModelManager;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.ide.IDE;

public class OpenManifestAction implements IWorkbenchWindowActionDelegate {
	
	private ISelection fSelection;
	
	public OpenManifestAction() {
		super();
	}
	
	public void dispose() {
	}
	
	public void init(IWorkbenchWindow window) {
	}
	
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			Iterator it = ssel.iterator();
			final ArrayList projects = new ArrayList();
			while (it.hasNext()) {
				Object element = it.next();
				IProject proj = null;
				if (element instanceof IFile)
					proj = ((IFile) element).getProject();
				else if (element instanceof IProject)
					proj = (IProject) element;
				if (proj != null
						&& (WorkspaceModelManager.hasBundleManifest(proj) 
							|| WorkspaceModelManager.hasPluginManifest(proj)))
					projects.add(proj);
			}
			if (projects.size() > 0) {
				BusyIndicator.showWhile(PDEPlugin.getActiveWorkbenchShell()
						.getDisplay(), new Runnable() {
					public void run() {
						Iterator it = projects.iterator();
						while (it.hasNext()) {
							IProject project = (IProject) it.next();
							IFile file = project.getFile("META-INF/MANIFEST.MF"); //$NON-NLS-1$
							if (file == null || !file.exists())
								file = project.getFile("plugin.xml"); //$NON-NLS-1$
							if (file == null || !file.exists())
								file = project.getFile("fragment.xml"); //$NON-NLS-1$
							if (file == null || !file.exists())
								MessageDialog.openError(PDEPlugin
										.getActiveWorkbenchShell(),
										PDEUIMessages.OpenManifestsAction_title,
										NLS.bind(PDEUIMessages.OpenManifestsAction_cannotFind, project.getName()));
							else
								try {
									IDE.openEditor(PDEPlugin.getActivePage(), file);
								} catch (PartInitException e) {
									MessageDialog.openError(PDEPlugin
											.getActiveWorkbenchShell(),
											PDEUIMessages.OpenManifestsAction_title,
											NLS.bind(PDEUIMessages.OpenManifestsAction_cannotOpen, project.getName()));
								}
						}
					}
				});
			} else
				MessageDialog.openInformation(PDEPlugin
						.getActiveWorkbenchShell(),
						PDEUIMessages.OpenManifestsAction_title,
						PDEUIMessages.OpenManifestAction_noManifest);
		}
	}
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;
	}
}
