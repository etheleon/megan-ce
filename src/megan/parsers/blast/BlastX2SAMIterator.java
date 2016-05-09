/*
 *  Copyright (C) 2016 Daniel H. Huson
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

import jloda.util.Basic;
import megan.util.BlastXTextFileFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.TreeSet;


/**
 * parses a blastx files into SAM format
 * Daniel Huson, 4.2015
 */
public class BlastX2SAMIterator extends SAMIteratorBase implements ISAMIterator {
    public final static String NEW_QUERY = "Query=";
    public final static String NEW_MATCH = ">";
    public final static String QUERY = "Query";
    public final static String SUBJECT = "Sbjct";
    public final static String SCORE = "Score";
    public final static String EXPECT = "Expect";
    public final static String LENGTH = "Length";
    public final static String IDENTITIES = "Identities";
    public final static String FRAME = "Frame";
    public final static String EQUALS = "=";

    private byte[] matchesText = new byte[10000];
    private int matchesTextLength = 0;

    private final boolean blastPMode;

    private final ArrayList<String> refHeaderLines = new ArrayList<>(1000);

    private TreeSet<Match> matches = new TreeSet<>(new Match());

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    public BlastX2SAMIterator(String fileName, int maxNumberOfMatchesPerRead) throws IOException {
        this(fileName, maxNumberOfMatchesPerRead, false);
        if (!BlastXTextFileFilter.getInstance().accept(fileName)) {
            close();
            throw new IOException("File not a BLASTX file in text format: " + fileName);
        }
    }

    /**
     * constructor
     *
     * @param fileName
     * @throws IOException
     */
    protected BlastX2SAMIterator(String fileName, int maxNumberOfMatchesPerRead, boolean blastPMode) throws IOException {
        super(fileName, maxNumberOfMatchesPerRead);
        this.blastPMode = blastPMode;
    }

    /**
     * is there more data?
     *
     * @return true, if more data available
     */
    @Override
    public boolean hasNext() {
        return hasNextLine();
    }

    /**
     * gets the next matches
     *
     * @return number of matches
     */
    public int next() {
        matchesTextLength = 0;

        String queryLine = getNextLineStartsWith(NEW_QUERY);
        if (queryLine == null)
            return -1; // at end of file

        final String queryName = getNextToken(queryLine, NEW_QUERY);

        int matchId = 0; // used to distinguish between matches when sorting
        matches.clear();

        // get all matches for given query:
        try {
            while (hasNextLine()) {
                // move to next match or next query:
                String line = getNextLineStartsWith(NEW_QUERY, NEW_MATCH);

                if (line == null)// at end of file
                    break;

                if (line.startsWith(NEW_QUERY)) { // at start of next query
                    pushBackLine(line);
                    break;
                }

                // line is at start of new match
                // collect all the reference header lines:
                refHeaderLines.clear();
                while (true) {
                    if (startsWith(line, LENGTH))
                        break;
                    else
                        refHeaderLines.add(line.replaceAll("\\s+", " "));
                    line = nextLine().trim();
                }
                final int referenceLength = Basic.parseInt(getNextToken(line, LENGTH, EQUALS));
                final String refName = Basic.swallowLeadingGreaterSign(Basic.toString(refHeaderLines, " "));
                line = skipEmptyLines();
                float bitScore = Basic.parseFloat(getNextToken(line, SCORE, EQUALS));
                int rawScore = Basic.parseInt(getNextToken(line, "("));
                float expect = Basic.parseFloat(getNextToken(line, EXPECT, EQUALS)); // usually Expect = but can also be Expect(2)=
                line = nextLine();
                float percentIdentities = Basic.parseFloat(getNextToken(line, IDENTITIES, "("));
                int frame;
                if (blastPMode) {
                    frame = 0;
                } else {
                    line = nextLine();
                    frame = Basic.parseInt(getNextToken(line, FRAME, EQUALS));
                }
                String[] queryLineTokens = getNextLineStartsWith(QUERY).split("\\s+"); // split on white space
                int queryStart = Basic.parseInt(queryLineTokens[1]);
                StringBuilder queryBuf = new StringBuilder();
                queryBuf.append(queryLineTokens[2]);
                int queryEnd = Basic.parseInt(queryLineTokens[3]);

                if (!hasNextLine())
                    break;
                nextLine(); // skip middle line
                String[] subjectLineTokens = getNextLineStartsWith(SUBJECT).split("\\s+");
                int subjStart = Basic.parseInt(subjectLineTokens[1]);
                StringBuilder subjBuf = new StringBuilder();
                subjBuf.append(subjectLineTokens[2]);
                int subjEnd = Basic.parseInt(subjectLineTokens[3]);

                // if match is broken over multiple lines, collect all parts of match
                while (hasNextLine()) {
                    line = skipEmptyLines();
                    if (line == null)
                        break; // at EOF...
                    if (line.startsWith(NEW_QUERY)) { // at new query
                        pushBackLine(line);
                        break;
                    } else if (line.startsWith(NEW_MATCH)) { // start of new match
                        pushBackLine(line);
                        break;
                    } else if (line.startsWith(SCORE)) { // there is another match to the same query, skip it
                        pushBackLine(getNextLineStartsWith(NEW_QUERY));
                    } else if (line.startsWith(QUERY)) { // match continues...
                        queryLineTokens = line.split("\\s+");
                        queryBuf.append(queryLineTokens[2]);
                        queryEnd = Basic.parseInt(queryLineTokens[3]);
                        subjectLineTokens = getNextLineStartsWith(SUBJECT).split("\\s+");
                        subjBuf.append(subjectLineTokens[2]);
                        subjEnd = Basic.parseInt(subjectLineTokens[3]);
                    }
                }

                if (matches.size() < getMaxNumberOfMatchesPerRead() || bitScore > matches.last().bitScore) {
                    Match match = new Match();
                    match.bitScore = bitScore;
                    match.id = matchId++;
                    match.samLine = makeSAM(queryName, refName, referenceLength, bitScore, expect, rawScore, percentIdentities, frame, queryStart, queryEnd, subjStart, subjEnd, queryBuf.toString(), subjBuf.toString());
                    matches.add(match);
                    if (matches.size() > getMaxNumberOfMatchesPerRead())
                        matches.remove(matches.last());
                }
            }
        } catch (Exception ex) {
            System.err.println("Error parsing file near line: " + getLineNumber() + ": " + ex.getMessage());
            if (incrementNumberOfErrors() >= getMaxNumberOfErrors())
                throw new RuntimeException("Too many errors");
        }

        if (matches.size() == 0) { // no matches, so return query name only
            if (queryName.length() > matchesText.length) {
                matchesText = new byte[2 * queryName.length()];
            }
            for (int i = 0; i < queryName.length(); i++)
                matchesText[matchesTextLength++] = (byte) queryName.charAt(i);
            matchesText[matchesTextLength++] = '\n';
            return 0;
        } else {
            for (Match match : matches) {
                byte[] bytes = match.samLine.getBytes();
                if (matchesTextLength + bytes.length + 1 >= matchesText.length) {
                    byte[] tmp = new byte[2 * (matchesTextLength + bytes.length + 1)];
                    System.arraycopy(matchesText, 0, tmp, 0, matchesTextLength);
                    matchesText = tmp;
                }
                System.arraycopy(bytes, 0, matchesText, matchesTextLength, bytes.length);
                matchesTextLength += bytes.length;
                matchesText[matchesTextLength++] = '\n';
            }
            return matches.size();
        }
    }

    /**
     * gets the matches text
     *
     * @return matches text
     */
    @Override
    public byte[] getMatchesText() {
        return matchesText;
    }

    /**
     * length of matches text
     *
     * @return length of text
     */
    @Override
    public int getMatchesTextLength() {
        return matchesTextLength;
    }

    /**
     * make a SAM line
     */
    private String makeSAM(String queryName, String refName, int referenceLength, float bitScore, float expect, int rawScore, float percentIdentity, int frame, int queryStart, int queryEnd, int referenceStart, int referenceEnd, String alignedQuery, String alignedReference) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(queryName).append("\t");
        buffer.append(0);
        buffer.append("\t");
        buffer.append(refName).append("\t");
        buffer.append(referenceStart).append("\t");
        buffer.append("255\t");
        Utilities.appendCigar(alignedQuery, alignedReference, buffer);

        buffer.append("\t");
        buffer.append("*\t");
        buffer.append("0\t");
        buffer.append("0\t");
        buffer.append(alignedQuery.replaceAll("-", "")).append("\t");
        buffer.append("*\t");

        buffer.append(String.format("AS:i:%d\t", (int) Math.round(bitScore)));
        buffer.append(String.format("NM:i:%d\t", Utilities.computeEditDistance(alignedQuery, alignedReference)));
        buffer.append(String.format("ZL:i:%d\t", referenceLength));
        buffer.append(String.format("ZR:i:%d\t", rawScore));
        buffer.append(String.format("ZE:f:%g\t", expect));
        buffer.append(String.format("ZI:i:%d\t", (int) Math.round(percentIdentity)));
        if (frame != 0)
            buffer.append(String.format("ZF:i:%d\t", frame));
        buffer.append(String.format("ZS:i:%s\t", queryStart));

        Utilities.appendMDString(alignedQuery, alignedReference, buffer);

        return buffer.toString();
    }
}
