ARG BASE_IMAGE=tobiasehlert/teslamateapi@sha256:6f7eea55c6475c2bc4639ee582dfec01d5cf618d30b0c7444020274a4786704e
FROM ${BASE_IMAGE}
COPY --chmod=555 teslamateapi-meridian /opt/app/app
COPY LICENSE /licenses/TeslaMateAPI.txt

ARG EXTENSION_SHA=unknown
LABEL org.opencontainers.image.source="https://github.com/Gott-mit-Uns/Meridian" \
      org.opencontainers.image.version="1.25.0-meridian.1" \
      meridian.extension.sha256="${EXTENSION_SHA}"
