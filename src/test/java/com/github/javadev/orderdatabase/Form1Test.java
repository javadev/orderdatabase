/*
 * $Id$
 *
 * Copyright 2016 Valentyn Kolesnikov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.javadev.orderdatabase;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 * .
 * @author Valentyn Kolesnikov
 * @version $Revision$ $Date$
 */
public class Form1Test {

    private Form1 form1;

    @Before
    public void setUp() throws Exception {
        form1 = new Form1();
        Field field = Form1.class.getDeclaredField("useMySql");
        field.setAccessible(true);
        field.set(form1, true);
    }

    @Test
    public void calcOrderNumberEmptyFields() throws Exception {
        Method method = form1.getClass().getDeclaredMethod("calcOrderNumber",
                new Class[]{String.class, String.class, String.class, String.class});
        method.setAccessible(true);
        String result = (String) method.invoke(form1, "", "", "", "");
        assertEquals("М-1", result);
    }

    @Test
    public void calcOrderNumberEmptySurname() throws Exception {
        Method method = form1.getClass().getDeclaredMethod("calcOrderNumber",
                new Class[]{String.class, String.class, String.class, String.class});
        method.setAccessible(true);
        String result = (String) method.invoke(form1, "", "Иван", "Петрович", "");
        assertEquals("ИП-М-1", result);
    }
    
    @Test
    public void calcOrderNumberEmptyFirstName() throws Exception {
        Method method = form1.getClass().getDeclaredMethod("calcOrderNumber",
                new Class[]{String.class, String.class, String.class, String.class});
        method.setAccessible(true);
        String result = (String) method.invoke(form1, "Сидоров", "", "Петрович", "");
        assertEquals("СП-М-1", result);
    }
    
    @Test
    public void calcOrderNumberEmptyMiddleName() throws Exception {
        Method method = form1.getClass().getDeclaredMethod("calcOrderNumber",
                new Class[]{String.class, String.class, String.class, String.class});
        method.setAccessible(true);
        String result = (String) method.invoke(form1, "Сидоров", "Иван", "", "");
        assertEquals("СИ-М-1", result);
    }

    @Test
    public void calcOrderNumberWithAllFields() throws Exception {
        Method method = form1.getClass().getDeclaredMethod("calcOrderNumber",
                new Class[]{String.class, String.class, String.class, String.class});
        method.setAccessible(true);
        String result = (String) method.invoke(form1, "Сидоров", "Иван", "Петрович", "Тверь");
        assertEquals("СИП-Т-1", result);
    }
}
