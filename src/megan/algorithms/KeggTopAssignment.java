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
package megan.algorithms;

import jloda.util.Pair;
import megan.classification.IdMapper;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.util.*;

/**
 * computes the top KEGG assignments for a read
 * Daniel Huson, 5.2012
 */
public class KeggTopAssignment {
    /**
     * computes the top KEGG assignments for a read
     *
     * @param readBlock
     * @return top assignments
     */
    public static String compute(BitSet activeMatches, IReadBlock readBlock, int ranksToReport) {

        if (activeMatches.cardinality() == 0)
            return "";

        int totalKEGGMatches = 0;
        Map<Integer, Integer> ko2count = new HashMap<>();
        for (int i = activeMatches.nextSetBit(0); i != -1; i = activeMatches.nextSetBit(i + 1)) {
            final IMatchBlock matchBlock = readBlock.getMatchBlock(i);
            int keggId = matchBlock.getId("KEGG");
            if (keggId > 0) {
                Integer count = ko2count.get(keggId);
                ko2count.put(keggId, count == null ? 1 : count + 1);
                totalKEGGMatches++;
            }
        }

        if (ko2count.size() == 0)
            return "";
        else if (ko2count.size() == 1) {
            Integer keggId = ko2count.keySet().iterator().next();
            return String.format(" [1] K%05d: 100 # %d", keggId, ko2count.get(keggId));
        } else {
            SortedSet<Pair<Integer, Integer>> sorted = new TreeSet<>(new Comparator<Pair<Integer, Integer>>() {
                public int compare(Pair<Integer, Integer> idAndCount1, Pair<Integer, Integer> idAndCount2) {
                    if (idAndCount1.get2() > idAndCount2.get2())
                        return -1;
                    else if (idAndCount1.get2() < idAndCount2.get2())
                        return 1;
                    else
                        return idAndCount1.get1().compareTo(idAndCount2.get1());
                }
            });

            for (Map.Entry<Integer, Integer> entry : ko2count.entrySet()) {
                sorted.add(new Pair<>(entry.getKey(), entry.getValue()));
            }
            int top = Math.min(sorted.size(), ranksToReport);
            if (top == 0)
                return "";
            else {
                int countItems = 0;
                StringBuilder buf = new StringBuilder();
                for (Pair<Integer, Integer> idAndCount : sorted) {
                    countItems++;
                    buf.append(String.format(" [%d] K%05d: %.1f", countItems, idAndCount.getFirst(), (100.0 * idAndCount.get2()) / totalKEGGMatches));
                    if (countItems >= top)
                        break;
                }
                buf.append(" # ").append(totalKEGGMatches);
                return buf.toString();
            }
        }
    }

    /**
     * compute the class id for a read from its matches
     * matches
     *
     * @param minScore
     * @param readBlock
     * @return id or 0
     */
    public static int computeId(String cName, float minScore, float maxExpected, float minPercentIdentity, IReadBlock readBlock) {
        if (readBlock.getNumberOfMatches() == 0)
            return IdMapper.NOHITS_ID;

        for (int i = 0; i < readBlock.getNumberOfAvailableMatchBlocks(); i++) {
            IMatchBlock match = readBlock.getMatchBlock(i);
            if (match.getBitScore() >= minScore && match.getExpected() <= maxExpected && match.getPercentIdentity() >= minPercentIdentity) {
                int id = match.getId(cName);
                if (id != 0)
                    return id;
            }
        }
        return IdMapper.UNASSIGNED_ID;
    }

}

