package dev.sunbirdrc.views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ViewTemplateTest {

    private ViewTemplate vt;

    @BeforeEach
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

    @Test
    public void testGetExpressionException() {
        assertThrows(IllegalArgumentException.class, () -> {
            vt.getFunctionDefinition("invalid_name");
        });
    }
}