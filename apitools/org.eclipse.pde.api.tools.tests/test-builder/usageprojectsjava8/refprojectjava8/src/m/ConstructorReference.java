/*******************************************************************************
 * Copyright (c) April 5, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package m;
import java.util.List;

public class ConstructorReference {
	  private String str;
		 
	    private List<String> strs;
	 
	    /**
	     * @noreference This constructor is not intended to be referenced by clients.
	     */
	    public ConstructorReference() {
	        this.str = "test1";
	    }
	    /**
	     * @noreference This constructor is not intended to be referenced by clients.
	     */
	    public ConstructorReference(String str) {
	        this.str = str;
	    }
	    /**
	     * @noreference This constructor is not intended to be referenced by clients.
	     */
	    public ConstructorReference(List<String> strs) {
	        this.strs = strs;
	    }
	 
	    public String getString()
	    {
	        return str;
	    }
	 
	    public List<String> getStrings()
	    {
	        return strs;
	    }


}
