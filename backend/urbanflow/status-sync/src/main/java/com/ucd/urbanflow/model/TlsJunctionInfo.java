package com.ucd.urbanflow.model;

public class TlsJunctionInfo {
    private String tlsId;
    private String junctionId;
    private String junctionName;
    private double junctionX;
    private double junctionY;
    private String junctionShape;

    // Getters and setters
    public String getTlsId() {
        return tlsId;
    }

    public void setTlsId(String tlsId) {
        this.tlsId = tlsId;
    }

    public String getJunctionId() {
        return junctionId;
    }

    public void setJunctionId(String junctionId) {
        this.junctionId = junctionId;
    }

    public String getJunctionName() {
        return junctionName;
    }

    public void setJunctionName(String junctionName) {
        this.junctionName = junctionName;
    }

    public double getJunctionX() {
        return junctionX;
    }

    public void setJunctionX(double junctionX) {
        this.junctionX = junctionX;
    }

    public double getJunctionY() {
        return junctionY;
    }

    public void setJunctionY(double junctionY) {
        this.junctionY = junctionY;
    }

    public String getJunctionShape() {
        return junctionShape;
    }

    public void setJunctionShape(String junctionShape) {
        this.junctionShape = junctionShape;
    }
}
