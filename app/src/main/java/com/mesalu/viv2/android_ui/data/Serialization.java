package com.mesalu.viv2.android_ui.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public final class Serialization {
    /**
     * Builds and returns a Gson instance suitable for general use in this application.
     * @return a Gson instance.
     */
    public static Gson getGson() {
        return new GsonBuilder()
                // Parsing for ZonedDateTime, OffsetDateTime (trivial implementations)
                .registerTypeAdapter(OffsetDateTime.class, new TypeAdapter<OffsetDateTime>() {
                    @Override
                    public void write(JsonWriter out, OffsetDateTime value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public OffsetDateTime read(JsonReader in) throws IOException {
                        return OffsetDateTime.parse(in.nextString());
                    }
                })
                .registerTypeAdapter(ZonedDateTime.class, new TypeAdapter<ZonedDateTime>() {
                    @Override
                    public void write(JsonWriter out, ZonedDateTime value) throws IOException {
                        out.value(value.toString());
                    }

                    @Override
                    public ZonedDateTime read(JsonReader in) throws IOException {
                        return ZonedDateTime.parse(in.nextString());
                    }
                })
                .create();
    }
}
