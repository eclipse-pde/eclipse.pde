package org.eclipse.ui.internal.macro;


import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
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
public class RecordAction implements IWorkbenchWindowActionDelegate, IRecorderListener {
	private RecordBlock recordBlock;
	private IAction action;
	
	/**
	 * The constructor.
	 */
	public RecordAction() {
		recordBlock = new RecordBlock();
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
		if (recorder.isRecording()) {
			action.setEnabled(false);
			return;
		}
		recordBlock.startRecording();
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

	/**
	 * We can use this method to dispose of any system
	 * resources we previously allocated.
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.removeRecorderListener(this);
		recordBlock.dispose();
	}
	
	public void recordingStarted() {
		this.action.setEnabled(false);
	}
	
	public void recordingStopped() {
		this.action.setEnabled(true);
	}

	public void recordingInterrupted(int type) {
	}

	/**
	 * We will cache window object in order to
	 * be able to provide parent shell for the message dialog.
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		recordBlock.init(window);
	}
}