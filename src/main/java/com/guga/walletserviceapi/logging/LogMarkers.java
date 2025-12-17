package com.guga.walletserviceapi.logging;

import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public final class LogMarkers {

    private LogMarkers() { }

    public static final Marker LOG   = MarkerManager.getMarker("LOG");
    public static final Marker TRACE = MarkerManager.getMarker("TRACE");
    public static final Marker AUDIT = MarkerManager.getMarker("AUDIT");

}
