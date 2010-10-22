/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
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
import org.eclipse.pde.api.tools.internal.util.FilteredElements;
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
	private boolean skipped = false; // whether the target component was skipped based on scope settings
	private IMemberDescriptor targetMember; // member a reference has been made to
	private IReferenceTypeDescriptor targetType; // the enclosing type the reference has been made to
	private IApiType currType; // corresponding type for current member
	
	private List missingComponents = new ArrayList(); // list of missing component descriptors
	private List skippedComponents = new ArrayList(); // list of skipped component descriptors
	
	private String location; // path in file system to create report in
	
	private List unresolved = null; // list of reference descriptors (errors)
	
	private String analysisScope = null; // the bundles to analyze references from (search scope)
	private String targetScope = null; // the bundles to analyze references to (target scope)
	
	private FilteredElements excludedElements = null; //List of elements excluded from the scope
	private FilteredElements includedElements = null; //List of elements explicitly limiting the scope
	
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
		skipped = false;
		String id = targetComponent.getId();
		if (includedElements != null && !includedElements.isEmpty() &&
				!(includedElements.containsExactMatch(id) || includedElements.containsPartialMatch(id))) {
			skipped = true;
			return false;
		}
		if (excludedElements != null && (excludedElements.containsExactMatch(id) || excludedElements.containsPartialMatch(id))) {
			skipped = true;
			return false;
		}
		if (targetScope == null || id.matches(targetScope)) {
			// only analyze if it matches our scope
			currComponent = baseline.getApiComponent(id);
			return true;			
		}
		skipped = true;
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReferencingComponent(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public boolean visitReferencingComponent(IComponentDescriptor component) {
		referencingComponent = component;
		if (currComponent == null) {
			return false;
		}
		if (analysisScope == null || component.getId().matches(analysisScope)) {
			// only consider if in scope
			return true;
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitMember(org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor)
	 */
	public boolean visitMember(IMemberDescriptor referencedMember) {
		targetMember = referencedMember;
		switch (targetMember.getElementType()) {
			case IElementDescriptor.TYPE: {
				targetType = (IReferenceTypeDescriptor)targetMember;
				break;
			}
			case IElementDescriptor.METHOD:
			case IElementDescriptor.FIELD: {
				targetType = targetMember.getEnclosingType();
				break;
			}
		}
		currType = null;
		try {
			IApiTypeRoot typeRoot = null;
			IApiComponent[] comps = currComponent.getBaseline().resolvePackage(currComponent, targetType.getPackage().getName());
			for (int i = 0; i < comps.length; i++) {
				typeRoot = comps[i].findTypeRoot(targetType.getQualifiedName());
				if(typeRoot != null) {
					break;
				}
			}
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
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#visitReference(org.eclipse.pde.api.tools.internal.search.IReferenceDescriptor)
	 */
	public void visitReference(IReferenceDescriptor reference) {
		Reference ref = null;
		IApiMember resolved = null;
		int refKind = reference.getReferenceKind();
		int lineNumber = reference.getLineNumber();
		IMemberDescriptor origin = reference.getMember();
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
			addError(Factory.referenceDescriptor(
					referencingComponent, 
					origin, 
					lineNumber, 
					targetComponent, 
					targetMember, 
					refKind, 
					reference.getReferenceFlags(), 
					reference.getVisibility(),
					null));
		}
	}
	
	private void addError(IReferenceDescriptor error) {
		unresolved.add(error);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.UseScanVisitor#endVisit(org.eclipse.pde.api.tools.internal.provisional.descriptors.IComponentDescriptor)
	 */
	public void endVisitComponent(IComponentDescriptor target) {
		if (skipped) {
			skippedComponents.add(target);
		} else {
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
			addMissingComponents(missingComponents, SearchMessages.ReferenceLookupVisitor_0, doc, root);
			addMissingComponents(skippedComponents, SearchMessages.SkippedComponent_component_was_excluded, doc, root);
			writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), IApiCoreConstants.UTF_8));
			writer.write(Util.serializeDocument(doc));
			writer.flush();
		}
		catch(FileNotFoundException fnfe) {}
		catch(IOException ioe) {}
		catch(CoreException ce) {}
		finally {
			if(writer != null) {
				try {
					writer.close();
				} catch (IOException e) {}
			}
		}
	}
	
	private void addMissingComponents(List missing, String details, Document doc, Element root) {
		Iterator iter = missing.iterator();
		while (iter.hasNext()) {
			IComponentDescriptor component = (IComponentDescriptor)iter.next();
			Element comp = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
			comp.setAttribute(IApiXmlConstants.ATTR_ID, component.getId());
			comp.setAttribute(IApiXmlConstants.ATTR_VERSION, component.getVersion());
			comp.setAttribute(IApiXmlConstants.SKIPPED_DETAILS, details);
			root.appendChild(comp);
		}
	}
	
	/**
	 * Limits the scope of bundles to consider references from, as a regular expression.
	 * 
	 * @param regex regular expression or <code>null</code> if all
	 */
	public void setAnalysisScope(String regex) {
		analysisScope = regex;
	}
	
	/**
	 * Limits the set of bundles to consider analyzing references to, as a regular expression.
	 *  
	 * @param regex regular expression or <code>null</code> if all.
	 */
	public void setTargetScope(String regex) {
		targetScope = regex;
	}
	
	/**
	 * @param excludedElements the list of elements excluded from the scope
	 */
	public void setExcludedElements(FilteredElements excludedElements) {
		this.excludedElements = excludedElements;
	}
	
	/**
	 * @param includedElements Sets the List of elements explicitly limiting the scope
	 */
	public void setIncludedElements(FilteredElements includedElements) {
		this.includedElements = includedElements;
	}
		

}
