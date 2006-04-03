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
package org.eclipse.pde.internal.ui.correction;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.plugin.PluginAttribute;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;

public class CreateClassXMLResolution extends AbstractXMLMarkerResolution {

	public CreateClassXMLResolution(int resolutionType, IMarker marker) {
		super(resolutionType, marker);
	}

	
	// create class code copied from org.eclipse.pde.internal.ui.editor.plugin.rows.ClassAttributeRow
	protected void createChange(IPluginModelBase model) {
		Object object = findNode(model);
		if (!(object instanceof PluginAttribute))
			return;
		
		PluginAttribute attr = (PluginAttribute)object;
		String name = attr.getValue();
		name = trimNonAlphaChars(name).replace('$', '.');
		IProject project = model.getUnderlyingResource().getProject();
		
		JavaAttributeValue value = new JavaAttributeValue(project, model, getAttribute(attr), name);
		name = createClass(name, model, value);
		if (!name.equals(attr.getValue())) 
			attr.getEnclosingElement().setXMLAttribute(attr.getName(), name);
	}

	private ISchemaAttribute getAttribute(PluginAttribute attr) {
		SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
		IDocumentNode element = attr.getEnclosingElement();
		IPluginExtension extension = null;
		while (element.getParentNode() != null) {
			if (element instanceof IPluginExtension) {
				extension = (IPluginExtension)element;
				break;
			}
			element = element.getParentNode();
		}
		if (extension == null)
			return null;
		
		ISchema schema = registry.getSchema(extension.getPoint());
		ISchemaElement schemaElement = schema.findElement(attr.getEnclosingElement().getXMLTagName());
		if (schemaElement == null)
			return null;
		return schemaElement.getAttribute(attr.getName());
	}

	public String getLabel() {
		return NLS.bind(PDEUIMessages.CreateClassXMLResolution_label, getNameOfNode());
	}
}
