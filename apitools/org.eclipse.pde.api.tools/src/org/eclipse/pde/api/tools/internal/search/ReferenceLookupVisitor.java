/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.search;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
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
 * Resolves references from an API use scan in an alternate baseline to see if
 * the reference still exists in that baseline. Can be used to detect potential
 * migration issues.
 */
public class ReferenceLookupVisitor extends UseScanVisitor {

	private IApiBaseline baseline;
	private IComponentDescriptor targetComponent;
	private IComponentDescriptor referencingComponent;
	private IApiComponent currComponent;
	private boolean skipped = false;
	private IMemberDescriptor targetMember;
	private IReferenceTypeDescriptor targetType;
	private IApiType currType;
	private List<IComponentDescriptor> missingComponents = new ArrayList<>();
	private List<IComponentDescriptor> skippedComponents = new ArrayList<>();
	private String location;
	private List<IReferenceDescriptor> unresolved = null;
	private String analysisScope = null;
	private String targetScope = null;
	private FilteredElements excludedElements = null;
	private FilteredElements includedElements = null;

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

	@Override
	public boolean visitComponent(IComponentDescriptor target) {
		unresolved = new ArrayList<>();
		targetComponent = target;
		skipped = false;
		String id = targetComponent.getId();
		if (includedElements != null && !includedElements.isEmpty() && !(includedElements.containsExactMatch(id) || includedElements.containsPartialMatch(id))) {
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

	@Override
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

	@Override
	public boolean visitMember(IMemberDescriptor referencedMember) {
		targetMember = referencedMember;
		switch (targetMember.getElementType()) {
			case IElementDescriptor.TYPE: {
				targetType = (IReferenceTypeDescriptor) targetMember;
				break;
			}
			case IElementDescriptor.METHOD:
			case IElementDescriptor.FIELD: {
				targetType = targetMember.getEnclosingType();
				break;
			}
			default:
				break;
		}
		currType = null;
		try {
			IApiTypeRoot typeRoot = null;
			IApiComponent[] comps = currComponent.getBaseline().resolvePackage(currComponent, targetType.getPackage().getName());
			for (IApiComponent comp : comps) {
				typeRoot = comp.findTypeRoot(targetType.getQualifiedName());
				if (typeRoot != null) {
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

	@Override
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
					ref = Reference.methodReference(currType, targetType.getQualifiedName(), targetMember.getName(), ((IMethodDescriptor) targetMember).getSignature(), refKind);
					break;
				case IElementDescriptor.FIELD:
					ref = Reference.fieldReference(currType, targetType.getQualifiedName(), targetMember.getName(), refKind);
					break;
				default:
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
			addError(Factory.referenceDescriptor(referencingComponent, origin, lineNumber, targetComponent, targetMember, refKind, reference.getReferenceFlags(), reference.getVisibility(), null));
		}
	}

	private void addError(IReferenceDescriptor error) {
		unresolved.add(error);
	}

	@Override
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
					writer.writeReferences(unresolved.toArray(new IReferenceDescriptor[unresolved.size()]));
				}
			}
		}
	}

	@Override
	public void endVisitScan() {
		File rootfile = new File(location);
		File file = new File(rootfile, "not_searched.xml"); //$NON-NLS-1$
		try {
			// generate missing bundles information
			if (!rootfile.exists()) {
				rootfile.mkdirs();
			}
			if (!file.exists()) {
				file.createNewFile();
			}
			Document doc = Util.newDocument();
			Element root = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENTS);
			doc.appendChild(root);
			addMissingComponents(missingComponents, SearchMessages.ReferenceLookupVisitor_0, doc, root);
			addMissingComponents(skippedComponents, SearchMessages.SkippedComponent_component_was_excluded, doc, root);
			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));) {
				writer.write(Util.serializeDocument(doc));
				writer.flush();
			}
		} catch (IOException | CoreException e) {
			ApiPlugin.log("Failed to report missing bundles into " + file, e); //$NON-NLS-1$
		}
	}

	private void addMissingComponents(List<IComponentDescriptor> missing, String details, Document doc, Element root) {
		for (IComponentDescriptor component : missing) {
			Element comp = doc.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
			comp.setAttribute(IApiXmlConstants.ATTR_ID, component.getId());
			comp.setAttribute(IApiXmlConstants.ATTR_VERSION, component.getVersion());
			comp.setAttribute(IApiXmlConstants.SKIPPED_DETAILS, details);
			root.appendChild(comp);
		}
	}

	/**
	 * Limits the scope of bundles to consider references from, as a regular
	 * expression.
	 *
	 * @param regex regular expression or <code>null</code> if all
	 */
	public void setAnalysisScope(String regex) {
		analysisScope = regex;
	}

	/**
	 * Limits the set of bundles to consider analyzing references to, as a
	 * regular expression.
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
	 * @param includedElements Sets the List of elements explicitly limiting the
	 *            scope
	 */
	public void setIncludedElements(FilteredElements includedElements) {
		this.includedElements = includedElements;
	}

}
