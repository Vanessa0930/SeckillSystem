package main.java.com.seckillservice.utils.redis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class LuaScriptUtil {
    /**
     * Read the file as string variable
     * @param filePath the file path
     * @return a String represented the script content
     */
    public static String readScript(String filePath) {
        StringBuilder res = new StringBuilder();
        InputStream inputStream = LuaScriptUtil.class.getClassLoader().getResourceAsStream(filePath);
        try (InputStreamReader is = new InputStreamReader(Objects.requireNonNull(inputStream));
             BufferedReader reader = new BufferedReader(is)){
            String line;
            while ((line = reader.readLine()) != null) {
                res.append(line).append(System.lineSeparator());
            }
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
        return res.toString();
    }

    public static String getRateLimiterScriptSource() {
        return "limit.lua";
    }
}
