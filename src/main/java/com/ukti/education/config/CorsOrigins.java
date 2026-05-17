package com.ukti.education.config;

import java.util.List;

/**
 * Single list of allowed browser origins for CORS (frontend domains).
 * Update here when the marketing / app domain changes (e.g. meetukti.com).
 */
public final class CorsOrigins {

    private CorsOrigins() {}

    public static List<String> allowedOrigins() {
        return List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:5172",
                "https://miraista.com",
                "http://miraista.com",
                "https://www.miraista.com",
                "http://www.miraista.com",
                "https://education.miraista.com",
                "http://education.miraista.com",
                "https://educationuat.miraista.com",
                "http://educationuat.miraista.com",
                "https://ukti.example.com",
                "https://meetukti.com",
                "http://meetukti.com",
                "https://www.meetukti.com",
                "http://www.meetukti.com",
                "https://uat.meetukti.com",
                "http://uat.meetukti.com",
                "https://www.uat.meetukti.com",
                "http://www.uat.meetukti.com",
                "https://ags.meetukti.com",
                "http://ags.meetukti.com",
                "https://www.ags.meetukti.com",
                "http://www.ags.meetukti.com");
    }
}
