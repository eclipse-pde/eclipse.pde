package org.eclipse.pde.ds.internal.annotations;

public enum ValidationErrorLevel {

	error,

	warning,

	none;

	public boolean isError() {
		return this == error;
	}

	public boolean isWarning() {
		return this == warning;
	}

	public boolean isNone() {
		return this == none;
	}
}
