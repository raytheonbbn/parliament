# Docker Parliament triple store

# This is the latest LTS release of Ubuntu as of 11/2026:
FROM ubuntu:resolute

# See https://github.com/phusion/baseimage-docker:
#FROM phusion/baseimage:resolute

ARG distro_ver
ARG distro_arch

USER root

RUN <<EOF
apt-get -qq -y update
apt-get install --fix-missing -y --allow-downgrades openjdk-25-jre-headless
apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

adduser --system --group --uid 501 tenant

mkdir -p /var/parliament-data
chmod -R u=rwx,go=rx /var/parliament-data
chown -R tenant:tenant /var/parliament-data

mkdir -p /opt/parliament-$distro_ver-$distro_arch
EOF

WORKDIR /opt/parliament-$distro_ver-$distro_arch
COPY ./parliament-*-ubuntu*-*64 .
RUN <<EOF
#rm -r clientJars
#rm -r javadoc
#rm ParliamentUserGuide.pdf
sed -Ei 's|^(kbDirectoryPath *= *)kb-data$|\1/var/parliament-data|' ParliamentKbConfig.txt
sed -Ei 's|^(SPRING_BOOT_HOST=localhost)|#\1|' parliament
sed -Ei 's|^#(SPRING_BOOT_HOST=0.0.0.0)|\1|' parliament
EOF

USER tenant
EXPOSE 8089
VOLUME /var/parliament-data
WORKDIR /opt/parliament-$distro_ver-$distro_arch
ENTRYPOINT ["./parliament", "--container"]
