package at.wpichler.documents2ocr;

import java.io.File;

class OCRException extends Exception {
    File file;

    OCRException(String msg, File file) {
        super(msg);
        this.file = file;
    }
}