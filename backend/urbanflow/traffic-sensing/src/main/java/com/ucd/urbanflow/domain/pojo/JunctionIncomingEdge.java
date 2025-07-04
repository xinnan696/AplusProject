package com.ucd.urbanflow.domain.pojo;

import lombok.Data;

@Data
public class JunctionIncomingEdge {
    private String junctionId;
    private String junctionName;
    private String incomingEdgeId;
    private String incomingEdgeName;
}