vcl 4.1;

import std;

# The Nuxt SSR frontend is the cache backend (Apache terminates TLS in front of Varnish).
backend default {
    .host = "frontend";
    .port = "3000";
}

# Who may send PURGE requests (the api-gateway on the internal network).
acl purgers {
    "localhost";
    "127.0.0.1";
    "172.16.0.0"/12;  # default docker bridge range
    "10.0.0.0"/8;
}

sub vcl_recv {
    # Targeted cache invalidation (triggered by the gateway on article.* events).
    if (req.method == "PURGE") {
        if (!client.ip ~ purgers) {
            return (synth(405, "Purge not allowed"));
        }
        return (purge);
    }

    # Only cache safe methods.
    if (req.method != "GET" && req.method != "HEAD") {
        return (pass);
    }

    # Don't cache authenticated requests.
    if (req.http.Authorization || req.http.Cookie ~ "session=") {
        return (pass);
    }

    # Strip cookies for cacheable content so they don't defeat the cache.
    unset req.http.Cookie;
    return (hash);
}

sub vcl_backend_response {
    # TTLs per content type (Akzeptanzkriterien / Epic 3).
    if (bereq.url ~ "^/_nuxt/" || bereq.url ~ "^/images/" || bereq.url ~ "\.(css|js|png|jpg|svg|woff2?)$") {
        set beresp.ttl = 24h;                 # static assets
        set beresp.http.Cache-Control = "public, max-age=86400";
    } elsif (bereq.url ~ "^/article/") {
        set beresp.ttl = 5m;                  # article pages
    } elsif (bereq.url == "/" || bereq.url ~ "^/\?") {
        set beresp.ttl = 1m;                  # homepage
    } else {
        set beresp.ttl = 30s;                 # everything else (search etc.)
    }

    # Allow serving slightly stale content while revalidating.
    set beresp.grace = 1h;
    return (deliver);
}

sub vcl_deliver {
    # Expose cache hit/miss for observability and the >85% hit-rate target.
    if (obj.hits > 0) {
        set resp.http.X-Cache = "HIT";
        set resp.http.X-Cache-Hits = obj.hits;
    } else {
        set resp.http.X-Cache = "MISS";
    }
    unset resp.http.Via;
    return (deliver);
}
