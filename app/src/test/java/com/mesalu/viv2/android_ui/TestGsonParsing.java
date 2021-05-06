package com.mesalu.viv2.android_ui;

import com.google.gson.Gson;
import com.mesalu.viv2.android_ui.data.model.EnvDataSample;
import com.mesalu.viv2.android_ui.data.model.Pet;
import com.mesalu.viv2.android_ui.data.model.PreliminaryPetInfo;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestGsonParsing {
    @Test
    public void sampleParsing() {
        String sample = "{\n" +
                "        \"environment\": \"e722514f-b9d9-38ae-acea-0ab06f4cf72c\",\n" +
                "        \"occupant\": 1,\n" +
                "        \"captured\": \"2021-04-30T23:50:43.70056Z\",\n" +
                "        \"hotGlass\": 22.114465713500977,\n" +
                "        \"hotMat\": 0,\n" +
                "        \"midGlass\": 19.5256404876709,\n" +
                "        \"coldGlass\": 0,\n" +
                "        \"coldMat\": 0\n" +
                "    }";

        Gson gson = new Gson();
        EnvDataSample parsed = gson.fromJson(sample, EnvDataSample.class);

        assertNotNull(parsed);
        assertNotNull(parsed.getEnvId());
        assertNotNull(parsed.getCaptureTime());

        assertEquals(0, parsed.getEnvId().compareTo("e722514f-b9d9-38ae-acea-0ab06f4cf72c"));
    }

    @Test
    public void petParsing() {
        String pet = "{\n" +
                "    \"id\": 1,\n" +
                "    \"name\": \"Kaine\",\n" +
                "    \"morph\": \"Butter Spider\",\n" +
                "    \"hatchDate\": null,\n" +
                "    \"species\": {\n" +
                "        \"id\": 2,\n" +
                "        \"name\": \"Ball Python\",\n" +
                "        \"scientificName\": \"Python Regius\"\n" +
                "    },\n" +
                "    \"careTakerId\": \"e810a95a-cbeb-4b0d-803f-1d7fe9bf58fa\"\n" +
                "}";

        Gson gson = new Gson();
        Pet parsed = gson.fromJson(pet, Pet.class);

        assertNotNull(parsed);
        assertNotNull(parsed.getName());
        assertNotNull(parsed.getMorph());
        assertNotNull(parsed.getSpecies());

        assertEquals(0, parsed.getName().compareTo("Kaine"));
        assertEquals(0, parsed.getSpecies().getName().compareTo("Ball Python"));
        assertEquals(0, parsed.getMorph().compareTo("Butter Spider"));
        assertEquals(1, parsed.getId());
    }

    @Test
    public void prelimPetParsing() {
        String payload = "{\n" +
                "    \"pet\": {\n" +
                "        \"id\": 1,\n" +
                "        \"name\": \"Kaine\",\n" +
                "        \"morph\": \"Butter Spider\",\n" +
                "        \"hatchDate\": null,\n" +
                "        \"species\": {\n" +
                "            \"id\": 2,\n" +
                "            \"name\": \"Ball Python\",\n" +
                "            \"scientificName\": \"Python Regius\"\n" +
                "        },\n" +
                "        \"careTakerId\": \"e810a95a-cbeb-4b0d-803f-1d7fe9bf58fa\"\n" +
                "    },\n" +
                "    \"latestSample\": {\n" +
                "        \"environment\": \"e722514f-b9d9-38ae-acea-0ab06f4cf72c\",\n" +
                "        \"occupant\": 1,\n" +
                "        \"captured\": \"2021-04-30T23:50:43.70056Z\",\n" +
                "        \"hotGlass\": 22.114465713500977,\n" +
                "        \"hotMat\": 0,\n" +
                "        \"midGlass\": 19.5256404876709,\n" +
                "        \"coldGlass\": 0,\n" +
                "        \"coldMat\": 0\n" +
                "    }\n" +
                "}";

        Gson gson = new Gson();
        PreliminaryPetInfo parsed = gson.fromJson(payload, PreliminaryPetInfo.class);

        assertNotNull(parsed);
        assertNotNull(parsed.getPet());
        assertNotNull(parsed.getSample());
    }

    @Test
    public void sanity() {
        double centigrade = 10.0;
        double fahrenheit = ((centigrade * 1.8) + 32);

        assertEquals(50.0, fahrenheit, 0.001);
    }
}
