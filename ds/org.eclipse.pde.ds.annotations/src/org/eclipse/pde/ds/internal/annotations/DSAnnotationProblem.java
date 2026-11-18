/*******************************************************************************
 * Copyright (c) 2013, 2016 Ecliptical Software Inc. and others.
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

import org.eclipse.jdt.core.compiler.CategorizedProblem;

public class DSAnnotationProblem extends CategorizedProblem {

	public static final String ATTRIBUTE_TYPE = "org.eclipse.pde.ds.annotations.problemKind"; //$NON-NLS-1$

	public static final String CONSTRUCTOR_NOT_PUBLIC = "constructorNotPublic"; //$NON-NLS-1$

	private String kind;
	
	private final boolean error;

	private final String message;

	private final String[] args;

	private char[] filename;

	private int sourceStart;

	private int sourceEnd;

	private int sourceLineNumber;

	public DSAnnotationProblem(boolean error, String message, String... args) {
		this.error = error;
		this.message = message;
		this.args = args;
	}

	@Override
	public boolean isError() {
		return error;
	}

	@Override
	public boolean isWarning() {
		return !error;
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public int getCategoryID() {
		return CAT_POTENTIAL_PROGRAMMING_PROBLEM;
	}

	@Override
	public String getMarkerType() {
		return "org.eclipse.pde.ds.annotations.problem"; //$NON-NLS-1$
	}

	public void setKind(String kind) {
		this.kind = kind;
	}

	@Override
	public String[] getExtraMarkerAttributeNames() {
		if (kind == null) {
			return super.getExtraMarkerAttributeNames();
		}

		return new String[] { ATTRIBUTE_TYPE };
	}

	@Override
	public Object[] getExtraMarkerAttributeValues() {
		if (kind == null) {
			return super.getExtraMarkerAttributeValues();
		}

		return new Object[] { kind };
	}

	@Override
	public char[] getOriginatingFileName() {
		return filename;
	}

	public void setOriginatingFileName(char[] filename) {
		this.filename = filename;
	}

	@Override
	public String[] getArguments() {
		return args;
	}

	@Override
	public String getMessage() {
		return message;
	}

	@Override
	public int getSourceStart() {
		return sourceStart;
	}

	@Override
	public void setSourceStart(int sourceStart) {
		this.sourceStart = sourceStart;
	}

	@Override
	public int getSourceEnd() {
		return sourceEnd;
	}

	@Override
	public void setSourceEnd(int sourceEnd) {
		this.sourceEnd = sourceEnd;
	}

	@Override
	public int getSourceLineNumber() {
		return sourceLineNumber;
	}

	@Override
	public void setSourceLineNumber(int sourceLineNumber) {
		this.sourceLineNumber = sourceLineNumber;
	}
}
