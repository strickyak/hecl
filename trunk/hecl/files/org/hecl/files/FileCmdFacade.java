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

package org.hecl.files;

import org.hecl.Command;
import org.hecl.HeclException;
import org.hecl.Interp;
import org.hecl.Thing;

/**
 * <code>FileCmdFacade</code> acts as a front-end to the commands in
 * the FileCmds class.
 *
 * @author <a href="mailto:davidw@dedasys.com">David N. Welton</a>
 * @version 1.0
 */
class FileCmdFacade implements Command {
    private int cmdtype;

    public FileCmdFacade(int cmd) {
	cmdtype = cmd;
    }

    public void cmdCode(Interp interp, Thing[] argv) throws HeclException {
	FileCmds.dispatch(cmdtype, interp, argv);
    }
}