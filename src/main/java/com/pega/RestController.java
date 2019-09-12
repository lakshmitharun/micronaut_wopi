package com.pega;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pega.entities.CheckFileInfo;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.server.types.files.SystemFile;

import java.io.*;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Controller
public class RestController {

    @Get("wopi/files/{name}/contents")
    public SystemFile getFile(@PathVariable String name) {
        SystemFile systemFile = null;
        try {
            File file = File.createTempFile("test", "docx");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] decodeBase64 = Base64.getDecoder().decode(getFileDataFromPRPC(name.substring(0, name.length() - 4)));
            fos.write(decodeBase64);
            fos.close();
            systemFile = new SystemFile(file, MediaType.APPLICATION_OCTET_STREAM_TYPE).attach("test.docx");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return systemFile;
    }

    @Get("wopi/files/{name}")
    public String getFileInfo(@PathVariable String name) {
        String result = null;
        try {
            File file = File.createTempFile("test", "docx");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] decodeBase64 = Base64.getDecoder().decode(getFileDataFromPRPC(name.substring(0, name.length() - 4)));
            fos.write(decodeBase64);
            fos.close();

            CheckFileInfo info = new CheckFileInfo();
            if (name.substring(name.length() - 4).equalsIgnoreCase("view")) {
                info.setUserCanWrite(false);
            }
            try {
                if (file.exists()) {
                    info.setBaseFileName(file.getName());
                    info.setSize(file.length());
                    info.setOwnerId("admin");
                    info.setVersion(2);
                    info.setSha256(getHash256(file));
                }
                ObjectMapper mapper = new ObjectMapper();
                result = mapper.writeValueAsString(info);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getFileDataFromPRPC(String fileID) throws Exception {
        final String uri = "https://lab5367.lab.pega.com/prweb/PRRestService/wopi/1/FileData/" + fileID;
        HttpClient client = HttpClient.create(new URL(uri));
        return client.toBlocking().retrieve(HttpRequest.GET(uri));
    }

    private String getHash256(File file) throws IOException, NoSuchAlgorithmException {
        String value;
        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int numRead;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    digest.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            value = new String(Base64.getEncoder().encode(digest.digest()));
        }
        return value;
    }
}
