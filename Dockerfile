FROM clojure:openjdk-14-tools-deps-alpine

RUN apk add ttf-dejavu
RUN mkdir build
WORKDIR build


COPY respatialized ./respatialized
COPY deps.edn ./deps.edn
COPY content ./content
COPY public ./public
COPY build ./build

RUN (cd respatialized && clojure -R:cambada -m cambada.jar --app-version=SNAPSHOT && clojure -A:install target/respatialized-SNAPSHOT.jar)

RUN clojure -m build
