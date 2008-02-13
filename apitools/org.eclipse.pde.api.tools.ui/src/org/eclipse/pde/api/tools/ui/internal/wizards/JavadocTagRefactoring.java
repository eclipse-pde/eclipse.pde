/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.NullChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;

import com.ibm.icu.text.MessageFormat;

/**
 * A refactoring to change javadoc tags during API tooling setup
 * 
 * @since 0.1.0
 */
public class JavadocTagRefactoring extends Refactoring {
	
	/**
	 * Map that text changes are collected into.
	 * <pre>
	 * HashMap<IFile, HashSet<TextEdit>>
	 * </pre> 
	 */
	private HashMap fChangeMap = null;
	
	/**
	 * Map of projects to their composite changes
	 * <pre>
	 * HashMap<IProject, CompositeChange>
	 * </pre>
	 */
	private HashMap fProjectChangeMap = new HashMap();
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkFinalConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if(fChangeMap == null) {
			return RefactoringStatus.createErrorStatus(WizardMessages.JavadocTagRefactoring_0);
		}
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#checkInitialConditions(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return RefactoringStatus.create(Status.OK_STATUS);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#createChange(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		if(fChangeMap == null) {
			return new NullChange();
		}
		CompositeChange cchange = new CompositeChange(WizardMessages.JavadocTagRefactoring_1);
		IFile file = null;
		MultiTextEdit alledits = null;
		TextFileChange change = null;
		HashSet edits = null;
		TextEdit edit = null;
		IProject project = null;
		CompositeChange pchange = null;
		for(Iterator iter = fChangeMap.keySet().iterator(); iter.hasNext();) {
			file = (IFile) iter.next();
			project = file.getProject();
			pchange = (CompositeChange) fProjectChangeMap.get(project);
			if(pchange == null) {
				pchange = new CompositeChange(project.getName());
				fProjectChangeMap.put(project, pchange);
			}
			change = new TextFileChange(MessageFormat.format(WizardMessages.JavadocTagRefactoring_2, new String[] {file.getName()}), file);
			alledits = new MultiTextEdit();
			change.setEdit(alledits);
			edits = (HashSet) fChangeMap.get(file);
			if(edits != null) {
				for(Iterator iter2 = edits.iterator(); iter2.hasNext();) {
					edit = (TextEdit) iter2.next();
					alledits.addChild(edit);
				}
				pchange.add(change);
			}
		}
		for(Iterator iter = fProjectChangeMap.keySet().iterator(); iter.hasNext();) {
			cchange.add((Change) fProjectChangeMap.get(iter.next()));
		}
		fProjectChangeMap.clear();
		return cchange;
	}
	
	/**
	 * Sets the current input that the changes for this refactoring should be created from
	 * @param changemap
	 */
	public void setChangeInput(HashMap changemap) {
		fChangeMap = changemap;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
	 */
	public String getName() {
		return WizardMessages.JavadocTagRefactoring_3;
	}
}
