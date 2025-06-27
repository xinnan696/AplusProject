package com.ucd.urbanflow.model;

import java.util.List;

public class MapRealtimeData {
    private List<Edge> edges;
    private List<Junction> junctions;

    public List<Edge> getEdges() { return edges; }
    public void setEdges(List<Edge> edges) { this.edges = edges; }

    public List<Junction> getJunctions() { return junctions; }
    public void setJunctions(List<Junction> junctions) { this.junctions = junctions; }
}
