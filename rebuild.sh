cd respatialized
clj -R:cambada -m cambada.jar --app-version=SNAPSHOT
clj -A:install target/respatialized-SNAPSHOT.jar
e38d4
cd ../
clj -m build

