/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.builder.IApiProblemDetector;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

import com.ibm.icu.text.MessageFormat;

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
	
	/**
	 * Creates a problem for a specific reference in the workspace
	 * 
	 * @param reference reference
	 * @param associated java project (with reference source location)
	 * @return problem or <code>null</code> if none
	 * @exception CoreException if something goes wrong
	 */
	protected IApiProblem createProblem(IReference reference, IJavaProject project) {
		if (ApiPlugin.getDefault().getSeverityLevel(getSeverityKey(), project.getProject()) == ApiPlugin.SEVERITY_IGNORE) {
			return null;
		}		
		try {
			String lookupName = getTypeName(reference.getMember()).replace('$', '.');
			IType type = project.findType(lookupName, new NullProgressMonitor());
			if (type == null) {
				return null;
			}
			ICompilationUnit compilationUnit = type.getCompilationUnit();
			if (compilationUnit == null) {
				return null;
			}
			IResource resource = compilationUnit.getCorrespondingResource();
			if (resource == null) {
				return null;
			}
			IDocument document = Util.getDocument(compilationUnit);
			// retrieve line number, char start and char end
			int lineNumber = reference.getLineNumber();
			int charStart = -1;
			int charEnd = -1;
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
			IJavaElement element = compilationUnit;
			if(charStart > -1) {
				element = compilationUnit.getElementAt(charStart);
			}
			return ApiProblemFactory.newApiUsageProblem(resource.getProjectRelativePath().toPortableString(),
					type.getFullyQualifiedName(),
					getMessageArgs(reference), 
					new String[] {IApiMarkerConstants.MARKER_ATTR_HANDLE_ID, IApiMarkerConstants.API_MARKER_ATTR_ID}, 
					new Object[] {(element == null ? compilationUnit.getHandleIdentifier() : element.getHandleIdentifier()),
								   new Integer(IApiMarkerConstants.API_USAGE_MARKER_ID)}, 
					lineNumber, 
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
	 * Throws a new exception to report that we could not locate a source position for the 
	 * given reference in the given type
	 * @param type the type
	 * @param reference the reference
	 * @throws CoreException
	 */
	protected void noSourcePosition(IType type, IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		String name = reference.getReferencedMemberName();
		if(name == null) {
			name = reference.getReferencedTypeName();
		}
		if(member.getType() == IApiElement.TYPE) {
			IApiType atype = (IApiType) member;
			if(atype.isAnonymous()) {
				name = BuilderMessages.IllegalExtendsProblemDetector_an_anonymous_declaration;
			}
			else if(atype.isLocal()) {
				name = NLS.bind(BuilderMessages.AbstractProblemDetector_local_type_T, atype.getName());
			}
		}
		IStatus status = new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID,
				MessageFormat.format(BuilderMessages.AbstractProblemDetector_could_not_locate_src_pos, 
						new String[] {name, type.getElementName()}));
		throw new CoreException(status);
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
						if (component instanceof PluginProjectApiComponent) {
							PluginProjectApiComponent ppac = (PluginProjectApiComponent) component;
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
	protected abstract boolean isProblem(IReference reference);
	
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
	protected Position getMethodNameRange(String name, IDocument document, IReference reference) throws CoreException, BadLocationException {
		int linenumber = reference.getLineNumber();
		if (linenumber > 0) {
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
			start = line.indexOf("new"); //$NON-NLS-1$
			if(start < 0) {
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
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#createProblem(org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	protected IApiProblem createProblem(IReference reference) throws CoreException {
		int lineNumber = reference.getLineNumber();
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
