package multiparser.format;

import multiparser.goextractor.ConstantString;
import multiparser.util.Configure;
import multiparser.util.Tuple;
import multiparser.extractor.FinalRelation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapObject {
    FinalRelation getRelation = new FinalRelation();
    private ArrayList<String> files;
    private Map<Integer, Map<Integer, Map<String, Integer>>> finalRes = new HashMap<Integer, Map<Integer, Map<String, Integer>>>();
    private String[] depStrs;

    public MapObject(String[] depStrs) {
        this.depStrs = depStrs;
        init();
    }


    private void init() {
        getRelation = new FinalRelation();
        files =  getRelation.outputFiles();
        buildDepMap(files, depStrs);
    }


    public ArrayList<String> getFiles() {
        return files;
    }

    public void setFiles(ArrayList<String> files) {
        this.files = files;
    }


    public String[] getDepStrs() {
        return depStrs;
    }

    public void setDepStrs(String[] depStrs) {
        this.depStrs = depStrs;
    }

    public Map<Integer, Map<Integer, Map<String, Integer>>> getFinalRes() {
        return finalRes;
    }

    public void setFinalRes(Map<Integer, Map<Integer, Map<String, Integer>>> finalRes) {
        this.finalRes = finalRes;
    }

    /**
     * build map from fileName to new id
     * store into fileName2Id.
     * @param files
     */
    private Map<String, Integer> buildFileMap(ArrayList<String> files) {
        Map<String, Integer> fileName2Id = new HashMap<String, Integer>();
        int index = 0;
        for (String fileName : files) {
            fileName2Id.put(fileName, index);
            index ++;
        }
        return fileName2Id;
    }

    /**
     * build fileDeps into a map.
     * @param files
     * @param depStrs  strs=[Import,Implement,...]
     */
    private void buildDepMap(ArrayList<String> files, String[] depStrs) {
        System.out.println(getRelation.basicStatis());

        Map<String, Integer> fileName2Id =  buildFileMap(files);
        for (int i =  0; i < depStrs.length; i++) {
            String depType = depStrs[i];
            ArrayList<Tuple<String, String>> deps = getRelation.getDepByType(Configure.RELATION_LEVEL_FILE, depType);
            addDepsInMap(deps, depType, fileName2Id);
        }

    }

    /**
     *
     * @param deps
     * @param depType
     * @param fileName2Id
     */
    private void addDepsInMap(ArrayList<Tuple<String, String>> deps, String depType, Map<String, Integer> fileName2Id) {
        for(Tuple<String, String> dep : deps) {
            String name1 = dep.x;
            String name2 = dep.y;
            int index1 = -1;
            int index2 = -1;
            if(fileName2Id.containsKey(name1)) {
                index1 = fileName2Id.get(name1);

            }
            if (fileName2Id.containsKey(name2)) {
                index2 = fileName2Id.get(name2);

            }

            if(name1.equals(name2) || index1 == -1 || index2 == -1) {
                continue;
            }
            if(!finalRes.containsKey(index1)) {
                finalRes.put(index1, new HashMap<Integer, Map<String, Integer>>());
            }
            if(!finalRes.get(index1).containsKey(index2)) {
                finalRes.get(index1).put(index2, new HashMap<String, Integer>());
            }
            if(!finalRes.get(index1).get(index2).containsKey(depType)) {
                finalRes.get(index1).get(index2).put(depType, 0);
            }

            int newWeight = finalRes.get(index1).get(index2).get(depType) + 1;
            finalRes.get(index1).get(index2).put(depType, newWeight);

        }
    }
}
