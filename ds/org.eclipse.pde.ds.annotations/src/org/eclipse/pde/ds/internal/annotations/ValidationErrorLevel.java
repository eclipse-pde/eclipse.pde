package org.eclipse.pde.ds.internal.annotations;

public enum ValidationErrorLevel {

	error,

	warning,

	ignore;

	public boolean isError() {
		return this == error;
	}

	public boolean isWarning() {
		return this == warning;
	}

	public boolean isIgnore() {
		return this == ignore;
	}
}
