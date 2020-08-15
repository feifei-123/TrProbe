package com.sogou.tm.commonlib.log.service.log;


import java.io.FilterWriter;
import java.io.Writer;


public class InnerWriter extends FilterWriter {


    public InnerWriter(Writer writer) {
        super(writer);
    }

    public void write(String string) {
        if (string != null) {
            try {
                out.write(string);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void flush() {
        try {
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

