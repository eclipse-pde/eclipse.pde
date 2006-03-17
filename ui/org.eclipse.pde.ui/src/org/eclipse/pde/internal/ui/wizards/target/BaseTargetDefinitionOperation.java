package org.eclipse.pde.internal.ui.wizards.target;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.itarget.ILocationInfo;
import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.eclipse.pde.internal.core.target.WorkspaceTargetModel;
import org.eclipse.pde.internal.ui.IPDEUIConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ISetSelectionTarget;

public class BaseTargetDefinitionOperation extends WorkspaceModifyOperation {
	
	private IFile fFile;

	public BaseTargetDefinitionOperation(IFile file){
		fFile = file;
	}

	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
        WorkspaceTargetModel model = new WorkspaceTargetModel(fFile, false);
        initializeTarget(model);
        model.save();
        model.dispose();
        openFile();
        monitor.done();
	}
	
	protected void openFile() {
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchWindow ww = PDEPlugin.getActiveWorkbenchWindow();
				if (ww == null) {
					return;
				}
				IWorkbenchPage page = ww.getActivePage();
				if (page == null || !fFile.exists())
					return;
				IWorkbenchPart focusPart = page.getActivePart();
				if (focusPart instanceof ISetSelectionTarget) {
					ISelection selection = new StructuredSelection(fFile);
					((ISetSelectionTarget) focusPart).selectReveal(selection);
				}
				try {
					IDE.openEditor(page, fFile, IPDEUIConstants.TARGET_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});	
	}
	
	protected void initializeTarget(ITargetModel model) {
		ITarget target = model.getTarget();
		ILocationInfo info = model.getFactory().createLocation();
		info.setPath(PDECore.getDefault().getPluginPreferences().getString(ICoreConstants.PLATFORM_PATH));
		info.setDefault(true);
		target.setLocationInfo(info);
	}

}
