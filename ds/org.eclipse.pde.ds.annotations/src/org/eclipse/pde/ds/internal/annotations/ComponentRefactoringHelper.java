/*******************************************************************************
 * Copyright (c) 2015, 2017 Ecliptical Software Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ecliptical Software Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ds.internal.annotations;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.MoveArguments;
import org.eclipse.ltk.core.refactoring.participants.MoveParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringArguments;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RenameArguments;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.ltk.core.refactoring.participants.ResourceChangeChecker;
import org.eclipse.ltk.core.refactoring.resource.RenameResourceChange;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.core.text.IDocumentAttributeNode;
import org.eclipse.pde.internal.ds.core.IDSComponent;
import org.eclipse.pde.internal.ds.core.IDSConstants;
import org.eclipse.pde.internal.ds.core.text.DSModel;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

@SuppressWarnings("restriction")
public class ComponentRefactoringHelper {

	private static final Debug debug = Debug.getDebug("component-refactoring-helper"); //$NON-NLS-1$

	private final HashMap<Object, RefactoringArguments> elements = new HashMap<>();

	private Map<IType, IFile> modelFiles;

	private Map<IFile, String> componentNames;

	private Map<IFile, IFile> renames;

	private final RefactoringParticipant participant;

	public ComponentRefactoringHelper(RefactoringParticipant participant) {
		this.participant = participant;
	}

	public boolean initialize(Object element) {
		elements.put(element, getArguments());
		return true;
	}

	private RefactoringArguments getArguments() {
		if (participant instanceof RenameParticipant)
			return ((RenameParticipant) participant).getArguments();

		if (participant instanceof MoveParticipant)
			return ((MoveParticipant) participant).getArguments();

		return null;
	}

	public void addElement(Object element, RefactoringArguments arguments) {
		elements.put(element, arguments);
	}

	public RefactoringStatus checkConditions(IProgressMonitor monitor, CheckConditionsContext context) throws OperationCanceledException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ComponentRefactoringHelper_checkConditionsTaskLabel, elements.size());
		try {
			modelFiles = new HashMap<>(elements.size());
			componentNames = new HashMap<>(elements.size());
			renames = new HashMap<>(elements.size());
			HashMap<IJavaProject, ProjectState> states = new HashMap<>();
			HashSet<IJavaProject> unmanaged = new HashSet<>();

			ResourceChangeChecker checker = context.getChecker(ResourceChangeChecker.class);
			IResourceChangeDescriptionFactory deltaFactory = checker.getDeltaFactory();
			for (Map.Entry<Object, RefactoringArguments> entry : elements.entrySet()) {
				if (progress.isCanceled())
					throw new OperationCanceledException();

				progress.worked(1);

				RefactoringArguments args = entry.getValue();
				if (!getUpdateReferences(args))
					continue;

				IJavaElement element = (IJavaElement) entry.getKey();
				IJavaProject javaProject = element.getJavaProject();
				if (unmanaged.contains(javaProject))
					continue;

				ProjectState state = states.get(javaProject);
				if (state == null) {
					state = DSAnnotationCompilationParticipant.getState(javaProject);
				}

				if (state == null) {
					unmanaged.add(javaProject);
					continue;
				}

				states.put(javaProject, state);

				if (element.getElementType() == IJavaElement.TYPE)
					createRenames((IType) element, args, state, deltaFactory);
				else if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
					createRenames((IPackageFragment) element, args, state, deltaFactory);
			}

			return new RefactoringStatus();
		} catch (JavaModelException e) {
			// TODO double-check!
			return RefactoringStatus.create(e.getStatus());
		}
	}

	private boolean getUpdateReferences(RefactoringArguments args) {
		if (args instanceof RenameArguments)
			return ((RenameArguments) args).getUpdateReferences();

		if (args instanceof MoveArguments)
			return ((MoveArguments) args).getUpdateReferences();

		return false;
	}

	private void createRenames(IPackageFragment fragment, RefactoringArguments args, ProjectState state, IResourceChangeDescriptionFactory deltaFactory) throws JavaModelException {
		String compName = getComponentName(fragment, args);
		for (ICompilationUnit cu : fragment.getCompilationUnits()) {
			for (IType type : cu.getTypes()) {
				createRenames(type, type, compName.length() == 0 ? type.getElementName() : String.format("%s.%s", compName, type.getElementName()), args, state, deltaFactory); //$NON-NLS-1$
			}
		}
	}

	private void createRenames(IType type, RefactoringArguments args, ProjectState state, IResourceChangeDescriptionFactory deltaFactory) throws JavaModelException {
		createRenames(type, type, getComponentName(type, args), args, state, deltaFactory);
	}

	private void createRenames(IType type, IType rootType, String rootName, RefactoringArguments args, ProjectState state, IResourceChangeDescriptionFactory deltaFactory) throws JavaModelException {
		// check if this type is a component with explicit name
		if (ComponentPropertyTester.hasImplicitName(type)) {
			String modelPath = state.getModelFile(type.getFullyQualifiedName());
			if (modelPath != null) {
				IProject project = type.getJavaProject().getProject();
				IFile modelFile = PDEProject.getBundleRelativeFile(project, Path.fromPortableString(modelPath));
				if (modelFile.isAccessible()) {
					modelFiles.put(type, modelFile);
					deltaFactory.change(modelFile);

					String compName = String.format("%s%s", rootName, getTypeRelativeName(type, rootType)); //$NON-NLS-1$
					componentNames.put(modelFile, compName);

					// TODO centralize this?
					IPath newPath = new Path(state.getPath()).addTrailingSeparator()
							.append(compName).addFileExtension("xml"); //$NON-NLS-1$
					IFile newModelFile = PDEProject.getBundleRelativeFile(project, newPath);
					renames.put(modelFile, newModelFile);
					deltaFactory.move(modelFile, newModelFile.getFullPath());
				}
			}
		} else if (debug.isDebugging()) {
			debug.trace(String.format("Type %s does not have implicit component name.", type.getFullyQualifiedName())); //$NON-NLS-1$
		}

		// process any nested types
		for (IType child : type.getTypes()) {
			createRenames(child, rootType, rootName, args, state, deltaFactory);
		}
	}

	private String getComponentName(IJavaElement element, RefactoringArguments args) {
		return ((ComponentRefactoringParticipant) participant).getComponentNameRoot(element, args);
	}

	private String getTypeRelativeName(IType type, IType rootType) {
		ArrayList<String> segments = new ArrayList<>(2);
		while (type != null && !type.equals(rootType)) {
			segments.add(type.getElementName());
			type = type.getDeclaringType();
		}

		StringBuilder buf = new StringBuilder();
		for (ListIterator<String> i = segments.listIterator(segments.size()); i.hasPrevious();) {
			buf.append('$').append(i.previous());
		}

		return buf.toString();
	}

	public Change createChange(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		SubMonitor progress = SubMonitor.convert(monitor, Messages.ComponentRefactoringHelper_createChangeTaskLabel, elements.size());
		CompositeChange compositeChange = new CompositeChange(Messages.ComponentRefactoringHelper_topLevelChangeLabel);

		for (Map.Entry<Object, RefactoringArguments> entry : elements.entrySet()) {
			if (progress.isCanceled())
				throw new OperationCanceledException();

			progress.worked(1);

			RefactoringArguments args = entry.getValue();
			if (!getUpdateReferences(args))
				continue;

			IJavaElement element = (IJavaElement) entry.getKey();
			if (element.getElementType() == IJavaElement.PACKAGE_FRAGMENT)
				collectChanges((IPackageFragment) element, compositeChange);
			else if (element.getElementType() == IJavaElement.TYPE)
				collectChanges((IType) element, compositeChange);
		}

		return compositeChange;
	}

	private void collectChanges(IPackageFragment fragment, CompositeChange compositeChange) throws CoreException {
		for (ICompilationUnit cu : fragment.getCompilationUnits()) {
			for (IType type : cu.getTypes()) {
				collectChanges(type, compositeChange);
			}
		}
	}

	private void collectChanges(IType type, CompositeChange compositeChange) throws CoreException {
		Change change = createChange(type);
		if (change != null)
			compositeChange.add(change);

		for (IType child : type.getTypes()) {
			collectChanges(child, compositeChange);
		}
	}

	private Change createChange(IType type) throws CoreException {
		IFile modelFile = modelFiles.get(type);
		if (modelFile == null)
			return null;

		String componentName = componentNames.get(modelFile);
		if (componentName == null)
			return null;

		IFile newModelFile = renames.get(modelFile);
		if (newModelFile == null)
			return null;

		if (debug.isDebugging())
			debug.trace(String.format("Changing %s from %s to %s.", type.getFullyQualifiedName(), modelFile.getFullPath(), newModelFile.getFullPath())); //$NON-NLS-1$

		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		manager.connect(modelFile.getFullPath(), LocationKind.IFILE, null);
		DSModel model = null;
		IDocumentAttributeNode attrName, attrClass;
		try {
			ITextFileBuffer buf = manager.getTextFileBuffer(modelFile.getFullPath(), LocationKind.IFILE);
			if (buf == null)
				return null;

			IDocument doc = buf.getDocument();
			model = new DSModel(doc, false);
			model.setUnderlyingResource(modelFile);
			model.setCharset(Charset.forName(modelFile.getCharset()));
			model.load();

			IDSComponent component = model.getDSComponent();
			attrName = component.getDocumentAttribute(IDSConstants.ATTRIBUTE_COMPONENT_NAME);
			attrClass = component.getImplementation().getDocumentAttribute(IDSConstants.ATTRIBUTE_IMPLEMENTATION_CLASS);
		} finally {
			if (model != null)
				model.dispose();

			manager.disconnect(modelFile.getFullPath(), LocationKind.IFILE, null);
		}

		CompositeChange change = new CompositeChange(attrName.getAttributeValue());

		TextFileChange textChange = new TextFileChange(modelFile.getName(), modelFile);
		textChange.setTextType("xml"); //$NON-NLS-1$	// TODO verify!
		textChange.setEdit(new MultiTextEdit());
		textChange.addEdit(new ReplaceEdit(attrName.getValueOffset(), attrName.getValueLength(), componentName));
		textChange.addEdit(new ReplaceEdit(attrClass.getValueOffset(), attrClass.getValueLength(), componentName));
		change.add(textChange);

		RenameResourceChange renameChange = new RenameResourceChange(modelFile.getFullPath(), newModelFile.getName());
		change.add(renameChange);

		return change;
	}
}
