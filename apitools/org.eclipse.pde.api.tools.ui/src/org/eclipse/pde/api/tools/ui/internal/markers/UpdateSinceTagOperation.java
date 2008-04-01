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
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.text.edits.TextEdit;

public class UpdateSinceTagOperation {
	static class NodeFinder extends ASTVisitor {
		BodyDeclaration declaration;
		int position;
		public NodeFinder(int position) {
			this.position = position;
		}

		public boolean visit(AnnotationTypeDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(EnumDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(TypeDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(MethodDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(FieldDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(EnumConstantDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(AnnotationTypeMemberDeclaration node) {
			return visitNode(node);
		}
		public boolean visit(Initializer node) {
			return false;
		}
		private boolean visitNode(BodyDeclaration bodyDeclaration) {
			int start = bodyDeclaration.getStartPosition();
			int end = bodyDeclaration.getLength() - 1 + start;
			switch(bodyDeclaration.getNodeType()) {
				case ASTNode.TYPE_DECLARATION :
				case ASTNode.ENUM_DECLARATION :
				case ASTNode.ANNOTATION_TYPE_DECLARATION :
					if (start <= this.position && this.position <= end) {
						this.declaration = bodyDeclaration;
						return true;
					}
					return false;
				case ASTNode.ENUM_CONSTANT_DECLARATION :
				case ASTNode.FIELD_DECLARATION :
				case ASTNode.METHOD_DECLARATION :
				case ASTNode.ANNOTATION_TYPE_MEMBER_DECLARATION :
					if (start <= this.position && this.position <= end) {
						this.declaration = bodyDeclaration;
					}
					return false;
				default :
					return false;
			}
		}
		public BodyDeclaration getNode() {
			return this.declaration;
		}
	}
	private IMarker fMarker;
	private int sinceTagType;
	private String sinceTagVersion;

	public UpdateSinceTagOperation(IMarker marker, int sinceTagType, String sinceTagVersion) {
		this.fMarker = marker;
		this.sinceTagType = sinceTagType;
		this.sinceTagVersion = sinceTagVersion;
	}

	public void run(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) return;
		if (monitor != null) {
			monitor.beginTask(MarkerMessages.UpdateSinceTagOperation_title, 3);
		}
		// retrieve the AST node compilation unit
		IResource resource = this.fMarker.getResource();
		IJavaElement javaElement = JavaCore.create(resource);
		try {
			if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setSource(compilationUnit);
				Integer charStartAttribute = null;
				try {
					charStartAttribute = (Integer) this.fMarker.getAttribute(IMarker.CHAR_START);
					int intValue = charStartAttribute.intValue();
					parser.setFocalPosition(intValue);
					parser.setResolveBindings(true);
					Map options = compilationUnit.getJavaProject().getOptions(true);
					options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
					parser.setCompilerOptions(options);
					final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
					NodeFinder nodeFinder = new NodeFinder(intValue);
					unit.accept(nodeFinder);
					if (monitor != null) {
						monitor.worked(1);
					}
					BodyDeclaration node = nodeFinder.getNode();
					if (node != null) {
						unit.recordModifications();
						AST ast = unit.getAST();
						ASTRewrite rewrite = ASTRewrite.create(ast);
						if (IApiProblem.SINCE_TAG_MISSING == this.sinceTagType) {
							Javadoc docnode = node.getJavadoc();
							if (docnode == null) {
								docnode = ast.newJavadoc();
								//we do not want to create a new empty Javadoc node in
								//the AST if there are no missing tags
								rewrite.set(node, node.getJavadocProperty(), docnode, null);
							} else {
								List tags = docnode.tags();
								boolean found = false;
								loop: for (Iterator iterator = tags.iterator(); iterator.hasNext();) {
									TagElement element = (TagElement) iterator.next();
									String tagName = element.getTagName();
									if (TagElement.TAG_SINCE.equals(tagName)) {
										found = true;
										break loop;
									}
								}
								if (found) return;
							}
							ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
							// check the existing tags list
							TagElement newtag = ast.newTagElement();
							newtag.setTagName(TagElement.TAG_SINCE);
							TextElement textElement = ast.newTextElement();
							textElement.setText(this.sinceTagVersion);
							newtag.fragments().add(textElement);
							lrewrite.insertLast(newtag, null);
						} else {
							Javadoc docnode = node.getJavadoc();
							List tags = docnode.tags();
							TagElement sinceTag = null;
							for (Iterator iterator = tags.iterator(); iterator.hasNext(); ) {
								TagElement tagElement = (TagElement) iterator.next();
								if (TagElement.TAG_SINCE.equals(tagElement.getTagName())) {
									sinceTag = tagElement;
									break;
								}
							}
							if (sinceTag != null) {
								List fragments = sinceTag.fragments();
								if (fragments.size() == 1) {
									TextElement textElement = (TextElement) fragments.get(0);
									StringBuffer buffer = new StringBuffer();
									buffer.append(' ').append(this.sinceTagVersion);
									rewrite.set(textElement, TextElement.TEXT_PROPERTY, String.valueOf(buffer), null);
								}
							}
						}
						ITextFileBufferManager textFileBufferManager = FileBuffers.getTextFileBufferManager(); 
						IPath path = compilationUnit.getPath(); 
						try {
							if (monitor != null) {
								monitor.worked(1);
							}
							textFileBufferManager.connect(path, LocationKind.IFILE, monitor);
							ITextFileBuffer textFileBuffer = textFileBufferManager.getTextFileBuffer(path, LocationKind.IFILE);
							IDocument document = textFileBuffer.getDocument(); 
							//TODO support undo??
							TextEdit edits = rewrite.rewriteAST(document, compilationUnit.getJavaProject().getOptions(true));
							edits.apply(document);
							textFileBuffer.commit(monitor, true);
							if (monitor != null) {
								monitor.worked(1);
							}
						} catch(BadLocationException e) {
							ApiUIPlugin.log(e);
						} finally {
							textFileBufferManager.disconnect(path, LocationKind.IFILE, monitor);
						}
					}
				} catch (CoreException e) {
					ApiUIPlugin.log(e);
				}
			}
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}
}
