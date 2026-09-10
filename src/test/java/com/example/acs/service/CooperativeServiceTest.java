package com.example.acs.service;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class CooperativeServiceTest {

    @Test
    public void testHello() {
        CooperativeService service = new CooperativeService();
        String result = service.hello("World");
        assertEquals("Hello, World!", result);
    }
}
