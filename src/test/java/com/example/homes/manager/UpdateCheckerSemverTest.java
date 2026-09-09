package com.example.homes.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UpdateCheckerSemverTest {

    @Test
    void parsesTagNameFromGitHubLatestRelease() {
        String json = "{\"tag_name\":\"v1.16.0\",\"html_url\":\"https://github.com/paper0319/homes-plugin/releases/tag/v1.16.0\","
                + "\"draft\":false,\"prerelease\":false}";
        assertEquals("1.16.0", UpdateChecker.parseLatestVersionNumber(json));
    }

    @Test
    void parseStripsLeadingVFromTag() {
        assertEquals("2.0.7", UpdateChecker.stripTagPrefix("v2.0.7"));
        assertEquals("2.0.7", UpdateChecker.stripTagPrefix("V2.0.7"));
        assertEquals("2.0.7", UpdateChecker.stripTagPrefix("2.0.7"));
    }

    @Test
    void parseToleratesWhitespaceAndExtraFields() {
        String json = "{ \"id\": 1, \"tag_name\" : \"v2.0.1\", \"draft\": false }";
        assertEquals("2.0.1", UpdateChecker.parseLatestVersionNumber(json));
    }

    @Test
    void parseReturnsNullForDraftOrPrerelease() {
        assertNull(UpdateChecker.parseLatestVersionNumber(
                "{\"tag_name\":\"v2.0.8\",\"draft\":true,\"prerelease\":false}"));
        assertNull(UpdateChecker.parseLatestVersionNumber(
                "{\"tag_name\":\"v2.0.8\",\"draft\":false,\"prerelease\":true}"));
    }

    @Test
    void parseReturnsNullWhenTagNameMissing() {
        assertNull(UpdateChecker.parseLatestVersionNumber("{\"name\":\"no-tag\"}"));
    }

    @Test
    void parseReturnsNullForArrayJson() {
        assertNull(UpdateChecker.parseLatestVersionNumber("[{\"tag_name\":\"v1.0.0\"}]"));
    }

    @Test
    void parseReturnsNullForMalformedJson() {
        assertNull(UpdateChecker.parseLatestVersionNumber("not json at all"));
    }

    @Test
    void equalVersions() {
        assertEquals(0, UpdateChecker.compareSemver("1.13.1", "1.13.1"));
    }

    @Test
    void patchDifference() {
        assertTrue(UpdateChecker.compareSemver("1.13.1", "1.13.2") < 0);
        assertTrue(UpdateChecker.compareSemver("1.13.2", "1.13.1") > 0);
    }

    @Test
    void minorAndMajorDifference() {
        assertTrue(UpdateChecker.compareSemver("1.13.9", "1.14.0") < 0);
        assertTrue(UpdateChecker.compareSemver("1.99.0", "2.0.0") < 0);
    }

    @Test
    void missingTrailingSegmentsAreZero() {
        assertEquals(0, UpdateChecker.compareSemver("1.13", "1.13.0"));
        assertTrue(UpdateChecker.compareSemver("1.13", "1.13.1") < 0);
    }

    @Test
    void suffixIsStripped() {
        assertEquals(0, UpdateChecker.compareSemver("1.13.1-SNAPSHOT", "1.13.1"));
        assertTrue(UpdateChecker.compareSemver("1.13.1-RC1", "1.13.2") < 0);
    }

    @Test
    void nonNumericSegmentThrows() {
        assertThrows(NumberFormatException.class,
                () -> UpdateChecker.compareSemver("1.13.x", "1.13.1"));
    }
}
