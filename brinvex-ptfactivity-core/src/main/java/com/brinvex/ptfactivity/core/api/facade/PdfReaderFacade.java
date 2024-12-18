package com.brinvex.ptfactivity.core.api.facade;

import java.util.List;

public interface PdfReaderFacade {

    List<String> readPdfLines(byte[] pdfContent);

}
