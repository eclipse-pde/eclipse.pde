package org.eclipse.pde.internal.core;

import java.io.DataOutputStream;
import java.io.IOException;

import org.eclipse.core.internal.plugins.RegistryCacheWriter;


public class PDERegistryCacheWriter extends RegistryCacheWriter {

	private long code;
	/**
	 * Constructor for PDERegistryCacheWriter.
	 */
	public PDERegistryCacheWriter(long code) {
		super();
		this.code = code;
	}
	
	/**
	 * @see org.eclipse.core.internal.plugins.RegistryCacheWriter#writeHeaderInformation(java.io.DataOutputStream)
	 */
	public void writeHeaderInformation(DataOutputStream out) {
		try {
			out.writeLong(code);
		} catch (IOException e) {
		}
	}


}
