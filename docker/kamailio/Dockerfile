FROM debian:bullseye

RUN apt-get update && apt-get install -y \
    kamailio \
    kamailio-mysql-modules \
    kamailio-extra-modules \
    kamailio-tls-modules \
    && rm -rf /var/lib/apt/lists/*

COPY kamailio.cfg /etc/kamailio/kamailio.cfg
COPY dispatcher.list /etc/kamailio/dispatcher.list

CMD ["kamailio", "-DD", "-E", "-f", "/etc/kamailio/kamailio.cfg"]