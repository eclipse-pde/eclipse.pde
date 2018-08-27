/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeParameter;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMethodDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * This class contains utility methods for creating and working with Java
 * element signatures.
 *
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class Signatures {

	/**
	 * Constructor
	 */
	private Signatures() {
	}

	/**
	 * Collects which signature to use and de-qualifies it. If there is a
	 * generic signature it is returned, otherwise the standard signature is
	 * used
	 *
	 * @param method
	 * @return the de-qualified signature for the method
	 */
	public static String processMethodSignature(IApiMethod method) {
		String signature = method.getGenericSignature();
		if (signature == null) {
			signature = method.getSignature();
		}
		return dequalifySignature(signature);
	}

	/**
	 * Strips member type names off the signature and returns the primary type
	 * name. Member types are assumed to be delimited by the '$' character<br>
	 * <br>
	 * For example:<br>
	 * <code>a.b.c.Type$Member -&gt; a.b.c.Type</code><br>
	 * <code>x.y.z.Type -&gt; x.y.z.Type</code><br>
	 *
	 * @param typename the type name to process
	 * @return the primary type name stripped off of the given type name
	 */
	public static String getPrimaryTypeName(String typename) {
		int idx = typename.indexOf('$');
		if (idx > -1) {
			return typename.substring(0, idx);
		}
		return typename;
	}

	/**
	 * Returns the signature to use to display this {@link IApiMethod}. This
	 * method will load the enclosing type in the event the method is a
	 * constructor.
	 *
	 * @param method
	 * @return the display signature to use for this {@link IApiMethod}
	 * @throws CoreException if a lookup to the parent type of the method fails
	 */
	public static String getMethodSignature(IApiMethod method) throws CoreException {
		String methodsig = method.getGenericSignature();
		if (methodsig == null) {
			methodsig = method.getSignature();
		}
		String methodname = getMethodName(method);
		return Signature.toString(dequalifySignature(methodsig), methodname, null, false, false);
	}

	/**
	 * Returns the signature of the method qualified with the given type
	 *
	 * @param type
	 * @param method
	 * @return the given type qualified signature of the given method
	 * @throws CoreException if a lookup to the parent type of the given method
	 *             fails
	 */
	public static String getQualifiedMethodSignature(IApiMethod method) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		IApiType type = method.getEnclosingType();
		if (type != null) {
			buffer.append(getQualifiedTypeSignature(type)).append('.');
		}
		String methodsig = method.getGenericSignature();
		if (methodsig == null) {
			methodsig = method.getSignature();
		}
		String methodname = getMethodName(method);
		buffer.append(Signature.toString(dequalifySignature(methodsig), methodname, null, false, false));
		return buffer.toString();
	}

	/**
	 * Returns the signature of the method qualified with the given type
	 *
	 * @param method
	 * @return the given type qualified signature of the given method
	 * @throws CoreException if a lookup to the parent type of the given method
	 *             fails
	 */
	public static String getQualifiedMethodSignature(IMethodDescriptor method) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		IReferenceTypeDescriptor type = method.getEnclosingType();
		if (type != null) {
			buffer.append(getQualifiedTypeSignature(type)).append('.');
		}
		String methodsig = method.getSignature();
		String methodname = getMethodName(method);
		buffer.append(Signature.toString(dequalifySignature(methodsig), methodname, null, false, false));
		return buffer.toString();
	}

	/**
	 * Returns the signature of the method qualified with the given type with
	 * the type names qualified (if specified) and the return type appended, in
	 * the format of a content assist proposal.<br>
	 * <br>
	 * For example: <code>x.y.z.Clazz.mymethod() : Object</code>
	 *
	 * @param method the descriptor to get a formatted signature for
	 * @param qualifiedparams
	 * @param includereturn if the return type should be returned
	 * @return the given type qualified signature of the given method
	 * @throws CoreException if a lookup to the parent type of the given method
	 *             fails
	 */
	public static String getQualifiedMethodSignature(IMethodDescriptor method, boolean qualifiedparams, boolean includereturn) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		IReferenceTypeDescriptor type = method.getEnclosingType();
		if (type != null) {
			buffer.append(getQualifiedTypeSignature(type)).append('.');
		}
		String methodsig = method.getSignature();
		String methodname = getMethodName(method);
		buffer.append(Signature.toString(methodsig, methodname, null, qualifiedparams, false).replace('/', '.'));
		if (includereturn) {
			buffer.append(" : "); //$NON-NLS-1$
			buffer.append(Signature.toString(Signature.getReturnType(methodsig)).replace('/', '.'));
		}
		return buffer.toString();
	}

	/**
	 * Returns the de-qualified method signature
	 *
	 * @param method
	 * @param includereturn
	 * @return the de-qualified method signature
	 * @throws CoreException
	 */
	public static String getMethodSignature(IMethodDescriptor method, boolean includereturn) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		String methodsig = method.getSignature();
		String methodname = getMethodName(method);
		String dqsig = dequalifySignature(methodsig);
		buffer.append(Signature.toString(dqsig, methodname, null, false, false));
		if (includereturn) {
			buffer.append(" : "); //$NON-NLS-1$
			buffer.append(Signature.toString(Signature.getReturnType(dqsig)).replace('/', '.'));
		}
		return buffer.toString();
	}

	/**
	 * Returns the name to use for the method. If the method is a constructor,
	 * the enclosing type is loaded to get its simple name
	 *
	 * @param method
	 * @return the name for the method. If the method is a constructor the
	 *         simple name of the enclosing type is substituted.
	 * @throws CoreException
	 */
	public static String getMethodName(IApiMethod method) throws CoreException {
		String mname = method.getName();
		if ("<init>".equals(method.getName())) { //$NON-NLS-1$
			IApiType type = method.getEnclosingType();
			if (type != null) {
				return type.getSimpleName();
			}
		}
		return mname;
	}

	/**
	 * Returns the name to use for the method. If the method is a constructor,
	 * the enclosing type is loaded to get its simple name
	 *
	 * @param method
	 * @return the name for the method. If the method is a constructor the
	 *         simple name of the enclosing type is substituted.
	 * @throws CoreException
	 */
	public static String getMethodName(IMethodDescriptor method) throws CoreException {
		String mname = method.getName();
		if ("<init>".equals(method.getName())) { //$NON-NLS-1$
			IReferenceTypeDescriptor type = method.getEnclosingType();
			if (type != null) {
				return type.getName();
			}
		}
		return mname;
	}

	/**
	 * Returns the unqualified signature of the given {@link IApiField}
	 *
	 * @param field
	 * @return the unqualified signature of the given {@link IApiField}
	 */
	public static String getFieldSignature(IApiField field) {
		return field.getName();
	}

	/**
	 * Returns the type-qualified field signature
	 *
	 * @param field
	 * @return the type-qualified field signature
	 */
	public static String getQualifiedFieldSignature(IApiField field) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		IApiType type = field.getEnclosingType();
		if (type != null) {
			buffer.append(getQualifiedTypeSignature(type)).append('.');
		}
		buffer.append(field.getName());
		return buffer.toString();
	}

	/**
	 * Returns the type-qualified field signature
	 *
	 * @param field
	 * @return the type-qualified field signature
	 * @throws Core
	 */
	public static String getQualifiedFieldSignature(IFieldDescriptor field) {
		StringBuilder buffer = new StringBuilder();
		IReferenceTypeDescriptor type = field.getEnclosingType();
		if (type != null) {
			buffer.append(getQualifiedTypeSignature(type)).append('.');
		}
		buffer.append(field.getName());
		return buffer.toString();
	}

	/**
	 * Returns the type signature to use for displaying the given
	 * {@link IApiType}
	 *
	 * @param type
	 * @return the display signature to use for the given {@link IApiType}
	 */
	public static String getQualifiedTypeSignature(IApiType type) {
		return getTypeSignature(type.getSignature(), type.getGenericSignature(), true);
	}

	/**
	 * Returns the type signature to use for displaying the given
	 * {@link IReferenceTypeDescriptor}
	 *
	 * @param type
	 * @return the display signature to use for the given
	 *         {@link IReferenceTypeDescriptor}
	 */
	public static String getQualifiedTypeSignature(IReferenceTypeDescriptor type) {
		return getTypeSignature(type.getSignature(), type.getGenericSignature(), true);
	}

	/**
	 * Returns the de-qualified signature for the given {@link IApiType}
	 *
	 * @param type the type to get the signature for
	 * @return the de-qualified signature for the given {@link IApiType}
	 */
	public static String getTypeSignature(IApiType type) {
		return getTypeSignature(type.getSignature(), type.getGenericSignature(), false);
	}

	/**
	 * Returns the display-able representation of the given signature and
	 * generic signature
	 *
	 * @param signature
	 * @param genericsignature
	 * @param qualified
	 * @return
	 */
	public static String getTypeSignature(String signature, String genericsignature, boolean qualified) {
		StringBuilder buffer = new StringBuilder();
		String sig = signature.replace('/', '.');
		if (qualified == false) {
			sig = dequalifySignature(sig);
		}
		buffer.append(Signature.toString(sig.replace('$', '.')));
		if (genericsignature != null) {
			appendTypeParameters(buffer, Signature.getTypeParameters(genericsignature.replace('/', '.')));
		}
		return buffer.toString();
	}

	/**
	 * Returns the name of an anonymous or local type with all qualification
	 * removed. For example:
	 *
	 * <pre>
	 * <code>
	 *  Class$3inner --> inner
	 *  Class$3 --> null
	 * </code>
	 * </pre>
	 *
	 * @param name the name to resolve
	 * @return the name of an anonymous or local type with qualification removed
	 *         or <code>null</code> if the anonymous type has no name
	 */
	public static String getAnonymousTypeName(String name) {
		if (name != null) {
			int idx = name.lastIndexOf('$');
			if (idx > -1) {
				String num = name.substring(idx + 1, name.length());
				try {
					Integer.parseInt(num);
					return null;
				} catch (NumberFormatException nfe) {
					// ignore
				}
				for (int i = 0; i < name.length(); i++) {
					if (!Character.isDigit(num.charAt(i))) {
						return num.substring(i, num.length());
					}
				}
			}
		}
		return null;
	}

	/**
	 * Appends the given listing of type parameter names to the signature
	 * contained in the given buffer
	 *
	 * @param buffer
	 * @param parameters
	 */
	public static void appendTypeParameters(StringBuilder buffer, String[] parameters) {
		if (parameters == null) {
			return;
		}
		if (parameters.length == 0) {
			return;
		}
		buffer.append(getLT());
		for (int i = 0; i < parameters.length; i++) {
			if (i > 0) {
				buffer.append(getComma());
			}
			buffer.append(Signature.getTypeVariable(parameters[i]));
		}
		buffer.append(getGT());
	}

	/**
	 * Returns a comma and space used for displaying comma-separated lists in
	 * signatures.
	 *
	 * @return the string rendering for a comma and following space
	 */
	public static String getComma() {
		return ", "; //$NON-NLS-1$
	}

	/**
	 * Returns the string for rendering the '<code>&lt;</code>' character.
	 *
	 * @return the string for rendering '<code>&lt;</code>'
	 */
	public static String getLT() {
		return "<"; //$NON-NLS-1$
	}

	/**
	 * Returns the string for rendering the '<code>&gt;</code>' character.
	 *
	 * @return the string for rendering '<code>&gt;</code>'
	 */
	public static String getGT() {
		return ">"; //$NON-NLS-1$
	}

	/**
	 * Convert fully qualified signature to unqualified one. The descriptor can
	 * be dot or slashed based.
	 *
	 * @param descriptor the given descriptor to convert
	 * @return the converted signature
	 */
	public static String dequalifySignature(String signature) {
		StringBuilder buffer = new StringBuilder();
		char[] chars = signature.toCharArray();
		for (int i = 0, max = chars.length; i < max; i++) {
			char currentChar = chars[i];
			switch (currentChar) {
				case 'L': {
					if (chars[i + 1] != ';') {
						buffer.append('Q');
						// read reference type
						int lastDotPosition = i;
						i++;
						while (i < chars.length && currentChar != ';' && currentChar != '<') {
							switch (currentChar) {
								case '/':
								case '.':
									lastDotPosition = i;
									break;
								default:
									break;
							}
							i++;
							currentChar = chars[i];
						}
						buffer.append(chars, lastDotPosition + 1, i - lastDotPosition - 1);
						buffer.append(currentChar);
					} else {
						buffer.append(currentChar);
					}
					break;
				}
				case 'Q': {
					while (i < chars.length && currentChar != ';') {
						buffer.append(currentChar);
						currentChar = chars[++i];
					}
				}
				//$FALL-THROUGH$
				default: {
					buffer.append(currentChar);
				}
			}
		}
		return String.valueOf(buffer);
	}

	/**
	 * Creates a method signature from a specified {@link MethodDeclaration}
	 *
	 * @param node
	 * @param erased if true then perform type erasure
	 * @return the signature for the given method node or <code>null</code>
	 */
	public static String getMethodSignatureFromNode(MethodDeclaration node, boolean erased) {
		Assert.isNotNull(node);
		List<SingleVariableDeclaration> params = node.parameters();
		List<String> rparams = Signatures.getParametersTypeNames(params, erased);
		if (rparams.size() == params.size()) {
			if (!node.isConstructor()) {
				Type returnType = getType(node);
				if (returnType != null) {
					String rtype = Signatures.getTypeSignature(returnType, erased);
					if (rtype != null) {
						return Signature.createMethodSignature(rparams.toArray(new String[rparams.size()]), rtype);
					}
				}
			} else {
				Signatures.collectSyntheticParam(node, rparams);
				return Signature.createMethodSignature(rparams.toArray(new String[rparams.size()]), Signature.SIG_VOID);
			}
		}
		return null;
	}

	/**
	 * Returns the listing of the signatures of the parameters passed in, where
	 * the list elements are all {@link SingleVariableDeclaration}s and the
	 * elements in the returned list are of type {@link String}
	 *
	 * @param rawparams
	 * @param erased if true then perform type erasure
	 * @return a listing of signatures for the specified parameters
	 */
	private static List<String> getParametersTypeNames(List<SingleVariableDeclaration> rawparams, boolean erased) {
		List<String> rparams = new ArrayList<>(rawparams.size());
		String pname = null;
		for (SingleVariableDeclaration param : rawparams) {
			pname = Signatures.getTypeSignature(getType(param), erased);
			if (pname != null) {
				rparams.add(pname);
			}
		}
		return rparams;
	}

	/*
	 * This is called for SingleVariableDeclaration and MethodDeclaration
	 */
	private static Type getType(ASTNode node) {
		switch (node.getNodeType()) {
			case ASTNode.SINGLE_VARIABLE_DECLARATION: {
				SingleVariableDeclaration param = (SingleVariableDeclaration) node;
				Type type = param.getType();
				int extraDim = param.getExtraDimensions();

				if (extraDim == 0) {
					return type;
				}
				AST ast = type.getAST();
				type = (Type) ASTNode.copySubtree(ast, type);
				type = ast.newArrayType(type, extraDim);
				return type;
			}
			default: {
				// ASTNode.METHOD_DECLARATION
				MethodDeclaration methodDeclaration = (MethodDeclaration) node;
				Type type = methodDeclaration.getReturnType2();
				int extraDim = methodDeclaration.getExtraDimensions();

				if (extraDim == 0) {
					return type;
				}
				AST ast = type.getAST();
				type = (Type) ASTNode.copySubtree(ast, type);
				type = ast.newArrayType(type, extraDim);
				return type;
			}
		}
	}

	/**
	 * Returns the simple name of the type, by stripping off the last '.'
	 * segment and returning it. This method assumes that qualified type names
	 * are '.' separated. If the type specified is a package than an empty
	 * string is returned.
	 *
	 * @param qualifiedname the fully qualified name of a type, '.' separated
	 *            (e.g. a.b.c.Type)
	 * @return the simple name from the qualified name. For example if the
	 *         qualified name is a.b.c.Type this method will return Type
	 *         (stripping off the package qualification)
	 */
	public static String getTypeName(String qualifiedname) {
		int idx = qualifiedname.lastIndexOf('.');
		idx++;
		if (idx > 0) {
			return qualifiedname.substring(idx, qualifiedname.length());
		}
		// default package
		return qualifiedname;
	}

	/**
	 * Processes the signature for the given {@link Type}
	 *
	 * @param type the type to process
	 * @param erased if true then perform type erasure
	 * @return the signature for the type or <code>null</code> if one could not
	 *         be derived
	 */
	public static String getTypeSignature(Type type, boolean erased) {
		switch (type.getNodeType()) {
			case ASTNode.SIMPLE_TYPE: {
				String fullyQualifiedName = ((SimpleType) type).getName().getFullyQualifiedName();
				if (!fullyQualifiedName.contains(".") && erased) { //$NON-NLS-1$
					// check if generic parameter
					ASTNode parent = type.getParent();
					while (parent != null) {
						try {
							List<?> typeParameters = null;
							if (parent.getNodeType() == ASTNode.METHOD_DECLARATION) {
								MethodDeclaration md = (MethodDeclaration) parent;
								typeParameters = md.typeParameters();
							} else if (parent.getNodeType() == ASTNode.TYPE_DECLARATION) {
								TypeDeclaration td = (TypeDeclaration) parent;
								typeParameters = td.typeParameters();
							}
							if (typeParameters != null) {
								for (Object o : typeParameters) {
									if (o instanceof TypeParameter) {
										TypeParameter typeParameter = (TypeParameter) o;
										if (fullyQualifiedName.equals(typeParameter.getName().getFullyQualifiedName())) {
											if (typeParameter.typeBounds().isEmpty()) {
												return Signature.createTypeSignature("Object", false); //$NON-NLS-1$
											} else {
												// the erasure of a type
												// variable is the erasure of
												// its leftmost bound
												Type bound = (Type) typeParameter.typeBounds().get(0);
												return getTypeSignature(bound, erased);
											}
										}
									}
								}
							}
						} catch (UnsupportedOperationException e) {
							// thrown if code is JLS2
						}
						parent = parent.getParent();
					}

				}
				return Signature.createTypeSignature(fullyQualifiedName, false);
			}
			case ASTNode.QUALIFIED_TYPE: {
				return Signature.createTypeSignature(((QualifiedType) type).getName().getFullyQualifiedName(), false);
			}
			case ASTNode.ARRAY_TYPE: {
				ArrayType a = (ArrayType) type;
				return Signature.createArraySignature(getTypeSignature(a.getElementType(), erased), a.getDimensions());
			}
			case ASTNode.PARAMETERIZED_TYPE: {
				// we don't need to care about the other scoping types only the
				// base type
				return getTypeSignature(((ParameterizedType) type).getType(), erased);
			}
			case ASTNode.PRIMITIVE_TYPE: {
				return Signature.createTypeSignature(((PrimitiveType) type).getPrimitiveTypeCode().toString(), false);
			}
			default:
				break;
		}
		return null;
	}

	/**
	 * Returns if the given method signatures match, where method signatures are
	 * considered to match iff the return type, name and parameters are the
	 * same.
	 *
	 * @param signature a method signature
	 * @param signature2 a method signature
	 * @return true if the method signatures are equal, false otherwise
	 */
	public static boolean matchesSignatures(String signature, String signature2) {
		if (!matches(Signature.getReturnType(signature), Signature.getReturnType(signature2))) {
			return false;
		}
		String[] parameterTypes = Signature.getParameterTypes(signature);
		String[] parameterTypes2 = Signature.getParameterTypes(signature2);
		int length = parameterTypes.length;
		int length2 = parameterTypes2.length;
		if (length != length2) {
			return false;
		}
		for (int i = 0; i < length2; i++) {
			if (!matches(parameterTypes[i], parameterTypes2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if the given type signatures match. Type signatures are
	 * considered to match iff the type name and array count (if any) are the
	 * same
	 *
	 * @param type a type signature
	 * @param type2 a type signature
	 * @return true if the type signatures match, false otherwise
	 */
	public static boolean matches(String type, String type2) {
		if (Signature.getArrayCount(type) != Signature.getArrayCount(type2)) {
			return false;
		}
		String el1 = Signature.getElementType(type);
		String el2 = Signature.getElementType(type2);
		if (Signature.getTypeSignatureKind(el1) == Signature.TYPE_VARIABLE_SIGNATURE || Signature.getTypeSignatureKind(el2) == Signature.TYPE_VARIABLE_SIGNATURE) {
			// E.g., "TT;" vs "QT;"
			return el1.substring(1).equals(el2.substring(1));
		} else if (Signature.getTypeSignatureKind(el1) != Signature.getTypeSignatureKind(el2)) {
			return false;
		}
		// we know the type signature kinds are the same
		if (Signature.getTypeSignatureKind(el1) == Signature.WILDCARD_TYPE_SIGNATURE) {
			if (el1.charAt(0) != el2.charAt(0)) {
				return false;
			} else if (el1.charAt(0) == Signature.C_STAR) {
				// there should be nothing else to these signatures
				return el1.length() == 1 && el2.length() == 1;
			}
			// Strip off the Signature.C_EXTEND/C_SUPER
			el1 = el1.substring(1);
			el2 = el2.substring(1);
		}
		if (Signature.getTypeSignatureKind(el1) != Signature.BASE_TYPE_SIGNATURE && Signature.getTypeSignatureKind(el1) != Signature.CLASS_TYPE_SIGNATURE) {
			ApiPlugin.logErrorMessage("Signatures#matches: Unhandled signature kind: " + el1); //$NON-NLS-1$
			return false;
		}
		String[] typeargs1 = Signature.getTypeArguments(el1);
		String[] typeargs2 = Signature.getTypeArguments(el2);
		if (typeargs1.length != typeargs2.length) {
			return false;
		}
		String erased1 = Signature.getTypeErasure(el1);
		String erased2 = Signature.getTypeErasure(el2);
		if (el1.charAt(0) != Signature.C_UNRESOLVED && el2.charAt(0) != Signature.C_UNRESOLVED) {
			if (!erased1.equals(erased2)) {
				return false;
			}
		} else {
			// If an inner type, we want just the inner type name
			String signatureSimpleName = Signature.getSimpleName(Signature.getSignatureSimpleName(erased1));
			String signatureSimpleName2 = Signature.getSimpleName(Signature.getSignatureSimpleName(erased2));
			if (!signatureSimpleName.equals(signatureSimpleName2)) {
				return false;
			}
		}
		for (int i = 0; i < typeargs1.length; i++) {
			if (!matches(typeargs1[i], typeargs2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if the specified signature is qualified or not. Qualification is
	 * determined if there is a token in the signature the begins with an 'L'.
	 *
	 * @param signature
	 * @return true if the signature is qualified, false otherwise
	 */
	public static boolean isQualifiedSignature(String signature) {
		StringTokenizer tokenizer = new StringTokenizer(signature, "();IJCSBDFTZ!["); //$NON-NLS-1$
		if (tokenizer.hasMoreTokens()) {
			return tokenizer.nextToken().charAt(0) == 'L';
		}
		return false;
	}

	/**
	 * The type name is dot-separated
	 *
	 * @param typeName the given type name
	 * @return the package name for the given type name or an empty string if
	 *         none
	 */
	public static String getPackageName(String typeName) {
		int index = typeName.lastIndexOf('.');
		return index == -1 ? Util.DEFAULT_PACKAGE_NAME : typeName.substring(0, index);
	}

	/**
	 * Collects the synthetic parameter of the fully qualified name of the
	 * enclosing context for a constructor of an inner type
	 *
	 * @param method the constructor declaration
	 * @param rparams the listing of parameters to add to
	 */
	static void collectSyntheticParam(final MethodDeclaration method, List<String> rparams) {
		Assert.isNotNull(method);
		if (Signatures.isInTopLevelType(method)) {
			return;
		}
		ASTNode parent = method.getParent();
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) parent;
		if (Signatures.isStatic(type)) {
			// if the type is static it doesn't need the enclosing type
			return;
		}
		StringBuilder name = new StringBuilder();
		while (parent != null) {
			parent = parent.getParent();
			if (parent instanceof AbstractTypeDeclaration) {
				type = (AbstractTypeDeclaration) parent;
				name.insert(0, type.getName().getFullyQualifiedName());
				if (type.isMemberTypeDeclaration()) {
					name.insert(0, '$');
				}
				continue;
			}
			if (parent instanceof CompilationUnit) {
				CompilationUnit cunit = (CompilationUnit) parent;
				PackageDeclaration pdec = cunit.getPackage();
				if (pdec != null) {
					name.insert(0, '.');
					name.insert(0, cunit.getPackage().getName().getFullyQualifiedName());
				}
			}
		}
		name.insert(0, "L"); //$NON-NLS-1$
		name.append(';');
		if (name.length() > 2) {
			rparams.add(0, name.toString());
		}
	}

	/**
	 * Returns if the {@link AbstractTypeDeclaration} is static or not (has the
	 * static keyword or not)
	 *
	 * @param typeDeclaration
	 * @return true if it is static, false otherwise
	 */
	static boolean isStatic(AbstractTypeDeclaration typeDeclaration) {
		List<IExtendedModifier> modifiers = typeDeclaration.modifiers();
		if (modifiers.isEmpty()) {
			return false;
		}
		for (IExtendedModifier modifier : modifiers) {
			if (!modifier.isModifier()) {
				continue;
			}
			Modifier modifier2 = (Modifier) modifier;
			if (modifier2.isStatic()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determines if the given {@link MethodDeclaration} is present in a top
	 * level type
	 *
	 * @param method the given method
	 * @return true if the given {@link MethodDeclaration} is present in a top
	 *         level type, false otherwise
	 */
	static boolean isInTopLevelType(final MethodDeclaration method) {
		AbstractTypeDeclaration type = (AbstractTypeDeclaration) method.getParent();
		return type != null && type.isPackageMemberTypeDeclaration();
	}

	/**
	 * Returns the type name with any package qualification removed.<br>
	 * <br>
	 * For example:<br>
	 * <code>a.b.c.Type -&gt; Type</code><br>
	 * <code>a.b.c.Type$Member -&gt; Type$Member</code>
	 *
	 * @param referencedTypeName
	 * @return the type name with package qualification removed
	 */
	public static String getSimpleTypeName(String referencedTypeName) {
		int index = referencedTypeName.lastIndexOf('.');
		if (index == -1) {
			return referencedTypeName;
		}
		return referencedTypeName.substring(index + 1);
	}
}
