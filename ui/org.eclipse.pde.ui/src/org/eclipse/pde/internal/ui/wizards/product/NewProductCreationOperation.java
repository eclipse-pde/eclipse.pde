package org.eclipse.pde.internal.ui.wizards.product;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.iproduct.*;
import org.eclipse.pde.internal.core.product.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.launcher.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.part.*;


public class NewProductCreationOperation extends WorkspaceModifyOperation {

	private IFile fFile;
	private ILaunchConfiguration fLaunchConfiguration;

	public NewProductCreationOperation(IFile file, ILaunchConfiguration config) {
		fFile = file;
		fLaunchConfiguration = config;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.WorkspaceModifyOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(IProgressMonitor monitor) throws CoreException,
			InvocationTargetException, InterruptedException {
		monitor.beginTask("Creating product configuration file...", 2);
		createContent();
		monitor.worked(1);
        openFile();
        monitor.done();
	}
	
	private void createContent() {
        WorkspaceProductModel model = new WorkspaceProductModel(fFile);
        IProduct product = model.getProduct();
        if (fLaunchConfiguration != null)
        	initializeProduct(product);
        model.save();		
	}
	
	private void initializeProduct(IProduct product) {
		try {
			product.setUseProduct(fLaunchConfiguration.getAttribute(ILauncherSettings.USE_PRODUCT, false));
			product.setApplication("com.example.xyz.application");
			product.setId("com.example.xyz.product");
			product.setName("RCP Product");
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
	}
	
	private void openFile() {
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
					page.openEditor(new FileEditorInput(fFile), EditorsUI.DEFAULT_TEXT_EDITOR_ID);
				} catch (PartInitException e) {
				}
			}
		});
	}

}
