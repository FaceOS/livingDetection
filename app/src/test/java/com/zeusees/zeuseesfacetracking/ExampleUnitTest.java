package com.zeusees.zeuseesfacetracking;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        //随机动作
        Random random = new Random();
        List<Integer> indexArray = new ArrayList<>();
        while (indexArray.size() != 3) {
            int index = random.nextInt(3);
            if (!indexArray.contains(index))
                indexArray.add(index);
        }
        for (Integer integer : indexArray) {
            System.out.println(integer);
        }

    }
}