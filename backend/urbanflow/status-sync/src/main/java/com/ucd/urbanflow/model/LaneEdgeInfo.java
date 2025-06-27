package com.ucd.urbanflow.model;

public class LaneEdgeInfo {
    private String laneId;
    private String laneShape;
    private String edgeId;
    private String edgeName;

    // Getter 和 Setter
    public String getLaneId() {
        return laneId;
    }

    public void setLaneId(String laneId) {
        this.laneId = laneId;
    }

    public String getLaneShape() {
        return laneShape;
    }

    public void setLaneShape(String laneShape) {
        this.laneShape = laneShape;
    }

    public String getEdgeId() {
        return edgeId;
    }

    public void setEdgeId(String edgeId) {
        this.edgeId = edgeId;
    }

    public String getEdgeName() {
        return edgeName;
    }

    public void setEdgeName(String edgeName) {
        this.edgeName = edgeName;
    }
}
