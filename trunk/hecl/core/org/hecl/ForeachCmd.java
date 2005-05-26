/* Copyright 2004-2005 David N. Welton

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

import java.util.*;

/**
 * <code>ForeachCmd</code> implements the "foreach" command.
 * 
 * @author <a href="mailto:davidw@dedasys.com">David N. Welton </a>
 * @version 1.0
 */

/*
 * FIXME - could/should also handle multiple sets of variable/list, like:
 * foreach var $list othervar $otherlist
 */

class ForeachCmd implements Command {

    public void cmdCode(Interp interp, Thing[] argv) throws HeclException {
        Vector list = ListThing.get(argv[2]);
	if (list.size() == 0) {
	    return;
	}
        Vector varlist = ListThing.get(argv[1]);
        int i = 0;
        boolean end = false;
        while (true) {
            /*
             * This is for foreach loops where we have more than one variable to
             * set: foreach {m n} $somelist { code ... }
             */
            for (Enumeration e = varlist.elements(); e.hasMoreElements();) {
                if (end == true) {
                    throw new HeclException(
                            "Foreach argument list does not match list length");
                }

                Thing element = (Thing) list.elementAt(i);
                String varname = ((Thing) e.nextElement()).getStringRep();
                interp.setVar(varname, element);
                i++;
                if (i == list.size()) {
                    end = true;
                }
            }

            try {
                Eval.eval(interp, argv[3]);
            } catch (HeclException e) {
                if (e.code == HeclException.BREAK) {
                    break;
                } else if (e.code == HeclException.CONTINUE) {
                } else {
                    throw e;
                }
            }
            if (end == true)
                break;
        }
    }
}