package org.pureglx.engine;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
public class test {

    @Test
    void test(){


        System.out.println(new Date().getTime());
        System.out.println("test");
    }
}
