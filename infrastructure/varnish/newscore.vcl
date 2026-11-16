# Caching rules for NewsCore (see docs/adr/ADR-008-varnish-caching.md).
# Loaded by default.vcl; tested by tests/*.vtc.

import std;

# Who may invalidate: the api-gateway on the internal network. Apache does not forward PURGE
# (see infrastructure/apache/newscore.conf), so requests from outside never get here — the ACL
# alone would not stop them, because Varnish sees Apache's address, which is inside these ranges.
acl purgers {
    "localhost";
    "127.0.0.1";
    "172.16.0.0"/12;  # default docker bridge range
    "10.0.0.0"/8;
}

sub vcl_recv {
    # Invalidation, triggered by the gateway on article events.
    if (req.method == "PURGE") {
        if (!client.ip ~ purgers) {
            return (synth(405, "Purge not allowed"));
        }
        # The path ends up in a regular expression, so only plain path characters are accepted.
        if (req.url !~ "^/[A-Za-z0-9/_-]*$") {
            return (synth(400, "Unsupported purge path"));
        }
        # A ban on the stored path instead of `return (purge)`: purge only removes the object under
        # the caller's own Host header, and the gateway reaches Varnish as "varnish" while pages are
        # cached under the public host name. The ban also covers the query-string variants of the
        # path (e.g. /?category=politik for /).
        ban("obj.http.x-url ~ ^" + req.url + "(\?.*)?$");
        return (synth(200, "Banned"));
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
    # Remember the path on the object so bans can match on it (and the ban lurker can evict).
    set beresp.http.x-url = bereq.url;

    # Never store origin errors. During a background refresh keep serving the stale copy instead.
    if (beresp.status >= 500) {
        if (bereq.is_bgfetch) {
            return (abandon);
        }
        set beresp.uncacheable = true;
        set beresp.ttl = 5s;
        return (deliver);
    }

    # "Not found" is cached only briefly: the article may be published a moment later.
    if (beresp.status == 404) {
        set beresp.ttl = 30s;
        set beresp.grace = 0s;
        return (deliver);
    }

    # TTLs per content type.
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
    # Expose cache hit/miss for observability.
    if (obj.hits > 0) {
        set resp.http.X-Cache = "HIT";
        set resp.http.X-Cache-Hits = obj.hits;
    } else {
        set resp.http.X-Cache = "MISS";
    }
    unset resp.http.x-url;
    unset resp.http.Via;
    return (deliver);
}
