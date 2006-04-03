/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.xhtml.TocReplaceTable.TocReplaceEntry;

public class XHTMLConversionWizard extends Wizard {

	private XHTMLConversionWizardPage page1;
	private TocReplaceTable fTable;

	public XHTMLConversionWizard(TocReplaceTable table) {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_XHTML_CONVERT_WIZ);
		setWindowTitle(PDEUIMessages.XHTMLConversionWizard_title);
		setNeedsProgressMonitor(true);
		fTable = table;
	}
	
	public boolean performFinish() {
		try {
			IRunnableWithProgress op = getConversionOperation(page1.getCheckedEntries());
			getContainer().run(true, true, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return true; // exception handled
		}		
		return true;
	}
	
	public IRunnableWithProgress getConversionOperation(final TocReplaceEntry[] models) {
			return new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor)
					throws InvocationTargetException, InterruptedException {
					try {
						XHTMLConversionOperation op = new XHTMLConversionOperation(models, getShell());
						PDEPlugin.getWorkspace().run(op, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} catch (OperationCanceledException e) {
						throw new InterruptedException(e.getMessage());
					} finally {
						monitor.done();
					}
				}
			};
		}
	
	public void addPages() {
		page1 = new XHTMLConversionWizardPage(fTable);
		addPage(page1);
	}
	
}
