package com.platypus.pangolin;
import org.junit.Test;

import static org.junit.Assert.*;

import com.platypus.pangolin.utils.MGRSTools;

import mil.nga.mgrs.MGRS;

public class MGRSToolsTest {

    @Test
    public void fromStringToMGRS(){
        String MGRScoord = "33TTM94";
        String res = "33TTM9000040000";

        MGRS temp = MGRSTools.fromStringToMGRS(MGRScoord);
        assertEquals(res, temp.toString());
    }

}
