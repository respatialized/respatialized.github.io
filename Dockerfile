FROM ubuntu:bionic

RUN apt-get update && apt-get install -y ca-certificates && apt-get update

RUN export DEBIAN_FRONTEND=noninteractive
RUN ln -fs /usr/share/zoneinfo/America/New_York /etc/localtime
RUN apt-get install -y tzdata
RUN dpkg-reconfigure --frontend noninteractive tzdata
RUN apt-get install -y wget sqlite3 tidy texlive-xetex make rsync libpango-1.0 libpangocairo-1.0

ENV RACKET_VERSION 7.4
ENV RACKET_INSTALLER_URL http://mirror.racket-lang.org/installers/$RACKET_VERSION/racket-$RACKET_VERSION-x86_64-linux.sh

RUN wget --output-document=racket-install.sh $RACKET_INSTALLER_URL && \
  echo "yes\n1\n" | /bin/bash racket-install.sh && \
  rm racket-install.sh

RUN raco pkg install --batch --deps search-auto pollen hyphenate xexpr-path
