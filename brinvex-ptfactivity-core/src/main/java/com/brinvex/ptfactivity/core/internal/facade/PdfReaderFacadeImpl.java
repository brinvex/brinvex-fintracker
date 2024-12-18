package com.brinvex.ptfactivity.core.internal.facade;

import com.brinvex.ptfactivity.core.api.facade.PdfReaderFacade;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;

public class PdfReaderFacadeImpl implements PdfReaderFacade {

    @Override
    public List<String> readPdfLines(byte[] pdfContent) {
        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfContent))) {
            if (document.isEncrypted()) {
                throw new IllegalArgumentException("Cannot read encrypted pdf");
            }

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);

            PDFTextStripper tStripper = new PDFTextStripper();

            String text = tStripper.getText(document);

            return Arrays.asList(text.split("\\r?\\n"));

        } catch (InvalidPasswordException e) {
            throw new IllegalArgumentException("Cannot read encrypted pdf", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
