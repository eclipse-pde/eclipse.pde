package org.eclipse.ui.internal.macro;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Our sample action implements workbench action delegate.
 * The action proxy will be created by the workbench and
 * shown in the UI. When the user tries to use the action,
 * this delegate will be created and execution will be 
 * delegated to it.
 * @see IWorkbenchWindowActionDelegate
 */
public class StopAction implements IWorkbenchWindowActionDelegate, IRecorderListener {
	private IWorkbenchWindow window;
	private IAction action;
	/**
	 * The constructor.
	 */
	public StopAction() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.addRecorderListener(this);
	}

	/**
	 * The action has been activated. The argument of the
	 * method represents the 'real' action sitting
	 * in the workbench UI.
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		this.action = action;
		if (!recorder.isRecording()) {
			action.setEnabled(false);
			return;
		}
		Macro macro = recorder.stopRecording();
		StringWriter swriter = new StringWriter();
		PrintWriter pwriter = new PrintWriter(swriter);
		macro.write("", pwriter);
		pwriter.close();
		try {
			swriter.close();
		}
		catch (IOException e) {
			System.out.println(e);
		}
		String contents = swriter.toString();
		NewMacroWizard wizard = new NewMacroWizard(contents);
		WizardDialog wd = new WizardDialog(window.getShell(), wizard);
		wd.setMinimumPageSize(500, 500);
		wd.open();
	}

	/**
	 * Selection in the workbench has been changed. We 
	 * can change the state of the 'real' action here
	 * if we want, but this can only happen after 
	 * the delegate has been created.
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	public void recordingStarted() {
		if (action!=null)
			action.setEnabled(true);
	}
	
	public void recordingStopped() {
		if (action!=null)
			action.setEnabled(false);
	}

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.removeRecorderListener(this);
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}
}