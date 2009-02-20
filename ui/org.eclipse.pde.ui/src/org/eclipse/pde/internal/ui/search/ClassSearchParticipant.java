/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Benjamin Cabe <benjamin.cabe@anyware-technologies.com> - bug 261404, 262353, 264176
 *******************************************************************************/
package org.eclipse.pde.internal.ui.search;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.ui.search.*;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.ibundle.*;
import org.eclipse.pde.internal.core.ischema.*;
import org.eclipse.pde.internal.core.schema.SchemaRegistry;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.core.text.IEditingModel;
import org.eclipse.pde.internal.core.text.plugin.PluginElementNode;
import org.eclipse.pde.internal.core.util.PatternConstructor;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.ModelModification;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.search.ui.text.Match;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

public class ClassSearchParticipant implements IQueryParticipant {

	private static final int H_IMP = 0;
	private static final int H_EXP = 1;
	private static final int H_BUNACT = 2;
	private static final int H_PLUGCLASS = 3;
	private static final int H_TOTAL = 4;
	private static final String[] SEARCH_HEADERS = new String[H_TOTAL];
	static {
		SEARCH_HEADERS[H_IMP] = Constants.IMPORT_PACKAGE;
		SEARCH_HEADERS[H_EXP] = Constants.EXPORT_PACKAGE;
		SEARCH_HEADERS[H_BUNACT] = Constants.BUNDLE_ACTIVATOR;
		SEARCH_HEADERS[H_PLUGCLASS] = ICoreConstants.PLUGIN_CLASS;
	}
	// the following are from JavaSearchPage (radio button indexes)
	private static final int S_LIMIT_REF = 2;
	private static final int S_LIMIT_ALL = 3;
	private static final int S_FOR_TYPES = 0;
	private static final int S_FOR_PACKAGES = 2;

	private ISearchRequestor fSearchRequestor;
	private Pattern fSearchPattern;
	private int fSearchFor = -1; // set since S_FOR_TYPES = 0;

	public ClassSearchParticipant() {
	}

	public void search(ISearchRequestor requestor, QuerySpecification querySpecification, IProgressMonitor monitor) throws CoreException {

		if (querySpecification.getLimitTo() != S_LIMIT_REF && querySpecification.getLimitTo() != S_LIMIT_ALL)
			return;

		String search;
		if (querySpecification instanceof ElementQuerySpecification) {
			IJavaElement element = ((ElementQuerySpecification) querySpecification).getElement();
			if (element instanceof IType)
				search = ((IType) element).getFullyQualifiedName('.');
			else
				search = element.getElementName();
			int type = element.getElementType();
			if (type == IJavaElement.TYPE)
				fSearchFor = S_FOR_TYPES;
			else if (type == IJavaElement.PACKAGE_FRAGMENT || type == IJavaElement.PACKAGE_FRAGMENT_ROOT)
				fSearchFor = S_FOR_PACKAGES;
		} else {
			fSearchFor = ((PatternQuerySpecification) querySpecification).getSearchFor();
			search = ((PatternQuerySpecification) querySpecification).getPattern();
		}
		if (fSearchFor != S_FOR_TYPES && fSearchFor != S_FOR_PACKAGES)
			return;
		fSearchPattern = PatternConstructor.createPattern(search, true);
		fSearchRequestor = requestor;

		IPath[] enclosingPaths = querySpecification.getScope().enclosingProjectsAndJars();
		IPluginModelBase[] pluginModels = PluginRegistry.getWorkspaceModels();
		monitor.beginTask(PDEUIMessages.ClassSearchParticipant_taskMessage, pluginModels.length);
		for (int i = 0; i < pluginModels.length; i++) {
			IProject project = pluginModels[i].getUnderlyingResource().getProject();
			if (!monitor.isCanceled() && encloses(enclosingPaths, project.getFullPath()))
				searchProject(project, monitor);
		}
	}

	private boolean encloses(IPath[] paths, IPath path) {
		for (int i = 0; i < paths.length; i++)
			if (paths[i].equals(path))
				return true;
		return false;
	}

	private void searchProject(IProject project, IProgressMonitor monitor) throws CoreException {
		ModelModification mod = new ModelModification(project) {
			protected void modifyModel(IBaseModel model, IProgressMonitor monitor) throws CoreException {
				if (model instanceof IBundlePluginModelBase) {
					IBundleModel bmodel = ((IBundlePluginModelBase) model).getBundleModel();
					inspectBundle(bmodel.getBundle());
					model = ((IBundlePluginModelBase) model).getExtensionsModel();
				}
				if (model instanceof IPluginModelBase) {
					IFile file = (IFile) ((IPluginModelBase) model).getUnderlyingResource();
					SchemaRegistry registry = PDECore.getDefault().getSchemaRegistry();
					IPluginBase pbase = ((IPluginModelBase) model).getPluginBase();
					IPluginExtension[] extensions = pbase.getExtensions();
					for (int j = 0; j < extensions.length; j++) {
						ISchema schema = registry.getSchema(extensions[j].getPoint());
						if (schema != null && !monitor.isCanceled())
							inspectExtension(schema, extensions[j], file);
					}
				}
			}
		};
		PDEModelUtility.modifyModel(mod, monitor);
	}

	private void inspectExtension(ISchema schema, IPluginParent parent, IFile file) {
		IPluginObject[] children = parent.getChildren();

		if (parent instanceof PluginElementNode && parent != null && parent.getParent() instanceof PluginElementNode) {
			// check if this node corresponds to a Java type attribute which would have been defined has an element
			PluginElementNode node = (PluginElementNode) parent;
			PluginElementNode parentNode = (PluginElementNode) parent.getParent();
			ISchemaElement schemaElement = schema.findElement(parentNode.getName());
			if (schemaElement != null) {
				ISchemaAttribute attInfo = schemaElement.getAttribute(node.getName());
				if (attInfo != null && attInfo.getKind() == IMetaAttribute.JAVA)
					checkMatch(node.getAttribute("class"), file); //$NON-NLS-1$
			}
		}

		for (int i = 0; i < children.length; i++) {
			IPluginElement child = (IPluginElement) children[i];
			ISchemaElement schemaElement = schema.findElement(child.getName());
			if (schemaElement != null) {
				IPluginAttribute[] attributes = child.getAttributes();
				for (int j = 0; j < attributes.length; j++) {
					IPluginAttribute attr = attributes[j];
					ISchemaAttribute attInfo = schemaElement.getAttribute(attr.getName());
					if (attInfo != null && attInfo.getKind() == IMetaAttribute.JAVA && attr instanceof IDocumentAttributeNode)
						checkMatch(attr, file);
				}
			}
			inspectExtension(schema, child, file);
		}
	}

	private void checkMatch(IPluginAttribute attr, IFile file) {
		String value = null;
		Matcher matcher = null;
		if (fSearchFor == S_FOR_TYPES) {
			value = removeInitializationData(attr.getValue()).replaceAll("\\$", "."); //$NON-NLS-1$ //$NON-NLS-2$
			matcher = getMatcher(value);
		}
		if (value == null || (matcher != null && !matcher.matches())) {
			value = removeInitializationData(getProperValue(attr.getValue())).replaceAll("\\$", "."); //$NON-NLS-1$ //$NON-NLS-2$
			matcher = getMatcher(value);
		}
		if (matcher.matches()) {
			String group = matcher.group(0);
			int offset = ((IDocumentAttributeNode) attr).getValueOffset() + value.indexOf(group);
			int attOffset = attr.getValue().indexOf(value);
			if (attOffset != -1)
				offset += attOffset;
			int length = group.length();
			fSearchRequestor.reportMatch(new Match(file, Match.UNIT_CHARACTER, offset, length));
		}
	}

	private String removeInitializationData(String attrValue) {
		int i = attrValue.indexOf(':');
		if (i != -1)
			return attrValue.substring(0, i).trim();
		return attrValue;
	}

	private void inspectBundle(IBundle bundle) {
		for (int i = 0; i < H_TOTAL; i++) {
			if (fSearchFor == S_FOR_TYPES && (i == H_IMP || i == H_EXP))
				continue;
			IManifestHeader header = bundle.getManifestHeader(SEARCH_HEADERS[i]);
			if (header != null) {
				try {
					ManifestElement[] elements = ManifestElement.parseHeader(header.getName(), header.getValue());
					if (elements == null)
						continue;
					for (int j = 0; j < elements.length; j++) {
						String value = null;
						Matcher matcher = null;
						if (fSearchFor == S_FOR_TYPES) {
							value = elements[j].getValue();
							matcher = getMatcher(value);
						}
						if (value == null || (matcher != null && !matcher.matches())) {
							value = getProperValue(elements[j].getValue());
							matcher = getMatcher(value);
						}
						if (matcher.matches()) {
							String group = matcher.group(0);
							int[] offlen;
							try {
								offlen = getOffsetOfElement(bundle, header, group);
							} catch (CoreException e) {
								offlen = new int[] {header.getOffset(), header.getLength()};
							}
							fSearchRequestor.reportMatch(new Match(bundle.getModel().getUnderlyingResource(), Match.UNIT_CHARACTER, offlen[0], offlen[1]));
							break; // only one package will be listed per header
						}
					}
				} catch (BundleException e) {
				}
			}
		}
	}

	private Matcher getMatcher(String value) {
		return fSearchPattern.matcher(value.subSequence(0, value.length()));
	}

	private int[] getOffsetOfElement(IBundle bundle, IManifestHeader header, String value) throws CoreException {
		int[] offlen = new int[] {0, 0};
		IBundleModel model = bundle.getModel();
		if (model instanceof IEditingModel) {
			IDocument pDoc = ((IEditingModel) model).getDocument();
			int headerOffset = header.getOffset() + header.getName().length();
			try {
				String headerString = pDoc.get(headerOffset, header.getLength() - header.getName().length());
				int internalOffset = headerString.indexOf(value);
				if (internalOffset != -1)
					offlen[0] = headerOffset + internalOffset;
				else
					offlen[0] = headerOffset + header.getName().length() + header.getValue().indexOf(value);
				offlen[1] = value.length();
			} catch (BadLocationException e) {
			}
		}
		return offlen;
	}

	private String getProperValue(String value) {
		return fSearchFor == S_FOR_TYPES ? extractType(value) : extractPackage(value);
	}

	private String extractType(String value) {
		int index = value.lastIndexOf("."); //$NON-NLS-1$
		if (index == -1 || index == value.length() - 1)
			return value;
		return value.substring(index + 1);
	}

	private String extractPackage(String value) {
		int index = value.lastIndexOf("."); //$NON-NLS-1$
		if (index == -1 || index == value.length() - 1)
			return value;
		char afterPeriod = value.charAt(index + 1);
		if (afterPeriod >= 'A' && afterPeriod <= 'Z')
			return value.substring(0, index);
		return value;
	}

	public int estimateTicks(QuerySpecification specification) {
		return 100;
	}

	public IMatchPresentation getUIParticipant() {
		return null;
	}
}
