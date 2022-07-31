package com.orbital.scribex;

import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TranscriptionUnitTest {
    private static String RESULT = "This is a sparse test for the OCR model to check the correctness of our transcription backend. The expected result should be exactly the same as the text typed out here, and we should be able to copy the text to clipboard or delete it. ";


    @Test
    public void Request_To_Backend_Working() {
        //uses a dev account to check the backend is live
        assertTrue(RequesterUtils.sendRequest("t68SzYWj9vZHTErjxcFZ2fQxHGH3"));
    }


    @Test
    public void Check_Document_Result_Correct() throws IOException {
        String url = "https://firebasestorage.googleapis.com/v0/b/scribex-1653106340524.appspot.com/o/transcribed%2Ft68SzYWj9vZHTErjxcFZ2fQxHGH3%2Ftyped%20test.txt?alt=media&token=de82c8e0-b6a3-4913-8c7b-0ac20b41624f";
        InputStream in = new URL(url).openStream();
        Path path = Paths.get("test.txt");
        Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        File file = path.toFile();
        assertEquals(RequesterUtils.readFile(file), RESULT);
    }
}