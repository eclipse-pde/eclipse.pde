/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.text.bundle;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.internal.core.ibundle.IBundleModel;
import org.osgi.framework.Constants;

public class ExportPackageObject extends PackageObject {
    
    private static final String INTERNAL = "x-internal"; //$NON-NLS-1$
    private static final String FRIENDS = "x-friends"; //$NON-NLS-1$
    
    private static final long serialVersionUID = 1L;
    
    private TreeMap fFriends = new TreeMap();
     
    public ExportPackageObject(ManifestHeader header, ManifestElement element, String versionAttribute) {
        super(header, element, versionAttribute);
        processFriends();
    }
    
    public ExportPackageObject(ManifestHeader header, IPackageFragment fragment, String versionAttribute) {
        super(header, fragment.getElementName(), null, versionAttribute);
    }
    
    public ExportPackageObject(ManifestHeader header, String id, String version, String versionAttribute) {
    	super(header, id, version, versionAttribute);
    }
    
    protected void processFriends() {
    	String[] friends = getDirectives(FRIENDS);
    	if (friends != null) {
	        for (int i = 0; i < friends.length; i++) {
	            fFriends.put(friends[i], new PackageFriend(this, friends[i]));
	        }
    	}
    }

    public boolean isInternal() {
        return "true".equals(getDirective(INTERNAL)) || getDirective(FRIENDS) != null; //$NON-NLS-1$
    }
    
    public void removeInternalDirective() {
    	setDirective(INTERNAL, null);
    	((CompositeManifestHeader)fHeader).update(true);
    }

    public void setInternal(boolean internal) {
    	boolean old = isInternal();
    	if (!internal) {
    		setDirective(INTERNAL, null);
    		setDirective(FRIENDS, null);
    	} else {
    		if (fFriends.size() == 0)
    			setDirective(INTERNAL, "true"); //$NON-NLS-1$
    		else {
    			Iterator iter = fFriends.keySet().iterator();
    			while (iter.hasNext())
    				addDirective(FRIENDS, iter.next().toString());
    		}
    	}
    	fHeader.update();
    	firePropertyChanged(this, INTERNAL, Boolean.toString(old), Boolean.toString(internal));
    }
    
    public PackageFriend[] getFriends() {
        return (PackageFriend[])fFriends.values().toArray(new PackageFriend[fFriends.size()]);
    }
    
    public void addFriend(PackageFriend friend) {
        fFriends.put(friend.getName(), friend);
        addDirective(FRIENDS, friend.getName());
        setDirective(INTERNAL, null);
        fHeader.update();
        fireStructureChanged(friend, IModelChangedEvent.INSERT);        
    }
    
    public void removeFriend(PackageFriend friend) {
        fFriends.remove(friend.getName());
        setDirective(FRIENDS, null);
        if (fFriends.size() == 0)
        	setDirective(INTERNAL, "true"); //$NON-NLS-1$
        else {
	        Iterator iter = fFriends.keySet().iterator();
	        while (iter.hasNext())
	        	addDirective(FRIENDS, iter.next().toString());
        }
        fHeader.update();
        fireStructureChanged(friend, IModelChangedEvent.REMOVE);       
    }
    
    public boolean hasFriend(String name) {
        return fFriends.containsKey(name);
    }
    
    public boolean hasSameVisibility(ExportPackageObject object) {
    	if (object.isInternal() != isInternal())
    		return false;
    	
    	if (fFriends.size() != object.fFriends.size())
    		return false;
    	
    	Iterator iter = fFriends.keySet().iterator();
    	while (iter.hasNext()) {
    		if (!object.fFriends.containsKey(iter.next()))
    			return false;
    	}
    	return true;
    }
    
    public void setUsesDirective(String value) {
    	String oldValue = getUsesDirective();
    	setDirective(Constants.USES_DIRECTIVE, value);
    	fHeader.update();
    	firePropertyChanged(this, Constants.USES_DIRECTIVE, oldValue, value);
    }
    
    public String getUsesDirective() {
    	return getDirective(Constants.USES_DIRECTIVE);
    }
    
    protected void appendValuesToBuffer(StringBuffer sb, TreeMap table) {
    	if (table == null)
    		return;
    	Object usesValue = null;
    	// remove the Uses directive, we will make sure to put it at the end
    	if (table.containsKey(Constants.USES_DIRECTIVE))
    		usesValue = table.remove(Constants.USES_DIRECTIVE);
    	super.appendValuesToBuffer(sb, table);
    	if (usesValue != null) {
    		table.put(Constants.USES_DIRECTIVE, usesValue);
    		formatUsesDirective(sb, usesValue);
    	}
    }
    
    private void formatUsesDirective(StringBuffer sb, Object usesValue) {
    	StringTokenizer tokenizer = null;
		if (usesValue instanceof String) 
			tokenizer = new StringTokenizer((String)usesValue, ","); //$NON-NLS-1$
		boolean newLine = (tokenizer != null) ? tokenizer.countTokens() > 3 :
			((ArrayList)usesValue).size() > 3;
		String eol = getHeader().getLineLimiter();
		sb.append(';');
		if (newLine)
			sb.append(eol).append("  "); //$NON-NLS-1$
		sb.append(Constants.USES_DIRECTIVE);
		sb.append(":=\""); //$NON-NLS-1$
		if (tokenizer != null) 
			while (tokenizer.hasMoreTokens()) {
				sb.append(tokenizer.nextToken());
				if (tokenizer.hasMoreTokens()) {
					sb.append(',');
					if (newLine) 
						sb.append(eol).append("   "); //$NON-NLS-1$
				}
			}
		else {
			ArrayList list = ((ArrayList)usesValue);
			for (int i = 0; i < list.size(); i++) {
				if (i != 0) {
					sb.append(',');
					if (newLine) 
						sb.append(eol).append("   "); //$NON-NLS-1$
				}
				sb.append(list.get(i));
			}
		}
		sb.append("\""); //$NON-NLS-1$
    }

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.bundle.BundleObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		// Used for text transfers for copy, cut, paste operations
		writer.write(write());
	}    
    
	/**
	 * @param model
	 * @param header
	 * @param versionAttribute
	 */
	public void reconnect(IBundleModel model, ExportPackageHeader header, 
			String versionAttribute) {
		super.reconnect(model, header, versionAttribute);
		// Non-Transient Field:  Friends
		reconnectFriends();
	}

	/**
	 * 
	 */
	private void reconnectFriends() {
		// Get all the friends
		Iterator keys = fFriends.keySet().iterator();
		// Fill in appropriate transient field values for all friends
		while (keys.hasNext()) {
			String key = (String)keys.next();
			PackageFriend friend = (PackageFriend)fFriends.get(key);
			friend.reconnect(this);
		}
	}
	
}
