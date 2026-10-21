package io.pyhoma.domain.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SafetyFiltersTest {

    @Test
    void documentPolicyBlocksExecutablesAndOddSchemes() {
        assertEquals(DocumentPolicy.Kind.WEB, DocumentPolicy.classify("https://intranet.example.com/guia.pdf"));
        assertEquals(DocumentPolicy.Kind.MAIL, DocumentPolicy.classify("mailto:suporte@example.com"));
        assertEquals(DocumentPolicy.Kind.FILE, DocumentPolicy.classify("\\\\servidor\\manuais\\vpn.pdf"));
        assertEquals(DocumentPolicy.Kind.FILE, DocumentPolicy.classify("D:\\Contratos\\locacao.docx"));
        for (String bad : List.of("C:\\Temp\\setup.exe", "\\\\srv\\share\\run.BAT", "javascript:alert(1)", "file:///C:/x.exe",
                "C:\\docs\\atalho.lnk", "C:\\docs\\trojan.exe.", "C:\\docs\\a.pdf:hidden.exe", "https://user:pw@example.com/",
                "ms-settings:privacy", "C:\\docs\\script.ps1")) {
            assertThrows(BusinessException.class, () -> DocumentPolicy.classify(bad), bad);
        }
    }

    @Test
    void csvCellsCannotBecomeFormulas() {
        String out = Csv.write(List.of("a", "b", "c", "d"), List.of(List.of("=HYPERLINK(\"x\")", "-5", "@SUM(A1)", "+cmd")), ';');
        assertTrue(out.contains("'=HYPERLINK"));
        assertTrue(out.contains(";-5;"));
        assertTrue(out.contains("'@SUM"));
        assertTrue(out.contains("'+cmd"));
    }
}
