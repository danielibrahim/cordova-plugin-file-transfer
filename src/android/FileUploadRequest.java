package org.apache.cordova.filetransfer;

import android.net.Uri;
import android.util.Log;
import org.apache.cordova.CordovaResourceApi;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sidia on 6/15/2015.
 */
public class FileUploadRequest {

    private static final String LOG_TAG = "FileTransfer";
    private static final String LINE_START = "--";
    private static final String LINE_END = "\r\n";
    private static final String BOUNDARY =  "+++++";

    private static final String LIMITER_CONTINUE = LINE_END + LINE_START + BOUNDARY + LINE_END;
    private static final String LIMITER_END = LINE_END + LINE_START + BOUNDARY + LINE_START + LINE_END;

    private JSONObject params;
    private List<FileTransferring> files;
    private CordovaResourceApi resourceApi;

    public FileUploadRequest(JSONObject params, JSONArray files, CordovaResourceApi resourceApi) throws IOException, JSONException {
        this.params = params;
        this.resourceApi = resourceApi;
        this.files = loadFiles(files);
    }

    public String getParamsDataString() {
        StringBuilder beforeDataHeaders = new StringBuilder();
        try {
            for (Iterator<?> iter = params.keys(); iter.hasNext();) {
                Object key = iter.next();
                if(!String.valueOf(key).equals("headers"))
                {
                    beforeDataHeaders.append(LINE_START).append(BOUNDARY).append(LINE_END);
                    beforeDataHeaders.append("Content-Disposition: form-data; name=\"").append(key.toString()).append('"');
                    beforeDataHeaders.append(LINE_END).append(LINE_END);
                    beforeDataHeaders.append(params.getString(key.toString()));
                    beforeDataHeaders.append(LINE_END);
                }
            }
            if (files.isEmpty()) {
                beforeDataHeaders.append(LIMITER_END);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return beforeDataHeaders.toString();
    }

    public List<FileTransferring> getFiles() {
        return this.files;
    }

    public long getContentLength() {
        long totalLength = 0;
        try {
            totalLength = getParamsDataString().getBytes("UTF-8").length;
            for (FileTransferring fileTransferring : files) {
                totalLength += fileTransferring.getDataLength();
            }
        } catch (UnsupportedEncodingException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return totalLength;
    }

    private List<FileTransferring> loadFiles(JSONArray files) throws IOException, JSONException {

        List<FileTransferring> filesTransferring = new ArrayList<FileTransferring>();

        for (int i = 0; i < files.length(); i++) {
            JSONObject file = files.getJSONObject(i);

            String fileKey = file.getString("key");
            String fileName = file.getString("name");
            String mimeType = file.optString("mimeType", "image/jpg");

            StringBuilder beforeDataHeaderBuilder = new StringBuilder();

            beforeDataHeaderBuilder.append(LINE_START).append(BOUNDARY).append(LINE_END);
            beforeDataHeaderBuilder.append("Content-Disposition: form-data; name=\"").append(fileKey).append("\";");
            beforeDataHeaderBuilder.append(" filename=\"").append(fileName).append('"').append(LINE_END);
            beforeDataHeaderBuilder.append("Content-Type: ").append(mimeType).append(LINE_END).append(LINE_END);

            String beforeDataHeader = beforeDataHeaderBuilder.toString();
            String afterDataHeader;

            if (i == files.length() - 1) {
                afterDataHeader = LIMITER_END;
            } else {
                afterDataHeader = LIMITER_CONTINUE;
            }

            Log.d(LOG_TAG, beforeDataHeader);
            Log.d(LOG_TAG, afterDataHeader);

            // Accept a path or a URI for the source.
            Uri tmpSrc = Uri.parse(fileName);
            final Uri sourceUri = resourceApi.remapUri(
                    tmpSrc.getScheme() != null ? tmpSrc : Uri.fromFile(new File(fileName)));

            // Get a input stream of the file on the phone
            CordovaResourceApi.OpenForReadResult readResult = resourceApi.openForRead(sourceUri);

            FileTransferring fileTransferring = new FileTransferring();
            fileTransferring.beforeDataHeader = beforeDataHeader;
            fileTransferring.afterDataHeader = afterDataHeader;
            fileTransferring.dataInputStream = readResult.inputStream;
            fileTransferring.dataLength = beforeDataHeader.getBytes("UTF-8").length +
                    afterDataHeader.getBytes("UTF-8").length + readResult.length;

            filesTransferring.add(fileTransferring);
        }

        return filesTransferring;
    }

    public class FileTransferring {
        private String beforeDataHeader;
        private String afterDataHeader;
        private InputStream dataInputStream;
        private long dataLength;

        public String getBeforeDataHeader() {
            return beforeDataHeader;
        }

        public String getAfterDataHeader() {
            return afterDataHeader;
        }

        public InputStream getDataInputStream() {
            return dataInputStream;
        }

        public long getDataLength() {
            return dataLength;
        }
    }

}
