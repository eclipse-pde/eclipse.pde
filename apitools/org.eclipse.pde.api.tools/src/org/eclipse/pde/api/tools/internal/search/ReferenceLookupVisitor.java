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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	private List missingComponents = new ArrayList(); // list of missing component descriptors
	
	private String location; // path in file system to create report in
	
	private List unresolved = null; // list of reference descriptors (errors)
	
	/**
	 * Creates a visitor to resolve references in the given baseline
	 * 
	 * @param base baseline
	 * @param location to create XML report
	 */
	public ReferenceLookupVisitor(IApiBaseline base, String xmlLocation) {
		baseline = base;
		location = xmlLocation;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public boolean visitComponent(IComponentDescriptor target) {
		unresolved = new ArrayList();
		targetComponent = target;
		currComponent = baseline.getApiComponent(targetComponent.getId());
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public boolean visitReferencingComponent(IComponentDescriptor component) {
		referencingComponent = component;
		if (currComponent == null) {
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
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(int, java.lang.String, int, int)
	 */
	public void visitReference(int refKind, IMemberDescriptor origin, int lineNumber, int visibility) {
		Reference ref = null;
		IApiMember resolved = null;
		if (currType != null) {
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
		}
		if (ref != null) {
			try {
				ref.resolve();
				resolved = ref.getResolvedReference();
			} catch (CoreException e) {
				ApiPlugin.log(e.getStatus());
			}
		}
		if (resolved == null) {
			// ERROR - failed to resolve
			addError(new ReferenceDescriptor(referencingComponent, origin, lineNumber, targetComponent, targetMember, refKind, visibility));
		}
	}
	
	private void addError(IReferenceDescriptor error) {
		unresolved.add(error);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisit(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public void endVisit(IComponentDescriptor target) {
		if (currComponent == null) {
			missingComponents.add(target);
		} else {
			if (!unresolved.isEmpty()) {
				XmlReferenceDescriptorWriter writer = new XmlReferenceDescriptorWriter(location);
				writer.setAlternate((IComponentDescriptor) currComponent.getHandle());
				writer.writeReferences((IReferenceDescriptor[]) unresolved.toArray(new IReferenceDescriptor[unresolved.size()]));
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisitScan()
	 */
	public void endVisitScan() {
		BufferedWriter writer = null;
		try {
			// generate missing bundles information
			File rootfile = new File(location);
			if(!rootfile.exists()) {
				rootfile.mkdirs();
			}
			File file = new File(rootfile, "not_searched.xml"); //$NON-NLS-1$
			if(!file.exists()) {
				file.createNewFile();
			}
			Document doc = Util.newDocument();
			Element root = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENTS);
			doc.appendChild(root);
			Element comp = null;
			IComponentDescriptor component = null;
			Iterator iter = missingComponents.iterator();
			while (iter.hasNext()) {
				component = (IComponentDescriptor)iter.next();
				comp = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
				comp.setAttribute(IApiXmlConstants.ATTR_ID, component.getId());
				comp.setAttribute(IApiXmlConstants.ATTR_VERSION, component.getVersion());
				comp.setAttribute(IApiXmlConstants.SKIPPED_DETAILS, SearchMessages.ReferenceLookupVisitor_0);
				root.appendChild(comp);
			}
			writer = new BufferedWriter(new FileWriter(file));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		}
		catch(FileNotFoundException fnfe) {}
		catch(IOException ioe) {}
		catch(CoreException ce) {}
		finally {
			try {
				if(writer != null) {
					writer.close();
				}
			} 
			catch (IOException e) {}
		}
	}

}
