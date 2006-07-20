/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.contentassist;

import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.IModel;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaComplexType;
import org.eclipse.pde.internal.core.ischema.ISchemaCompositor;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.ischema.ISchemaObject;
import org.eclipse.pde.internal.core.ischema.ISchemaRestriction;
import org.eclipse.pde.internal.core.ischema.ISchemaSimpleType;
import org.eclipse.pde.internal.core.ischema.ISchemaType;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.XMLUtil;

/**
 * XMLInsertionComputer
 *
 */
public class XMLInsertionComputer {


	/**
	 * @param sElement
	 * @param pElement
	 */
	public static void computeInsertion(ISchemaElement sElement,
			IPluginParent pElement) {
		HashSet visited = new HashSet();
		visited.add(sElement.getName());
		computeInsertionType(sElement, pElement, visited);
	}
	
	/**
	 * @param sElement
	 * @param pElement
	 * @param visited
	 */
	protected static void computeInsertionType(ISchemaElement sElement,
			IPluginParent pElement, HashSet visited) {
		
		if ((sElement == null) ||
				(pElement == null)) {
			// If there is no corresponding schema information or plug-in 
			// model, then there is nothing to augment
			return;
		} else if (sElement.getType() instanceof ISchemaSimpleType) {
			// For simple types, insert a comment informing the user to
			// add element content text
			try {
				if (pElement instanceof IPluginElement)
					((IPluginElement)pElement).setText(NLS.bind(
							PDEUIMessages.XMLCompletionProposal_InfoElement,
							pElement.getName()));
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
			return;
		} else if (sElement.getType() instanceof ISchemaComplexType) {
			// Note:  Mixed content types do not affect auto-generation
			// Note:  Deprecated elements do not affect auto-generation
			// Insert element attributes
			computeInsertionAllAttributes(pElement, sElement);
			// Get this element's compositor
			ISchemaCompositor compositor = ((ISchemaComplexType)sElement.getType()).getCompositor();
			// Process the compositor
			computeInsertionSequence(compositor, pElement, visited);
		} else {
			// Unknown element type
			return;
		}
	}

	/**
	 * @param pElement
	 * @param visited
	 * @param schemaObject
	 */
	protected static void computeInsertionObject(IPluginParent pElement, HashSet visited, ISchemaObject schemaObject) {
		if (schemaObject instanceof ISchemaElement) {
			ISchemaElement schemaElement = (ISchemaElement) schemaObject;
			computeInsertionElement(pElement, visited, schemaElement);
		} else if (schemaObject instanceof ISchemaCompositor) {
			ISchemaCompositor sCompositor = (ISchemaCompositor) schemaObject;
			computeInsertionSequence(sCompositor, pElement, visited);
		} else {
			// Unknown schema object
		}
	}

	/**
	 * @param pElement
	 * @param compositor
	 */
	protected static boolean computeInsertionCompositor(IPluginParent pElement, ISchemaCompositor compositor) {
		if (compositor == null) {
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.CHOICE) {
			// Too presumption to choose for the user
			// Avoid processing and generate a comment to inform the user that
			// they need to update this element accordingly
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.ALL) {
			// Not supported by PDE - should never get here
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.GROUP) {
			// Not supported by PDE - should never get here
			return false;
		} else if (compositor.getKind() == ISchemaCompositor.SEQUENCE) {
			return true;
		} else {
			// Unknown compositor
			return false;
		}
	}

	/**
	 * @param pElement
	 * @param visited
	 * @param schemaElement
	 */
	protected static void computeInsertionElement(IPluginParent pElement, HashSet visited, ISchemaElement schemaElement) {
		try {
			for (int j = 0; j < schemaElement.getMinOccurs(); j++) {
				// Update Model
				IPluginElement childElement = null;
				childElement = 
					pElement.getModel().getFactory().createElement(pElement);
				childElement.setName(schemaElement.getName());
				pElement.add(childElement);
				// Track visited
				HashSet newSet = (HashSet) visited.clone();
				if (newSet.add(schemaElement.getName())) {
					computeInsertionType(schemaElement, childElement, newSet);
				} else {
					childElement.setText(PDEUIMessages.XMLCompletionProposal_ErrorCycle);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	/**
	 * @param pElement
	 * @param type
	 * @param attributes
	 */
	protected static void computeInsertionAllAttributes(IPluginParent pElement, ISchemaElement sElement) {
		// Has to be a complex type if there are attributes
		ISchemaComplexType type = (ISchemaComplexType)sElement.getType();
		// Get the underlying project
		IResource resource = pElement.getModel().getUnderlyingResource();
		IProject project = null;
		if (resource != null)
			project = resource.getProject();
		// Get all the attributes
		ISchemaAttribute[] attributes = type.getAttributes();
		// Generate a unique number for IDs
		int counter = XMLUtil.getCounterValue(sElement);
		// Process all attributes
		for (int i = 0; i < type.getAttributeCount(); i++) {
			ISchemaAttribute attribute = attributes[i];
			// Note:  If an attribute is deprecated, it does not affect
			// auto-generation.
			try {
				if (attribute.getUse() == ISchemaAttribute.REQUIRED || 
						attribute.getUse() == ISchemaAttribute.DEFAULT) {
					String value = generateAttributeValue(project, counter, attribute);
					// Update Model
					setAttribute(pElement, attribute.getName(), value);
				}
				// Ignore optional attributes
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
	}

	/**
	 * @param project
	 * @param counter
	 * @param attribute
	 * @return
	 */
	protected static String generateAttributeValue(IProject project, int counter, ISchemaAttribute attribute) {
		String value = ""; //$NON-NLS-1$
		ISchemaRestriction restriction = 
			attribute.getType().getRestriction();

		if (attribute.getKind() == IMetaAttribute.JAVA &&
			project != null) {
			value = XMLUtil.createDefaultClassName(project,
					attribute, counter);
		} else if ((attribute.getUse() == ISchemaAttribute.DEFAULT) &&
					(attribute.getValue() != null)) {
			value = attribute.getValue().toString();
		} else if (restriction != null) {
			// Check for enumeration restrictions, if there is one, 
			// just pick the first enumerated value
			value = restriction.getChildren()[0].toString();
		} else if (project != null) {
			// Cases:
			// IMetaAttribute.STRING
			// IMetaAttribute.RESOURCE
			value = XMLUtil.createDefaultName(project,
					attribute, counter);
		}
		return value;
	}
	
	public static String generateAttributeValue(ISchemaAttribute attribute, IBaseModel baseModel) {
		if (baseModel instanceof IModel) {
			IResource resource = ((IModel)baseModel).getUnderlyingResource();
			if (resource != null) {
				int counter = 1;
				if (attribute.getParent() instanceof ISchemaElement) {
					ISchemaElement sElement = (ISchemaElement)attribute.getParent();
					// Generate a unique number for IDs
					counter = XMLUtil.getCounterValue(sElement);
				}					
				return generateAttributeValue(resource.getProject(), counter, attribute);
			}
		}
		return " "; //$NON-NLS-1$
	}
	
	/**
	 * @param compositor
	 * @param pElement
	 * @param visited
	 */
	protected static void computeInsertionSequence(ISchemaCompositor compositor,
			IPluginParent pElement, HashSet visited) {
		if (compositor == null)
			return;
		// Process the compositor the minimum number of times
		for (int k = 0; k < compositor.getMinOccurs(); k++) {
			// Only continue processing if the compositor is a sequence
			if (computeInsertionCompositor(pElement, compositor) == false)
				continue;
			// We have a sequence
			ISchemaObject[] schemaObject = compositor.getChildren();
			// Process the compositors children
			for (int i = 0; i < compositor.getChildCount(); i++) {
				computeInsertionObject(pElement, visited, schemaObject[i]);
			}
		}		
	}
	
	/**
	 * @param parent
	 * @param attName
	 * @param attValue
	 * @throws CoreException
	 */
	
	protected static void setAttribute(IPluginParent parent, String attName, String attValue) throws CoreException {
		if (parent instanceof IPluginElement) {
			((IPluginElement)parent).setAttribute(attName, attValue);
		} else if (parent instanceof IPluginExtension) {
			IPluginExtension pe = (IPluginExtension)parent;
			if (attName.equals(IIdentifiable.P_ID)) {
				String currValue = pe.getId();
				if (currValue == null || currValue.length() == 0)
					pe.setId(attValue);
			} else if (attName.equals(IPluginObject.P_NAME)) {
				String currValue = pe.getName();
				if (currValue == null || currValue.length() == 0)
					pe.setName(attName);
			} else if (attName.equals(IPluginExtension.P_POINT)) {
				String currValue = pe.getPoint();
				if (currValue == null || currValue.length() == 0)
					pe.setPoint(attValue);
			}
		}
	}

	// TODO:  MP:  REVISIT:  Functionality could be done implicitly in above
	// helper methods and remembered?
	
	public static boolean hasOptionalAttributes(ISchemaElement ele) {
		ISchemaAttribute[] attrs = ele.getAttributes();
		for (int i = 0; i < attrs.length; i++)
			if (attrs[i].getUse() == ISchemaAttribute.OPTIONAL || 
					attrs[i].getUse() == ISchemaAttribute.DEFAULT)
				return true;
		return false;
	}

	// TODO:  MP:  REVISIT:  Functionality could be done implicitly in above
	// helper methods and remembered?
	
	public static boolean hasOptionalChildren(ISchemaObject obj, boolean onChild, HashSet set) {
		if (obj == null || set.contains(obj))
			return false;
		set.add(obj);
		if (obj instanceof ISchemaElement) {
			if (onChild 
					&& ((ISchemaElement)obj).getMinOccurs() == 0
					&& ((ISchemaElement)obj).getMaxOccurs() > 0)
				return true;
			ISchemaType type = ((ISchemaElement) obj).getType();
			if (type instanceof ISchemaComplexType)
				return hasOptionalChildren(((ISchemaComplexType)type).getCompositor(), true, set);
		} else if (obj instanceof ISchemaCompositor) {
			ISchemaObject[] children = ((ISchemaCompositor)obj).getChildren();
			if (children != null)
				for (int i = 0; i < children.length; i++)
					if (hasOptionalChildren(children[i], true, set))
						return true;
		}
		return false;
	}	
	
}
