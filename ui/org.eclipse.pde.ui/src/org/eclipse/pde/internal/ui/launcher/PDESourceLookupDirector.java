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
package org.eclipse.pde.internal.ui.launcher;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.containers.ProjectSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.WorkspaceSourceContainer;
import org.eclipse.debug.ui.sourcelookup.WorkingSetSourceContainer;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.launching.sourcelookup.containers.JavaSourceLookupParticipant;

public class PDESourceLookupDirector extends AbstractSourceLookupDirector {

	private static Set fFilteredTypes;
	
	static {
		fFilteredTypes = new HashSet(3);
		fFilteredTypes.add(ProjectSourceContainer.TYPE_ID);
		fFilteredTypes.add(WorkspaceSourceContainer.TYPE_ID);
		fFilteredTypes.add(WorkingSetSourceContainer.TYPE_ID); //$NON-NLS-1$
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#initializeParticipants()
	 */
	public void initializeParticipants() {
		addParticipants(new ISourceLookupParticipant[] {new JavaSourceLookupParticipant()});
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.sourcelookup.ISourceLookupDirector#supportsSourceContainerType(org.eclipse.debug.internal.core.sourcelookup.ISourceContainerType)
	 */
	public boolean supportsSourceContainerType(ISourceContainerType type) {
		return !fFilteredTypes.contains(type.getId());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector#getSourceElement(java.lang.Object)
	 */
	public Object getSourceElement(Object element) {
		PDESourceLookupQuery query = new PDESourceLookupQuery(element);
		SafeRunner.run(query);
		Object result = query.getResult();
		return result != null ? result : super.getSourceElement(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector#findSourceElements(java.lang.Object)
	 */
	public Object[] findSourceElements(Object object) throws CoreException {
		Object[] sourceElements = null;
		if (object instanceof IJavaStackFrame || object instanceof IJavaObject || object instanceof IJavaReferenceType){
			sourceElements = new Object[] {getSourceElement(object)};
		}
		if (sourceElements == null) {
			sourceElements = super.findSourceElements(object);
		}
		return sourceElements;
	}
	
}
