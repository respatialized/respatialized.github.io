cd respatialized
clj -R:cambada -m cambada.jar --app-version=SNAPSHOT
clj -A:install target/respatialized-SNAPSHOT.jar
cd ../
clj -m build

