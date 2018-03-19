package com.pykj.download;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(5, 2 + 2);

        List<Integer> array = new ArrayList<>();

        for (int i = 0; i < 90; i++) {
            array.add(i);
        }

        System.out.println("xixi");
        for (int i = 0; i < 90; i++) {
//            System.out.println(array.get(i));
        }
    }
}