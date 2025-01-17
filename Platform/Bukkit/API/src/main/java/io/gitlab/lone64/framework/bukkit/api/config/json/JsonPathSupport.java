package io.gitlab.lone64.framework.bukkit.api.config.json;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@Getter
public class JsonPathSupport {

    private final Plugin plugin;
    private final String fileName;
    private final File file;

    public JsonPathSupport(Plugin plugin, String fileName, boolean isCreateNewFile) {
        this.plugin = plugin;
        this.fileName = fileName;
        this.file = new File(plugin.getDataFolder(), fileName);
        if (isCreateNewFile && !createNewFile()) plugin.getLogger().severe("An error occurred while creating the file.");
    }

    public boolean createNewFile() {
        try {
            if (this.plugin.getResource(this.fileName) != null) {
                this.plugin.saveResource(this.fileName, false);
                return true;
            }
            return !file.exists() && file.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteFile() {
        return file.exists() && file.delete();
    }

    public boolean exists() {
        return file.exists();
    }

    public JsonElement get(String path) {
        try (FileReader reader = new FileReader(this.file)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            return object.get(path);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean has(String path) {
        return get(path) != null;
    }

}