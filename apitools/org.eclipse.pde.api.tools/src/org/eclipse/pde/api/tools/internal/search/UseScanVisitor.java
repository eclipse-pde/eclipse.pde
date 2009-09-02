/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;

/**
 * Used to visit an API use scan. This visitor visits each referenced component in 
 * a use scan, by each component that refers to it, by each reference.
 */
public class UseScanVisitor {
	
	/**
	 * Start visiting an API use scan
	 */
	public void visitScan() {
	}
	
	/**
	 * Ends visiting an API use scan
	 */
	public void endVisitScan() {
	}

	/**
	 * Visits the given component and returns whether to visit components referencing
	 * this component.
	 * 
	 * @param target API component to which references exist
	 * @return whether to visit components that reference this component
	 */
	public boolean visitComponent(IComponentDescriptor target) {
		return true;
	}
	
	/**
	 * End visiting a component that was referenced by others
	 * 
	 * @param target the component that was visited
	 */
	public void endVisit(IComponentDescriptor target) {
	}
	
	/**
	 * Visits a component that makes references to the current target component being visited
	 * and returns whether to visit individual references.
	 * 
	 * @param component the component that references the current target component
	 * @return whether to visit reference members within the component
	 */
	public boolean visitReferencingComponent(IComponentDescriptor component) {
		return true;
	}
	
	/**
	 * Ends visiting a component that made references to the current target component.
	 * 
	 * @param component that component that was visited
	 */
	public void endVisitReferencingComponent(IComponentDescriptor component) {	
	}
	
	/**
	 * Visits a referenced member and returns whether to visit reference locations
	 * 
	 * @param referencedMember the member that was referenced
	 * @return whether to visit individual reference locations
	 */
	public boolean visitMember(IMemberDescriptor referencedMember) {
		return true;
	}
	
	/**
	 * End visits a referenced member
	 * 
	 * @param referencedMember the member that was referenced
	 */
	public void endVisitMember(IMemberDescriptor referencedMember) {
		
	}	
	
	/**
	 * Visits a reference to the current member.
	 * 
	 * @param refKind the kind of {@link org.eclipse.pde.api.tools.internal.provisional.builder.IReference} 
	 * @param fromMember describes the member the reference was made from 
	 * @param lineNumber the line number the reference was on or -1 if unknown
	 * @param visibility see {@link org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers}
	 */
	public void visitReference(int refKind, IMemberDescriptor fromMember, int lineNumber, int visibility) {
		
	}
	
}
