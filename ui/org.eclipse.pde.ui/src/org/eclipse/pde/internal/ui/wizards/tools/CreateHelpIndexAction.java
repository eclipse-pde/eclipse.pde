package org.eclipse.pde.internal.ui.wizards.tools;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.help.search.HelpIndexBuilder;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * Creates the help search index by parsing the selected
 * plugin.xml file and generating index for TOC extensions.
 *
 * @since 3.1
 */

public class CreateHelpIndexAction implements IObjectActionDelegate {
	private ISelection selection;

	private HelpIndexBuilder indexBuilder;

	public CreateHelpIndexAction() {
	}

	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	public void run(IAction action) {
		File file = getFile();
		if (file == null)
			return;
		if (indexBuilder == null)
			indexBuilder = new HelpIndexBuilder(file);
		else
			indexBuilder.setManifest(file);
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor)
					throws InvocationTargetException {
				try {
					indexBuilder.execute(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(op);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		}
	}

	private File getFile() {
		if (selection == null)
			return null;
		Object obj = ((IStructuredSelection) selection).getFirstElement();
		if (obj instanceof IFile) {
			IFile fileResource = (IFile) obj;
			return fileResource.getLocation().toFile();
		}
		return null;
	}

	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}
}
