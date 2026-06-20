# syntax=docker/dockerfile:1
FROM httpd:2.4

# Enable the modules the reverse proxy needs (commented out in the stock httpd.conf).
RUN sed -i \
    -e 's|^#\(LoadModule proxy_module \)|\1|' \
    -e 's|^#\(LoadModule proxy_http_module \)|\1|' \
    -e 's|^#\(LoadModule headers_module \)|\1|' \
    -e 's|^#\(LoadModule deflate_module \)|\1|' \
    -e 's|^#\(LoadModule ratelimit_module \)|\1|' \
    conf/httpd.conf \
    && echo "Include conf/extra/newscore.conf" >> conf/httpd.conf

COPY newscore.conf conf/extra/newscore.conf

# Fail the build early if the configuration is invalid.
RUN httpd -t
