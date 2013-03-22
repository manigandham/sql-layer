
package com.akiban.server.entity.changes;

import com.akiban.junit.NamedParameterizedRunner;
import com.akiban.junit.Parameterization;
import com.akiban.server.entity.model.Space;
import com.akiban.server.entity.model.diff.JsonDiffPreview;
import com.akiban.util.JUnitUtils;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.UUID;

import static com.akiban.util.JsonUtils.readTree;
import static org.junit.Assert.assertEquals;

@RunWith(NamedParameterizedRunner.class)
public final class SpaceDiffTest {
    private static final String ORIG_SUFFIX = "-orig.json";
    private static final String UPDATE_SUFFIX = "-update.json";
    private static final String EXPECTED_SUFFIX = "-expected.json";
    private static final String UUIDS_SUFFIX = "-uuids.properties";

    private static String getShortName(String testName) {
        return testName.substring(0, testName.length() - ORIG_SUFFIX.length());
    }

    @NamedParameterizedRunner.TestParameters
    public static Collection<Parameterization> params() throws IOException {
        String[] testNames = JUnitUtils.getContainingFile(SpaceDiffTest.class).list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(ORIG_SUFFIX) &&
                        new File(dir, getShortName(name) + UPDATE_SUFFIX).exists();
            }
        });
        return Collections2.transform(Arrays.asList(testNames), new Function<String, Parameterization>() {
            @Override
            public Parameterization apply(String testName) {
                String shortName = getShortName(testName);
                return new Parameterization(shortName, true, shortName);
            }
        });
    }

    @Test
    public void test() throws IOException {
        Function<String, UUID> uuidGenerator;
        File uuidsFile = new File(dir, testName + UUIDS_SUFFIX);
        if (uuidsFile.exists()) {
            final Properties uuids = new Properties();
            uuids.load(new FileReader(uuidsFile));
            uuidGenerator = new Function<String, UUID>() {
                @Override
                public UUID apply(String input) {
                    String uuid = uuids.getProperty(input);
                    return UUID.fromString(uuid);
                }
            };
        }
        else {
            uuidGenerator = null;
        }
        Space orig = Space.readSpace(testName + ORIG_SUFFIX, SpaceDiffTest.class, null);
        Space updated = Space.readSpace(testName + UPDATE_SUFFIX, SpaceDiffTest.class, uuidGenerator);
        JsonNode expected = readTree(new File(dir, testName + EXPECTED_SUFFIX));
        StringWriter writer = new StringWriter();
        JsonDiffPreview diff = new JsonDiffPreview(writer);
        new SpaceDiff(orig, updated).apply(diff);
        diff.finish();
        JsonNode actual = readTree(new StringReader(writer.toString()));
        assertEquals("changes", expected, actual);
    }

    public SpaceDiffTest(String testName) {
        this.testName = testName;
    }

    private final String testName;
    private static final File dir = JUnitUtils.getContainingFile(SpaceDiffTest.class);
}
