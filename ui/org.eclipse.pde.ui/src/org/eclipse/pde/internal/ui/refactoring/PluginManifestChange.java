/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.refactoring;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.core.plugin.IPluginParent;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ischema.IMetaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchema;
import org.eclipse.pde.internal.core.ischema.ISchemaAttribute;
import org.eclipse.pde.internal.core.ischema.ISchemaElement;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.ui.model.IDocumentAttribute;
import org.eclipse.pde.internal.ui.model.plugin.FragmentModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModel;
import org.eclipse.pde.internal.ui.model.plugin.PluginModelBase;
import org.eclipse.pde.internal.ui.model.plugin.PluginNode;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

public class PluginManifestChange {

	public static Change createChange(IFile file, IType type, String newName, IProgressMonitor monitor) 
			throws CoreException {
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		try {
			manager.connect(file.getFullPath(), monitor);
			ITextFileBuffer buffer = manager.getTextFileBuffer(file.getFullPath());
			
			IDocument document = buffer.getDocument();
			
			try {
				PluginModelBase model;
				if ("fragment.xml".equals(file.getName())) //$NON-NLS-1$
					model = new FragmentModel(document, false);
				else
					model = new PluginModel(document, false);

				model.load();
				if (!model.isLoaded())
					return null;
				
				MultiTextEdit multiEdit = new MultiTextEdit();
				
				if (model instanceof PluginModel) {
					PluginNode plugin = (PluginNode)((PluginModel)model).getPlugin();
					String className = plugin.getClassName();
					String shortName = type.getElementName();
					String oldName = type.getFullyQualifiedName('$');
					if (className != null && className.startsWith(oldName)) {
						IDocumentAttribute attr = plugin.getDocumentAttribute("class"); //$NON-NLS-1$
						multiEdit.addChild(new ReplaceEdit(attr.getValueOffset() + oldName.length() - shortName.length() , shortName.length(), newName));
					}					
				}
				
				SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
				IPluginExtension[] extensions = model.getPluginBase().getExtensions();
				for (int i = 0; i < extensions.length; i++) {
					ISchema schema = registry.getSchema(extensions[i].getPoint());
					if (schema != null)
						addExtensionAttributeEdit(schema, extensions[i], multiEdit, type, newName);
				}
				
				if (multiEdit.hasChildren()) {
					TextFileChange change = new TextFileChange("", file);
					change.setEdit(multiEdit);
					return change;
				}
			} catch (CoreException e) {
				return null;
			}	
			return null;
		} finally {
			manager.disconnect(file.getFullPath(), monitor);
		}	
	}
	
	private static void addExtensionAttributeEdit(ISchema schema, IPluginParent parent, MultiTextEdit multi, IType type, String newName) {
		IPluginObject[] children = parent.getChildren();
		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement)children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.JAVA) {
						String value = attr.getValue();
						String shortName = type.getElementName();
						String oldName = type.getFullyQualifiedName('$');
						if (value != null && value.startsWith(oldName)) {
							IDocumentAttribute docAttr = (IDocumentAttribute)attr;
							multi.addChild(new ReplaceEdit(docAttr.getValueOffset() + oldName.length() - shortName.length() , shortName.length(), newName));
						}
					}
				}
			}
			addExtensionAttributeEdit(schema, child, multi, type, newName);
		}
	}
	
}
