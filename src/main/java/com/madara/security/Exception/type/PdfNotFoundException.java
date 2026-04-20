package com.madara.security.Exception.type;

public class PdfNotFoundException extends RuntimeException{
    public PdfNotFoundException() {
        super("Pdf not found");
    }
}
