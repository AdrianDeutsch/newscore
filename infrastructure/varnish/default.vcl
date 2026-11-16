vcl 4.1;

# Entry point loaded by varnishd. It only names the origin; the caching rules live in
# newscore.vcl so the varnishtest cases in tests/ can load them against a mock backend.

# The Nuxt SSR frontend is the cache backend (Apache sits in front of Varnish).
backend default {
    .host = "frontend";
    .port = "3000";
}

include "newscore.vcl";
