/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * @since 1.1
 */
public abstract class AbstractProblemDetector implements IApiProblemDetector {

	/**
	 * List of potential {@link IReference} problems
	 */
	private List fPotentialProblems = new LinkedList();
	
	/**
	 * Retains the reference for further analysis.
	 *  
	 * @param reference reference
	 */
	protected void retainReference(IReference reference) {
		fPotentialProblems.add(reference);
	}

	/**
	 * Return the list of retained references.
	 * 
	 * @return references
	 */
	protected List getRetainedReferences() {
		return fPotentialProblems;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector#considerReference(org.eclipse.pde.api.tools.internal.provisional.builder.IReference)
	 */
	public boolean considerReference(IReference reference) {
		return reference != null && (reference.getReferenceKind() & getReferenceKinds()) > 0;
	}
	
	/**
	 * Creates a problem for a specific reference in the workspace
	 * 
	 * @param reference reference
	 * @param associated java project (with reference source location)
	 * @return problem or <code>null</code> if none
	 * @exception CoreException if something goes wrong
	 */
	protected IApiProblem createProblem(IReference reference, IJavaProject javaProject) {
		IProject project = javaProject.getProject();
		if (ApiPlugin.getDefault().getSeverityLevel(getSeverityKey(), project) == ApiPlugin.SEVERITY_IGNORE) {
			return null;
		}		
		try {
			IApiMember member = reference.getMember();
			String lookupName = getTypeName(member).replace('$', '.');
			IType type = javaProject.findType(lookupName, new NullProgressMonitor());
			if (type == null) {
				return null;
			}
			ICompilationUnit compilationUnit = type.getCompilationUnit();
			if (compilationUnit == null) {
				return null;
			}
			IResource resource = Util.getResource(project, type);
			if (resource == null) {
				return null;
			}
			int charStart = -1;
			int charEnd = -1;
			int lineNumber = reference.getLineNumber();
			IJavaElement element = compilationUnit;
			if (!Util.isManifest(resource.getProjectRelativePath()) && !type.isBinary()) {
				IDocument document = Util.getDocument(compilationUnit);
				if (lineNumber > 0) {
					// reference line number are 1-based, but the api problem 
					// line number are 0-based
					// they will be converted to 1-based at marker creation time
					lineNumber--;
				}
				// retrieve line number, char start and char end
				if ((reference.getReferenceKind() & 
						(IReference.REF_OVERRIDE | IReference.REF_EXTENDS | IReference.REF_IMPLEMENTS
						| IReference.REF_PARAMETER | IReference.REF_RETURNTYPE | IReference.REF_THROWS)) != 0) {
					IApiType enclosingType = member.getEnclosingType();
					if (lineNumber > 0 && enclosingType != null && enclosingType.isAnonymous()) {
						String superclass = enclosingType.getSuperclassName();
						String name = null;
						if ("java.lang.Object".equals(superclass)) { //$NON-NLS-1$
							// check the superinterfaces
							String[] superinterfaces = enclosingType.getSuperInterfaceNames();
							if (superinterfaces != null) {
								String superinterface = superinterfaces[0];
								name = superinterface.substring(superinterface.lastIndexOf('.') + 1);
							} else {
								// this is really an anonymous class of Object
								name = superclass.substring(superclass.lastIndexOf('.') + 1);
							}
						} else if (superclass != null) {
							name = superclass.substring(superclass.lastIndexOf('.') + 1);
						}
						if (name != null) {
							try {
								IRegion lineInformation = document.getLineInformation(lineNumber);
								String lineContents = document.get(lineInformation.getOffset(), lineInformation.getLength());
								charStart = lineInformation.getOffset() + lineContents.indexOf(name);
								charEnd = charStart + name.length();
							} catch (BadLocationException e) {
								ApiPlugin.log(e);
								return null;
							}
						}
					}
				}
				if (charStart == -1) {
					// get the source range for the problem
					try {
						Position pos = getSourceRange(type, document, reference);
						if (pos != null) {
							charStart = pos.getOffset();
							if (charStart != -1) {
								charEnd = charStart + pos.getLength();
								lineNumber = document.getLineOfOffset(charStart);
							}
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
						return null;
					}
					catch (BadLocationException e) {
						ApiPlugin.log(e);
						return null;
					}
				}
				if(charStart > -1) {
					element = compilationUnit.getElementAt(charStart);
				}
			}
			return ApiProblemFactory.newApiUsageProblem(resource.getProjectRelativePath().toPortableString(),
					type.getFullyQualifiedName(),
					getMessageArgs(reference), 
					new String[] {IApiMarkerConstants.MARKER_ATTR_HANDLE_ID, IApiMarkerConstants.API_MARKER_ATTR_ID}, 
					new Object[] {(element == null ? compilationUnit.getHandleIdentifier() : element.getHandleIdentifier()),
								   new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
					lineNumber, // 0-based
					charStart, 
					charEnd, 
					getElementType(reference), 
					getProblemKind(),
					getProblemFlags(reference));
		} catch (CoreException e) {
			ApiPlugin.log(e);
		}
		return null;
	}
	
	/**
	 * Returns the source range to include in the associated problem or <code>null</code>
	 * if a valid source range could not be computed.
	 * 
	 * @param type resolved type where the reference occurs
	 * @param doc source document of the type
	 * @param reference associated reference
	 * @return source range as a position
	 */
	protected abstract Position getSourceRange(IType type, IDocument doc, IReference reference) throws CoreException, BadLocationException;

	/**
	 * Returns the element type the problem is reported on.
	 * 
	 * @return
	 */
	protected abstract int getElementType(IReference reference);
	
	/**
	 * Returns problem flags, if any.
	 * 
	 * @param reference
	 * @return problem flags
	 */
	protected abstract int getProblemFlags(IReference reference);
	
	/**
	 * Returns problem message arguments
	 * 
	 * @return message arguments
	 */
	protected abstract String[] getMessageArgs(IReference reference) throws CoreException;
	
	/**
	 * Returns problem message arguments to be used in headless build
	 * 
	 * @return message arguments
	 */	
	protected abstract String[] getQualifiedMessageArgs(IReference reference) throws CoreException;
	
	/**
	 * Returns the kind of problem to create
	 * 
	 * @return problem kind
	 */
	protected abstract int getProblemKind();
	
	/**
	 * Returns the key used to lookup problem severity.
	 * 
	 * @return problem severity key
	 */
	protected abstract String getSeverityKey();
	
	/**
	 * Returns the fully qualified type name associated with the given member.
	 * 
	 * @param member
	 * @return fully qualified type name
	 */
	protected String getTypeName(IApiMember member) throws CoreException {
		switch (member.getType()) {
			case IApiElement.TYPE: {
				IApiType type = (IApiType) member;
				if(type.isAnonymous()) {
					return getTypeName(member.getEnclosingType());
				}
				else if(type.isLocal()) {
					return getTypeName(member.getEnclosingType());
				}
				return member.getName();
			}
			default: {
				return getTypeName(member.getEnclosingType());
			}
		}
	}
	
	/**
	 * Returns the qualified type name to display. This method delegates to the 
	 * {@link Signatures} class to build the display signatures
	 * @param member
	 * @return fully qualified display signature for the given {@link IApiType} or enclosing
	 * type if the member is not a type itself
	 * @throws CoreException
	 */
	protected String getQualifiedTypeName(IApiMember member) throws CoreException {
		switch (member.getType()) {
			case IApiElement.TYPE: {
				IApiType type = (IApiType) member;
				if(type.isAnonymous()) {
					return getQualifiedTypeName(member.getEnclosingType());
				}
				else if(type.isLocal()) {
					String name = getTypeName(member.getEnclosingType());
					int idx = name.indexOf('$');
					if(idx > -1) {
						return name.substring(0, idx);
					}
					return name;
				}
				return Signatures.getQualifiedTypeSignature((IApiType) member);
			}
			default: {
				return getQualifiedTypeName(member.getEnclosingType());
			}
		}
	}
	
	/**
	 * Returns the unqualified type name associated with the given member.
	 * 
	 * @param member
	 * @return unqualified type name
	 */
	protected String getSimpleTypeName(IApiMember member) throws CoreException {
		switch (member.getType()) {
			case IApiElement.TYPE: {
				IApiType type = (IApiType) member;
				if(type.isAnonymous()) {
					return getSimpleTypeName(type.getEnclosingType());
				}
				else if(type.isLocal()) {
					String name = getSimpleTypeName(member.getEnclosingType());
					int idx = name.indexOf('$');
					if(idx > -1) {
						return name.substring(0, idx);
					}
					return name;
				}
				return Signatures.getTypeName(Signatures.getTypeSignature(type));
			}
			default:
				return getSimpleTypeName(member.getEnclosingType());
		}
	}	
	
	/**
	 * Default strategy for when no source position can be computed: creates
	 * a {@link Position} for the name of the given {@link IType}. Returns <code>null</code> in the event
	 * the given {@link IType} is <code>null</code> or the name range cannot be computed for the type.
	 * 
	 * @param type the type
	 * @param reference the reference
	 * @throws CoreException
	 * @return returns a default {@link Position} for the name range of the given {@link IType}
	 */
	protected Position defaultSourcePosition(IType type, IReference reference) throws CoreException {
		if(type != null) {
			ISourceRange range = type.getNameRange();
			if(range != null) {
				return new Position(range.getOffset(), range.getLength());
			}
		}
		return null;
	}
	
	/**
	 * Finds the method name to select on the given line of code starting from the given index.
	 * This method will recurse to find a method name in the even there is a name clash with the type.
	 * For example:
	 * <pre>
	 * 		MyType type = new MyType();
	 * </pre>
	 * If we are trying to find the constructor method call we have a name collision (and the first occurrence of MyType would be selected). 
	 * <br>
	 * A name is determined to be a method name if it is followed by a '(' character (excluding spaces)
	 * @param namepart
	 * @param line
	 * @param index
	 * @return the index of the method name on the given line or -1 if not found
	 */
	protected int findMethodNameStart(String namepart, String line, int index) {
		int start = line.indexOf(namepart, index);
		if(start < 0) {
			return -1;
		}
		int offset = start+namepart.length();
		while(line.charAt(offset) == ' ') {
			offset++;
		}
		if(line.charAt(offset) == '(' ||
				line.charAt(offset) == '<') {
			return start;
		}
		return findMethodNameStart(namepart, line, offset);
	}	
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector#createProblems()
	 */
	public List createProblems() {
		List references = getRetainedReferences();
		List problems = new LinkedList();
		Iterator iterator = references.iterator();
		while (iterator.hasNext()) {
			IReference reference = (IReference) iterator.next();
			if (reference.getResolvedReference() == null) {
				// TODO: unresolved reference
			} else {
				if (isProblem(reference)) {
					try {
						IApiProblem problem = null;
						IApiComponent component = reference.getMember().getApiComponent();
						if (component instanceof ProjectComponent) {
							ProjectComponent ppac = (ProjectComponent) component;
							IJavaProject project = ppac.getJavaProject();
							problem = createProblem(reference, project);
						} else {
							problem = createProblem(reference);
						}
						if (problem != null) {
							problems.add(problem);
						}
					} catch (CoreException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
			}
		}
		return problems;
	}

	/**
	 * Returns whether the resolved reference is a real problem.
	 * 
	 * @param reference
	 * @return whether a problem
	 */
	protected boolean isProblem(IReference reference) {
		//by default fragment -> host references are not problems 
		//https://bugs.eclipse.org/bugs/show_bug.cgi?id=255659
		IApiMember member = reference.getResolvedReference();
		if(member != null) {
			IApiMember local = reference.getMember();
			try {
				IApiComponent lcomp = local.getApiComponent();
				if(lcomp != null && lcomp.isFragment()) {
					return !lcomp.getHost().equals(member.getApiComponent());
				}
			}
			catch(CoreException ce) {
				ApiPlugin.log(ce);
			}
		}
		return true;
	}
	protected boolean isReferenceFromComponent(IReference reference,
			Object componentId) {
		if (componentId != null) {
			final IApiComponent apiComponent = reference.getResolvedReference().getApiComponent();
			// API component is either component id itself or one of its fragment
			if (apiComponent.getSymbolicName().equals(componentId)) {
				return true;
			}
			try {
				final IApiComponent host = apiComponent.getHost();
				return host != null && host.getSymbolicName().equals(componentId);
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
		}
		return false;
	}
	/**
	 * Tries to find the given {@link IApiMethod} in the given {@link IType}. If a matching method is not
	 * found <code>null</code> is returned 
	 * @param type the type top look in for the given {@link IApiMethod}
	 * @param method the {@link IApiMethod} to look for
	 * @return the {@link IMethod} from the given {@link IType} that matches the given {@link IApiMethod} or <code>null</code> if no
	 * matching method is found
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	protected IMethod findMethodInType(IType type, IApiMethod method) throws JavaModelException, CoreException {
		String[] parameterTypes = Signature.getParameterTypes(method.getSignature());
		for (int i = 0; i < parameterTypes.length; i++) {
			parameterTypes[i] = parameterTypes[i].replace('/', '.');
		}
		String methodname = method.getName();
		if(method.isConstructor()) {
			IApiType enclosingType = method.getEnclosingType();
			if (enclosingType.isMemberType() && !Flags.isStatic(enclosingType.getModifiers())) {
				// remove the synthetic argument that corresponds to the enclosing type
				int length = parameterTypes.length - 1;
				System.arraycopy(parameterTypes, 1, (parameterTypes = new String[length]), 0, length);
			}
			methodname = enclosingType.getSimpleName();
		}
		IMethod Qmethod = type.getMethod(methodname, parameterTypes);
		IMethod[] methods = type.getMethods();
		IMethod match = null;
		for (int i = 0; i < methods.length; i++) {
			IMethod m = methods[i];
			if (m.isSimilar(Qmethod)) {
				match = m;
				break;
			}
		}
		return match;
	}
	
	/**
	 * Returns the source range for the given {@link IApiMethod} within the given {@link IType}
	 * @param type the type to look for the method within
	 * @param reference the reference the method comes from
	 * @param method the {@link IApiMethod} to look for the source range for
	 * @return the {@link ISourceRange} in the {@link IType} enclosing the given {@link IApiMethod}
	 * @throws CoreException
	 * @throws JavaModelException
	 */
	protected Position getSourceRangeForMethod(IType type, IReference reference, IApiMethod method) throws CoreException, JavaModelException {
		IMethod match = findMethodInType(type, method);
		Position pos = null;
		if (match != null) {
			ISourceRange range = match.getNameRange();
			if(range != null) {
				pos = new Position(range.getOffset(), range.getLength());
			}
		}
		if(pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}
	
	/**
	 * Returns the source range to use for the given field within the given {@link IType}
	 * @param type the type to look in for the given {@link IApiField}
	 * @param reference the reference the field is involved in
	 * @param field the field to find the range for
	 * @return the {@link ISourceRange} in the given {@link IType} that encloses the given {@link IApiField} 
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	protected Position getSourceRangeForField(IType type, IReference reference, IApiField field) throws JavaModelException, CoreException {
		IField javaField = type.getField(field.getName());
		Position pos = null;
		if (javaField.exists()) {
			ISourceRange range = javaField.getNameRange();
			if(range != null) {
				pos = new Position(range.getOffset(), range.getLength()); 
			}
		}
		if(pos == null) {
			return defaultSourcePosition(type, reference);
		}
		return pos;
	}
	
	/**
	 * Returns the range of the name of the given {@link IApiField} to select when creating {@link IApiProblem}s.
	 * Source ranges are computed and tried in the following order:
	 * <ol>
	 * <li>Try the type-qualified name of the variable</li>
	 * <li>Try looking for 'super.variable'</li>
	 * <li>Try looking for 'this.variable'</li>
	 * <li>Try looking for pattern '*.variable'</li>
	 * <li>Else select the entire line optimistically</li>
	 * </ol>
	 * @param field the field to find the name range for
	 * @param document the document to look within
	 * @param reference the reference the field is from
	 * @return the range of text to select, or <code>null</code> if one could not be computed
	 * @throws BadLocationException
	 * @throws CoreException
	 */
	protected Position getFieldNameRange(IApiField field, IDocument document, IReference reference) throws BadLocationException, CoreException {
		return getFieldNameRange(field.getEnclosingType().getName(), field.getName(), document, reference);
	}
	protected Position getFieldNameRange(String typeName, String fieldName, IDocument document, IReference reference) throws BadLocationException, CoreException {
		int linenumber = reference.getLineNumber();
		if (linenumber > 0) {
			// line number are 1-based for the reference, but 0-based for the document
			linenumber--;
		}
		if (linenumber > 0) {
			int offset = document.getLineOffset(linenumber);
			String line = document.get(offset, document.getLineLength(linenumber));
			String qname = typeName +"."+fieldName; //$NON-NLS-1$
			int first = line.indexOf(qname);
			if(first < 0) {
				qname = "super."+fieldName; //$NON-NLS-1$
				first = line.indexOf(qname);
			}
			if(first < 0) {
				qname = "this."+fieldName; //$NON-NLS-1$
				first = line.indexOf(qname);
			}
			if(first < 0) {
				//try a pattern [.*fieldname] 
				//the field might be ref'd via a constant, e.g. enum constant
				int idx = line.indexOf(fieldName);
				while(idx > -1) {
					if(line.charAt(idx-1) == '.') {
						first = idx;
						qname = fieldName;
						break;
					}
					idx = line.indexOf(fieldName, idx+1);
				}
			}
			Position pos = null;
			if(first > -1) {
				pos = new Position(offset + first, qname.length());
			}
			else {
				//optimistically select the whole line since we can't find the correct variable name and we can't just select
				//the first occurrence
				pos = new Position(offset, line.length());
			}
			return pos;
		}
		return null;
	}
	
	/**
	 * Searches for the name of a method at the line number specified in the given
	 * reference.
	 * 
	 * @param name method name
	 * @param document document to search in
	 * @param reference provides line number
	 * @return method name range
	 * @throws CoreException
	 */
	protected Position getMethodNameRange(boolean isContructor, String name, IDocument document, IReference reference) throws CoreException, BadLocationException {
		int linenumber = reference.getLineNumber();
		if (linenumber > 0) {
			// line number are 1-based for the reference, but 0-based for the document
			linenumber--;
		}
		String methodname = name;
		int idx = methodname.indexOf('$');
		if(idx > -1) {
			methodname = methodname.substring(0, idx);
		}
		idx = methodname.indexOf(Signatures.getLT());
		if(idx > -1) {
			methodname = methodname.substring(0, idx);
		}
		int offset = document.getLineOffset(linenumber);
		String line = document.get(offset, document.getLineLength(linenumber));
		int start = line.indexOf('=');
		if(start < 0) {
			if (isContructor) {
				// new keyword should only be checked if the method is a constructor
				start = line.indexOf("new"); //$NON-NLS-1$
				if(start < 0) {
					start = 0;
				}
			} else {
				start = 0;
			}
		}
		else {
			char charat = line.charAt(start-1);
			//make sure its not '==' | '!=' | '<=' | '>='
			if(line.charAt(start+1) == '=' ||
					charat == '!' || charat == '<' || charat == '>') {
				start = 0;
			}
		}
		int first = findMethodNameStart(methodname, line, start);
		if(first < 0) {
			methodname = "super"; //$NON-NLS-1$
			first = findMethodNameStart(methodname, line, start);
		}
		if(first > -1) {
			return new Position(offset + first, methodname.length());
		}
		return null;
	}
	
	/**
	 * @param reference
	 * @return
	 * @throws CoreException
	 */
	public IApiProblem createProblem(IReference reference) throws CoreException {
		int lineNumber = reference.getLineNumber();
		if (lineNumber > 0) {
			lineNumber--;
		}
		String ltypename = getTypeName(reference.getMember());
		return ApiProblemFactory.newApiUsageProblem(
				null,
				ltypename,
				getQualifiedMessageArgs(reference),
				new String[] {IApiMarkerConstants.API_MARKER_ATTR_ID}, 
				new Object[] {new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
				lineNumber, 
				IApiProblem.NO_CHARRANGE, 
				IApiProblem.NO_CHARRANGE,
				getElementType(reference), 
				getProblemKind(),
				getProblemFlags(reference));
	}	
	
}
