package com.ucd.urbanflow.service;

import com.ucd.urbanflow.mapper.TlsJunctionMapper;
import com.ucd.urbanflow.model.TlsJunctionInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TlsJunctionService {

    @Autowired
    private TlsJunctionMapper tlsJunctionMapper;

    public List<TlsJunctionInfo> getAllTlsJunctions() {
        return tlsJunctionMapper.getAllTlsJunctions();
    }
}
