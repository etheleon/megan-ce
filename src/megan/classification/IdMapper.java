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
package megan.classification;

import jloda.util.Basic;
import jloda.util.CanceledException;
import jloda.util.ProgramProperties;
import jloda.util.ProgressListener;
import megan.classification.data.ClassificationFullTree;
import megan.classification.data.LoadableLong2IntegerMap;
import megan.classification.data.LoadableString2IntegerMap;
import megan.classification.data.Name2IdMap;
import megan.main.MeganProperties;

import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

/**
 * tracks mapping files for a named type of classification
 *
 * Daniel Huson, 4.2015
 */
public class IdMapper {
    static public final int NOHITS_ID = -1;
    static public final String NOHITS_LABEL = "No hits";
    static public final int UNASSIGNED_ID = -2;
    static public final String UNASSIGNED_LABEL = "Not assigned";
    static public final int LOW_COMPLEXITY_ID = -3;
    static public final String LOW_COMPLEXITY_LABEL = "Low complexity";
    static public final int UNCLASSIFIED_ID = -4;
    static public final String UNCLASSIFIED_LABEL = "Unclassified";

    public enum MapType {GI, Accession, Synonyms}

    private final String cName;

    protected final EnumMap<MapType, String> map2Filename = new EnumMap<>(MapType.class);

    protected final EnumSet<MapType> loadedMaps = EnumSet.noneOf(MapType.class);

    protected final EnumSet<MapType> activeMaps = EnumSet.noneOf(MapType.class);

    protected final ClassificationFullTree fullTree;
    protected final String[] idTags;
    protected final Name2IdMap name2IdMap;

    protected boolean useTextParsing;

    private final Set<Integer> disabledIds = new HashSet<>();

    protected LoadableLong2IntegerMap giMap = null;
    protected Accession2IdMap accessionMap = null;
    protected LoadableString2IntegerMap synonymsMap = null;

    protected IdParser.Algorithm algorithm;

    /**
     * constructor
     *
     * @param idTags
     * @param name2IdMap
     */
    public IdMapper(String name, ClassificationFullTree fullTree, String[] idTags, Name2IdMap name2IdMap) {
        this.cName = name;
        this.fullTree = fullTree;
        this.idTags = idTags;
        this.name2IdMap = name2IdMap;

        algorithm = (ProgramProperties.get(cName + "UseLCAToParse", name.equals(Classification.Taxonomy)) ? IdParser.Algorithm.LCA : IdParser.Algorithm.First_Hit);

        if (name.equals(Classification.Taxonomy)) { // todo: generalize this so that it applies to all classifications
            for (int i : ProgramProperties.get(MeganProperties.DISABLED_TAXA, new int[0])) {
                disabledIds.add(i);
            }
        }
    }

    /**
     * load the named file of the given map type
     *
     * @param fileName
     * @param mapType
     * @param reload
     * @param progress
     * @throws CanceledException
     */
    public void loadMappingFile(String fileName, MapType mapType, boolean reload, ProgressListener progress) throws CanceledException {
        switch (mapType) {
            default:
            case GI: {
                if (giMap == null || reload) {
                    if (giMap != null) {
                        try {
                            giMap.close();
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                    final LoadableLong2IntegerMap giMap = new LoadableLong2IntegerMap();
                    try {
                        giMap.loadFile(name2IdMap, fileName, progress);
                        this.giMap = giMap;
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        if (e instanceof CanceledException)
                            throw (CanceledException) e;
                        Basic.caught(e);
                    }
                }
                break;
            }
            case Accession: {
                if (accessionMap == null || reload) {
                    if (accessionMap != null) {
                        try {
                            accessionMap.close();
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                    try {
                        this.accessionMap = new Accession2IdMap(fileName, progress);
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        if (e instanceof CanceledException)
                            throw (CanceledException) e;
                        Basic.caught(e);
                    }
                }
                break;
            }
            case Synonyms: {
                if (synonymsMap == null || reload) {
                    if (synonymsMap != null) {
                        try {
                            synonymsMap.close();
                        } catch (IOException e) {
                            Basic.caught(e);
                        }
                    }
                    final LoadableString2IntegerMap synonymsMap = new LoadableString2IntegerMap();
                    try {
                        synonymsMap.loadFile(name2IdMap, fileName, false, progress);
                        this.synonymsMap = synonymsMap;
                        loadedMaps.add(mapType);
                        activeMaps.add(mapType);
                        map2Filename.put(mapType, fileName);
                    } catch (Exception e) {
                        if (e instanceof CanceledException)
                            throw (CanceledException) e;
                        Basic.caught(e);
                    }
                }
            }
        }
    }

    /**
     * is the named parsing method loaded
     *
     * @param mapType
     * @return true, if loaded
     */
    public boolean isLoaded(MapType mapType) {
        return loadedMaps.contains(mapType);
    }

    public boolean isActiveMap(MapType mapType) {
        return activeMaps.contains(mapType);
    }

    public void setActiveMap(MapType mapType, boolean state) {
        if (state)
            activeMaps.add(mapType);
        else
            activeMaps.remove(mapType);
    }

    public void setUseTextParsing(boolean useTextParsing) {
        this.useTextParsing = useTextParsing;
    }

    public boolean isUseTextParsing() {
        return useTextParsing;
    }

    /**
     * creates a new id parser for this mapper
     * @return
     */
    public IdParser createIdParser() {
        final IdParser idParser = new IdParser(this);
        idParser.setAlgorithm(algorithm);
        return idParser;
    }

    /**
     * get a  id from a giNumber
     *
     * @param giNumber
     * @return KO id or null
     */
    public Integer getIdFromGI(long giNumber) throws IOException {
        if (isLoaded(MapType.GI)) {
            return getGiMap().get(giNumber);
        }
        return null;
    }

    public String getMappingFile(MapType mapType) {
        return map2Filename.get(mapType);
    }

    public LoadableLong2IntegerMap getGiMap() {
        return giMap;
    }

    public Accession2IdMap getAccessionMap() {
        return accessionMap;
    }

    public LoadableString2IntegerMap getSynonymsMap() {
        return synonymsMap;
    }

    public boolean hasActiveAndLoaded() {
        return activeMaps.size() > 0 && loadedMaps.size() > 0;
    }

    public String getCName() {
        return cName;
    }

    public String[] getIdTags() {
        return idTags;
    }

    public Name2IdMap getName2IdMap() {
        return name2IdMap;
    }

    public Set<Integer> getDisabledIds() {
        return disabledIds;
    }

    public boolean isDisabled(int id) {
        return disabledIds.size() > 0 && disabledIds.contains(id);
    }
}
