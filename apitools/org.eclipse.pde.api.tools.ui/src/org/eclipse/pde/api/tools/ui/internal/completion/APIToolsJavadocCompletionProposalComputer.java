/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.completion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.CompletionContext;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer;
import org.eclipse.jdt.ui.text.java.JavaContentAssistInvocationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiJavadocTag;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.swt.graphics.Image;

/**
 * This class creates completion proposals to javadoc header blocks
 * for the javadoc tags contributed via the apiJavadocTags extension point.
 * 
 * @see IApiJavadocTag
 * @see JavadocTagManager
 * @see APIToolsJavadocCompletionProposal
 * 
 * @since 1.0.0
 */
public class APIToolsJavadocCompletionProposalComputer implements IJavaCompletionProposalComputer {
	
	private String fErrorMessage = null;
	private Image fImageHandle = null;
	private ASTParser fParser = null;
	HashSet fExistingTags = null;
	
	/**
	 * Collects all of the existing API Tools Javadoc tags form a given Javadoc node
	 */
	class TagCollector extends ASTVisitor {
		/* (non-Javadoc)
		 * @see org.eclipse.jdt.core.dom.ASTVisitor#visit(org.eclipse.jdt.core.dom.Javadoc)
		 */
		public boolean visit(Javadoc node) {
			Set tagnames = ApiPlugin.getJavadocTagManager().getAllTagNames();
			List tags = node.tags();
			if(fExistingTags == null) {
				fExistingTags = new HashSet(tags.size());
			}
			TagElement tag = null;
			String name = null;
			for(Iterator iter = tags.iterator(); iter.hasNext();) {
				tag = (TagElement) iter.next();
				name = tag.getTagName();
				if(name == null) {
					continue;
				}
				if(tagnames.contains(name)) {
					//only add existing api tools tags
					fExistingTags.add(name);
				}
			}
			return false;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#computeCompletionProposals(org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeCompletionProposals(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		JavaContentAssistInvocationContext jcontext = null;
		if(context instanceof JavaContentAssistInvocationContext) {
			jcontext = (JavaContentAssistInvocationContext) context;
		}
		else {
			return Collections.EMPTY_LIST;
		}
		IJavaProject project = jcontext.getProject();
		if(project == null || !Util.isApiProject(project)) {
			return Collections.EMPTY_LIST;
		}
		CompletionContext corecontext = jcontext.getCoreContext();
		if(!corecontext.isInJavadoc()) {
			return Collections.EMPTY_LIST;
		}
		ICompilationUnit cunit = jcontext.getCompilationUnit();
		if(cunit != null) {
			try {
				int offset = jcontext.getInvocationOffset();
				IJavaElement element = cunit.getElementAt(offset);
				if (element == null) {
					return Collections.EMPTY_LIST;
				}
				ImageDescriptor imagedesc = jcontext.getLabelProvider().createImageDescriptor(
						org.eclipse.jdt.core.CompletionProposal.create(org.eclipse.jdt.core.CompletionProposal.JAVADOC_BLOCK_TAG, 
						offset));
				fImageHandle = (imagedesc == null ? null : imagedesc.createImage());
				int type = getType(element);
				int member = IApiJavadocTag.MEMBER_NONE;
				int elementtype = element.getElementType();
				switch(elementtype) {
					case IJavaElement.METHOD: {
						IMethod method = (IMethod) element;
						if(Flags.isPrivate(method.getFlags())) {
							return Collections.EMPTY_LIST;
						}
						member = IApiJavadocTag.MEMBER_METHOD;
						if(method.isConstructor()) {
							member = IApiJavadocTag.MEMBER_CONSTRUCTOR;
						}
						break;
					}
					case IJavaElement.FIELD: {
						IField field  = (IField) element;
						int flags = field.getFlags();
						if(Flags.isFinal(flags) || field.isEnumConstant() || Flags.isPrivate(flags)) {
							return Collections.EMPTY_LIST;
						}
						member = IApiJavadocTag.MEMBER_FIELD;
						break;
					}
				}
				IApiJavadocTag[] tags = ApiPlugin.getJavadocTagManager().getTagsForType(type, member);
				int tagcount = tags.length;
				if(tagcount > 0) {
					ArrayList list = null;
					collectExistingTags(element, jcontext);
					String completiontext = null;
					int tokenstart = corecontext.getTokenStart();
					int length = offset - tokenstart;
					for(int i = 0; i < tagcount; i++) {
						if(!acceptTag(tags[i], element)) {
							continue;
						}
						completiontext = tags[i].getCompleteTag(type, member);
						if(appliesToContext(jcontext.getDocument(), completiontext, tokenstart, (length > 0 ? length : 1))) {
							if(list == null) {
								list = new ArrayList(tagcount-i);
							}
							list.add(new APIToolsJavadocCompletionProposal(corecontext, completiontext, tags[i].getTagName(), fImageHandle));
						}
					}
					if(list != null) {
						return list;
					}
				}
			} 
			catch (JavaModelException e) {
				fErrorMessage = e.getMessage();
			}
		}
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * Method to post process returned flags from the {@link org.eclipse.pde.api.tools.internal.JavadocTagManager}
	 * @param tag the tag to process
	 * @param element the {@link IJavaElement} the tag will appear on
	 * @return true if the tag should be included in completion proposals, false otherwise
	 */
	private boolean acceptTag(IApiJavadocTag tag, IJavaElement element) throws JavaModelException {
		if(fExistingTags != null && fExistingTags.contains(tag.getTagName())) {
			return false;
		}
		switch(element.getElementType()) {
			case IJavaElement.TYPE: {
				IType type = (IType) element;
				int flags = type.getFlags();
				String tagname = tag.getTagName();
				if(Flags.isAbstract(flags)) {
					return !tagname.equals("@noinstantiate");  //$NON-NLS-1$
				}
				if(Flags.isFinal(flags)) {
					return !tagname.equals("@noextend");  //$NON-NLS-1$
				}
				break;
			}
			case IJavaElement.METHOD: {
				IMethod method = (IMethod) element;
				if(Flags.isFinal(method.getFlags()) || Flags.isStatic(method.getFlags())) {
					return !tag.getTagName().equals("@nooverride"); //$NON-NLS-1$
				}
				IType type = method.getDeclaringType();
				if(type != null && Flags.isFinal(type.getFlags())) {
					return !tag.getTagName().equals("@nooverride"); //$NON-NLS-1$
				}
			}
		}
		return true;
	}
	
	/**
	 * Returns the type of the enclosing type.
	 * 
	 * @param element java element
	 * @return TYPE_INTERFACE, TYPE_CLASS, TYPE_ENUM, TYPE_ANNOTATION or -1 
	 * @throws JavaModelException
	 */
	private int getType(IJavaElement element) throws JavaModelException {
		IJavaElement lelement = element;
		while (lelement != null && lelement.getElementType() != IJavaElement.TYPE) {
			lelement = lelement.getParent();
		}
		if (lelement instanceof IType) {
			IType type = (IType) lelement;
			if(type.isAnnotation()) {
				return IApiJavadocTag.TYPE_ANNOTATION;
			}
			else if (type.isInterface()) {
				return IApiJavadocTag.TYPE_INTERFACE;
			}
			else if(type.isEnum()) {
				return IApiJavadocTag.TYPE_ENUM;
			}
		}
		return IApiJavadocTag.TYPE_CLASS;
	}

	/**
	 * Collects the existing tags on the {@link IJavaElement} we have been activated on
	 * @param element
	 * @param jcontext
	 * @throws JavaModelException
	 * @throws BadLocationException
	 */
	private void collectExistingTags(IJavaElement element, JavaContentAssistInvocationContext jcontext) throws JavaModelException {
		if(element instanceof IMember) {
			IMember member = (IMember) element;
			ICompilationUnit cunit = jcontext.getCompilationUnit();
			if(cunit != null) {
				if(cunit.isWorkingCopy()) {
					cunit.reconcile(ICompilationUnit.NO_AST, false, false, null, null);
				}
				fParser.setSource(member.getSource().toCharArray());
				fParser.setKind(ASTParser.K_CLASS_BODY_DECLARATIONS);
				Map options = element.getJavaProject().getOptions(true);
				options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				fParser.setCompilerOptions(options);
				fParser.setStatementsRecovery(false);
				fParser.setResolveBindings(false);
				fParser.setBindingsRecovery(false);
				ASTNode ast = fParser.createAST(null);
				TagCollector collector = new TagCollector();
				if (ast.getNodeType() == ASTNode.TYPE_DECLARATION) {
					TypeDeclaration typeDeclaration = (TypeDeclaration) ast;
					List bodyDeclarations = typeDeclaration.bodyDeclarations();
					if (bodyDeclarations.size() == 1) {
						// only one element should be there as we are parsing a specific member
						BodyDeclaration bodyDeclaration = (BodyDeclaration) bodyDeclarations.iterator().next();
						Javadoc javadoc = bodyDeclaration.getJavadoc();
						if (javadoc != null) {
							javadoc.accept(collector);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Determines if the specified completion applies to the current offset context in the document
	 * @param document
	 * @param completiontext
	 * @param offset
	 * @return true if the completion applies, false otherwise
	 */
	private boolean appliesToContext(IDocument document, String completiontext, int tokenstart, int length) {
		if(length > completiontext.length()) {
			return false;
		}
		try {
			String prefix = document.get(tokenstart, length);
			return prefix.equals(completiontext.substring(0, length));
		}
		catch (BadLocationException e) {
			ApiUIPlugin.log(e);
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#computeContextInformation(org.eclipse.jdt.ui.text.java.ContentAssistInvocationContext, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public List computeContextInformation(ContentAssistInvocationContext context, IProgressMonitor monitor) {
		return Collections.EMPTY_LIST;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fErrorMessage;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionEnded()
	 */
	public void sessionEnded() {
		if(fImageHandle != null) {
			fImageHandle.dispose();
		}
		fParser = null;
		if(fExistingTags != null) {
			fExistingTags.clear();
			fExistingTags = null;
		}
		fErrorMessage = null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer#sessionStarted()
	 */
	public void sessionStarted() {
		fParser = ASTParser.newParser(AST.JLS4);
		fErrorMessage = null;
	}

}
