/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.macro;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class RecordBlock implements IRecorderListener {
	private IWorkbenchWindow window;
	
	public void init(IWorkbenchWindow window) {
		this.window = window;
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.addRecorderListener(this);
	}
	
	public void dispose() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.removeRecorderListener(this);
	}
	
	public void startRecording() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		recorder.startRecording();
	}

	public void stopRecording() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
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

	public void recordingStarted() {
	}

	public void recordingStopped() {
	}

	public void recordingInterrupted(int interruptType) {
		if (interruptType==STOP)
			stopRecording();
		else if (interruptType==INDEX)
			insertIndex();
	}

	public void insertIndex() {
		MacroManager recorder = MacroPlugin.getDefault().getMacroManager();
		IndexWizard wizard = new IndexWizard(recorder);
		WizardDialog wd = new WizardDialog(window.getShell(), wizard);
		wd.setMinimumPageSize(300, 400);
		wd.create();
		wd.getShell().setData(MacroManager.IGNORE, Boolean.TRUE);
		wd.open();
	}
}