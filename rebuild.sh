cd respatialized
clojure -R:cambada -m cambada.jar --app-version=SNAPSHOT
clojure -A:install target/respatialized-SNAPSHOT.jar
cd ../
clojure -m build

