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
package org.eclipse.pde.api.tools.internal.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;

/**
 * This class contains utility methods for creating and working with 
 * Java element signatures.
 * 
 * @since 1.0.0
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public final class Signatures {
	
	/**
	 * Constructor
	 */
	private Signatures() {}
	
	/**
	 * Processes the method name. In the event it is a constructor the simple name of
	 * the enclosing type is returned
	 * @param type
	 * @param methodname
	 * @return
	 * @throws CoreException
	 */
	public static String processMethodName(IApiType type, String methodname) throws CoreException {
		if("<init>".equals(methodname)) { //$NON-NLS-1$
			return type.getSimpleName();
		}
		return methodname;
	}
	
	/**
	 * Collects which signature to use and de-qualifies it. If there is a generic signature
	 * it is returned, otherwise the standard signature is used
	 * @param method
	 * @return the de-qualified signature for the method
	 */
	public static String processMethodSignature(IApiMethod method) {
		String signature = method.getGenericSignature();
		if(signature == null) {
			signature = method.getSignature();
		}
		return dequalifySignature(signature);
	}
	
	/**
	 * Returns the signature to use to display this {@link IApiMethod}.
	 * This method will load the enclosing type to qualify the method signature.
	 * @param method
	 * @return the display signature to use for this {@link IApiMethod}
	 */
	public static String getMethodSignature(IApiMethod method) {
		StringBuffer buffer = new StringBuffer();
		try {
			IApiType type = method.getEnclosingType();
			buffer.append(getTypeSignature(type)).append('.');
			String methodsig = method.getGenericSignature();
			if(methodsig == null) {
				methodsig = method.getSignature();
			}
			String methodname = processMethodName(type, method.getName());
			return Signature.toString(Signatures.dequalifySignature(methodsig), methodname, null, false, false);
		}
		catch(CoreException ce) {}
		return buffer.toString();
	}
	
	/**
	 * Returns the signature of the method qualified with the given type
	 * @param type
	 * @param method
	 * @return the given type qualified signature of the given method
	 */
	public static String getQualifiedMethodSignature(IApiType type, IApiMethod method) {
		StringBuffer buffer = new StringBuffer();
		try {
			buffer.append(getTypeSignature(type)).append('.');
			String methodsig = method.getGenericSignature();
			if(methodsig == null) {
				methodsig = method.getSignature();
			}
			String methodname = processMethodName(type, method.getName());
			buffer.append(Signature.toString(Signatures.dequalifySignature(methodsig), methodname, null, false, false));
		}
		catch(CoreException ce) {}
		return buffer.toString();
	}
	
	/**
	 * Returns the type signature to use for displaying the given {@link IApiType}
	 * @param type
	 * @return the display signature to use for the given {@link IApiType}
	 */
	public static String getTypeSignature(IApiType type) {
		StringBuffer buffer = new StringBuffer();
		String sig = type.getGenericSignature();
		if(sig != null) {
			buffer.append(Signature.toString(sig));
		}
		else {
			buffer.append(type.getName());
		}
		return buffer.toString();
	}

	/**
	 * Convert fully qualified signature to unqualified one.
	 * The descriptor can be dot or slashed based.
	 * 
	 * @param descriptor the given descriptor to convert
	 * @return the converted signature
	 */
	public static String dequalifySignature(String signature) {
		StringBuffer buffer = new StringBuffer();
		char[] chars = signature.toCharArray();
		for (int i = 0, max = chars.length; i < max; i++) {
			char currentChar = chars[i];
			switch(currentChar) {
				case 'L' : {
					buffer.append('Q');
					// read reference type
					int lastDotPosition = i;
					i++;
					while(i < chars.length && currentChar != ';' && currentChar != '<' ) {
						switch(currentChar) {
							case '/' :
							case '.' :
								lastDotPosition = i;
								break;
						}
						i++;
						currentChar = chars[i];
					}
					buffer.append(chars, lastDotPosition + 1, i - lastDotPosition - 1);
					buffer.append(currentChar);
					break;
				}
				case 'Q': {
					while(i < chars.length && currentChar != ';') {
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
	 * @param node
	 * @return the signature for the given method node or <code>null</code>
	 */
	public static String getMethodSignatureFromNode(MethodDeclaration node) {
		Assert.isNotNull(node);
		List params = node.parameters();
		List rparams = Signatures.getParametersTypeNames(params);
		if(rparams.size() == params.size()) {
			if(!node.isConstructor()) {
				Type returnType = node.getReturnType2();
				if (returnType != null) {
					String rtype = Signatures.getTypeSignature(returnType);
					if(rtype != null) {
						return Signature.createMethodSignature((String[]) rparams.toArray(new String[rparams.size()]), rtype);
					}
				}
			}
			else {
				Util.collectSyntheticParam(node, rparams);
				return Signature.createMethodSignature((String[]) rparams.toArray(new String[rparams.size()]), Signature.SIG_VOID);
			}
		}
		return null;
	}

	/**
	 * Returns the listing of the signatures of the parameters passed in
	 * @param rawparams
	 * @return a listing of signatures for the specified parameters
	 */
	public static List getParametersTypeNames(List rawparams) {
		List rparams = new ArrayList(rawparams.size());
		SingleVariableDeclaration param = null;
		String pname = null;
		for(Iterator iter = rawparams.iterator(); iter.hasNext();) {
			param = (SingleVariableDeclaration) iter.next();
			pname = Signatures.getTypeSignature(param.getType());
			if(pname != null) {
				rparams.add(pname);
			}
		}
		return rparams;
	}

	/**
	 * Returns the simple name of the type, by stripping off the last '.' segment and returning it.
	 * This method assumes that qualified type names are '.' separated. If the type specified is a package
	 * than an empty string is returned.
	 * @param qualifiedname the fully qualified name of a type, '.' separated (e.g. a.b.c.Type)
	 * @return the simple name from the qualified name. For example if the qualified name is a.b.c.Type this method 
	 * will return Type (stripping off the package qualification)
	 */
	public static String getTypeName(String qualifiedname) {
		int idx = qualifiedname.lastIndexOf('.');
		idx++;
		if(idx > 0) {
			return qualifiedname.substring(idx, qualifiedname.length());
		}
		// default package
		return qualifiedname;
	}

	/**
	 * Processes the signature for the given {@link Type}
	 * @param type the type to process
	 * @return the signature for the type or <code>null</code> if one could not be 
	 * derived
	 */
	public static String getTypeSignature(Type type) {
		switch(type.getNodeType()) {
		case ASTNode.SIMPLE_TYPE: {
			return Signature.createTypeSignature(((SimpleType) type).getName().getFullyQualifiedName(), false);
		}
		case ASTNode.QUALIFIED_TYPE: {
			return Signature.createTypeSignature(((QualifiedType)type).getName().getFullyQualifiedName(), false);
		}
		case ASTNode.ARRAY_TYPE: {
			ArrayType a = (ArrayType) type;
			return Signature.createArraySignature(getTypeSignature(a.getElementType()), a.getDimensions());
		}
		case ASTNode.PARAMETERIZED_TYPE: {
			//we don't need to care about the other scoping types only the base type
			return getTypeSignature(((ParameterizedType) type).getType());
		}
		case ASTNode.PRIMITIVE_TYPE: {
			return Signature.createTypeSignature(((PrimitiveType)type).getPrimitiveTypeCode().toString(), false);
		}
		}
		return null;
	}

	/**
	 * Returns if the given signatures match. Where signatures are considered to match
	 * iff the return type, name and parameters are the same.
	 * @param signature
	 * @param signature2
	 * @return true if the signatures are equal, false otherwise
	 */
	public static boolean matchesSignatures(String signature, String signature2) {
		if (!matches(Signature.getReturnType(signature), Signature.getReturnType(signature2))) {
			return false;
		}
		String[] parameterTypes = Signature.getParameterTypes(signature);
		String[] parameterTypes2 = Signature.getParameterTypes(signature2);
		int length = parameterTypes.length;
		int length2 = parameterTypes2.length;
		if (length != length2) return false;
		for (int i = 0; i < length2; i++) {
			if (!matches(parameterTypes[i], parameterTypes2[i])) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if the two types match. Types are considered to match 
	 * iff the type name and array count (if any) are the same
	 * @param type
	 * @param type2
	 * @return true if the type names match, false otherwise
	 */
	private static boolean matches(String type, String type2) {
		if (Signature.getArrayCount(type) == Signature.getArrayCount(type2)) {
			String el1 = Signature.getElementType(type);
			String el2 = Signature.getElementType(type2);
			String signatureSimpleName = Signature.getSignatureSimpleName(el1);
			String signatureSimpleName2 = Signature.getSignatureSimpleName(el2);
			if (signatureSimpleName.equals(signatureSimpleName2)) {
				return true;
			}
			int index = signatureSimpleName2.lastIndexOf('.');
			if (index != -1) {
				// the right side is a member type
				return signatureSimpleName.equals(signatureSimpleName2.subSequence(index + 1, signatureSimpleName2.length()));
			}
		}
		return false;
	}
}
