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
package megan.dialogs.extractor;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgressListener;
import megan.classification.Classification;
import megan.classification.ClassificationManager;
import megan.core.ClassificationType;
import megan.core.Document;
import megan.data.IClassificationBlock;
import megan.data.IConnector;
import megan.data.IReadBlock;
import megan.data.IReadBlockIterator;
import megan.viewer.TaxonomyData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * extract reads using the IConnector
 * Daniel Huson, 4.2010
 */
public class ReadsExtractor {
    /**
     * extracts all reads for the given classes
     *
     * @param progressListener
     * @param classificationName
     * @param classIds
     * @param classId2Name
     * @param classId2Descendants
     * @param outDirectory
     * @param outFileName
     * @param doc
     * @param summarized
     * @throws IOException
     * @throws CanceledException
     */
    private static int extractReads(final ProgressListener progressListener, final String classificationName, final Collection<Integer> classIds, final Map<Integer, String> classId2Name,
                                    Map<Integer, Collection<Integer>> classId2Descendants,
                                    final String outDirectory, final String outFileName, final Document doc, final boolean summarized) throws IOException, CanceledException {
        progressListener.setSubtask("Extracting by " + classificationName);

        final IConnector connector = doc.getConnector();
        int numberOfReads = 0;

        final IClassificationBlock classificationBlock = connector.getClassificationBlock(classificationName);

        if (classificationBlock == null)
            return 0;

        BufferedWriter w = null;
        try {
            final boolean useMultipleFileNames = outFileName.contains("%t");

            for (Integer classId : classIds) {
                Set<Integer> all = new HashSet<>();
                all.add(classId);
                if (summarized && classId2Descendants.get(classId) != null)
                    all.addAll(classId2Descendants.get(classId));

                final String outFileFinalName;
                if (useMultipleFileNames)
                    outFileFinalName = outFileName.replaceAll("%t", Basic.toCleanName(classId2Name.get(classId)));
                else
                    outFileFinalName = outFileName;
                File outFile = new File(outDirectory, outFileFinalName);

                for (Integer id : all) {
                    if (classificationBlock.getSum(id) > 0) {
                        try (IReadBlockIterator it = connector.getReadsIterator(classificationName, id, 0, 10000, true, false)) {
                            progressListener.setMaximum(it.getMaximumProgress());
                            progressListener.setProgress(0);
                            while (it.hasNext()) {
                                if (w == null)
                                    w = new BufferedWriter(new FileWriter(outFile));
                                IReadBlock readBlock = it.next();
                                String readHeader = readBlock.getReadHeader();
                                if (!readHeader.startsWith(">"))
                                    w.write(">");
                                w.write(readHeader);
                                if (!readHeader.endsWith("\n"))
                                    w.write("\n");
                                String readData = readBlock.getReadSequence();
                                if (readData != null) {
                                    w.write(readData);
                                    if (!readData.endsWith("\n"))
                                        w.write("\n");
                                }
                                numberOfReads++;
                                progressListener.setProgress(it.getProgress());
                            }
                        }
                    }
                }
                if (useMultipleFileNames && w != null) {
                    w.close();
                    w = null;
                }
            }
        } catch (CanceledException ex) {
            System.err.println("USER CANCELED");
        } finally {
            if (w != null)
                w.close();
        }
        return numberOfReads;
    }

    /**
     * extract all reads belonging to a given set of taxon ids
     *
     * @param progressListener
     * @param taxIds
     * @param outDirectory
     * @param outFileName
     * @param doc
     * @param summarized
     * @throws IOException
     * @throws CanceledException
     */
    public static int extractReadsByTaxonomy(final ProgressListener progressListener, final Set<Integer> taxIds,
                                             final String outDirectory, final String outFileName, final Document doc, final boolean summarized) throws IOException, CanceledException {
        Map<Integer, String> classId2Name = new HashMap<>();
        Map<Integer, Collection<Integer>> classId2Descendants = new HashMap<>();
        for (Integer id : taxIds) {
            classId2Name.put(id, TaxonomyData.getName2IdMap().get(id));
            if (summarized)
                classId2Descendants.put(id, TaxonomyData.getTree().getAllDescendants(id));
        }
        return extractReads(progressListener, ClassificationType.Taxonomy.toString(), taxIds, classId2Name, classId2Descendants, outDirectory, outFileName, doc, summarized);
    }

    /**
     * extract all reads belonging to a given set of  ids
     *
     * @param progressListener
     * @param classIds
     * @param outDirectory
     * @param outFileName
     * @param doc
     * @throws IOException
     * @throws CanceledException
     */
    public static int extractReadsByFViewer(final String cName, final ProgressListener progressListener, final Collection<Integer> classIds,
                                            final String outDirectory, final String outFileName, final Document doc) throws IOException, CanceledException {

        final Classification classification = ClassificationManager.get(cName, true);
        Map<Integer, String> classId2Name = new HashMap<>();
        Map<Integer, Collection<Integer>> classId2Descendants = new HashMap<>();
        for (Integer id : classIds) {
            classId2Name.put(id, classification.getName2IdMap().get(id));
            classId2Descendants.put(id, classification.getFullTree().getAllDescendants(id));
        }
        return extractReads(progressListener, cName, classIds, classId2Name, classId2Descendants, outDirectory, outFileName, doc, true);
    }
}
