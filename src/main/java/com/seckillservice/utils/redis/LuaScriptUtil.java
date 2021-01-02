package main.java.com.seckillservice.utils.redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LuaScriptUtil {
    /**
     * Read the file as string variable
     * @param filePath the file path
     * @return a String represented the script content
     */
    public static String readScript(String filePath) {
        StringBuilder res = new StringBuilder();
        InputStream inputStream = LuaScriptUtil.class.getClassLoader().getResourceAsStream(filePath);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))){
            String str;
            while ((str = reader.readLine()) != null) {
                res.append(str).append(System.lineSeparator());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res.toString();
    }

    public static String getRateLimiterScriptSource() {
        return "limit.lua";
    }
}
