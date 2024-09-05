/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.ImportPackageSpecification;
import org.eclipse.pde.api.tools.internal.model.BundleComponent;
import org.eclipse.pde.api.tools.internal.model.ProjectComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ProfileModifiers;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMember;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Detects references to fields, methods and types that are not available for a
 * specific EE.
 *
 * @since 1.1
 */
public class SystemApiDetector extends AbstractProblemDetector {

	private Map<IReference, Integer> referenceEEs;

	/**
	 * Constructor
	 */
	public SystemApiDetector() {
	}

	@Override
	protected int getElementType(IReference reference) {
		IApiMember member = reference.getMember();
		return switch (member.getType())
			{
			case IApiElement.TYPE -> IElementDescriptor.TYPE;
			case IApiElement.METHOD -> IElementDescriptor.METHOD;
			case IApiElement.FIELD -> IElementDescriptor.FIELD;
			default -> 0;
			};
	}

	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		String eeValue = ProfileModifiers.getName(this.referenceEEs.get(reference).intValue());
		String simpleTypeName = Signatures.getSimpleTypeName(reference.getReferencedTypeName());
		if (simpleTypeName.indexOf('$') != -1) {
			simpleTypeName = simpleTypeName.replace('$', '.');
		}
		return switch (reference.getReferenceType())
			{
			case IReference.T_TYPE_REFERENCE -> new String[] {
						getDisplay(member, false), simpleTypeName, eeValue, };
			case IReference.T_FIELD_REFERENCE -> new String[] {
						getDisplay(member, false), simpleTypeName,
						reference.getReferencedMemberName(), eeValue, };
			case IReference.T_METHOD_REFERENCE -> {
				String referenceMemberName = reference.getReferencedMemberName();
				yield Util.isConstructor(referenceMemberName) ? new String[] {
							getDisplay(member, false),
							Signature.toString(reference.getReferencedSignature(), simpleTypeName, null, false, false),
						eeValue, }
						: new String[] {
							getDisplay(member, false),
							simpleTypeName,
							Signature.toString(reference.getReferencedSignature(), referenceMemberName, null, false, false),
							eeValue, };
				}
			default -> null;
			};

	}

	/**
	 * Returns the signature to display for found problems
	 *
	 * @param member the member to get the signature from
	 * @param qualified if the returned signature should be type-qualified or
	 *            not
	 */
	private String getDisplay(IApiMember member, boolean qualified) throws CoreException {
		String typeName = qualified ? getTypeName(member) : getSimpleTypeName(member);
		if (typeName.indexOf('$') != -1) {
			typeName = typeName.replace('$', '.');
		}
		return switch (member.getType())
			{
			case IApiElement.FIELD -> {
				StringBuilder buffer = new StringBuilder();
				IApiField field = (IApiField) member;
				buffer.append(typeName).append('.').append(field.getName());
				yield String.valueOf(buffer);
			}
			case IApiElement.METHOD -> {
				// reference in a method declaration
				IApiMethod method = (IApiMethod) member;
				yield qualified ? Signatures.getMethodSignature(method)
						: Signatures.getQualifiedMethodSignature(method);
			}
			default -> typeName;
			};
	}

	@Override
	protected int getProblemFlags(IReference reference) {
		return switch (reference.getReferenceType())
			{
			case IReference.T_TYPE_REFERENCE -> IApiProblem.NO_FLAGS;
			case IReference.T_METHOD_REFERENCE -> Util.isConstructor(reference.getReferencedMemberName())
					? IApiProblem.CONSTRUCTOR_METHOD
					: IApiProblem.METHOD;
			case IReference.T_FIELD_REFERENCE -> IApiProblem.FIELD;
			default -> IApiProblem.NO_FLAGS;
			};
	}

	@Override
	protected int getProblemKind() {
		return IApiProblem.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiMember member = reference.getMember();
		String eeValue = ProfileModifiers.getName(this.referenceEEs.get(reference).intValue());
		String simpleTypeName = reference.getReferencedTypeName();
		if (simpleTypeName.indexOf('$') != -1) {
			simpleTypeName = simpleTypeName.replace('$', '.');
		}
		return switch (reference.getReferenceType()) {
			case IReference.T_TYPE_REFERENCE -> {
				yield new String[] {
						getDisplay(member, false), simpleTypeName, eeValue, };
			}
			case IReference.T_FIELD_REFERENCE -> {
				yield new String[] {
						getDisplay(member, false), simpleTypeName,
						reference.getReferencedMemberName(), eeValue, };
			}
			case IReference.T_METHOD_REFERENCE -> {
				String referenceMemberName = reference.getReferencedMemberName();
				if (Util.isConstructor(referenceMemberName)) {
					yield new String[] {
							getDisplay(member, false),
							Signature.toString(reference.getReferencedSignature(), simpleTypeName, null, false, false),
							eeValue, };
				} else {
					yield new String[] {
							getDisplay(member, false),
							simpleTypeName,
							Signature.toString(reference.getReferencedSignature(), referenceMemberName, null, false, false),
							eeValue, };
				}
			}
			default -> null;
			};
	}

	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.INVALID_REFERENCE_IN_SYSTEM_LIBRARIES;
	}

	@Override
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
		return switch (reference.getReferenceType())
			{
			case IReference.T_TYPE_REFERENCE -> {
				int linenumber = reference.getLineNumber();
				if (linenumber > 0) {
					// line number starts at 0 for the
					linenumber--;
				}
				if (linenumber > 0) {
					int offset = document.getLineOffset(linenumber);
					String line = document.get(offset, document.getLineLength(linenumber));
					String qname = reference.getReferencedTypeName().replace('$', '.');
					int first = line.indexOf(qname);
					if (first < 0) {
						qname = Signatures.getSimpleTypeName(reference.getReferencedTypeName());
						qname = qname.replace('$', '.');
						first = line.indexOf(qname);
					}
					Position pos = null;
					if (first > -1) {
						pos = new Position(offset + first, qname.length());
					} else {
						// optimistically select the whole line since we can't
						// find the correct variable name and we can't just
						// select
						// the first occurrence
						pos = new Position(offset, line.length());
					}
					yield pos;
				} else {
					IApiMember apiMember = reference.getMember();
					switch (apiMember.getType()) {
						case IApiElement.FIELD -> {
							IApiField field = (IApiField) reference.getMember();
							yield getSourceRangeForField(type, reference, field);
						}
						case IApiElement.METHOD -> {
							// reference in a method declaration
							IApiMethod method = (IApiMethod) reference.getMember();
							yield getSourceRangeForMethod(type, reference, method);
						}
						default -> { /**/ }
					}
					// reference in a type declaration
					ISourceRange range = type.getNameRange();
					Position pos = null;
					if (range != null) {
						pos = new Position(range.getOffset(), range.getLength());
					}
					if (pos == null) {
						yield defaultSourcePosition(type, reference);
					}
					yield pos;
				}
			}
			case IReference.T_FIELD_REFERENCE -> {
				int linenumber = reference.getLineNumber();
				if (linenumber > 0) {
					yield getFieldNameRange(reference.getReferencedTypeName(), reference.getReferencedMemberName(),
							document, reference);
				}
				// reference in a field declaration
				IApiField field = (IApiField) reference.getMember();
				yield getSourceRangeForField(type, reference, field);
			}
			case IReference.T_METHOD_REFERENCE -> {
				if (reference.getLineNumber() >= 0) {
					String referenceMemberName = reference.getReferencedMemberName();
					String methodName = null;
					boolean isConstructor = Util.isConstructor(referenceMemberName);
					if (isConstructor) {
						methodName = Signatures.getSimpleTypeName(reference.getReferencedTypeName().replace('$', '.'));
					} else {
						methodName = referenceMemberName;
					}
					Position pos = getMethodNameRange(isConstructor, methodName, document, reference);
					if (pos == null) {
						yield defaultSourcePosition(type, reference);
					}
					yield pos;
				}
				// reference in a method declaration
				IApiMethod method = (IApiMethod) reference.getMember();
				yield getSourceRangeForMethod(type, reference, method);
			}
			default -> null;
			};
	}

	@Override
	protected boolean isProblem(IReference reference, IProgressMonitor monitor) {
		// the reference must be in the system library
		try {
			IApiMember member = reference.getMember();
			IApiComponent apiComponent = member.getApiComponent();
			List<String> lowestEEs = apiComponent.getLowestEEs();
			if (lowestEEs.isEmpty()) {
				// this should not be true for Eclipse bundle as they should
				// always have a EE set
				return false;
			}
			loop: for (String lowestEE : lowestEEs) {
				int eeValue = ProfileModifiers.getValue(lowestEE);
				if (eeValue == ProfileModifiers.NO_PROFILE_VALUE) {
					return false;
				}
				if (!((Reference) reference).resolve(eeValue)) {
					/*
					 * Make sure that the resolved reference doesn't below to
					 * one of the imported package of the current component
					 */
					if (apiComponent instanceof BundleComponent) {
						BundleDescription bundle = ((BundleComponent) apiComponent).getBundleDescription();
						ImportPackageSpecification[] importPackages = bundle.getImportPackages();
						String referencedTypeName = reference.getReferencedTypeName();
						int index = referencedTypeName.lastIndexOf('.');
						String packageName = referencedTypeName.substring(0, index);
						for (ImportPackageSpecification importPackageSpecification : importPackages) {
							// get the IPackageDescriptor for the element
							// descriptor
							String importPackageName = importPackageSpecification.getName();
							if (importPackageName.equals(packageName)) {
								continue loop;
							}
						}
					}
					if (this.referenceEEs == null) {
						this.referenceEEs = new HashMap<>(3);
					}
					this.referenceEEs.put(reference, Integer.valueOf(eeValue));
					return true;
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
			checkIfDisposed(reference.getMember().getApiComponent(), monitor);
		}
		return false;
	}

	@Override
	public boolean considerReference(IReference reference, IProgressMonitor monitor) {
		try {
			IApiComponent apiComponent = reference.getMember().getApiComponent();
			IApiBaseline baseline = apiComponent.getBaseline();
			if (baseline == null) {
				return false;
			}
			String referencedTypeName = reference.getReferencedTypeName();
			// extract the package name
			int index = referencedTypeName.lastIndexOf('.');
			if (index == -1) {
				return false;
			}
			String substring = referencedTypeName.substring(0, index);
			IApiComponent[] resolvePackages = baseline.resolvePackage(apiComponent, substring);
			if (resolvePackages.length > 0) {
				if (resolvePackages[0].isSystemComponent()) {
					if (reference.getReferenceKind() == IReference.REF_OVERRIDE || reference.getReferenceKind() == IReference.REF_CONSTANTPOOL) {
						return false;
					}
					((Reference) reference).setResolveStatus(false);
					retainReference(reference);
					return true;
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e);
			checkIfDisposed(reference.getMember().getApiComponent(), monitor);
		}
		return false;
	}

	@Override
	public int getReferenceKinds() {
		return IReference.MASK_REF_ALL & ~IReference.REF_OVERRIDE;
	}

	@Override
	public List<IApiProblem> createProblems(IProgressMonitor monitor) {
		List<IReference> references = getRetainedReferences();
		List<IApiProblem> problems = new LinkedList<>();
		SubMonitor loopMonitor = SubMonitor.convert(monitor, references.size());
		for (IReference reference : references) {
			if (monitor.isCanceled()) {
				break;
			}
			loopMonitor.split(1);
			if (isProblem(reference, monitor)) {
				try {
					IApiProblem problem = null;
					IApiComponent component = reference.getMember().getApiComponent();
					if (component instanceof ProjectComponent ppac) {
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
					AbstractProblemDetector.checkIfDisposed(reference.getMember().getApiComponent(), monitor);
				}
			}
		}
		return problems;
	}
}
