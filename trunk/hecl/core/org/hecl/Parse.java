/* Copyright 2004-2006 David N. Welton

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
import java.util.Vector;

/**
 * The <code>Parse</code> class takes care of parsing Hecl scripts.
 *
 * @author <a href="mailto:davidw@dedasys.com">David N. Welton </a>
 * @version 1.0
 */

public class Parse {
    /* Used to build up commands. */
    protected Vector outList;

    protected ParseState state;

    protected Interp interp = null;

    protected String in;

    /* Used to build up individual Things with. */
    protected StringThing outBuf;
    /* Used to build up words (that might be GroupThings). */
    protected Vector outGroup;

    protected boolean parselist = false;

    /**
     * The <code>more</code> method returns a boolean value indicating whether
     * there is more text to be parsed or not.
     *
     * @return a <code>boolean</code> value
     */
    public boolean more() {
        return !state.eof;
    }

    /**
     * Creates a new <code>Parse</code> instance. Not actually used by
     * anything.
     *
     */
    public Parse() {
    }

    /**
     * Creates a new <code>Parse</code> instance.
     *
     * @param interp_in
     *            a <code>Interp</code> value
     * @param in_in
     *            a <code>String</code> value
     */
    public Parse(Interp interp_in, String in_in) {
        interp = interp_in;
        in = in_in;
        state = new ParseState(in);
    }

    /**
     * The <code>parse</code> method runs the parser on the text added by
     * creating a new Parse instance.
     *
     * @return a <code>Vector</code> value
     * @exception HeclException if an error occurs
     */
    public Vector parse() throws HeclException {
        outList = new Vector();
	newCurrent();
        state.eoc = false;

        parseLine(in, state);
        if (outList.size() > 0) {
            // System.out.println("outlist is : " + outList);
            return outList;
        }
        return null;
    }


    /**
     * <code>parseToCode</code> parses up a [] section as code.
     *
     * @return a <code>CodeThing</code> value
     * @exception HeclException if an error occurs
     */
    public CodeThing parseToCode() throws HeclException {
        CodeThing code = new CodeThing();
        int i = 0;
	Vector cmd;
	Thing[] argv;
	int cmdsize = 0;

	int beginline = 0;

        while (more()) {
	    beginline = state.lineno;
            cmd = parse();
            // System.out.println("CMD is " + cmd + " lineno is " + beginline);

            if (cmd == null) {
                continue;
            }
	    cmdsize = cmd.size();
	    if (cmdsize == 0) {
		continue;
	    }

            argv = new Thing[cmdsize];
            for (i = 0; i < cmdsize; i++) {
                argv[i] = (Thing) cmd.elementAt(i);
            }

            code.addStanza(interp, argv, beginline);
        }
        return code;
    }


    /**
     * The <code>newCurrent</code> method creates a new 'context' to
     * be added to.
     *
     * @exception HeclException if an error occurs
     */
    protected void newCurrent() throws HeclException {
	outGroup = new Vector();
	outBuf = new StringThing();
	outGroup.addElement(outBuf);
    }


    /**
     * The <code>addCurrent</code> method adds a new element to the command
     * parsed.
     *
     */
    protected void addCurrent() throws HeclException {
	/* If it's only got one element, don't make a groupthing out
	 * of it. */
	if (outGroup.size() == 1) {
	    outList.addElement(new Thing((RealThing)outGroup.elementAt(0)));
	} else {
	    Vector outv = new Vector();
	    for (Enumeration e = outGroup.elements(); e.hasMoreElements();) {
		outv.addElement(new Thing((RealThing)e.nextElement()));
	    }

	    outList.addElement(GroupThing.create(outv));
	}
	newCurrent();
    }

    /**
     * The <code>appendToCurrent</code> method adds a character to the group
     * object.
     *
     * @param ch a <code>char</code>
     */
    protected void appendToCurrent(char ch) throws HeclException {
	outBuf.append(ch);
    }

    /* Used internally. */
    private static final int DOLLAR = 0;
    private static final int COMMAND = 1;

    /**
     * The <code>addCommand</code> method adds a command to the current
     * output.
     *
     * @exception HeclException if an error occurs
     */
    protected void addCommand() throws HeclException {
	addSub(COMMAND);
    }

    /**
     * The <code>addDollar</code> method adds a $var lookup to the current
     * output.
     *
     * @exception HeclException if an error occurs
     */
    public void addDollar() throws HeclException {
	addSub(DOLLAR);
    }

    public void addSub(int type) throws HeclException {
        Vector savegroup = outGroup;
	StringThing savebuf = outBuf;
	newCurrent();
	if (type == DOLLAR) {
	    parseDollar(state);
	} else {
	    parseCommand(state);
	}

	for (Enumeration e = outGroup.elements(); e.hasMoreElements();) {
	    RealThing rt = (RealThing)e.nextElement();
	    savegroup.addElement(rt);
	}

	outBuf = new StringThing();
	savegroup.addElement(outBuf);
	outGroup = savegroup;
    }

    /**
     * The <code>parseLine</code> method is where parsing starts on a new
     * line.
     *
     * @param in a <code>String</code> value
     * @param state a <code>ParseState</code> value
     * @exception HeclException if an error occurs
     */
    public void parseLine(String in, ParseState state) throws HeclException {
        char ch;

        while (true) {
            ch = state.nextchar();
            if (state.done()) {
                return;
            }
            switch (ch) {
                case '#' :
                    parseComment(state);
                    return;
                case '\r' :
                case '\n' :
		    state.lineno ++;
                    return;
                case ';' :
		    return;
                case ' ' :
                case '	' :
                    continue;
                case '{' :
                    parseBlock(state);
                    break;
                case '[' :
                    parseCommand(state);
                    break;
                case '$' :
                    parseDollar(state);
                    break;
                case '"' :
                    parseText(state);
                    break;
		case '\\' :
		    if (!parseEscape(state)) {
			parseWord(state);
			break;
		    }
		    continue;
                default :
		    appendToCurrent(ch);
                    //		    state.rewind();
                    parseWord(state);
                    break;
            }
	    addCurrent();
        }
    }

    /**
     * The <code>parseComment</code> method keeps reading until a newline,
     * this 'eating' the comment.
     *
     * @param state a <code>ParseState</code> value
     */
    private void parseComment(ParseState state) {
        char ch;
        while (true) {
            ch = state.nextchar();
            if (((ch == '\n') || (ch == '\r')) || state.done()) {
		state.lineno ++;
                return;
            }
        }
    }

    /* Various bits and pieces utilized by parseDollar, below.  */

    private static String allowed = "_/@:-";
    private static String xchars="0123456789ABCDEFabcdef";
    private static boolean isLetter(char ch) {
	return ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'));
    }
    private static boolean isDigit(char ch) {
	return (ch >= '0' && ch <= '9');
    }
    private static boolean isXDigit(char ch) {
	return xchars.indexOf(ch) >= 0;
    }
    private static boolean isValidVarPunct(char ch) {
	return allowed.indexOf(ch) >= 0;
    }

    /**
     * The <code>parseDollar</code> method parses a $\ foo (or &foo)
     * variable. These can also be of the form $\ {foo} so that we can
     * separate them from any surrounding text.
     *
     * @param state a <code>ParseState</code> value
     * @param docopy a <code>boolean</code> value
     * @exception HeclException if an error occurs
     */
    private void parseDollar(ParseState state)
	throws HeclException {
	char ch;
	ch = state.nextchar();
	if (ch == '{') {
	    parseVarBlock(state);
	} else {
	    /* Variable names use this range here. */
	    while((isLetter(ch) || isDigit(ch) || isValidVarPunct(ch))) {
		appendToCurrent(ch);
		ch = state.nextchar();
	    }
	    if (!state.done()) {
		state.rewind();
	    }
	}
	/*
	 * System.out.println("parser vvvv"); Thing.printThing(argv[1]);
	 * System.out.println("parser ^^^^");
	 */
//	Thing t = new Thing(new SubstThing(outBuf.toString()));
//	outGroup.setElementAt(t, outGroup.size() - 1);

	outGroup.setElementAt(new SubstThing(outBuf.getStringRep()),
			      outGroup.size() - 1);
    }

    /**
     * <code>parseBlock</code> parses a {} block.
     *
     * @param state a <code>ParseState</code> value
     * @exception HeclException if an error occurs
     */
    protected void parseBlock(ParseState state) throws HeclException {
        parseBlockOrCommand(state, true, false);
    }

    protected void parseVarBlock(ParseState state) throws HeclException {
	parseBlockOrCommand(state, true, true);
    }

    /**
     * <code>parseCommand</code> parses a [] command.
     *
     * @param state a <code>ParseState</code> value
     * @exception HeclException if an error occurs
     */
    protected void parseCommand(ParseState state) throws HeclException {
        parseBlockOrCommand(state, false, false);
    }

    /**
     * <code>parseBlockOrCommand</code> is what parseCommand and parseBlock
     * use internally.
     *
     * @param state a <code>ParseState</code> value
     * @param block a <code>boolean</code> value
     * @exception HeclException if an error occurs
     */
    protected void parseBlockOrCommand(ParseState state, boolean block, boolean invar)
            throws HeclException {
        int level = 1;
        char ldelim, rdelim;
        char ch;

        if (block == true) {
            ldelim = '{';
            rdelim = '}';
        } else {
            ldelim = '[';
            rdelim = ']';
        }

        while (true) {
            ch = state.nextchar();
            if (state.done()) {
		throw new HeclException("Unbalanced " +
		    (block ? "{}" : "[]"), "PARSE_ERROR");
            }
            if (ch == ldelim) {
                level++;
            } else if (ch == rdelim) {
                level--;
            }

	    if (ch == '\n' || ch == '\r') {
		state.lineno ++;
	    }

            if (level == 0) {
                /* It's just a block, return it. */
                if (block || parselist) {
		    ch = state.nextchar();
		    /* If we are not dealing with a variable parse
		     * such as $\ {foo}, and the next character
		     * isn't a space, we have a problem. */
		    if (!invar && ch != ' ' && ch != '	' &&
			ch != '\n' && ch != '\r' && ch != ';' && ch != 0) {
			throw new HeclException("extra characters after close-brace");
		    }
		    state.rewind();
                    //return new Thing(out);
                    return;
                } else {
                    /* We parse it up for later consumption. */
		    Parse hp = new Parse(interp, outBuf.getStringRep());
                    CodeThing code = hp.parseToCode();
                    code.marksubst = true;
		    /* Replace outBuf in the vector. */
		    outGroup.setElementAt(code, outGroup.size() - 1);
                    return;
                }
            } else {
                appendToCurrent(ch);
            }
        }
    }

    /**
     * <code>parseText</code> parses a "string in quotes".
     *
     * @param state a <code>ParseState</code> value
     * @exception HeclException if an error occurs
     */
    protected void parseText(ParseState state) throws HeclException {
        char ch;
        while (true) {
            ch = state.nextchar();
            if (state.done()) {
                return;
            }
            switch (ch) {
                case '"' :
                    return;
                case '\\' :
		    parseEscape(state);
                    break;
                case '[' :
                    addCommand();
                    break;
                case '$' :
                    addDollar();
                    break;
                default :
                    appendToCurrent(ch);
                    break;
            }
        }
    }

    /**
     * <code>parseWord</code> parses a regular word not in quotes.
     *
     * @param state a <code>ParseState</code> value
     * @exception HeclException if an error occurs
     */
    protected void parseWord(ParseState state) throws HeclException {
        char ch;
        while (true) {
            ch = state.nextchar();
            if (state.done()) {
                return;
            }
            switch (ch) {
                case '[' :
                    addCommand();
                    break;
                case '$' :
                    addDollar();
                    break;
                case ' ' :
                case '	' :
                    return;
                case '\r' :
                case '\n' :
		    state.lineno ++;
		    /* Fall through on purpose. */
                case ';' :
                    state.eoc = true;
                    return;
                case '\\' :
		    if (parseEscape(state)) return;
                default :
                    appendToCurrent(ch);
                    break;
            }
        }
    }


    /**
     * The <code>parseEscape</code> method parses \n \t style escapes
     * - or just prints the next character.
     *
     * @param state a <code>ParseState</code> value
     * @return a <code>boolean</code> value
     * @exception HeclException if an error occurs
     */
    protected boolean parseEscape(ParseState state) throws HeclException {
	char ch = state.nextchar();
	if (state.done()) {
	    return true;
	}
	/* \n style escapes */
	switch (ch) {
	    case '\n':
		return true;
	    case 'n':
		appendToCurrent('\n');
		break;
	    case 't':
		appendToCurrent('\t');
		break;
	    case 'u':
		/* Add unicode sequences. */
		StringBuffer num = new StringBuffer("");
		char nextc;
		for (int i = 0; i < 4; i++) {
		    nextc = state.nextchar();
		    if (state.done()) {
			return true;
		    }
		    if (!isXDigit(nextc)) {
			state.rewind();
			break;
		    }
		    num.append(nextc);
		}
		try {
		    appendToCurrent((char)Integer.parseInt(num.toString(), 16));
		} catch (NumberFormatException e) {
		    throw new HeclException("illegal unicode escape: \\u" + num);
		}
		num = null;
		break;
	    default:
		appendToCurrent(ch);
	}
	return false;
    }
}
