package resources;

import java.io.InputStream;

public class Resources {

    /**
     * 根据文件路径，将配置文件加载为stream流
     * @param path
     * @return
     */
    public static InputStream getResourceAsStream(String path) {
        return Resources.class.getClassLoader().getResourceAsStream(path);
    }
}
