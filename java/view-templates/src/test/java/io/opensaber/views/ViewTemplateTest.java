package io.opensaber.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class ViewTemplateTest {

    private ViewTemplate vt;

    @Before
    public void init() {
        vt = new ViewTemplate();
        FunctionDefinition fd = new FunctionDefinition();
        fd.setName("name");
        fd.setResult("expression");
        List<FunctionDefinition> fds = new ArrayList<>();
        fds.add(fd);
        vt.setFunctionDefinitions(fds);
    }

    @Test
    public void testGetExpression() {

        FunctionDefinition fd = vt.getFunctionDefinition("name");

        assertEquals(vt.getFunctionDefinitions().get(0).getResult(), fd.getResult());
        assertNotEquals("unexpected", fd.getResult());

    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetExpressionException() {

        vt.getFunctionDefinition("invalid_name");

    }

}
