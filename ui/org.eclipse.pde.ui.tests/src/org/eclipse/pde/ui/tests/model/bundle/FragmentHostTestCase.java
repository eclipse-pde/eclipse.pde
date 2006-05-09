package org.eclipse.pde.ui.tests.model.bundle;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.pde.internal.core.ibundle.IManifestHeader;
import org.eclipse.pde.internal.core.text.bundle.FragmentHostHeader;
import org.eclipse.text.edits.TextEdit;
import org.osgi.framework.Constants;

public class FragmentHostTestCase extends BundleModelTestCase {

	public FragmentHostTestCase() {
		super(Constants.FRAGMENT_HOST);
	}
	
	public static Test suite() {
		return new TestSuite(FragmentHostTestCase.class);
	}
	
	public void testAddFragmentHost() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		fDocument.set(buffer.toString());
		load(true);
		fModel.getBundle().setHeader(Constants.FRAGMENT_HOST, "org.eclipse.pde");
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);	
		assertEquals("Fragment-Host: org.eclipse.pde\n", header.write());
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(buffer.toString() + header.write(), fDocument.get());
	}
	
	public void testRemoveFragmentHost() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Fragment-Host: org.eclipse.pde\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);
		((FragmentHostHeader)header).setHostId("");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(4, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(3));
	}
	
	public void testChangeFragmentHost() throws Exception {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Manifest-Version: 1.0\n");
		buffer.append("Bundle-ManifestVersion: 2\n");
		buffer.append("Bundle-SymoblicName: com.example.xyz\n");
		buffer.append("Fragment-Host: org.eclipse.pde\n");
		fDocument.set(buffer.toString());
		load(true);
		
		IManifestHeader header = fModel.getBundle().getManifestHeader(Constants.FRAGMENT_HOST);
		assertNotNull(header);
		((FragmentHostHeader)header).setHostId("org.eclipse.jdt");
		
		TextEdit[] ops = fListener.getTextOperations();
		assertEquals(1, ops.length);
		
		ops[0].apply(fDocument);
		assertEquals(5, fDocument.getNumberOfLines());
		assertEquals(0, fDocument.getLineLength(4));
		
		int pos = fDocument.getLineOffset(3);
		int length = fDocument.getLineLength(3);
		
		StringBuffer expected = new StringBuffer("Fragment-Host: org.eclipse.jdt\n");
		assertEquals(expected.toString(), fDocument.get(pos, length));
	}

}
