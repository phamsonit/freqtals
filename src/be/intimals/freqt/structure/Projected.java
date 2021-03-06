package be.intimals.freqt.structure;

import java.util.*;

public class Projected {
    private int depth = -1;
    private int support = -1;
    private int rootSupport = -1;
    private List<Location> locations = new  ArrayList<>();

    //////////////////////////////////////////////////////////

    public void setProjectedDepth(int d){
        this.depth = d;
    }

    public int getProjectedDepth() {
        return this.depth;
    }

    public void setProjectedSupport(int s) {
        this.support = s;
    }

    public int getProjectedSupport(){
        return this.support;
    }

    public void setProjectedRootSupport(int s) {
        this.rootSupport = s;
    }

    public int getProjectedRootSupport(){
        return this.rootSupport;
    }

    public void setProjectLocation(int i, int j) {
        Location l = new Location();
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }

    public Location getProjectLocation(int i){
        return this.locations.get(i);
    }

    public void deleteProjectLocation(int i){
        this.locations.remove(i);
    }

    public int getProjectLocationSize(){
        return this.locations.size();
    }

    public void addProjectLocation(int id, int pos, Location occurrences) {
        //check if this location doesn't exist in the locations
        Location l = new Location(occurrences,id,pos);
        boolean found = false;
        for(Location location: this.locations){
            if(l.getLocationId() == location.getLocationId()
                    && l.getRoot() == location.getRoot()
                    && l.getLocationPos() == location.getLocationPos()  )
                found = true;
        }
        if(!found)
            this.locations.add(l);
    }

    //new procedure for 2-class data
    public void addProjectLocation(int classID, int id, int pos, Location occurrences) {
        //check if this location doesn't exist in the locations
        Location l = new Location(occurrences,classID,id,pos);
        boolean found = false;
        for(Location location: this.locations){
            if(l.getLocationId() == location.getLocationId()
                    && l.getRoot() == location.getRoot()
                    && l.getLocationPos() == location.getLocationPos()  )
                found = true;
        }
        if(!found)
            this.locations.add(l);
    }

    public void setProjectLocation(int classID, int i, int j) {
        Location l = new Location();
        l.setLocationId(classID);
        l.setLocationId(i);
        l.addLocationPos(j);
        this.locations.add(l);
    }
}
