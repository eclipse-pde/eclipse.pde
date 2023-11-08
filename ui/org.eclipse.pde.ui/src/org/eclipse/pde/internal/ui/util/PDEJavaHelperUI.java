/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - bug 61185
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IBuffer;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.bindings.TriggerSequence;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.IContentProposalListener2;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.util.PDEJavaHelper;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeContentProposalListener;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeContentProposalProvider;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeFieldAssistDisposer;
import org.eclipse.pde.internal.ui.editor.contentassist.TypeProposalLabelProvider;
import org.eclipse.pde.internal.ui.editor.contentassist.display.JavaDocCommentReader;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeValue;
import org.eclipse.pde.internal.ui.editor.plugin.JavaAttributeWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

public class PDEJavaHelperUI {

	private static HashMap<String, HashMap<String, String>> fDocMap = new HashMap<>();

	public static String selectType(IResource resource, int scope) {
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		try {
			SelectionDialog dialog = JavaUI.createTypeDialog(PDEPlugin.getActiveWorkbenchShell(), PlatformUI.getWorkbench().getProgressService(), PDEJavaHelper.getSearchScope(project), scope, false, ""); //$NON-NLS-1$
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
		}
		return null;
	}

	public static String selectType(IResource resource, int scope, String filter, String superTypeName) {
		if (resource == null)
			return null;
		IProject project = resource.getProject();
		try {
			IJavaSearchScope searchScope = null;
			if (superTypeName != null && !superTypeName.equals("java.lang.Object")) { //$NON-NLS-1$
				IJavaProject javaProject = JavaCore.create(project);
				IType superType = javaProject.findType(superTypeName);
				if (superType != null)
					searchScope = SearchEngine.createStrictHierarchyScope(javaProject, superType, true, false, null);
			}
			if (searchScope == null)
				searchScope = PDEJavaHelper.getSearchScope(project);

			SelectionDialog dialog = JavaUI.createTypeDialog(PDEPlugin.getActiveWorkbenchShell(), PlatformUI.getWorkbench().getProgressService(), searchScope, scope, false, filter);
			dialog.setTitle(PDEUIMessages.ClassAttributeRow_dialogTitle);
			if (dialog.open() == Window.OK) {
				IType type = (IType) dialog.getResult()[0];
				return type.getFullyQualifiedName('$');
			}
		} catch (JavaModelException e) {
		}
		return null;
	}

	/**
	 * Open/Create a java class
	 *
	 * @param name fully qualified java classname
	 * @param project
	 * @param value for creation of the class
	 * @param createIfNoNature will create the class even if the project has no java nature
	 * @return null if the class exists or the name of the newly created class
	 */
	public static String createClass(String name, IProject project, JavaAttributeValue value, boolean createIfNoNature) {
		name = TextUtil.trimNonAlphaChars(name).replace('$', '.');
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IJavaProject javaProject = JavaCore.create(project);
				IJavaElement result = null;
				if (name.length() > 0)
					result = javaProject.findType(name);
				if (result != null)
					JavaUI.openInEditor(result);
				else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK)
						return wizard.getQualifiedNameWithArgs();
				}
			} else if (createIfNoNature) {
				IResource resource = project.findMember(IPath.fromOSString(name));
				if (resource != null && resource instanceof IFile) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					IDE.openEditor(page, (IFile) resource, true);
				} else {
					JavaAttributeWizard wizard = new JavaAttributeWizard(value);
					WizardDialog dialog = new WizardDialog(PDEPlugin.getActiveWorkbenchShell(), wizard);
					dialog.create();
					SWTUtil.setDialogSize(dialog, 400, 500);
					int dResult = dialog.open();
					if (dResult == Window.OK) {
						String newValue = wizard.getQualifiedName();
						name = newValue.replace('.', '/') + ".java"; //$NON-NLS-1$
						resource = project.findMember(IPath.fromOSString(name));
						if (resource != null && resource instanceof IFile) {
							IWorkbenchPage page = PDEPlugin.getActivePage();
							IDE.openEditor(page, (IFile) resource, true);
						}
						return newValue;
					}
				}
			}
		} catch (JavaModelException e) {
			// nothing
			Display.getCurrent().beep();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return null;
	}

	public static String getOSGIConstantJavaDoc(String constant, IJavaProject jp) {
		return getJavaDoc(constant, jp, "org.osgi.framework.Constants"); //$NON-NLS-1$
	}

	public static String getJavaDoc(String constant, IJavaProject jp, String className) {
		HashMap<String, String> map = fDocMap.get(className);
		if (map == null)
			fDocMap.put(className, map = new HashMap<>());
		String javaDoc = map.get(constant);

		if (javaDoc == null) {
			try {
				IType type = jp.findType(className);
				if (type != null) {
					char[] chars = constant.toCharArray();
					for (int i = 0; i < chars.length; i++)
						chars[i] = chars[i] == '-' ? '_' : Character.toUpperCase(chars[i]);
					IField field = type.getField(new String(chars));
					ISourceRange range = field.getJavadocRange();
					if (range == null)
						return null;
					IBuffer buff = type.getOpenable().getBuffer();
					JavaDocCommentReader reader = new JavaDocCommentReader(buff, range.getOffset(), range.getOffset() + range.getLength() - 1);
					String text = getString(reader);
					javaDoc = formatJavaDoc(text);
					map.put(constant, javaDoc);
				}
			} catch (JavaModelException e) {
			}
		}
		return javaDoc;
	}

	private static String formatJavaDoc(String text) {
		StringBuilder buffer = new StringBuilder();
		HTMLPrinter.insertPageProlog(buffer, 0, TextUtil.getJavaDocStyleSheerURL());
		buffer.append(text);
		HTMLPrinter.addPageEpilog(buffer);
		return buffer.toString();
	}

	/**
	 * Gets the reader content as a String
	 */
	private static String getString(Reader reader) {
		StringBuilder buf = new StringBuilder();
		char[] buffer = new char[1024];
		int count;
		try {
			while ((count = reader.read(buffer)) != -1)
				buf.append(buffer, 0, count);
		} catch (IOException e) {
			return null;
		}
		return buf.toString();
	}

	/**
	 * Disposer returned used to dispose of label provider and remove listeners
	 * Callers responsibility to call dispose method when underlying text
	 * widget is being disposed
	 * @param text
	 * @param project
	 */
	public static TypeFieldAssistDisposer addTypeFieldAssistToText(Text text, IProject project, int searchScope) {
		// Decorate the text widget with the light-bulb image denoting content
		// assist
		int bits = SWT.TOP | SWT.LEFT;
		ControlDecoration controlDecoration = new ControlDecoration(text, bits);
		// Configure text widget decoration
		// No margin
		controlDecoration.setMarginWidth(0);
		// Custom hover tip text
		String description = null;
		IBindingService bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		TriggerSequence[] activeBindings = bindingService.getActiveBindingsFor(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		if (activeBindings.length == 0)
			description = PDEUIMessages.PDEJavaHelper_msgContentAssistAvailable;
		else
			description = NLS.bind(PDEUIMessages.PDEJavaHelper_msgContentAssistAvailableWithKeyBinding, activeBindings[0].format());

		controlDecoration.setDescriptionText(description);

		// Custom hover properties
		controlDecoration.setShowHover(true);
		controlDecoration.setShowOnlyOnFocus(true);
		// Hover image to use
		FieldDecoration contentProposalImage = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_CONTENT_PROPOSAL);
		controlDecoration.setImage(contentProposalImage.getImage());

		// Create the proposal provider
		TypeContentProposalProvider proposalProvider = new TypeContentProposalProvider(project, searchScope);
		// Default text widget adapter for field assist
		TextContentAdapter textContentAdapter = new TextContentAdapter();
		// Set auto activation character to be a '.'
		char[] autoActivationChars = new char[] {TypeContentProposalProvider.F_DOT};
		// Create the adapter
		ContentAssistCommandAdapter adapter = new ContentAssistCommandAdapter(text, textContentAdapter, proposalProvider, IWorkbenchCommandConstants.EDIT_CONTENT_ASSIST, autoActivationChars);
		// Configure the adapter
		// Add label provider
		ILabelProvider labelProvider = new TypeProposalLabelProvider();
		adapter.setLabelProvider(labelProvider);
		// Replace text field contents with accepted proposals
		adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
		// Disable default filtering - custom filtering done
		adapter.setFilterStyle(ContentProposalAdapter.FILTER_NONE);
		// Add listeners required to reset state for custom filtering
		TypeContentProposalListener proposalListener = new TypeContentProposalListener();
		adapter.addContentProposalListener((IContentProposalListener) proposalListener);
		adapter.addContentProposalListener((IContentProposalListener2) proposalListener);

		return new TypeFieldAssistDisposer(adapter, proposalListener);
	}

}
