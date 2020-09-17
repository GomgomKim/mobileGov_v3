package kr.go.mobile.agent.utils;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceUtils {
    public static String loadResourceRaw(Context c, int id) throws Exception {
        StringBuilder sb = new StringBuilder();
        InputStream in = null;
        BufferedInputStream bis = null;
        BufferedReader br = null;
        try {
            in = c.getResources().openRawResource(id);
            bis = new BufferedInputStream(in);
            br = new BufferedReader(new InputStreamReader(bis));
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
                sb.append(strLine);
            }
        } finally {
            try {
                if (in != null)
                    in.close();
                if (br != null)
                    br.close();
                if (bis != null)
                    bis.close();
            } catch (IOException ignored) {
            }

        }
        return sb.toString();
    }
}
