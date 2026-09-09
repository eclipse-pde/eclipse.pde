/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.internal.model;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.lang.classfile.Annotation;
import java.lang.classfile.AnnotationValue;
import java.lang.classfile.Attributes;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.CodeElement;
import java.lang.classfile.CodeModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.InnerClassInfo;
import java.lang.classfile.attribute.InnerClassesAttribute;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.lang.classfile.instruction.LineNumber;
import java.lang.classfile.instruction.NewObjectInstruction;
import java.lang.classfile.instruction.NewReferenceArrayInstruction;
import java.lang.classfile.instruction.TypeCheckInstruction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.Flags;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.model.StubArchiveApiTypeContainer.ArchiveApiTypeRoot;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;

/**
 * Builds an API type structure from a class file, using the
 * {@code java.lang.classfile} API (JEP 484) instead of ASM.
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class TypeStructureBuilder {

	private static final String POLYMORPHIC_SIGNATURE_ANNOTATION = "Ljava/lang/invoke/MethodHandle$PolymorphicSignature;"; //$NON-NLS-1$

	private TypeStructureBuilder() {
		// no instances
	}

	private static String internalToQualified(String internalName) {
		return internalName.replace('/', '.');
	}

	private static int withDeprecatedFlag(int access, boolean deprecated) {
		if (deprecated) {
			return access | Flags.AccDeprecated;
		}
		return access;
	}

	/**
	 * Builds a type structure with the given .class file bytes in the specified
	 * API component.
	 *
	 * @param bytes class file bytes
	 * @param component originating API component
	 * @param file associated class file
	 */
	public static IApiType buildTypeStructure(byte[] bytes, IApiComponent component, IApiTypeRoot file) {
		ClassModel model;
		try {
			model = ClassFile.of().parse(bytes);
		} catch (IllegalArgumentException iae) {
			// thrown for bad bytecodes
			return logAndReturn(file, iae);
		} catch (ArrayIndexOutOfBoundsException e) {
			return logAndReturn(file, e);
		}

		String name = model.thisClass().asInternalName();
		String enclosingName = null;
		int index = name.lastIndexOf('$');
		if (index > -1) {
			enclosingName = internalToQualified(name.substring(0, index));
		}
		// TODO: inner types should be have enclosing type as parent instead of
		// component
		int access = withDeprecatedFlag(model.flags().flagsMask(), model.findAttribute(Attributes.deprecated()).isPresent());

		StringBuilder simpleSig = new StringBuilder();
		simpleSig.append('L').append(name).append(';');

		ApiType type = new ApiType(component, internalToQualified(name), simpleSig.toString(),
				model.findAttribute(Attributes.signature()).map(sig -> sig.signature().stringValue()).orElse(null),
				access, enclosingName, file);

		model.superclass().ifPresent(superClass -> type.setSuperclassName(internalToQualified(superClass.asInternalName())));

		List<ClassEntry> interfaces = model.interfaces();
		if (!interfaces.isEmpty()) {
			type.setSuperInterfaceNames(interfaces.stream().map(ClassEntry::asInternalName)
					.map(TypeStructureBuilder::internalToQualified).toArray(String[]::new));
		}

		model.findAttribute(Attributes.innerClasses()).ifPresent(ica -> visitInnerClasses(type, ica));
		model.findAttribute(Attributes.enclosingMethod()).ifPresent(ema -> type.setEnclosingMethodInfo(
				ema.enclosingMethodName().map(Utf8Entry::stringValue).orElse(null),
				ema.enclosingMethodType().map(Utf8Entry::stringValue).orElse(null)));

		for (FieldModel field : model.fields()) {
			visitField(type, field);
		}
		for (MethodModel method : model.methods()) {
			visitMethod(type, method);
		}

		return type;
	}

	private static void visitInnerClasses(ApiType type, InnerClassesAttribute innerClasses) {
		for (InnerClassInfo info : innerClasses.classes()) {
			String currentName = internalToQualified(info.innerClass().asInternalName());
			if (currentName.equals(type.getName())) {
				if (info.innerName().isEmpty()) {
					type.setAnonymous();
				} else if (info.outerClass().isEmpty()) {
					type.setLocal();
					type.setSimpleName(info.innerName().get().stringValue());
				}
			}
			if (info.outerClass().isPresent() && info.innerName().isPresent()) {
				// technically speaking innerName != null is not necessary, but this
				// is a workaround for some bogus synthetic types created by another
				// compiler
				String currentOuterName = internalToQualified(info.outerClass().get().asInternalName());
				if (currentOuterName.equals(type.getName())) {
					// this is a real type member defined in the descriptor (not
					// just a reference to a type member)
					type.addMemberType(currentName);
				} else if (currentName.equals(type.getName())) {
					type.setModifiers(info.flagsMask());
					type.setSimpleName(info.innerName().get().stringValue());
					type.setMemberType();
				}
			}
		}
	}

	private static void visitField(ApiType type, FieldModel field) {
		int access = withDeprecatedFlag(field.flags().flagsMask(), field.findAttribute(Attributes.deprecated()).isPresent());
		String signature = field.findAttribute(Attributes.signature()).map(sig -> sig.signature().stringValue()).orElse(null);
		Object value = field.findAttribute(Attributes.constantValue()).map(cv -> cv.constant().constantValue()).orElse(null);
		type.addField(field.fieldName().stringValue(), field.fieldType().stringValue(), signature, access, value);
	}

	private static void visitMethod(ApiType type, MethodModel method) {
		int access = withDeprecatedFlag(method.flags().flagsMask(), method.findAttribute(Attributes.deprecated()).isPresent());
		String[] exceptionNames = null;
		List<ClassEntry> exceptions = method.findAttribute(Attributes.exceptions()).map(ea -> ea.exceptions()).orElse(List.of());
		if (!exceptions.isEmpty()) {
			exceptionNames = exceptions.stream().map(ClassEntry::asInternalName).map(TypeStructureBuilder::internalToQualified)
					.toArray(String[]::new);
		}
		String signature = method.findAttribute(Attributes.signature()).map(sig -> sig.signature().stringValue()).orElse(null);
		ApiMethod apiMethod = type.addMethod(method.methodName().stringValue(), method.methodType().stringValue(), signature, access,
				exceptionNames);

		boolean polymorphic = method.findAttribute(Attributes.runtimeVisibleAnnotations())
				.map(rva -> rva.annotations().stream().anyMatch(TypeStructureBuilder::isPolymorphicSignature)).orElse(false);
		if (polymorphic) {
			// note: kept identical to the original ASM based implementation, which
			// also just calls this getter without using its result
			apiMethod.isPolymorphic();
		}

		method.findAttribute(Attributes.annotationDefault())
				.ifPresent(ada -> apiMethod.setDefaultValue(annotationValueToString(ada.defaultValue())));
	}

	private static boolean isPolymorphicSignature(Annotation annotation) {
		return POLYMORPHIC_SIGNATURE_ANNOTATION.equals(annotation.className().stringValue());
	}

	/**
	 * Converts an annotation default value into a textual representation,
	 * mirroring the flattening behavior of the previous ASM based
	 * {@code AnnotationDefaultVisitor}.
	 */
	private static String annotationValueToString(AnnotationValue value) {
		return switch (value) {
			case AnnotationValue.OfString v -> v.stringValue();
			case AnnotationValue.OfInt v -> Integer.toString(v.intValue());
			case AnnotationValue.OfLong v -> Long.toString(v.longValue());
			case AnnotationValue.OfFloat v -> Float.toString(v.floatValue());
			case AnnotationValue.OfDouble v -> Double.toString(v.doubleValue());
			case AnnotationValue.OfBoolean v -> Boolean.toString(v.booleanValue());
			case AnnotationValue.OfByte v -> Byte.toString(v.byteValue());
			case AnnotationValue.OfShort v -> Short.toString(v.shortValue());
			case AnnotationValue.OfChar v -> Character.toString(v.charValue());
			case AnnotationValue.OfClass v -> v.className().stringValue();
			case AnnotationValue.OfEnum v -> v.constantName().stringValue();
			case AnnotationValue.OfAnnotation v -> v.annotation().elements().stream()
					.map(e -> annotationValueToString(e.value())).collect(Collectors.joining(","));//$NON-NLS-1$
			case AnnotationValue.OfArray v -> v.values().stream().map(TypeStructureBuilder::annotationValueToString)
					.collect(Collectors.joining(","));//$NON-NLS-1$
			default -> value.toString();
		};
	}

	private static IApiType logAndReturn(IApiTypeRoot file, Exception e) {
		if (ApiPlugin.DEBUG_BUILDER) {
			ApiPlugin.log(Status.error(NLS.bind(Messages.TypeStructureBuilder_badClassFileEncountered, file.getTypeName()), e));
		}
		return null;
	}

	/**
	 * Scans the type's class file to find the method that encloses an
	 * anonymous/local type, and sets that information on the given type.
	 */
	public static void setEnclosingMethod(IApiType enclosingType, ApiType currentAnonymousLocalType) {
		IApiTypeRoot typeRoot = enclosingType.getTypeRoot();
		if (typeRoot instanceof AbstractApiTypeRoot abstractApiTypeRoot) {
			String typeName = currentAnonymousLocalType.getName().replace('.', '/');
			try {
				ClassModel model = ClassFile.of().parse(abstractApiTypeRoot.getContents());
				EnclosingMethodResult result = findEnclosingMethod(model, typeName);
				if (result != null) {
					currentAnonymousLocalType.setEnclosingMethodInfo(result.name(), result.signature());
				}
			} catch (ArrayIndexOutOfBoundsException | CoreException e) {
				// bytes could not be retrieved for abstractApiTypeRoot
				ApiPlugin.log(e);
			}
		}
	}

	private record EnclosingMethodResult(String name, String signature) {
	}

	/**
	 * Scans all eligible (non-abstract, non-native) methods, in order, for a
	 * reference to {@code typeName}, exactly as the previous ASM based
	 * {@code EnclosingMethodSetter}/{@code TypeNameFinder} did.
	 */
	private static EnclosingMethodResult findEnclosingMethod(ClassModel model, String typeName) {
		for (MethodModel method : model.methods()) {
			String name = method.methodName().stringValue();
			if ("<clinit>".equals(name)) { //$NON-NLS-1$
				continue;
			}
			int access = method.flags().flagsMask();
			if ((access & (ClassFile.ACC_ABSTRACT | ClassFile.ACC_NATIVE)) != 0) {
				continue;
			}
			CodeModel code = method.code().orElse(null);
			if (code == null) {
				continue;
			}
			String signature = method.findAttribute(Attributes.signature()).map(sig -> sig.signature().stringValue())
					.orElse(method.methodType().stringValue());

			boolean isConstructor = "<init>".equals(name); //$NON-NLS-1$
			TypeNameFinder finder = new TypeNameFinder(typeName, isConstructor);
			for (CodeElement element : code) {
				finder.accept(element);
			}
			if (finder.finish()) {
				return new EnclosingMethodResult(name, signature);
			}
			// not found (or, for a constructor, the match fell outside the
			// constructor's line number bounds): keep scanning the next method
		}
		return null;
	}

	/**
	 * Scans a method body for a reference to a given type via a {@code new},
	 * {@code anewarray}, {@code checkcast} or {@code instanceof} instruction.
	 */
	private static final class TypeNameFinder {
		private final String typeName;
		private final boolean constructor;
		private boolean found;
		private int lineNumberStart = -1;
		private int currentLineNumber = -1;
		private int matchingLineNumber = -1;

		TypeNameFinder(String typeName, boolean constructor) {
			this.typeName = typeName;
			this.constructor = constructor;
		}

		void accept(CodeElement element) {
			switch (element) {
				case LineNumber ln -> {
					if (currentLineNumber == -1) {
						lineNumberStart = ln.line();
					}
					currentLineNumber = ln.line();
				}
				case NewObjectInstruction insn -> checkType(insn.className());
				case NewReferenceArrayInstruction insn -> checkType(insn.componentType());
				case TypeCheckInstruction insn -> checkType(insn.type());
				default -> {
					// not relevant for enclosing method detection
				}
			}
		}

		private void checkType(ClassEntry entry) {
			if (!found && typeName.equals(entry.asInternalName())) {
				matchingLineNumber = currentLineNumber;
				found = true;
			}
		}

		boolean finish() {
			if (found && constructor && (matchingLineNumber < lineNumberStart || matchingLineNumber > currentLineNumber)) {
				found = false;
			}
			return found;
		}
	}

	/**
	 * Builds a type structure with the given .class file bytes in the specified
	 * API component.
	 */
	public static IApiType buildStubTypeStructure(byte[] contents, IApiComponent apiComponent, ArchiveApiTypeRoot archiveApiTypeRoot) {
		// decode the byte[]
		ApiType type = null;
		try (DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(contents))) {
			Map<Integer, String> pool = new HashMap<>();
			short currentVersion = inputStream.readShort(); // read file version
															// (for now there is
															// only one version)
			short poolSize = inputStream.readShort();
			for (int i = 0; i < poolSize; i++) {
				String readUtf = inputStream.readUTF();
				int index = inputStream.readShort();
				pool.put(Integer.valueOf(index), readUtf);
			}
			int access = 0;
			// access flag was added in version 2 of the stub format
			if (currentVersion >= 2) {
				access = inputStream.readChar();
			}
			int classIndex = inputStream.readShort();
			String name = pool.get(Integer.valueOf(classIndex));
			StringBuilder simpleSig = new StringBuilder();
			simpleSig.append('L');
			simpleSig.append(name);
			simpleSig.append(';');
			type = new ApiType(apiComponent, name.replace('/', '.'), simpleSig.toString(), null, access, null, archiveApiTypeRoot);
			int superclassNameIndex = inputStream.readShort();
			if (superclassNameIndex != -1) {
				String superclassName = pool.get(Integer.valueOf(superclassNameIndex));
				type.setSuperclassName(superclassName.replace('/', '.'));
			}
			int interfacesLength = inputStream.readShort();
			if (interfacesLength != 0) {
				String[] names = new String[interfacesLength];
				for (int i = 0; i < names.length; i++) {
					String interfaceName = pool.get(Integer.valueOf(inputStream.readShort()));
					names[i] = interfaceName.replace('/', '.');
				}
				type.setSuperInterfaceNames(names);
			}
			int fieldsLength = inputStream.readShort();
			for (int i = 0; i < fieldsLength; i++) {
				String fieldName = pool.get(Integer.valueOf(inputStream.readShort()));
				type.addField(fieldName, null, null, 0, null);
			}
			int methodsLength = inputStream.readShort();
			for (int i = 0; i < methodsLength; i++) {
				int isPolymorphic = 0;
				String methodSelector = pool.get(Integer.valueOf(inputStream.readShort()));
				String methodSignature = pool.get(Integer.valueOf(inputStream.readShort()));
				if (currentVersion == 3) {
					isPolymorphic = inputStream.readByte();
				}
				type.addMethod(methodSelector, methodSignature, null, isPolymorphic == 1 ? ApiMethod.Polymorphic : 0, null);
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		return type;
	}
}
