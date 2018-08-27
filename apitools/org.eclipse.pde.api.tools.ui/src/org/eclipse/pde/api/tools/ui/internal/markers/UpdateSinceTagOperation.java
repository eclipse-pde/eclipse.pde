/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.TextElement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.text.edits.TextEdit;

public class UpdateSinceTagOperation {

	private IMarker fMarker;
	private int sinceTagType;
	private String sinceTagVersion;

	public UpdateSinceTagOperation(IMarker marker, int sinceTagType, String sinceTagVersion) {
		this.fMarker = marker;
		this.sinceTagType = sinceTagType;
		this.sinceTagVersion = sinceTagVersion;
	}

	public void run(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled()) {
			return;
		}
		if (monitor != null) {
			monitor.beginTask(MarkerMessages.UpdateSinceTagOperation_title, 3);
		}
		// retrieve the AST node compilation unit
		try {
			Integer charStartAttribute = (Integer) this.fMarker.getAttribute(IMarker.CHAR_START);
			int intValue = charStartAttribute.intValue();
			IJavaElement javaElement = null;
			IJavaElement handleElement = null;
			if (intValue > 0) {
				IResource resource = this.fMarker.getResource();
				javaElement = JavaCore.create(resource);
			} else {
				// this is a case where the marker is reported against the
				// MANIFEST.MF file
				String handle = (String) fMarker.getAttribute(IApiMarkerConstants.MARKER_ATTR_HANDLE_ID);
				if (handle != null) {
					handleElement = JavaCore.create(handle);
				}
				if (handleElement != null && handleElement.exists()) {
					javaElement = handleElement.getAncestor(IJavaElement.COMPILATION_UNIT);
				}
			}
			if (javaElement != null && javaElement.getElementType() == IJavaElement.COMPILATION_UNIT) {
				ICompilationUnit compilationUnit = (ICompilationUnit) javaElement;
				if (!compilationUnit.isWorkingCopy()) {
					// open an editor of the corresponding unit to "show" the
					// quickfix change
					JavaUI.openInEditor(compilationUnit);
				}
				ASTParser parser = ASTParser.newParser(AST.JLS10);
				parser.setSource(compilationUnit);
				if (intValue <= 0) {
					// try to use the name range of the corresponding element
					if (handleElement instanceof IMember) {
						IMember member = (IMember) handleElement;
						ISourceRange range = member.getNameRange();
						if (range != null) {
							intValue = range.getOffset();
						} else {
							range = member.getSourceRange();
							if (range != null && range.getOffset() > 0) {
								intValue = range.getOffset();
							} else {
								return;
							}
						}
					} else {
						return;
					}
				}
				parser.setFocalPosition(intValue);
				parser.setResolveBindings(true);
				Map<String, String> options = compilationUnit.getJavaProject().getOptions(true);
				options.put(JavaCore.COMPILER_DOC_COMMENT_SUPPORT, JavaCore.ENABLED);
				parser.setCompilerOptions(options);
				final CompilationUnit unit = (CompilationUnit) parser.createAST(new NullProgressMonitor());
				BodyDeclaration node = null;
				NodeFinder nodeFinder = new NodeFinder(intValue);
				unit.accept(nodeFinder);
				if (monitor != null) {
					monitor.worked(1);
				}
				node = nodeFinder.getNode();
				if (node != null) {
					unit.recordModifications();
					AST ast = unit.getAST();
					ASTRewrite rewrite = ASTRewrite.create(ast);
					if (IApiProblem.SINCE_TAG_MISSING == this.sinceTagType) {
						Javadoc docnode = node.getJavadoc();
						if (docnode == null) {
							docnode = ast.newJavadoc();
							// we do not want to create a new empty Javadoc node
							// in
							// the AST if there are no missing tags
							rewrite.set(node, node.getJavadocProperty(), docnode, null);
						} else {
							List<TagElement> tags = docnode.tags();
							boolean found = false;
							loop: for (TagElement element : tags) {
								String tagName = element.getTagName();
								if (TagElement.TAG_SINCE.equals(tagName)) {
									found = true;
									break loop;
								}
							}
							if (found) {
								return;
							}
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
						List<TagElement> tags = docnode.tags();
						TagElement sinceTag = null;
						for (TagElement tagElement : tags) {
							if (TagElement.TAG_SINCE.equals(tagElement.getTagName())) {
								sinceTag = tagElement;
								break;
							}
						}
						if (sinceTag != null) {
							List<TextElement> fragments = sinceTag.fragments();
							if (fragments.size() >= 1) {
								TextElement textElement = fragments.get(0);
								StringBuilder buffer = new StringBuilder();
								buffer.append(' ').append(this.sinceTagVersion);
								rewrite.set(textElement, TextElement.TEXT_PROPERTY, String.valueOf(buffer), null);
							} else {
								ListRewrite lrewrite = rewrite.getListRewrite(docnode, Javadoc.TAGS_PROPERTY);
								// check the existing tags list
								TagElement newtag = ast.newTagElement();
								newtag.setTagName(TagElement.TAG_SINCE);
								TextElement textElement = ast.newTextElement();
								textElement.setText(this.sinceTagVersion);
								newtag.fragments().add(textElement);
								lrewrite.replace(sinceTag, newtag, null);
							}
						}
					}
					try {
						if (monitor != null) {
							monitor.worked(1);
						}
						TextEdit edit = rewrite.rewriteAST();
						compilationUnit.applyTextEdit(edit, monitor);
						if (monitor != null) {
							monitor.worked(1);
						}
					} finally {
						compilationUnit.reconcile(ICompilationUnit.NO_AST, false /*
																				 * don
																				 * 't
																				 * force
																				 * problem
																				 * detection
																				 */, null /*
																						 * use
																						 * primary
																						 * owner
																						 */, null /*
																								 * no
																								 * progress
																								 * monitor
																								 */);
					}
				}
			}
		} catch (CoreException e) {
			ApiUIPlugin.log(e);
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}
	}
}
