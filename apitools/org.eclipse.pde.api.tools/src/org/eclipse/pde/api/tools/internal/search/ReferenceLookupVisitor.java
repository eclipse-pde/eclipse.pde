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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * Resolves references from an API use scan in an alternate baseline to see if the
 * reference still exists in that baseline. Can be used to detect potential migration
 * issues. 
 */
public class ReferenceLookupVisitor extends UseScanVisitor {
	
	private IApiBaseline baseline; // baseline to resolve in
	private IComponentDescriptor targetComponent; // references are made to this component
	private IComponentDescriptor referencingComponent; // references are made from this component
	private IApiComponent currComponent; // corresponding component in baseline
	private IMemberDescriptor targetMember; // member a reference has been made to
	private IReferenceTypeDescriptor targetType; // the enclosing type the reference has been made to
	private IApiType currType; // corresponding type for current member
	
	
	// used to collate resolution errors - maps element descriptors to resolution errors (list)
	private Map errors = null;
	
	/**
	 * Creates a visitor to resolve references in the given baseline
	 */
	public ReferenceLookupVisitor(IApiBaseline base) {
		baseline = base;
	}
	
	/**
	 * Describes a resolution error
	 */
	class ResolutionError {
		IElementDescriptor referencedElement;
		String referencedComponent;
		String originComponent;
		String originMember = null;
		int originLine = -1;
		int refKind = -1;
		
		ResolutionError(String oComponent, String tComponent, IElementDescriptor tMember) {
			referencedElement = tMember;
			referencedComponent = tComponent;
			originComponent = oComponent;
		}
		
		ResolutionError(String oComponent, String tComponent, IElementDescriptor tMember, String fromMember, int line, int kind) {
			this(oComponent, tComponent, tMember);
			originMember = fromMember;
			originLine = line;
			refKind = kind;
		}		
		
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			switch (referencedElement.getElementType()) {
			case IElementDescriptor.COMPONENT:
				buffer.append(NLS.bind("Missing component: {0}", ((IComponentDescriptor)referencedElement).getId())); //$NON-NLS-1$
				break;
			case IElementDescriptor.TYPE:
				buffer.append(NLS.bind("Missing type: {0}", ((IReferenceTypeDescriptor)referencedElement).getQualifiedName())); //$NON-NLS-1$
				break;
			case IElementDescriptor.METHOD:
				buffer.append(NLS.bind("Missing method: {0}", referencedElement.toString())); //$NON-NLS-1$
				break;
			case IElementDescriptor.FIELD:
				buffer.append(NLS.bind("Missing field: {0}", referencedElement.toString())); //$NON-NLS-1$
				break;
			}
			if (originComponent != null) {
				buffer.append("\n\t"); //$NON-NLS-1$
				buffer.append(originComponent);
			}
			if (originMember != null) {
				buffer.append(": "); //$NON-NLS-1$
				buffer.append(originMember);
			}
			if (refKind > -1) {
				buffer.append(" ("); //$NON-NLS-1$
				buffer.append(Reference.getReferenceText(refKind));
				buffer.append(")"); //$NON-NLS-1$
			}
			if (originLine > -1) {
				buffer.append(" (line "); //$NON-NLS-1$
				buffer.append(originLine);
				buffer.append(")"); //$NON-NLS-1$
			}
			return buffer.toString();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor, java.lang.String)
	 */
	public boolean visitComponent(IComponentDescriptor target, String version) {
		errors = new HashMap();
		targetComponent = target;
		currComponent = baseline.getApiComponent(targetComponent.getId());
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor, java.lang.String)
	 */
	public boolean visitReferencingComponent(IComponentDescriptor component, String version) {
		referencingComponent = component;
		if (currComponent == null) {
			// error - missing component in baseline
			addError(targetComponent, new ResolutionError(referencingComponent.getId(), targetComponent.getId(), targetComponent));
			return false;
		}
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
	 */
	public boolean visitMember(IMemberDescriptor referencedMember) {
		targetMember = referencedMember;
		switch (targetMember.getElementType()) {
		case IElementDescriptor.TYPE:
			targetType = (IReferenceTypeDescriptor)targetMember;
			break;
		case IElementDescriptor.METHOD:
		case IElementDescriptor.FIELD:
			targetType = targetMember.getEnclosingType();
			break;
		}
		currType = null;
		try {
			IApiTypeRoot typeRoot = currComponent.findTypeRoot(targetType.getQualifiedName());
			if (typeRoot != null) {
				currType = typeRoot.getStructure();
			}
			return true;
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(int, java.lang.String, int)
	 */
	public void visitReference(int refKind, String fromMember, int lineNumber) {
		if (currType == null) {
			// ERROR: missing type (collate at type level instead of member)
			addError(targetType, new ResolutionError(referencingComponent.getId(), currComponent.getId(), targetMember, fromMember, lineNumber, refKind));
		} else {
			Reference ref = null;
			switch (targetMember.getElementType()) {
			case IElementDescriptor.TYPE:
				ref = Reference.typeReference(currType, targetType.getQualifiedName(), refKind);
				break;
			case IElementDescriptor.METHOD:
				ref = Reference.methodReference(currType, targetType.getQualifiedName(), targetMember.getName(), ((IMethodDescriptor)targetMember).getSignature(), refKind);
				break;
			case IElementDescriptor.FIELD:
				ref = Reference.fieldReference(currType, targetType.getQualifiedName(), targetMember.getName(), refKind);
				break;
			}
			try {
				ref.resolve();
				if (ref.getResolvedReference() == null) {
					// ERROR - failed to resolve
					addError(targetType, new ResolutionError(referencingComponent.getId(), currComponent.getId(), targetMember, fromMember, lineNumber, refKind));
				}
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
			}
		}
	}
	
	private void addError(IElementDescriptor element, ResolutionError error) {
		List list = (List) errors.get(element);
		if (list == null) {
			list = new ArrayList();
			errors.put(element, list);
		}
		list.add(error);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisit(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor, java.lang.String)
	 */
	public void endVisit(IComponentDescriptor target, String version) {
		// TODO: write report
//		if (currComponent == null) {
//			// missing component, rather than errors within a component
//			System.out.println("Missing component: " + targetComponent.getId() + ", referenced by:");
//			List list = (List) errors.get(targetComponent);
//			Iterator iterator2 = list.iterator();
//			while (iterator2.hasNext()) {
//				ResolutionError error = (ResolutionError) iterator2.next();
//				System.out.println("\t" + error.originComponent);
//			}
//		} else {
//			Iterator iterator = errors.entrySet().iterator();
//			boolean headerWritten = false;
//			while (iterator.hasNext()) {
//				Entry entry = (Entry) iterator.next();
//				List errors = (List) entry.getValue();
//				if (!headerWritten) {
//					System.out.println("Problems in: " + target.getId());
//				}
//				headerWritten = true;
//				Iterator iterator2 = errors.iterator();
//				while (iterator2.hasNext()) {
//					ResolutionError error = (ResolutionError) iterator2.next();
//					System.out.println("\t" + error.toString());
//				}
//			}
//		}
	}

}
