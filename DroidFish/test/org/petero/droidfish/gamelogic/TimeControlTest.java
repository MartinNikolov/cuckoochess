package org.petero.droidfish.gamelogic;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class TimeControlTest {
    public TimeControlTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Test
    public void testElapsedTime() {
    	TimeControl tc = new TimeControl();
    	long totTime = 5 * 60 * 1000;
    	long t0 = 1000;
    	tc.setTimeControl(totTime, 0, 0);
    	tc.setCurrentMove(1, true);
    	assertEquals(0, tc.getMovesToTC());
    	assertEquals(0, tc.getIncrement());
    	assertEquals(totTime, tc.getRemainingTime(true, 0));
    	tc.startTimer(t0);
    	tc.moveMade(t0 + 1000);

    	tc.setCurrentMove(2, true);
    	assertEquals(0, tc.getMovesToTC());
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));
    	
    	tc.setCurrentMove(1, false);
    	assertEquals(0, tc.getMovesToTC());
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));

    	tc.startTimer(t0 + 3000);
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 5000));
    	assertEquals(totTime - 2000, tc.getRemainingTime(false, t0 + 5000));
    	tc.stopTimer(t0 + 8000);
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime - 5000, tc.getRemainingTime(false, t0 + 4711));
    	tc.moveMade(t0 + 8000);
    	tc.setCurrentMove(2, true);
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime - 5000, tc.getRemainingTime(false, t0 + 4711));

        // Test undo/redo
    	tc.setCurrentMove(1, false);
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));

    	tc.setCurrentMove(1, true);
    	assertEquals(totTime, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime, tc.getRemainingTime(false, t0 + 4711));
    	
    	tc.setCurrentMove(2, true);
    	assertEquals(totTime - 1000, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(totTime - 5000, tc.getRemainingTime(false, t0 + 4711));
    }

    /** Test getMovesToTC */
    @Test
    public void testTimeControl() {
    	TimeControl tc = new TimeControl();
    	tc.setTimeControl(2 * 60 * 1000, 40, 0);
    	tc.setCurrentMove(1, true);
    	assertEquals(40, tc.getMovesToTC());
    	tc.setCurrentMove(1, false);
    	assertEquals(40, tc.getMovesToTC());

    	tc.setCurrentMove(2, true);
    	assertEquals(39, tc.getMovesToTC());
    	
    	tc.setCurrentMove(40, true);
    	assertEquals(1, tc.getMovesToTC());
    	
    	tc.setCurrentMove(41, true);
    	assertEquals(40, tc.getMovesToTC());
    	
    	tc.setCurrentMove(80, true);
    	assertEquals(1, tc.getMovesToTC());

    	tc.setCurrentMove(81, true);
    	assertEquals(40, tc.getMovesToTC());
    }

    @Test
    public void testExtraTime() {
    	TimeControl tc = new TimeControl();
    	long timeCont = 60 * 1000;
    	long inc = 700;
    	tc.setTimeControl(timeCont, 5, inc);
    	tc.setCurrentMove(5, true);
    	long t0 = 1342134;
    	assertEquals(timeCont, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(timeCont, tc.getRemainingTime(false, t0 + 4711));
    	
    	tc.startTimer(t0 + 1000);
    	tc.moveMade(t0 + 2000);
    	tc.setCurrentMove(5, false);
    	assertEquals(timeCont - 1000 + timeCont + inc, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(timeCont, tc.getRemainingTime(false, t0 + 4711));

    	tc.startTimer(t0 + 2000);
    	tc.moveMade(t0 + 6000);
    	tc.setCurrentMove(6, true);
    	assertEquals(timeCont - 1000 + timeCont + inc, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(timeCont - 4000 + timeCont + inc, tc.getRemainingTime(false, t0 + 4711));
    	
    	tc.startTimer(t0 + 6000);
    	tc.moveMade(t0 + 9000);
    	tc.setCurrentMove(6, false);
    	assertEquals(timeCont - 1000 + timeCont + inc - 3000 + inc, tc.getRemainingTime(true, t0 + 4711));
    	assertEquals(timeCont - 4000 + timeCont + inc, tc.getRemainingTime(false, t0 + 4711));
    }
}
