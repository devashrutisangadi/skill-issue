package com.example.customwordle;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class WordLoader {
    public static Map<String, List<String>> loadCategories(Context context) {
        try {
            InputStream is = context.getAssets().open("words.json");
            InputStreamReader reader = new InputStreamReader(is);
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, List<String>>>() {}.getType();
            Map<String, List<String>> categories = gson.fromJson(reader, type);
            reader.close();
            return categories;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}