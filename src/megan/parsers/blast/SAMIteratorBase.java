/*
 *  Copyright (C) 2017 Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.parsers.blast;

import jloda.util.FileInputIterator;

import java.io.IOException;
import java.util.Comparator;

/**
 * base class
 * <p/>
 * Daniel Huson, 4.2015
 */
public class SAMIteratorBase {
    private final FileInputIterator iterator;
    private final int maxNumberOfMatchesPerRead;
    private int maxNumberOfErrors = 1000;
    private int numberOfErrors = 0;
    private String pushedBackLine;
    private boolean parseLongReads;

    /**
     * constructor
     *
     * @param fileName
     * @param maxNumberOfMatchesPerRead
     * @throws IOException
     */
    public SAMIteratorBase(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        iterator = new FileInputIterator(fileName);
        this.maxNumberOfMatchesPerRead = maxNumberOfMatchesPerRead;
    }

    /**
     * does string start with the given tag (allowing spaces inside tag to be missing in string)
     *
     * @param string
     * @param tag
     * @return
     */
    protected static boolean startsWith(String string, String tag) {
        return string.startsWith(tag) || (tag.contains(" ") && string.startsWith(tag.replaceAll(" ", "")));
    }

    /**
     * gets the next token following tag in aLine. Treats spaces in tag as match 0 or 1 spaces...
     *
     * @param aLine
     * @param tag
     * @return next token after tag
     */
    public static String getNextToken(String aLine, String tag) {
        int a = endOfTagMatch(aLine, tag);
        if (a >= 0) {
            while (a < aLine.length() && Character.isWhitespace(aLine.charAt(a)))
                a++;
            int b = a;
            while (b < aLine.length() && !Character.isWhitespace(aLine.charAt(b)))
                b++;
            return aLine.substring(a, b);
        }
        return "";
    }

    /**
     * gets the next token following tag1 and then tag2 in aLine. Treats spaces in tag as match 0 or 1 spaces...
     *
     * @param aLine
     * @param tag1
     * @return next token after tag
     */
    public static String getNextToken(String aLine, String tag1, String tag2) {
        int a = endOfTagMatch(aLine, tag1);
        if (a >= 0) {
            a = endOfTagMatch(aLine, a, tag2);
            while (a < aLine.length() && Character.isWhitespace(aLine.charAt(a)))
                a++;
            int b = a;
            while (b < aLine.length() && !Character.isWhitespace(aLine.charAt(b)))
                b++;
            return aLine.substring(a, b);
        }
        return "";
    }

    /**
     * gets the next token consisting only of letters, following tag1 and then tag2 in aLine. Treats spaces in tag as match 0 or 1 spaces...
     *
     * @param aLine
     * @param tag1
     * @return next token after tag
     */
    public static String getNextLetters(String aLine, String tag1, String tag2) {
        int a = endOfTagMatch(aLine, tag1);
        if (a >= 0) {
            a = endOfTagMatch(aLine, a, tag2);
            while (a < aLine.length() && Character.isWhitespace(aLine.charAt(a)))
                a++;
            int b = a;
            while (b < aLine.length() && Character.isLetter(aLine.charAt(b)))
                b++;
            return aLine.substring(a, b);
        }
        return "";
    }

    /**
     * matches tag to string (allowing spaces inside tag to be missing in string)
     *
     * @param string
     * @param tag
     * @return position after match and all trailing white space, or -1
     */
    private static int endOfTagMatch(String string, String tag) {
        return endOfTagMatch(string, 0, tag);

    }

    /**
     * matches tag to string (allowing spaces inside tag to be missing in string)
     *
     * @param string
     * @param fromIndex starting index in string
     * @param tag
     * @return position after match and all trailing white space, or -1
     */
    private static int endOfTagMatch(String string, int fromIndex, String tag) {
        int pos = string.indexOf(tag, fromIndex);
        if (pos != -1) {
            while (pos < string.length() && Character.isWhitespace(string.charAt(pos)))
                pos++;
            return pos + tag.length();
        }
        if (tag.contains(" ")) {
            tag = tag.replaceAll(" ", "");
            pos = string.indexOf(tag);
            if (pos != -1) {
                while (pos < string.length() && Character.isWhitespace(string.charAt(pos)))
                    pos++;
                return pos + tag.length();
            }
        }
        return -1;
    }

    public long getMaximumProgress() {
        return iterator.getMaximumProgress();
    }

    public long getProgress() {
        return iterator.getProgress();
    }

    /**
     * close the iterator
     *
     * @throws IOException
     */
    public void close() throws IOException {
        iterator.close();
    }

    /**
     * is there a next line?
     *
     * @return true, if next line available
     */
    protected boolean hasNextLine() {
        return pushedBackLine != null || iterator.hasNext();
    }

    /**
     * gets the next line
     *
     * @return next line
     */
    protected String nextLine() {
        if (pushedBackLine != null) {
            final String result = pushedBackLine;
            pushedBackLine = null;
            return result;
        } else
            return iterator.next();
    }

    /**
     * move to next line that starts with the given prefix
     *
     * @param prefix
     * @return line or null
     */
    protected String getNextLineStartsWith(String prefix) {
        while (hasNextLine()) {
            final String line = nextLine();
            if (line.startsWith(prefix))
                return line;
        }
        return null;
    }

    /**
     * move to next line that contains given infix
     *
     * @param infix
     * @return line or null
     */
    protected String getNextLineContains(String infix) {
        while (hasNextLine()) {
            final String line = nextLine();
            if (line.contains(infix))
                return line;
        }
        return null;
    }

    /**
     * moves to next query that starts with either of the given prefixes
     *
     * @return next line or null
     */
    protected String getNextLineStartsWith(String prefix1, String prefix2) {
        while (hasNextLine()) {
            final String line = nextLine();
            if (line.startsWith(prefix1) || line.startsWith(prefix2))
                return line;
        }
        return null;
    }

    /**
     * skips empty lines and returns the next non-empty one or null
     *
     * @return next line or null
     */
    protected String skipEmptyLines() {
        while (true) {
            if (hasNextLine()) {
                final String next = nextLine().trim();
                if (next.length() > 0)
                    return next;
            } else
                return null;
        }
    }

    /**
     * push back a line
     *
     * @param line
     */
    public void pushBackLine(String line) {
        if (pushedBackLine != null)
            System.err.println("Error: Push back line, but buffer not empty");
        pushedBackLine = line;
    }

    public long getLineNumber() {
        return iterator.getLineNumber();
    }

    public int getMaxNumberOfMatchesPerRead() {
        return maxNumberOfMatchesPerRead;
    }

    public int getMaxNumberOfErrors() {
        return maxNumberOfErrors;
    }

    public void setMaxNumberOfErrors(int maxNumberOfErrors) {
        this.maxNumberOfErrors = maxNumberOfErrors;
    }

    public int incrementNumberOfErrors() {
        return ++numberOfErrors;
    }

    /**
     * use this to sort matches
     */
    protected class Match implements Comparator<Match> {
        float bitScore;
        int id;
        String samLine;

        @Override
        public int compare(Match a, Match b) {
            if (a.bitScore > b.bitScore)
                return -1;
            else if (a.bitScore < b.bitScore)
                return 1;
            else if (a.id < b.id)
                return -1;
            else if (a.id > b.id)
                return 1;
            else
                return 0;
        }
    }

    /**
     * skip lines starting with #?
     *
     * @param skip
     */
    public void setSkipCommentLines(boolean skip) {
        iterator.setSkipCommentLines(skip);
    }

    /**
     * skip lines starting with #?
     *
     * @return
     */
    public boolean isSkipCommentLines() {
        return iterator.isSkipCommentLines();
    }

    public byte[] getQueryText() {
        return null;
    }

    public boolean isParseLongReads() {
        return parseLongReads;
    }

    public void setParseLongReads(boolean parseLongReads) {
        this.parseLongReads = parseLongReads;
    }
}
