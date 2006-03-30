/* Copyright 2006 David N. Welton

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.hecl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

class HashCmds extends Operator {
    public static final int HASH = 0;
    public static final int HGET = 1;
    public static final int HSET = 2;
    public static final int HKEYS = 3;
    public static final int HCLEAR = 4;
    public static final int HREMOVE = 5;


    public RealThing operate(int cmd, Interp interp, Thing[] argv) throws HeclException {
	Hashtable hash = cmd != 0 ? HashThing.get(argv[1]) : null;
	Thing result = null;
	    
	switch (cmd) {
	  case HASH:
	    result = HashThing.create(HashThing.get(argv[1]));
	    break;
	    
	  case HGET:
	    result = (Thing)hash.get(argv[2].toString());
	    break;
	    
	  case HSET:
	    result = argv[3];
	    hash.put(argv[2].toString(), result);
	    break;

	  case HKEYS:
	    Vector v = new Vector(hash.size());
	    Enumeration e = hash.keys();
	    while(e.hasMoreElements()) {
		v.addElement(new Thing((String)e.nextElement()));
	    }
	    return new ListThing(v);

	  case HCLEAR:
	    hash.clear();
	    result = argv[1];
	    break;

	  case HREMOVE:
	    Object o = hash.remove(argv[2].toString());
	    result = o != null ? (Thing)o : new Thing("");
	    break;

	  default:
	    throw new HeclException("Unknown hash command '"
				    + argv[0].toString() + "' with code '"
				    + cmd + "'.");
	}
	if(result != null)
	    interp.setResult(result);
	return null;
    }
    

    public static void load(Interp ip) throws HeclException {
	Operator.load(ip);
    }


    public static void unload(Interp ip) throws HeclException {
	Operator.unload(ip);
    }


    protected HashCmds(int cmdcode,int minargs,int maxargs) {
	super(cmdcode,minargs,maxargs);
    }


    static {
        cmdtable.put("hash", new HashCmds(HASH,1,1));
        cmdtable.put("hget", new HashCmds(HGET,2,2));
        cmdtable.put("hset", new HashCmds(HSET,3,3));
        cmdtable.put("hkeys", new HashCmds(HKEYS,1,1));
        cmdtable.put("hclear", new HashCmds(HCLEAR,1,1));
        cmdtable.put("hremove", new HashCmds(HREMOVE,2,2));
    }
}
