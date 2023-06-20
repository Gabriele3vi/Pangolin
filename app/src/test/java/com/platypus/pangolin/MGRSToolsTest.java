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

        MGRS temp = MGRSTools.fromStringToMGRS(MGRScoord, 1);
        assertEquals(res, temp.toString());
    }

    @Test
    public void fromStringToMGRSWithDifferentAccuracy(){
        String MGRScoord = "33TTM9830045000";
        MGRS temp = MGRSTools.fromStringToMGRS(MGRScoord);
        assertEquals(MGRScoord, temp.toString());
    }

    @Test
    //totest 10SDG0417742682
    public void testCastMGRScoord1(){
        String MGRScoord = "10SDG0417742682";
        String castedCoord = "33TTM94";
        String temp = MGRSTools.castMGRSCoord(MGRScoord,1);
        assertEquals(castedCoord, temp);
    }

    @Test
    public void testCastMGRScoord2(){
        String MGRScoord = "33TTM9830045000";
        String castedCoord = "33TTM9845";
        String temp = MGRSTools.castMGRSCoord(MGRScoord,2);
        assertEquals(castedCoord, temp);
    }

    @Test
    public void testCastMGRScoord3(){
        String MGRScoord = "33TTM9830045000";
        String castedCoord = "33TTM983450";
        String temp = MGRSTools.castMGRSCoord(MGRScoord,3);
        assertEquals(castedCoord, temp);
    }

    @Test
    public void testCastMGRScoord4(){
        String MGRScoord = "33TTM9830045000";
        String castedCoord = "33TTM98304500";
        String temp = MGRSTools.castMGRSCoord(MGRScoord,4);
        assertEquals(castedCoord, temp);
    }

}
